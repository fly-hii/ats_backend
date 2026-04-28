// ==================== MODEL 2: OrganizationFieldConfig.java ====================
package com.example.ATSCIRCLE.Models.ATS;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "organization_field_configs")
public class OrganizationFieldConfig {

    @Id
    private String id;
    
    private String organizationId;
    private List<String> selectedFields; // Field names that organization wants to use
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructor
    public OrganizationFieldConfig() {
        this.selectedFields = new ArrayList<>();
    }

    public OrganizationFieldConfig(String organizationId, List<String> selectedFields) {
        this.organizationId = organizationId;
        this.selectedFields = selectedFields != null ? selectedFields : new ArrayList<>();
    }

    // Helper Methods
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.selectedFields == null) {
            this.selectedFields = new ArrayList<>();
        }
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public List<String> getSelectedFields() { return selectedFields; }
    public void setSelectedFields(List<String> selectedFields) { this.selectedFields = selectedFields; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}