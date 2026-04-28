
package com.example.ATSCIRCLE.Models.ATS;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "client_shareable_links")
public class ClientShareableLink {

    @Id
    private String id;
    
    private String token; // Unique secure token for the link
    private String organizationId;
    private String clientId;
    private String jobId;
    private String clientEmail;
    
    private List<String> applicationIds; // List of application IDs shared with this link
    private List<String> timeSlots;      // Time slots provided by client
    private String clientTimeZone;       // Client's timezone (e.g., "Asia/Kolkata")
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastAccessedAt;
    
    private Boolean isExpired;
    private Boolean isRevoked; // Manual revocation by admin
    private Integer accessCount; // Track how many times link was accessed
    
    private String createdBy;
    
    // Constructor
    public ClientShareableLink() {
        this.isExpired = false;
        this.isRevoked = false;
        this.accessCount = 0;
    }

    // Helper Methods
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        // Default expiry: 24 hours from creation
        this.expiresAt = LocalDateTime.now().plusHours(24);
        this.token = generateSecureToken();
        this.isExpired = false;
        this.isRevoked = false;
        this.accessCount = 0;
    }
    
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "");
    }
    
    public boolean isLinkValid() {
        if (this.isRevoked) {
            return false;
        }
        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            this.isExpired = true;
            return false;
        }
        return true;
    }
    
    public void incrementAccessCount() {
        if (this.accessCount == null) {
            this.accessCount = 0;
        }
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void revokeLink() {
        this.isRevoked = true;
    }
    
    public void extendExpiry(int hours) {
        this.expiresAt = this.expiresAt.plusHours(hours);
        this.isExpired = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    
    public List<String> getApplicationIds() { return applicationIds; }
    public void setApplicationIds(List<String> applicationIds) { this.applicationIds = applicationIds; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }
    
    public Boolean getIsRevoked() { return isRevoked; }
    public void setIsRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; }
    
    public Integer getAccessCount() { return accessCount; }
    public void setAccessCount(Integer accessCount) { this.accessCount = accessCount; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public List<String> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<String> timeSlots) { this.timeSlots = timeSlots; }
    
    public String getClientTimeZone() { return clientTimeZone; }
    public void setClientTimeZone(String clientTimeZone) { this.clientTimeZone = clientTimeZone; }
}