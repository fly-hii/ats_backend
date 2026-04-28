package com.example.ATSCIRCLE.Models.Sales;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "contacts")
public class Contact {

    @Id
    private String id;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    private String email;

    @Field("phone")
    private String phone;

    @Field("job_title")
    private String jobTitle;

    @Field("linkedin_url")
    private String linkedinUrl;

    @Field("company_id")
    private String companyId;

    @Field("company_name")
    private String companyName;

    // -------------------------------
    // NEW FIELDS ADDED
    // -------------------------------

    @Field("department")
    private Department department;

    @Field("lead_status")
    private LeadStatus leadStatus;

    @Field("lead_source")
    private LeadSource leadSource;

    // -------------------------------

    @Field("notes")
    private String notes;

    // ORG RELATED FIELDS
    @Field("organization_id")
    private String organizationId;

    @Field("created_by")
    private String createdBy;

    @Field("assigned_to")
    private String assignedTo;

    @Field("is_shared")
    private boolean isShared = false;

    // Metadata
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Field("last_activity")
    private LocalDateTime lastActivity = LocalDateTime.now();

    @Field("do_not_call")
    private boolean doNotCall = false;

    @Field("do_not_email")
    private boolean doNotEmail = false;


    // ================================
    // ENUM DEFINITIONS
    // ================================

    public enum Department {
        EXECUTIVE_LEADERSHIP,
        HUMAN_RESOURCES,
        TALENT_ACQUISITION,
        SALES,
        MARKETING,
        CUSTOMER_SUPPORT,
        FINANCE,
        LEGAL,
        IT,
        ENGINEERING,
        PRODUCT_MANAGEMENT,
        DESIGN_UX,
        OPERATIONS,
        BUSINESS_DEVELOPMENT,
        PROCUREMENT,
        MANUFACTURING,
        LOGISTICS,
        QUALITY_ASSURANCE,
        RESEARCH_DEVELOPMENT,
        TRAINING_LD,
        SECURITY_RISK,
        PR_COMMUNICATIONS,
        FACILITIES_MANAGEMENT,
        CONSULTING,
        HEALTHCARE,
        EDUCATION,
        DATA_ANALYTICS,
        INVESTOR_RELATIONS,
        CORPORATE_STRATEGY,
        OTHER
    }

    public enum LeadStatus {
        NEW,
        PROSPECT,
        CONTACTED,
        QUALIFIED_LEAD,
        UNQUALIFIED_LEAD,
        NURTURING,
        MEETING_SCHEDULED,
        PROPOSAL_SENT,
        NEGOTIATION,
        CONTRACT_SENT,
        WON_CUSTOMER,
        LOST,
        ON_HOLD,
        OTHER
    }

    public enum LeadSource {
        GOOGLE_ADS,
        LINKEDIN_ADS,
        FACEBOOK_ADS,
        INSTAGRAM_ADS,
        WHATSAPP_ADS,
        TELEGRAM_ADS,
        CONTACT_FORM,
        EMAIL_CAMPAIGN,
        COLD_CALLING,
        REFERRAL,
        EVENTS,
        TRADE_SHOWS,
        PARTNERSHIP,
        ORGANIC_SEARCH,
        PAPER_ADS,
        OTHER
    }

    // ================================
    // CONSTRUCTOR
    // ================================

    public Contact() {}

    // ================================
    // GETTERS & SETTERS
    // ================================

    public String getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLinkedInUrl() { return linkedinUrl; }
    public void setLinkedInUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) {
        this.companyId = companyId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        this.updatedAt = LocalDateTime.now();
    }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) {
        this.department = department;
        this.updatedAt = LocalDateTime.now();
    }

    public LeadStatus getLeadStatus() { return leadStatus; }
    public void setLeadStatus(LeadStatus leadStatus) {
        this.leadStatus = leadStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public LeadSource getLeadSource() { return leadSource; }
    public void setLeadSource(LeadSource leadSource) {
        this.leadSource = leadSource;
        this.updatedAt = LocalDateTime.now();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) {
        isShared = shared;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isDoNotCall() { return doNotCall; }
    public void setDoNotCall(boolean doNotCall) {
        this.doNotCall = doNotCall;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDoNotEmail() { return doNotEmail; }
    public void setDoNotEmail(boolean doNotEmail) {
        this.doNotEmail = doNotEmail;
        this.updatedAt = LocalDateTime.now();
    }
}
