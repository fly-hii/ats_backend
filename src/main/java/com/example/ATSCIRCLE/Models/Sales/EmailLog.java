package com.example.ATSCIRCLE.Models.Sales;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "email_logs")
public class EmailLog {

    @Id
    private String id;

    @Field("organization_id")
    private String organizationId;

    @Field("sender_id")
    private String senderId;

    @Field("sender_email")
    private String senderEmail;

    @Field("recipient_emails")
    private List<String> recipientEmails;

    @Field("subject")
    private String subject;

    @Field("body")
    private String body;

    @Field("attachment_urls")
    private List<String> attachmentUrls;

    @Field("status")
    private EmailStatus status;

    @Field("failed_emails")
    private List<String> failedEmails;

    @Field("failure_reason")
    private String failureReason;

    @Field("sent_at")
    private LocalDateTime sentAt;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ================================
    // ENUM DEFINITION
    // ================================

    public enum EmailStatus {
        PENDING,
        SUCCESS,
        PARTIAL_FAILED,
        FAILED
    }

    // ================================
    // CONSTRUCTORS
    // ================================

    public EmailLog() {
    }

    public EmailLog(String organizationId, String senderId, String senderEmail, 
                   List<String> recipientEmails, String subject, String body) {
        this.organizationId = organizationId;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.recipientEmails = recipientEmails;
        this.subject = subject;
        this.body = body;
        this.status = EmailStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ================================
    // GETTERS AND SETTERS
    // ================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public List<String> getRecipientEmails() {
        return recipientEmails;
    }

    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getAttachmentUrls() {
        return attachmentUrls;
    }

    public void setAttachmentUrls(List<String> attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public List<String> getFailedEmails() {
        return failedEmails;
    }

    public void setFailedEmails(List<String> failedEmails) {
        this.failedEmails = failedEmails;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
