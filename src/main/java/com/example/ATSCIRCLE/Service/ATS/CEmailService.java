
// package com.example.ATSCIRCLE.Service.ATS;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.mail.javamail.MimeMessageHelper;
// import org.springframework.stereotype.Service;

// import jakarta.mail.MessagingException;
// import jakarta.mail.internet.MimeMessage;

// import java.util.Base64;

// @Service
// public class CEmailService {

//     @Autowired
//     private JavaMailSender mailSender;
    
//     @Value("${spring.mail.username}")
//     private String fromEmail;
//     public void sendEmail(String to, String subject, String body) {
//         try {
//             SimpleMailMessage message = new SimpleMailMessage();
//             message.setFrom(fromEmail);
//             message.setTo(to);
//             message.setSubject(subject);
//             message.setText(body);
//             mailSender.send(message);
//             System.out.println("📧 Plain email sent to: " + to);
//         } catch (Exception e) {
//             System.err.println("❌ Failed to send plain email: " + e.getMessage());
//             throw new RuntimeException("Failed to send email to: " + to, e);
//         }
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // EMAIL WITH BYTE-ARRAY ATTACHMENT
//     //
//     // Used by: ClientShareableLinkService.sendCandidateListAttachmentEmail()
//     // Attaches an in-memory Excel file (byte[]) built by Apache POI.
//     // ─────────────────────────────────────────────────────────────────────────
//     public void sendEmailWithBytesAttachment(String to,
//                                              String subject,
//                                              String body,
//                                              byte[] attachmentBytes,
//                                              String attachmentFilename) {
//         try {
//             MimeMessage message = mailSender.createMimeMessage();
//             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

//             helper.setFrom(fromEmail);
//             helper.setTo(to);
//             helper.setSubject(subject);
//             helper.setText(body, false); // plain text body

//             // Wrap the byte[] in a Spring ByteArrayResource for attachment
//             helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes));

//             mailSender.send(message);
//             System.out.println("📧 Email with attachment '" + attachmentFilename + "' sent to: " + to);

//         } catch (MessagingException e) {
//             System.err.println("❌ Failed to send attachment email: " + e.getMessage());
//             throw new RuntimeException("Failed to send attachment email to: " + to, e);
//         }
//     }


//     // public void sendEmailWithAttachment(String to, 
//     //                                    String subject, 
//     //                                    String body, 
//     //                                    String base64Attachment,
//     //                                    String attachmentName) {
//     //     try {
//     //         MimeMessage message = mailSender.createMimeMessage();
//     //         MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
//     //         helper.setFrom(fromEmail);
//     //         helper.setTo(to);
//     //         helper.setSubject(subject);
//     //         helper.setText(body, false);
            
//     //         // Decode base64 and attach file
//     //         if (base64Attachment != null && !base64Attachment.isEmpty()) {
//     //             byte[] decodedBytes = Base64.getDecoder().decode(base64Attachment);
//     //             ByteArrayResource resource = new ByteArrayResource(decodedBytes);
//     //             helper.addAttachment(attachmentName, resource);
//     //         }
            
//     //         mailSender.send(message);
//     //     } catch (MessagingException e) {
//     //         throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//     //     }
//     // }

//     // public void sendEmailWithByteAttachment(String to, 
//     //                                        String subject, 
//     //                                        String body, 
//     //                                        byte[] attachment,
//     //                                        String attachmentName) {
//     //     try {
//     //         MimeMessage message = mailSender.createMimeMessage();
//     //         MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
//     //         helper.setFrom(fromEmail);
//     //         helper.setTo(to);
//     //         helper.setSubject(subject);
//     //         helper.setText(body, false);
            
//     //         if (attachment != null && attachment.length > 0) {
//     //             ByteArrayResource resource = new ByteArrayResource(attachment);
//     //             helper.addAttachment(attachmentName, resource);
//     //         }
            
//     //         mailSender.send(message);
//     //     } catch (MessagingException e) {
//     //         throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//     //     }
//     // }

//     public void sendSimpleEmail(String to, String subject, String body) {
//         try {
//             MimeMessage message = mailSender.createMimeMessage();
//             MimeMessageHelper helper = new MimeMessageHelper(message, false);
            
//             helper.setFrom(fromEmail);
//             helper.setTo(to);
//             helper.setSubject(subject);
//             helper.setText(body, false);
            
//             mailSender.send(message);
//         } catch (MessagingException e) {
//             throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//         }
//     }
    
//     public void sendHtmlEmail(String to, String subject, String htmlBody) {
//         try {
//             MimeMessage message = mailSender.createMimeMessage();
//             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
//             helper.setFrom(fromEmail);
//             helper.setTo(to);
//             helper.setSubject(subject);
//             helper.setText(htmlBody, true);
            
//             mailSender.send(message);
//         } catch (MessagingException e) {
//             throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//         }
//     }
// }

package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Models.Users.EmailProvider;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private ObjectMapper objectMapper;  // Spring Boot auto-configures this

    @Value("${spring.mail.username}")
    private String fromEmail;

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

    // ─────────────────────────────────────────────────────────────────────────
    // PRIMARY — send plain email FROM org's connected account
    //
    // organizationId = Users._id  (same value stored on ClientShareableLink)
    // ─────────────────────────────────────────────────────────────────────────

    public void sendEmail(String organizationId, String to, String subject, String body) {
        Users org = resolveOrg(organizationId);

        if (org == null) {
            System.out.println("⚠️  Org not found id=" + organizationId + " → SMTP fallback");
            sendViaSMTP(to, subject, body);
            return;
        }

        EmailProvider provider = org.getDefaultEmailProvider();
        System.out.println("📨 sendEmail via " + provider
                + " | org=" + org.getOrganizationName() + " | to=" + to);

        switch (provider) {
            case GOOGLE    -> sendViaGmail(org, to, subject, body);
            case MICROSOFT -> sendViaMicrosoft(org, to, subject, body);
            default        -> {
                System.out.println("⚠️  Provider=NONE for org=" + organizationId + " → SMTP");
                sendViaSMTP(to, subject, body);
            }
        }
    }

    /** Backward-compat — no org context, always SMTP */
    public void sendEmail(String to, String subject, String body) {
        sendViaSMTP(to, subject, body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIMARY — send email WITH attachment FROM org's connected account
    // ─────────────────────────────────────────────────────────────────────────

    public void sendEmailWithBytesAttachment(String organizationId,
                                             String to,
                                             String subject,
                                             String body,
                                             byte[] attachmentBytes,
                                             String attachmentFilename) {
        Users org = resolveOrg(organizationId);

        if (org == null) {
            System.out.println("⚠️  Org not found id=" + organizationId + " → SMTP attachment fallback");
            sendAttachmentViaSMTP(to, subject, body, attachmentBytes, attachmentFilename);
            return;
        }

        EmailProvider provider = org.getDefaultEmailProvider();
        System.out.println("📨 sendAttachment via " + provider
                + " | org=" + org.getOrganizationName() + " | to=" + to);

        switch (provider) {
            case GOOGLE    -> sendAttachmentViaGmail(org, to, subject, body, attachmentBytes, attachmentFilename);
            case MICROSOFT -> sendAttachmentViaMicrosoft(org, to, subject, body, attachmentBytes, attachmentFilename);
            default        -> sendAttachmentViaSMTP(to, subject, body, attachmentBytes, attachmentFilename);
        }
    }

    /** Backward-compat — no org context, always SMTP */
    public void sendEmailWithBytesAttachment(String to, String subject, String body,
                                             byte[] attachmentBytes, String attachmentFilename) {
        sendAttachmentViaSMTP(to, subject, body, attachmentBytes, attachmentFilename);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORG RESOLVER
    // Users._id == organizationId (set in JWT + ClientShareableLink)
    // ─────────────────────────────────────────────────────────────────────────

    private Users resolveOrg(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) return null;
        return usersRepository.findById(organizationId).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GMAIL API — plain text
    // ─────────────────────────────────────────────────────────────────────────

    private void sendViaGmail(Users org, String to, String subject, String body) {
        try {
            String token   = getValidGoogleToken(org);
            String raw     = buildRfc2822(org.getGoogleEmail(), to, subject, body, null, null);
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            callGmailSend(token, Map.of("raw", encoded));
            System.out.println("✅ Gmail sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Gmail failed: " + e.getMessage());
            throw new RuntimeException("Gmail send failed", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GMAIL API — with attachment (multipart/mixed RFC 2822)
    // ─────────────────────────────────────────────────────────────────────────

    private void sendAttachmentViaGmail(Users org, String to, String subject, String body,
                                        byte[] bytes, String filename) {
        try {
            String token   = getValidGoogleToken(org);
            String raw     = buildRfc2822(org.getGoogleEmail(), to, subject, body, bytes, filename);
            String encoded = Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            callGmailSend(token, Map.of("raw", encoded));
            System.out.println("✅ Gmail attachment sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Gmail attachment failed: " + e.getMessage());
            throw new RuntimeException("Gmail attachment send failed", e);
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

    // ─────────────────────────────────────────────────────────────────────────
    // MICROSOFT GRAPH — plain text
    // ─────────────────────────────────────────────────────────────────────────

    private void sendViaMicrosoft(Users org, String to, String subject, String body) {
        try {
            String token = getValidMicrosoftToken(org);
            callGraphSend(token, buildGraphPayload(to, subject, body, null, null));
            System.out.println("✅ Graph sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Graph failed: " + e.getMessage());
            throw new RuntimeException("Microsoft send failed", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MICROSOFT GRAPH — with attachment
    // ─────────────────────────────────────────────────────────────────────────

    private void sendAttachmentViaMicrosoft(Users org, String to, String subject, String body,
                                            byte[] bytes, String filename) {
        try {
            String token = getValidMicrosoftToken(org);
            callGraphSend(token, buildGraphPayload(to, subject, body, bytes, filename));
            System.out.println("✅ Graph attachment sent → " + to);
        } catch (Exception e) {
            System.err.println("❌ Graph attachment failed: " + e.getMessage());
            throw new RuntimeException("Microsoft attachment send failed", e);
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

    // ─────────────────────────────────────────────────────────────────────────
    // TOKEN REFRESH — Google
    // ─────────────────────────────────────────────────────────────────────────

    private String getValidGoogleToken(Users org) {
        if (org.getGoogleTokenExpiry() != null
                && LocalDateTime.now().isAfter(org.getGoogleTokenExpiry())) {
            System.out.println("🔄 Refreshing Google token org=" + org.getId());

            RestTemplate rt = new RestTemplate();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String reqBody = "grant_type=refresh_token"
                    + "&refresh_token=" + org.getGoogleRefreshToken()
                    + "&client_id=" + googleClientId
                    + "&client_secret=" + googleClientSecret;

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.postForObject(
                    "https://oauth2.googleapis.com/token",
                    new HttpEntity<>(reqBody, h), Map.class);

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

    // ─────────────────────────────────────────────────────────────────────────
    // TOKEN REFRESH — Microsoft
    // ─────────────────────────────────────────────────────────────────────────

    private String getValidMicrosoftToken(Users org) {
        if (org.getMicrosoftTokenExpiry() != null
                && LocalDateTime.now().isAfter(org.getMicrosoftTokenExpiry())) {
            System.out.println("🔄 Refreshing Microsoft token org=" + org.getId());

            RestTemplate rt = new RestTemplate();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String reqBody = "grant_type=refresh_token"
                    + "&refresh_token=" + org.getMicrosoftRefreshToken()
                    + "&client_id=" + microsoftClientId
                    + "&client_secret=" + microsoftClientSecret
                    + "&scope=https://graph.microsoft.com/Mail.Send offline_access";

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.postForObject(
                    "https://login.microsoftonline.com/" + microsoftTenantId + "/oauth2/v2.0/token",
                    new HttpEntity<>(reqBody, h), Map.class);

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

    // ─────────────────────────────────────────────────────────────────────────
    // RFC 2822 builder — used by Gmail API
    // Subject is RFC 2047 encoded (=?UTF-8?B?...?=) to handle em-dashes,
    // accents, and any non-ASCII characters safely.
    // ─────────────────────────────────────────────────────────────────────────

    private String buildRfc2822(String from, String to, String subject, String body,
                                byte[] attachmentBytes, String attachmentFilename)
            throws Exception {

        // RFC 2047 Base64-encoded subject — handles em-dash, accents, etc.
        String encodedSubject = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                + "?=";

        if (attachmentBytes == null) {
            // Plain text message
            return "From: " + from + "\r\n"
                    + "To: " + to + "\r\n"
                    + "Subject: " + encodedSubject + "\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n"
                    + "\r\n"
                    + body;
        }

        // Multipart/mixed with attachment
        String boundary = "boundary_" + System.currentTimeMillis();
        String encodedAttachment = Base64.getMimeEncoder().encodeToString(attachmentBytes);

        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n"
                + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + body + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "Content-Disposition: attachment; filename=\"" + attachmentFilename + "\"\r\n"
                + "\r\n"
                + encodedAttachment + "\r\n"
                + "--" + boundary + "--";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Graph JSON builder — uses Jackson ObjectMapper for proper UTF-8 encoding.
    // No manual string escaping means em-dash, quotes, etc. all work correctly.
    // ─────────────────────────────────────────────────────────────────────────

    private String buildGraphPayload(String to, String subject, String body,
                                     byte[] attachmentBytes, String attachmentFilename) {
        try {
            Map<String, Object> messageMap = new LinkedHashMap<>();
            messageMap.put("subject", subject);  // Jackson handles UTF-8 automatically
            messageMap.put("body", Map.of(
                    "contentType", "Text",
                    "content", body
            ));
            messageMap.put("toRecipients", List.of(
                    Map.of("emailAddress", Map.of("address", to))
            ));

            if (attachmentBytes != null) {
                String encoded = Base64.getEncoder().encodeToString(attachmentBytes);
                messageMap.put("attachments", List.of(Map.of(
                        "@odata.type", "#microsoft.graph.fileAttachment",
                        "name", attachmentFilename,
                        "contentBytes", encoded
                )));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", messageMap);
            payload.put("saveToSentItems", "true");

            return objectMapper.writeValueAsString(payload);  // proper UTF-8 JSON

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Graph API payload", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMTP — uses MimeMessageHelper with explicit UTF-8 charset.
    // SimpleMailMessage does NOT set charset on subject, causing garbled chars.
    // MimeMessageHelper("UTF-8") handles em-dash and all non-ASCII correctly.
    // ─────────────────────────────────────────────────────────────────────────

    private void sendViaSMTP(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);   // MimeMessageHelper encodes subject as RFC 2047 UTF-8
            helper.setText(body, false);
            mailSender.send(message);
            System.out.println("📧 SMTP sent → " + to);
        } catch (MessagingException e) {
            System.err.println("❌ SMTP failed: " + e.getMessage());
            throw new RuntimeException("SMTP send failed → " + to, e);
        }
    }

    private void sendAttachmentViaSMTP(String to, String subject, String body,
                                       byte[] bytes, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(filename, new ByteArrayResource(bytes));
            mailSender.send(message);
            System.out.println("📧 SMTP attachment sent → " + to);
        } catch (MessagingException e) {
            System.err.println("❌ SMTP attachment failed: " + e.getMessage());
            throw new RuntimeException("SMTP attachment send failed → " + to, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility helpers — SMTP only (unchanged callers)
    // ─────────────────────────────────────────────────────────────────────────

    public void sendSimpleEmail(String to, String subject, String body) {
        sendViaSMTP(to, subject, body);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("HTML email send failed: " + e.getMessage(), e);
        }
    }
}