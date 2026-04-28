package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.DTO.ScheduleMeetDTO;
import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Models.ATS.Jobs;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;
import com.example.ATSCIRCLE.Repository.JobRepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.Util.TimeZoneUtil;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InterviewScheduleService {

    @Autowired
    private Candidateapplicationrepository candidateApplicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private MicrosoftTeamsService microsoftTeamsService;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Convert slots to user's timezone
    // ─────────────────────────────────────────────────────────────────────────
    public List<String> getConvertedSlots(String candidateApplicationId,
                                          String userTimeZone) throws Exception {

        CandidateApplication application = candidateApplicationRepository
                .findById(candidateApplicationId)
                .orElseThrow(() -> new Exception("Candidate application not found"));

        String clientTimeZone = application.getClientTimeZone();
        if (clientTimeZone == null || clientTimeZone.isBlank()) {
            throw new Exception(
                "Client timezone not set. " +
                "Client must submit 'Interview Scheduled' status with clientTimeZone first.");
        }

        List<String> originalSlots = application.getTimeslots();
        if (originalSlots == null || originalSlots.isEmpty()) {
            throw new Exception("No timeslots found on this application.");
        }

        if (!TimeZoneUtil.isValidTimeZone(userTimeZone)) {
            throw new Exception("Invalid userTimeZone: " + userTimeZone);
        }

        List<String> convertedSlots = TimeZoneUtil.convertSlotsToTimeZone(
                originalSlots, clientTimeZone, userTimeZone);

        application.setConvertedSlots(convertedSlots);
        application.setUserTimeZone(userTimeZone);
        candidateApplicationRepository.save(application);

        return convertedSlots;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3 — Schedule interview based on defaultEmailProvider
    // ─────────────────────────────────────────────────────────────────────────
    public String scheduleInterviewMeeting(String userId,
                                           ScheduleMeetDTO scheduleMeetDTO) throws Exception {

        // Step 1: Get user
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));

        // Step 2: Check defaultEmailProvider
        Users.EmailProvider provider = user.getDefaultEmailProvider();
        if (provider == null || provider == Users.EmailProvider.NONE) {
            throw new Exception(
                "No default email provider set. " +
                "Please connect Google or Microsoft account and set a default provider.");
        }

        // Step 3: Get candidate application
        CandidateApplication application = candidateApplicationRepository
                .findById(scheduleMeetDTO.getCandidateApplicationId())
                .orElseThrow(() -> new Exception("Candidate application not found"));

        // Step 4: Get job details
        Jobs job = jobRepository.findById(scheduleMeetDTO.getJobId())
                .orElseThrow(() -> new Exception("Job not found"));

        // Step 5: Validate selectedSlot is one of the convertedSlots
        List<String> convertedSlots = application.getConvertedSlots();
        if (convertedSlots == null || convertedSlots.isEmpty()) {
            throw new Exception(
                "No converted slots found. " +
                "Call GET /api/interviews/{appId}/converted-slots?userTimeZone=... first.");
        }
        if (!convertedSlots.contains(scheduleMeetDTO.getSelectedSlot())) {
            throw new Exception(
                "Selected slot '" + scheduleMeetDTO.getSelectedSlot() +
                "' is not in available slots. Available: " + convertedSlots);
        }

        String selectedSlot = scheduleMeetDTO.getSelectedSlot();
        String endTime      = calculateEndTime(selectedSlot);
        String meetLink     = null;

        // Step 6: Create meet link based on provider
        if (provider == Users.EmailProvider.GOOGLE) {
            // ── Google Meet ──
            if (!user.isGoogleConnected() || user.getGoogleAccessToken() == null) {
                throw new Exception(
                    "Google account not connected. Please connect Google account first.");
            }

            String fromEmail = user.getGoogleEmail();
            if (fromEmail == null || fromEmail.isBlank()) {
                throw new Exception(
                    "No Google email found. Please reconnect your Google account.");
            }

            meetLink = googleCalendarService.createGoogleMeetEvent(
                    user.getGoogleAccessToken(),
                    user.getGoogleRefreshToken(),
                    buildMeetingTitle(job.getJobTitle()),
                    buildMeetingDescription(job),
                    selectedSlot,
                    endTime,
                    scheduleMeetDTO.getUserTimeZone(),
                    scheduleMeetDTO.getCandidateEmail()
            );

        } else if (provider == Users.EmailProvider.MICROSOFT) {
            // ── Microsoft Teams ──
            if (!user.isMicrosoftConnected() || user.getMicrosoftAccessToken() == null) {
                throw new Exception(
                    "Microsoft account not connected. Please connect Microsoft account first.");
            }

            String fromEmail = user.getMicrosoftEmail();
            if (fromEmail == null || fromEmail.isBlank()) {
                throw new Exception(
                    "No Microsoft email found. Please reconnect your Microsoft account.");
            }

            meetLink = microsoftTeamsService.createTeamsMeeting(
                    user.getMicrosoftAccessToken(),
                    buildMeetingTitle(job.getJobTitle()),
                    selectedSlot,
                    endTime,
                    scheduleMeetDTO.getUserTimeZone(),
                    scheduleMeetDTO.getCandidateEmail()
            );
        }

        if (meetLink == null) {
            throw new Exception(
                "Failed to create meeting link. " +
                "Please check your " + provider.name() + " connection and try again.");
        }

        // Step 7: Send email based on provider
        if (provider == Users.EmailProvider.GOOGLE) {
            sendEmailViaGmailApi(
                    user.getGoogleAccessToken(),
                    user.getGoogleRefreshToken(),
                    user.getGoogleEmail(),
                    scheduleMeetDTO,
                    job,
                    meetLink
            );
        } else if (provider == Users.EmailProvider.MICROSOFT) {
            sendEmailViaMicrosoftApi(
                    user.getMicrosoftAccessToken(),
                    user.getMicrosoftEmail(),
                    scheduleMeetDTO,
                    job,
                    meetLink
            );
        }

        // Step 8: Save selected slot, meetLink, and update status on application
        application.setSelectedSlot(selectedSlot);
        application.setMeetLink(meetLink);
        application.setUserTimeZone(scheduleMeetDTO.getUserTimeZone());
        application.setStatus("Interview Scheduled");
        application.setUpdatedAt(LocalDateTime.now());
        candidateApplicationRepository.save(application);

        return "Interview scheduled successfully! Meet link: " + meetLink;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send email via Google Gmail API
    // ─────────────────────────────────────────────────────────────────────────
    private void sendEmailViaGmailApi(String accessToken,
                                      String refreshToken,
                                      String fromEmail,
                                      ScheduleMeetDTO dto,
                                      Jobs job,
                                      String meetLink) {
        try {
            AccessToken token = new AccessToken(accessToken, null);

            GoogleCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .setAccessToken(token)
                    .build()
                    .createScoped(List.of("https://www.googleapis.com/auth/gmail.send"));

            Gmail gmailService = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("ATS Circle")
                    .build();

            Properties props = new Properties();
            Session session  = Session.getDefaultInstance(props, null);
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setFrom(new InternetAddress(fromEmail));
            mimeMessage.setRecipients(
                    MimeMessage.RecipientType.TO,
                    InternetAddress.parse(dto.getToEmail()));
            mimeMessage.setSubject("Interview Scheduled - " + job.getJobTitle());
            mimeMessage.setContent(
                    buildInterviewEmailHtml(
                            dto.getCandidateName(),
                            job,
                            meetLink,
                            dto.getSelectedSlot(),
                            dto.getUserTimeZone(),
                            "Google Meet"),
                    "text/html; charset=utf-8");

            if (dto.getBccEmail() != null && !dto.getBccEmail().isBlank()) {
                mimeMessage.setRecipients(
                        MimeMessage.RecipientType.BCC,
                        InternetAddress.parse(dto.getBccEmail()));
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            String encodedEmail = Base64.encodeBase64URLSafeString(buffer.toByteArray());

            Message message = new Message();
            message.setRaw(encodedEmail);

            gmailService.users().messages().send("me", message).execute();

            System.out.println("Email sent via Gmail API from: " + fromEmail
                    + " to: " + dto.getToEmail());

        } catch (Exception e) {
            System.err.println("Error sending email via Gmail API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send email via Microsoft Graph API (/me/sendMail)
    // ─────────────────────────────────────────────────────────────────────────
    private void sendEmailViaMicrosoftApi(String accessToken,
                                          String fromEmail,
                                          ScheduleMeetDTO dto,
                                          Jobs job,
                                          String meetLink) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            // Build email body
            String htmlContent = buildInterviewEmailHtml(
                    dto.getCandidateName(),
                    job,
                    meetLink,
                    dto.getSelectedSlot(),
                    dto.getUserTimeZone(),
                    "Microsoft Teams");

            // Build recipient
            Map<String, Object> toRecipient = Map.of(
                "emailAddress", Map.of("address", dto.getToEmail())
            );

            List<Map<String, Object>> toList = new ArrayList<>();
            toList.add(toRecipient);

            // Build BCC if present
            List<Map<String, Object>> bccList = new ArrayList<>();
            if (dto.getBccEmail() != null && !dto.getBccEmail().isBlank()) {
                bccList.add(Map.of(
                    "emailAddress", Map.of("address", dto.getBccEmail())
                ));
            }

            // Build message object
            Map<String, Object> messageBody = new LinkedHashMap<>();
            messageBody.put("subject", "Interview Scheduled - " + job.getJobTitle());
            messageBody.put("body", Map.of(
                "contentType", "HTML",
                "content", htmlContent
            ));
            messageBody.put("toRecipients", toList);
            if (!bccList.isEmpty()) {
                messageBody.put("bccRecipients", bccList);
            }

            Map<String, Object> requestBody = Map.of("message", messageBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(
                    mapper.writeValueAsString(requestBody), headers);

            restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me/sendMail",
                    HttpMethod.POST,
                    request,
                    String.class);

            System.out.println("Email sent via Microsoft Graph API from: " + fromEmail
                    + " to: " + dto.getToEmail());

        } catch (Exception e) {
            System.err.println("Error sending email via Microsoft Graph API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private String buildInterviewEmailHtml(String candidateName, Jobs job,
                                           String meetLink, String slotTime,
                                           String timeZone, String meetPlatform) {
        return String.format(
            "<html><body style='font-family:Arial,sans-serif;color:#333;'>" +
            "<div style='max-width:600px;margin:0 auto;padding:20px;" +
                 "border:1px solid #ddd;border-radius:10px;'>" +
            "<h2 style='color:#4CAF50;'>Interview Scheduled! 🎉</h2>" +
            "<p>Hello <strong>%s</strong>,</p>" +
            "<p>Your interview has been scheduled. Please find the details below.</p>" +
            "<div style='background:#f4f4f4;padding:15px;border-radius:5px;margin:20px 0;'>" +
            "<p><strong>Position:</strong> %s</p>" +
            "<p><strong>Date &amp; Time:</strong> %s (%s)</p>" +
            "<p><strong>%s:</strong> " +
                 "<a href='%s' style='color:#4CAF50;'>Join Meeting</a></p>" +
            "</div>" +
            "<p style='color:#ff5722;'><strong>⚠️</strong> " +
                 "Please test your audio and video before the interview.</p>" +
            "<p>Best regards,<br><strong>ATS Circle Team</strong></p>" +
            "</div></body></html>",
            candidateName != null ? candidateName : "Candidate",
            job.getJobTitle()  != null ? job.getJobTitle()  : "N/A",
            slotTime,
            timeZone,
            meetPlatform,
            meetLink
        );
    }

    private String buildMeetingTitle(String jobTitle) {
        return "Interview - " + (jobTitle != null ? jobTitle : "Position");
    }

    private String buildMeetingDescription(Jobs job) {
        return "Job Title: "       + nvl(job.getJobTitle())       + "\n" +
               "Job Code: "        + nvl(job.getJobCode())        + "\n" +
               "Department: "      + nvl(job.getDepartment())     + "\n" +
               "Job Description: " + nvl(job.getJobDescription());
    }

    private String nvl(String s) { return s != null ? s : "N/A"; }

    private String calculateEndTime(String startTime) {
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(startTime, fmt)
                        .plusHours(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {}
        }
        return startTime;
    }
}