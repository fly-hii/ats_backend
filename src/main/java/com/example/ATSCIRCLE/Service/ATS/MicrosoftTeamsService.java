package com.example.ATSCIRCLE.Service.ATS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MicrosoftTeamsService {

    @Value("${microsoft.client.id}")
    private String clientId;

    @Value("${microsoft.client.secret}")
    private String clientSecret;

    @Value("${microsoft.tenant.id}")
    private String tenantId;

    private static final String GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0";

    // ISO-8601 with offset — required by Graph API onlineMeetings
    // e.g. "2025-07-01T10:00:00+05:30"
    private static final DateTimeFormatter GRAPH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

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

    /**
     * Creates a Microsoft Teams Online Meeting and returns the join URL.
     *
     * Graph API expects startDateTime / endDateTime as flat ISO-8601 strings
     * with timezone offset — NOT as nested {dateTime, timeZone} objects.
     *
     * Correct format: "2025-07-01T10:00:00+05:30"
     */
    public String createTeamsMeeting(String accessToken,
                                 String title,
                                 String startDateTime,
                                 String endDateTime,
                                 String timeZone,
                                 String attendeeEmail) {
    try {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        ZoneId zoneId = ZoneId.of(timeZone);

        // /me/events కి dateTime nested object గా కావాలి — flat string కాదు
        String startIso = parseDateTime(startDateTime)
                .atZone(zoneId)
                .format(GRAPH_FORMATTER);

        String endIso = parseDateTime(endDateTime)
                .atZone(zoneId)
                .format(GRAPH_FORMATTER);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", title);

        // ✅ /me/events కి nested object format
        body.put("start", Map.of(
            "dateTime", startIso,
            "timeZone", timeZone
        ));
        body.put("end", Map.of(
            "dateTime", endIso,
            "timeZone", timeZone
        ));

        // ✅ Teams meeting గా mark చేయడానికి
        body.put("isOnlineMeeting", true);
        body.put("onlineMeetingProvider", "teamsForBusiness");

        // ✅ Attendee format — /me/events కి ఇది correct format
        if (attendeeEmail != null && !attendeeEmail.isBlank()) {
            body.put("attendees", List.of(Map.of(
                "emailAddress", Map.of(
                    "address", attendeeEmail,
                    "name", attendeeEmail
                ),
                "type", "required"
            )));
        }

        String jsonBody = mapper.writeValueAsString(body);
        System.out.println("Calendar event request body: " + jsonBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        // ✅ /me/onlineMeetings కాదు — /me/events వాడాలి
        String url = GRAPH_BASE_URL + "/me/events";
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        JsonNode root = mapper.readTree(response.getBody());

        // ✅ /me/events response లో joinUrl ఇక్కడ ఉంటుంది
        String joinUrl = root
                .path("onlineMeeting")
                .path("joinUrl")
                .asText(null);

        System.out.println("Teams Meeting + Calendar Event created: " + joinUrl);
        return joinUrl;

    } catch (Exception e) {
        System.err.println("Error creating Teams meeting: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}

    /**
     * Refresh access token using refresh token (delegated flow).
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "client_id="     + clientId
                    + "&client_secret="    + clientSecret
                    + "&refresh_token="    + refreshToken
                    + "&grant_type=refresh_token"
                    + "&scope=https://graph.microsoft.com/OnlineMeetings.ReadWrite";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response =
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            return root.path("access_token").asText(null);

        } catch (Exception e) {
            System.err.println("Error refreshing MS token: " + e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }
}