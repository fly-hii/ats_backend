package com.example.ATSCIRCLE.Service.ATS;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleCalendarService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    // Support multiple datetime formats
    private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private LocalDateTime parseDateTime(String dateTime) {
        for (DateTimeFormatter fmt : SUPPORTED_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTime, fmt);
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Cannot parse datetime: " + dateTime);
    }

    public String createGoogleMeetEvent(String accessToken,
                                        String refreshToken,
                                        String title,
                                        String description,
                                        String startDateTime,
                                        String endDateTime,
                                        String timeZone,
                                        String attendeeEmail) {
        try {
            // 1. Build credentials — NO .createScoped() here
            //    Scopes must already be in the refresh token from OAuth login
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .setAccessToken(new AccessToken(accessToken, null))
                    .build();

            // 2. Build Calendar service
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("ATS Circle")
                    .build();

            // 3. Parse times — supports both "yyyy-MM-dd HH:mm:ss" and "yyyy-MM-dd'T'HH:mm"
            LocalDateTime startLdt = parseDateTime(startDateTime);
            LocalDateTime endLdt   = parseDateTime(endDateTime);
            ZoneId zoneId          = ZoneId.of(timeZone);

            EventDateTime eventStart = new EventDateTime()
                    .setDateTime(new DateTime(
                            Date.from(startLdt.atZone(zoneId).toInstant())))
                    .setTimeZone(timeZone);

            EventDateTime eventEnd = new EventDateTime()
                    .setDateTime(new DateTime(
                            Date.from(endLdt.atZone(zoneId).toInstant())))
                    .setTimeZone(timeZone);

            // 4. Build Event
            Event event = new Event()
                    .setSummary(title)
                    .setDescription(description)
                    .setStart(eventStart)
                    .setEnd(eventEnd);

            // 5. Add attendee
            if (attendeeEmail != null && !attendeeEmail.isBlank()) {
                event.setAttendees(List.of(
                        new EventAttendee().setEmail(attendeeEmail)));
            }

            // 6. Add Google Meet conference
            event.setConferenceData(new ConferenceData()
                    .setCreateRequest(new CreateConferenceRequest()
                            .setRequestId(UUID.randomUUID().toString())
                            .setConferenceSolutionKey(
                                    new ConferenceSolutionKey()
                                            .setType("hangoutsMeet"))));

            // 7. Insert into Calendar
            Event created = service.events()
                    .insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            System.out.println("Event created: " + created.getHtmlLink());

            // 8. Return Meet link
            ConferenceData conf = created.getConferenceData();
            if (conf == null || conf.getEntryPoints() == null) {
                System.err.println("No conference data in response.");
                return null;
            }

            return conf.getEntryPoints().stream()
                    .filter(ep -> "video".equals(ep.getEntryPointType()))
                    .findFirst()
                    .map(EntryPoint::getUri)
                    .orElse(null);

        } catch (Exception e) {
            System.err.println("Error creating Google Meet event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean isTokenValid(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }
}