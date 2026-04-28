package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Contact;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Client;
import com.example.ATSCIRCLE.Models.Sales.EmailLog;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.ContactRepository;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import com.example.ATSCIRCLE.Repository.ClientRepository;
import com.example.ATSCIRCLE.Repository.EmailLogRepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.DTO.EmailRecipient;

import com.fasterxml.jackson.databind.ObjectMapper;

// Google imports
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("bulkEmailService")
public class BulkEmailService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    // ================================================================================
    //                    CONTACT EMAIL SENDING METHODS
    // ================================================================================

    public EmailLog sendEmailToContacts(String organizationId, String senderId, List<String> contactIds,
                                        String subject, String body, List<MultipartFile> attachments) {

        Users sender = getValidatedSender(senderId);

        List<Contact> contacts = new ArrayList<>();
        for (String id : contactIds) {
            contactRepository.findById(id)
                    .filter(c -> c.getOrganizationId().equals(organizationId))
                    .ifPresent(contacts::add);
        }

        if (contacts.isEmpty()) throw new RuntimeException("No valid contacts found for the provided IDs");

        List<String> recipientEmails = contacts.stream().map(Contact::getEmail).collect(Collectors.toList());
        EmailLog emailLog = new EmailLog(organizationId, senderId, getSenderEmail(sender), recipientEmails, subject, body);

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (Contact contact : contacts) {
            try {
                String greeting = "Hello " + contact.getFirstName() + " " + contact.getLastName() + ",";
                sendEmail(sender, contact.getEmail(), subject, greeting, body, attachments);
                successCount++;
            } catch (Exception e) {
                failedEmails.add(contact.getEmail());
                System.err.println("Failed to send email to " + contact.getEmail() + ": " + e.getMessage());
            }
        }

        return finalizeEmailLog(emailLog, failedEmails, successCount);
    }

    public EmailLog sendEmailToDirectRecipients(String organizationId, String senderId,
                                                List<EmailRecipient> recipients, String subject,
                                                String body, List<MultipartFile> attachments) {

        Users sender = getValidatedSender(senderId);

        if (recipients == null || recipients.isEmpty()) throw new RuntimeException("No recipients provided");

        List<String> recipientEmails = recipients.stream().map(EmailRecipient::getEmail).collect(Collectors.toList());
        EmailLog emailLog = new EmailLog(organizationId, senderId, getSenderEmail(sender), recipientEmails, subject, body);

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (EmailRecipient recipient : recipients) {
            try {
                String name = (recipient.getFullName() != null && !recipient.getFullName().trim().isEmpty())
                        ? recipient.getFullName() : "Valued Client";
                String greeting = "Hello " + name + ",";
                sendEmail(sender, recipient.getEmail(), subject, greeting, body, attachments);
                successCount++;
            } catch (Exception e) {
                failedEmails.add(recipient.getEmail());
                System.err.println("Failed to send email to " + recipient.getEmail() + ": " + e.getMessage());
            }
        }

        return finalizeEmailLog(emailLog, failedEmails, successCount);
    }

    // ================================================================================
    //                    COMPANY EMAIL SENDING METHODS
    // ================================================================================

    public EmailLog sendEmailToCompanies(String organizationId, String senderId, List<String> companyIds,
                                         String subject, String body, List<MultipartFile> attachments) {

        Users sender = getValidatedSender(senderId);

        List<Company> companies = new ArrayList<>();
        for (String id : companyIds) {
            companyRepository.findById(id)
                    .filter(c -> c.getOrganizationId().equals(organizationId))
                    .ifPresent(companies::add);
        }

        if (companies.isEmpty()) throw new RuntimeException("No valid companies found for the provided IDs");

        List<String> recipientEmails = companies.stream().map(Company::getEmail).collect(Collectors.toList());
        EmailLog emailLog = new EmailLog(organizationId, senderId, getSenderEmail(sender), recipientEmails, subject, body);

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (Company company : companies) {
            try {
                String greeting = "Hello " + company.getCompanyName() + ",";
                sendEmail(sender, company.getEmail(), subject, greeting, body, attachments);
                successCount++;
            } catch (Exception e) {
                failedEmails.add(company.getEmail());
                System.err.println("Failed to send email to " + company.getEmail() + ": " + e.getMessage());
            }
        }

        return finalizeEmailLog(emailLog, failedEmails, successCount);
    }

    public EmailLog sendEmailToCompaniesViaExcel(String organizationId, String senderId,
                                                  List<EmailRecipient> recipients, String subject,
                                                  String body, List<MultipartFile> attachments) {
        return sendEmailToDirectRecipients(organizationId, senderId, recipients, subject, body, attachments);
    }

    // ================================================================================
    //                    CLIENT EMAIL SENDING METHODS
    // ================================================================================

    public EmailLog sendEmailToClients(String organizationId, String senderId, List<String> clientIds,
                                       String subject, String body, List<MultipartFile> attachments) {

        Users sender = getValidatedSender(senderId);

        List<Client> clients = new ArrayList<>();
        for (String id : clientIds) {
            clientRepository.findById(id)
                    .filter(c -> c.getOrganizationId().equals(organizationId))
                    .ifPresent(clients::add);
        }

        if (clients.isEmpty()) throw new RuntimeException("No valid clients found for the provided IDs");

        List<String> recipientEmails = clients.stream().map(Client::getEmail).collect(Collectors.toList());
        EmailLog emailLog = new EmailLog(organizationId, senderId, getSenderEmail(sender), recipientEmails, subject, body);

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (Client client : clients) {
            try {
                String greeting = "Hello " + client.getCompanyName() + ",";
                sendEmail(sender, client.getEmail(), subject, greeting, body, attachments);
                successCount++;
            } catch (Exception e) {
                failedEmails.add(client.getEmail());
                System.err.println("Failed to send email to " + client.getEmail() + ": " + e.getMessage());
            }
        }

        return finalizeEmailLog(emailLog, failedEmails, successCount);
    }

    public EmailLog sendEmailToClientsViaExcel(String organizationId, String senderId,
                                               List<EmailRecipient> recipients, String subject,
                                               String body, List<MultipartFile> attachments) {
        return sendEmailToDirectRecipients(organizationId, senderId, recipients, subject, body, attachments);
    }

    // ================================================================================
    //                    EMAIL LOG QUERY METHODS
    // ================================================================================

    public List<EmailLog> getEmailLogs(String organizationId) {
        return emailLogRepository.findByOrganizationId(organizationId);
    }

    public List<EmailLog> getEmailLogsByUser(String organizationId, String userId) {
        return emailLogRepository.findByOrganizationIdAndSenderId(organizationId, userId);
    }

    public List<EmailLog> getEmailLogsByStatus(String organizationId, EmailLog.EmailStatus status) {
        return emailLogRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    public Optional<EmailLog> getEmailLogById(String emailLogId) {
        return emailLogRepository.findById(emailLogId);
    }

    // ================================================================================
    //                    SMART EMAIL ROUTER — Google or Microsoft
    // ================================================================================

    /**
     * Central email dispatcher — checks defaultEmailProvider and routes accordingly.
     */
    private void sendEmail(Users sender, String toEmail, String subject,
                           String greeting, String body,
                           List<MultipartFile> attachments) throws Exception {

        Users.EmailProvider provider = sender.getDefaultEmailProvider();

        if (provider == Users.EmailProvider.GOOGLE) {
            Gmail gmailService = buildGmailService(sender);
            sendViaGmailApi(gmailService, sender.getGoogleEmail(),
                    toEmail, subject, greeting, body, attachments);

        } else if (provider == Users.EmailProvider.MICROSOFT) {
            sendViaMicrosoftApi(sender.getMicrosoftAccessToken(),
                    toEmail, subject, greeting, body, attachments);

        } else {
            throw new RuntimeException(
                "No default email provider set. " +
                "Please connect Google or Microsoft account and set a default provider.");
        }
    }

    // ================================================================================
    //                    GOOGLE GMAIL API SENDER
    // ================================================================================

    private Gmail buildGmailService(Users sender) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(sender.getGoogleAccessToken());

            return new Gmail.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("ATSCIRCLE")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Gmail service: " + e.getMessage(), e);
        }
    }

    private void sendViaGmailApi(Gmail gmailService, String fromEmail, String toEmail,
                                  String subject, String greeting, String body,
                                  List<MultipartFile> attachments) throws MessagingException, IOException {

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
        email.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        // HTML body part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildHtmlEmail(greeting, body), "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        // Attachments
        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    String originalFileName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "attachment";
                    String contentType = file.getContentType() != null
                            ? file.getContentType() : "application/octet-stream";
                    DataSource dataSource = new ByteArrayDataSource(file.getBytes(), contentType);
                    attachmentPart.setDataHandler(new DataHandler(dataSource));
                    attachmentPart.setFileName(originalFileName);
                    multipart.addBodyPart(attachmentPart);
                }
            }
        }

        email.setContent(multipart);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

        Message gmailMessage = new Message();
        gmailMessage.setRaw(encodedEmail);
        gmailService.users().messages().send("me", gmailMessage).execute();
    }

    // ================================================================================
    //                    MICROSOFT GRAPH API SENDER
    // ================================================================================

    private void sendViaMicrosoftApi(String accessToken, String toEmail,
                                      String subject, String greeting, String body,
                                      List<MultipartFile> attachments) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        String htmlContent = buildHtmlEmail(greeting, body);

        // Build toRecipients
        List<Map<String, Object>> toList = List.of(
            Map.of("emailAddress", Map.of("address", toEmail))
        );

        // Build message body
        Map<String, Object> messageBody = new LinkedHashMap<>();
        messageBody.put("subject", subject);
        messageBody.put("body", Map.of(
            "contentType", "HTML",
            "content", htmlContent
        ));
        messageBody.put("toRecipients", toList);

        // Add attachments if any
        if (attachments != null && !attachments.isEmpty()) {
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    String fileName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "attachment";
                    String encoded = Base64.getEncoder().encodeToString(file.getBytes());
                    attachmentList.add(Map.of(
                        "@odata.type", "#microsoft.graph.fileAttachment",
                        "name", fileName,
                        "contentType", file.getContentType() != null
                                ? file.getContentType() : "application/octet-stream",
                        "contentBytes", encoded
                    ));
                }
            }
            if (!attachmentList.isEmpty()) {
                messageBody.put("attachments", attachmentList);
            }
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
    }

    // ================================================================================
    //                    PRIVATE HELPER METHODS
    // ================================================================================

    /**
     * Validates sender has Google OR Microsoft connected based on defaultEmailProvider.
     */
    private Users getValidatedSender(String senderId) {
        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Users.EmailProvider provider = sender.getDefaultEmailProvider();

        if (provider == null || provider == Users.EmailProvider.NONE) {
            throw new RuntimeException(
                "No default email provider set. " +
                "Please connect Google or Microsoft account and set a default provider.");
        }

        if (provider == Users.EmailProvider.GOOGLE) {
            if (!sender.isGoogleConnected()
                    || sender.getGoogleAccessToken() == null
                    || sender.getGoogleEmail() == null
                    || sender.getGoogleEmail().trim().isEmpty()) {
                throw new RuntimeException(
                    "Google account not connected. Please connect your Google account first.");
            }
        }

        if (provider == Users.EmailProvider.MICROSOFT) {
            if (!sender.isMicrosoftConnected()
                    || sender.getMicrosoftAccessToken() == null
                    || sender.getMicrosoftEmail() == null
                    || sender.getMicrosoftEmail().trim().isEmpty()) {
                throw new RuntimeException(
                    "Microsoft account not connected. Please connect your Microsoft account first.");
            }
        }

        return sender;
    }

    /**
     * Returns the sender email based on defaultEmailProvider.
     */
    private String getSenderEmail(Users sender) {
        if (sender.getDefaultEmailProvider() == Users.EmailProvider.MICROSOFT) {
            return sender.getMicrosoftEmail();
        }
        return sender.getGoogleEmail();
    }

    private String buildHtmlEmail(String greeting, String body) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                "<h2>" + greeting + "</h2>" +
                "<div>" + body + "</div>" +
                "<br/>" +
                "<p>Best regards</p>" +
                "</body>" +
                "</html>";
    }

    private EmailLog finalizeEmailLog(EmailLog emailLog, List<String> failedEmails, int successCount) {
        emailLog.setSentAt(LocalDateTime.now());
        emailLog.setUpdatedAt(LocalDateTime.now());

        if (failedEmails.isEmpty()) {
            emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
        } else if (successCount > 0) {
            emailLog.setStatus(EmailLog.EmailStatus.PARTIAL_FAILED);
            emailLog.setFailedEmails(failedEmails);
        } else {
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setFailedEmails(failedEmails);
        }

        return emailLogRepository.save(emailLog);
    }
}