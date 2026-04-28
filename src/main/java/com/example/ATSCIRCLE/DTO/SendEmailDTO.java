package com.example.ATSCIRCLE.DTO;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class SendEmailDTO {

    private List<String> contactIds;  // Array of contact IDs
    private String subject;            // Email subject
    private String body;               // Email body
    private List<MultipartFile> attachments;  // Optional attachments

    // ================================
    // CONSTRUCTORS
    // ================================

    public SendEmailDTO() {
    }

    public SendEmailDTO(List<String> contactIds, String subject, String body) {
        this.contactIds = contactIds;
        this.subject = subject;
        this.body = body;
    }

    public SendEmailDTO(List<String> contactIds, String subject, String body, List<MultipartFile> attachments) {
        this.contactIds = contactIds;
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;
    }

    // ================================
    // GETTERS AND SETTERS
    // ================================

    public List<String> getContactIds() {
        return contactIds;
    }

    public void setContactIds(List<String> contactIds) {
        this.contactIds = contactIds;
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

    public List<MultipartFile> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "SendEmailDTO{" +
                "contactIds=" + contactIds +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", attachments=" + (attachments != null ? attachments.size() : 0) +
                '}';
    }
}
