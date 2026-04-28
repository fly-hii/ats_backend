package com.example.ATSCIRCLE.Service;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service("basicEmailService")
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.mail.username}")
    private String defaultFromEmail;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${microsoft.client.id}")
    private String microsoftClientId;

    @Value("${microsoft.client.secret}")
    private String microsoftClientSecret;

    @Value("${microsoft.tenant.id:common}")
    private String microsoftTenantId;

    // =========================================================================
    // CORE ROUTING — resolves user by organizationId and picks provider
    // =========================================================================

    /**
     * Resolves the Users doc from organizationId (Users._id).
     * Returns null if not found or id is blank.
     */
    private Users resolveOrg(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) return null;
        return usersRepository.findById(organizationId).orElse(null);
    }

    /**
     * Returns the effective provider for a user.
     * Falls back to NONE if user is null.
     */
    private Users.EmailProvider getProvider(Users user) {
        if (user == null) return Users.EmailProvider.NONE;
        Users.EmailProvider p = user.getDefaultEmailProvider();
        return p != null ? p : Users.EmailProvider.NONE;
    }

    // =========================================================================
    // PUBLIC API — all methods accept optional organizationId as first param
    // Overloads without organizationId fall back to SMTP (backward-compat)
    // =========================================================================

    // ── sendHtmlEmail ─────────────────────────────────────────────────────────

    /** Backward-compat: always SMTP */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendHtmlEmailInternal(null, to, subject, htmlContent);
    }

    /** Backward-compat with from param (ignored for routing, kept for callers) */
    public void sendHtmlEmail(String from, String to, String subject, String htmlContent) {
        sendHtmlEmailInternal(null, to, subject, htmlContent);
    }

    /** Routes through org's connected provider */
    public void sendHtmlEmailForOrg(String organizationId, String to, String subject, String htmlContent) {
        sendHtmlEmailInternal(organizationId, to, subject, htmlContent);
    }

    private void sendHtmlEmailInternal(String organizationId, String to, String subject, String htmlContent) {
        Users org = resolveOrg(organizationId);
        Users.EmailProvider provider = getProvider(org);
        System.out.println("📨 sendHtmlEmail via " + provider + " → " + to);

        switch (provider) {
            case GOOGLE    -> sendHtmlViaGmail(org, to, subject, htmlContent);
            case MICROSOFT -> sendHtmlViaMicrosoft(org, to, subject, htmlContent);
            default        -> sendHtmlViaSMTP(to, subject, htmlContent);
        }
    }

    // ── sendHtmlEmailWithAttachments ──────────────────────────────────────────

    /** Backward-compat: always SMTP */
    public void sendHtmlEmailWithAttachments(String to, String subject,
                                              String htmlContent, MultipartFile[] files) {
        sendAttachmentInternal(null, to, subject, htmlContent, files);
    }

    /** Backward-compat with from param */
    public void sendHtmlEmailWithAttachments(String from, String to, String subject,
                                              String htmlContent, MultipartFile[] files) {
        sendAttachmentInternal(null, to, subject, htmlContent, files);
    }

    /** Routes through org's connected provider */
    public void sendHtmlEmailWithAttachmentsForOrg(String organizationId, String to, String subject,
                                                    String htmlContent, MultipartFile[] files) {
        sendAttachmentInternal(organizationId, to, subject, htmlContent, files);
    }

    private void sendAttachmentInternal(String organizationId, String to, String subject,
                                         String htmlContent, MultipartFile[] files) {
        Users org = resolveOrg(organizationId);
        Users.EmailProvider provider = getProvider(org);
        System.out.println("📨 sendAttachment via " + provider + " → " + to);

        switch (provider) {
            case GOOGLE    -> sendAttachmentViaGmail(org, to, subject, htmlContent, files);
            case MICROSOFT -> sendAttachmentViaMicrosoft(org, to, subject, htmlContent, files);
            default        -> sendAttachmentViaSMTP(to, subject, htmlContent, files);
        }
    }

    // ── sendTaskReminder ──────────────────────────────────────────────────────

    /** Backward-compat: always SMTP */
    public void sendTaskReminder(String toEmail, String subject, String body) {
        sendPlainInternal(null, toEmail, subject, body);
    }

    /** Backward-compat with from param */
    public void sendTaskReminder(String from, String toEmail, String subject, String body) {
        sendPlainInternal(null, toEmail, subject, body);
    }

    /** Routes through org's connected provider */
    public void sendTaskReminderForOrg(String organizationId, String toEmail, String subject, String body) {
        sendPlainInternal(organizationId, toEmail, subject, body);
    }

    private void sendPlainInternal(String organizationId, String to, String subject, String body) {
        Users org = resolveOrg(organizationId);
        Users.EmailProvider provider = getProvider(org);
        System.out.println("📨 sendPlain via " + provider + " → " + to);

        switch (provider) {
            case GOOGLE    -> sendPlainViaGmail(org, to, subject, body);
            case MICROSOFT -> sendPlainViaMicrosoft(org, to, subject, body);
            default        -> sendPlainViaSMTP(to, subject, body);
        }
    }

    // ── sendCredentialsEmail ──────────────────────────────────────────────────

    /** Backward-compat: always SMTP */
    public void sendCredentialsEmail(String toEmail, String userName, String email, String password) {
        sendCredentialsInternal(null, toEmail, userName, email, password);
    }

    /** Backward-compat with from param */
    public void sendCredentialsEmail(String from, String toEmail, String userName,
                                      String email, String password) {
        sendCredentialsInternal(null, toEmail, userName, email, password);
    }

    /** Routes through org's connected provider */
    public void sendCredentialsEmailForOrg(String organizationId, String toEmail,
                                            String userName, String email, String password) {
        sendCredentialsInternal(organizationId, toEmail, userName, email, password);
    }

    private void sendCredentialsInternal(String organizationId, String toEmail,
                                          String userName, String email, String password) {
        String htmlContent = String.format(
            "<html><body style='font-family:Arial,sans-serif;'>" +
            "<div style='max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;'>" +
            "<h2 style='color:#4CAF50;'>Welcome to ATS Circle!</h2>" +
            "<p>Hello <strong>%s</strong>,</p>" +
            "<p>Your account has been created. Here are your login credentials:</p>" +
            "<div style='background:#f4f4f4;padding:15px;border-radius:5px;margin:20px 0;'>" +
            "<p><strong>Email:</strong> %s</p>" +
            "<p><strong>Password:</strong> <code style='background:#fff;padding:5px 10px;border-radius:3px;'>%s</code></p>" +
            "</div>" +
            "<p style='color:#ff5722;'><strong>Please change your password immediately after login.</strong></p>" +
            "<p>Best regards,<br><strong>ATS Circle Team</strong></p>" +
            "</div></body></html>",
            userName, email, password
        );
        sendHtmlEmailInternal(organizationId, toEmail,
                "Welcome to ATS Circle - Your Login Credentials", htmlContent);
    }

    // ── sendBulkEmail ─────────────────────────────────────────────────────────

    /** Backward-compat: always SMTP */
    public void sendBulkEmail(String[] toAddresses, String subject, String body) {
        for (String to : toAddresses) sendPlainInternal(null, to, subject, body);
    }

    /** Backward-compat with from param */
    public void sendBulkEmail(String from, String[] toAddresses, String subject, String body) {
        for (String to : toAddresses) sendPlainInternal(null, to, subject, body);
    }

    /** Routes through org's connected provider */
    public void sendBulkEmailForOrg(String organizationId, String[] toAddresses,
                                     String subject, String body) {
        for (String to : toAddresses) sendPlainInternal(organizationId, to, subject, body);
    }

    // =========================================================================
    // GMAIL IMPLEMENTATION
    // =========================================================================

    private void sendHtmlViaGmail(Users org, String to, String subject, String html) {
        try {
            String token   = getValidGoogleToken(org);
            String raw     = buildRfc2822Html(org.getGoogleEmail(), to, subject, html, null, null);
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            callGmailSend(token, Map.of("raw", encoded));
            System.out.println("✅ Gmail HTML sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Gmail HTML failed: " + e.getMessage() + " → SMTP fallback");
            sendHtmlViaSMTP(to, subject, html);
        }
    }

    private void sendPlainViaGmail(Users org, String to, String subject, String body) {
        try {
            String token   = getValidGoogleToken(org);
            String raw     = buildRfc2822Plain(org.getGoogleEmail(), to, subject, body);
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            callGmailSend(token, Map.of("raw", encoded));
            System.out.println("✅ Gmail plain sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Gmail plain failed: " + e.getMessage() + " → SMTP fallback");
            sendPlainViaSMTP(to, subject, body);
        }
    }

    private void sendAttachmentViaGmail(Users org, String to, String subject,
                                         String html, MultipartFile[] files) {
        try {
            String token   = getValidGoogleToken(org);
            String raw     = buildRfc2822Html(org.getGoogleEmail(), to, subject, html, files, null);
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            callGmailSend(token, Map.of("raw", encoded));
            System.out.println("✅ Gmail attachment sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Gmail attachment failed: " + e.getMessage() + " → SMTP fallback");
            sendAttachmentViaSMTP(to, subject, html, files);
        }
    }

    private void callGmailSend(String accessToken, Map<String, String> payload) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rt.postForEntity(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
                new HttpEntity<>(payload, h), String.class);
        if (!resp.getStatusCode().is2xxSuccessful())
            throw new RuntimeException("Gmail API error: " + resp.getStatusCode());
    }

    // =========================================================================
    // MICROSOFT GRAPH IMPLEMENTATION
    // =========================================================================

    private void sendHtmlViaMicrosoft(Users org, String to, String subject, String html) {
        try {
            String token   = getValidMicrosoftToken(org);
            String payload = buildGraphPayload(to, subject, html, true, null, null);
            callGraphSend(token, payload);
            System.out.println("✅ Graph HTML sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Graph HTML failed: " + e.getMessage() + " → SMTP fallback");
            sendHtmlViaSMTP(to, subject, html);
        }
    }

    private void sendPlainViaMicrosoft(Users org, String to, String subject, String body) {
        try {
            String token   = getValidMicrosoftToken(org);
            String payload = buildGraphPayload(to, subject, body, false, null, null);
            callGraphSend(token, payload);
            System.out.println("✅ Graph plain sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Graph plain failed: " + e.getMessage() + " → SMTP fallback");
            sendPlainViaSMTP(to, subject, body);
        }
    }

    private void sendAttachmentViaMicrosoft(Users org, String to, String subject,
                                             String html, MultipartFile[] files) {
        try {
            String token   = getValidMicrosoftToken(org);
            String payload = buildGraphPayload(to, subject, html, true, files, null);
            callGraphSend(token, payload);
            System.out.println("✅ Graph attachment sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Graph attachment failed: " + e.getMessage() + " → SMTP fallback");
            sendAttachmentViaSMTP(to, subject, html, files);
        }
    }

    private void callGraphSend(String accessToken, String jsonPayload) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rt.postForEntity(
                "https://graph.microsoft.com/v1.0/me/sendMail",
                new HttpEntity<>(jsonPayload, h), String.class);
        if (resp.getStatusCode() != HttpStatus.ACCEPTED && !resp.getStatusCode().is2xxSuccessful())
            throw new RuntimeException("Graph API error: " + resp.getStatusCode());
    }

    // =========================================================================
    // TOKEN REFRESH
    // =========================================================================

    private String getValidGoogleToken(Users org) {
        if (org.getGoogleTokenExpiry() != null
                && LocalDateTime.now().isAfter(org.getGoogleTokenExpiry())) {
            System.out.println("🔄 Refreshing Google token org=" + org.getId());

            RestTemplate rt = new RestTemplate();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=refresh_token"
                    + "&refresh_token=" + org.getGoogleRefreshToken()
                    + "&client_id=" + googleClientId
                    + "&client_secret=" + googleClientSecret;

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.postForObject(
                    "https://oauth2.googleapis.com/token",
                    new HttpEntity<>(body, h), Map.class);

            if (resp == null || !resp.containsKey("access_token"))
                throw new RuntimeException("Google token refresh failed org=" + org.getId());

            String newToken = (String) resp.get("access_token");
            int expiresIn   = ((Number) resp.get("expires_in")).intValue();
            org.setGoogleAccessToken(newToken);
            org.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn - 60));
            usersRepository.save(org);
            System.out.println("✅ Google token refreshed org=" + org.getId());
            return newToken;
        }
        return org.getGoogleAccessToken();
    }

    private String getValidMicrosoftToken(Users org) {
        if (org.getMicrosoftTokenExpiry() != null
                && LocalDateTime.now().isAfter(org.getMicrosoftTokenExpiry())) {
            System.out.println("🔄 Refreshing Microsoft token org=" + org.getId());

            RestTemplate rt = new RestTemplate();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=refresh_token"
                    + "&refresh_token=" + org.getMicrosoftRefreshToken()
                    + "&client_id=" + microsoftClientId
                    + "&client_secret=" + microsoftClientSecret
                    + "&scope=https://graph.microsoft.com/Mail.Send offline_access";

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.postForObject(
                    "https://login.microsoftonline.com/" + microsoftTenantId + "/oauth2/v2.0/token",
                    new HttpEntity<>(body, h), Map.class);

            if (resp == null || !resp.containsKey("access_token"))
                throw new RuntimeException("Microsoft token refresh failed org=" + org.getId());

            String newToken = (String) resp.get("access_token");
            int expiresIn   = ((Number) resp.get("expires_in")).intValue();
            org.setMicrosoftAccessToken(newToken);
            org.setMicrosoftTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn - 60));
            usersRepository.save(org);
            System.out.println("✅ Microsoft token refreshed org=" + org.getId());
            return newToken;
        }
        return org.getMicrosoftAccessToken();
    }

    // =========================================================================
    // RFC 2822 BUILDERS (Gmail API)
    // =========================================================================

    /** Plain text RFC 2822 message */
    private String buildRfc2822Plain(String from, String to, String subject, String body) {
        String encodedSubject = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                + "?=";
        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n" + body;
    }

    /**
     * HTML RFC 2822 message, optionally with MultipartFile attachments.
     * Pass files=null for no attachments.
     */
    private String buildRfc2822Html(String from, String to, String subject,
                                     String htmlBody, MultipartFile[] files,
                                     @SuppressWarnings("unused") Object unused) throws Exception {
        String encodedSubject = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                + "?=";

        if (files == null || files.length == 0) {
            // Simple HTML — no attachment
            return "From: " + from + "\r\n"
                    + "To: " + to + "\r\n"
                    + "Subject: " + encodedSubject + "\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "\r\n" + htmlBody;
        }

        // Multipart/mixed with HTML body + file attachments
        String boundary = "boundary_" + System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(from).append("\r\n");
        sb.append("To: ").append(to).append("\r\n");
        sb.append("Subject: ").append(encodedSubject).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");
        sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n\r\n");

        // HTML part
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Type: text/html; charset=UTF-8\r\n\r\n");
        sb.append(htmlBody).append("\r\n");

        // Attachment parts
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String encodedContent = Base64.getMimeEncoder().encodeToString(file.getBytes());
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: ").append(
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream"
            ).append("\r\n");
            sb.append("Content-Transfer-Encoding: base64\r\n");
            sb.append("Content-Disposition: attachment; filename=\"")
              .append(file.getOriginalFilename()).append("\"\r\n\r\n");
            sb.append(encodedContent).append("\r\n");
        }

        sb.append("--").append(boundary).append("--");
        return sb.toString();
    }

    // =========================================================================
    // GRAPH JSON BUILDER (Microsoft Graph API)
    // Uses Jackson ObjectMapper — handles all UTF-8 / special chars correctly
    // =========================================================================

    private String buildGraphPayload(String to, String subject, String content,
                                      boolean isHtml, MultipartFile[] files,
                                      @SuppressWarnings("unused") Object unused) {
        try {
            Map<String, Object> messageMap = new LinkedHashMap<>();
            messageMap.put("subject", subject);
            messageMap.put("body", Map.of(
                    "contentType", isHtml ? "HTML" : "Text",
                    "content", content
            ));
            messageMap.put("toRecipients", List.of(
                    Map.of("emailAddress", Map.of("address", to))
            ));

            if (files != null && files.length > 0) {
                List<Map<String, Object>> attachments = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;
                    attachments.add(Map.of(
                            "@odata.type", "#microsoft.graph.fileAttachment",
                            "name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                            "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                            "contentBytes", Base64.getEncoder().encodeToString(file.getBytes())
                    ));
                }
                if (!attachments.isEmpty()) messageMap.put("attachments", attachments);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", messageMap);
            payload.put("saveToSentItems", "true");

            return objectMapper.writeValueAsString(payload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Graph payload", e);
        }
    }

    // =========================================================================
    // SMTP FALLBACKS — MimeMessageHelper with UTF-8 (no SimpleMailMessage)
    // =========================================================================

    private void sendHtmlViaSMTP(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("📧 SMTP HTML sent → " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("SMTP HTML send failed → " + to, e);
        }
    }

    private void sendPlainViaSMTP(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            System.out.println("📧 SMTP plain sent → " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("SMTP plain send failed → " + to, e);
        }
    }

    private void sendAttachmentViaSMTP(String to, String subject,
                                        String htmlContent, MultipartFile[] files) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        helper.addAttachment(
                                file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                                new ByteArrayResource(file.getBytes()));
                    }
                }
            }

            mailSender.send(message);
            System.out.println("📧 SMTP attachment sent → " + to);
        } catch (Exception e) {
            throw new RuntimeException("SMTP attachment send failed → " + to, e);
        }
    }
}