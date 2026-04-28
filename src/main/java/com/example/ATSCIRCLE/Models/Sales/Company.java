package com.example.ATSCIRCLE.Models.Sales;

import org.springframework.data.annotation.Id; 
import org.springframework.data.mongodb.core.mapping.Document; 
import org.springframework.data.mongodb.core.mapping.Field; 

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;



 @Document(collection = "companies") 
 public class Company { 

@Id 
private String id; 
 
// -------- Main Form Fields -------- 
@Field("company_name") 
private String companyName; 
 
@Field("website_url") 
private String websiteUrl; 
 
private Industry industry; 
 
@Field("relationship_type") 
private RelationshipType relationshipType; 
 
@Field("lead_status") 
private LeadStatus leadStatus; 
 
@Field("lead_source") 
private LeadSource leadSource; 
 
@Field("linkedin_url") 
private String linkedInUrl; 
 
// -------- Add More Fields -------- 
@Field("company_stage") 
private CompanyStage companyStage; 
 
@Field("legal_type") 
private LegalType legalType; 
 
@Field("employee_count") 
private EmployeeCount employeeCount; 
 
@Field("annual_revenue") 
private AnnualRevenue annualRevenue; 
 
@Field("primary_contact") 
private String primaryContact; 
 
@Field("job_title") 
private String jobTitle; 
 
private String email; 
private String phone; 
 
@Field("contact_owner") 
private String contactOwner; 

@Field("contact_ids")
private List<String> contactIds = new ArrayList<>();
 
// -------- Address Fields -------- 
@Field("billing_address") 
private Address billingAddress; 
 
@Field("shipping_address") 
private Address shippingAddress; 
 
@Field("same_as_billing") 
private boolean sameAsBilling; 
 
// -------- Tax & Payment -------- 
@Field("tax_id_type") 
private TaxIdType taxIdType = TaxIdType.GST; 
 
@Field("tax_id_number") 
private String taxIdNumber; 
 
@Field("payment_terms") 
private PaymentTerms paymentTerms; 
 
private CurrencyType currency; 
@Field("notes")
    private String notes;
 
@Field("organization_id") 
private String organizationId;  // Which organization owns this company 
 
@Field("created_by") 
private String createdBy;       // Who created this company (userId/employeeId) 
 
@Field("assigned_to") 
private String assignedTo;      // Who is responsible for this company (can be null) 
 
@Field("is_shared") 
private boolean isShared = false;  // If true, all org members can view (read-only) 

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
 
// -------- Constructors -------- 
public Company() {} 
 
public Company(String companyName, String websiteUrl, Industry industry, 
               RelationshipType relationshipType, LeadStatus leadStatus, 
               LeadSource leadSource, String linkedInUrl, 
               CompanyStage companyStage, LegalType legalType, 
               EmployeeCount employeeCount, AnnualRevenue annualRevenue, 
               String primaryContact, String jobTitle, String email, String phone, 
               String contactOwner, Address billingAddress, Address shippingAddress, 
               boolean sameAsBilling, TaxIdType taxIdType, String taxIdNumber, 
               PaymentTerms paymentTerms, CurrencyType currency,String notes, 
               String organizationId, String createdBy, String assignedTo, boolean isShared) { 
 
    this.companyName = companyName; 
    this.websiteUrl = websiteUrl; 
    this.industry = industry; 
    this.relationshipType = relationshipType; 
    this.leadStatus = leadStatus; 
    this.leadSource = leadSource; 
    this.linkedInUrl = linkedInUrl; 
    this.companyStage = companyStage; 
    this.legalType = legalType; 
    this.employeeCount = employeeCount; 
    this.annualRevenue = annualRevenue; 
    this.primaryContact = primaryContact; 
    this.jobTitle = jobTitle; 
    this.email = email; 
    this.phone = phone; 
    this.contactOwner = contactOwner; 
    this.billingAddress = billingAddress; 
    this.shippingAddress = shippingAddress; 
    this.sameAsBilling = sameAsBilling; 
    this.taxIdType = taxIdType; 
    this.taxIdNumber = taxIdNumber; 
    this.paymentTerms = paymentTerms; 
    this.currency = currency; 
    this.notes=notes;
    this.organizationId = organizationId; 
    this.createdBy = createdBy; 
    this.assignedTo = assignedTo; 
    this.isShared = isShared; 
    this.createdAt = LocalDateTime.now(); 
    this.updatedAt = LocalDateTime.now(); 
    this.lastActivity = LocalDateTime.now(); 
} 
 
// -------- Getters & Setters -------- 
public String getId() { return id; } 
 
public String getCompanyName() { return companyName; } 
public void setCompanyName(String companyName) { 
    this.companyName = companyName; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getWebsiteUrl() { return websiteUrl; } 
public void setWebsiteUrl(String websiteUrl) { 
    this.websiteUrl = websiteUrl; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public Industry getIndustry() { return industry; } 
public void setIndustry(Industry industry) { 
    this.industry = industry; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public RelationshipType getRelationshipType() { return relationshipType; } 
public void setRelationshipType(RelationshipType relationshipType) { 
    this.relationshipType = relationshipType; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public LeadStatus getLeadStatus() { return leadStatus; } 
public void setLeadStatus(LeadStatus leadStatus) { 
    this.leadStatus = leadStatus; 
    this.updatedAt = LocalDateTime.now(); 
} 
 

 public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

public LeadSource getLeadSource() { return leadSource; } 
public void setLeadSource(LeadSource leadSource) { 
    this.leadSource = leadSource; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getLinkedInUrl() { return linkedInUrl; } 
public void setLinkedInUrl(String linkedInUrl) { 
    this.linkedInUrl = linkedInUrl; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public CompanyStage getCompanyStage() { return companyStage; } 
public void setCompanyStage(CompanyStage companyStage) { 
    this.companyStage = companyStage; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public LegalType getLegalType() { return legalType; } 
public void setLegalType(LegalType legalType) { 
    this.legalType = legalType; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public EmployeeCount getEmployeeCount() { return employeeCount; } 
public void setEmployeeCount(EmployeeCount employeeCount) { 
    this.employeeCount = employeeCount; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public AnnualRevenue getAnnualRevenue() { return annualRevenue; } 
public void setAnnualRevenue(AnnualRevenue annualRevenue) { 
    this.annualRevenue = annualRevenue; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getPrimaryContact() { return primaryContact; } 
public void setPrimaryContact(String primaryContact) { 
    this.primaryContact = primaryContact; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getJobTitle() { return jobTitle; } 
public void setJobTitle(String jobTitle) { 
    this.jobTitle = jobTitle; 
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
 
public String getContactOwner() { return contactOwner; } 
public void setContactOwner(String contactOwner) { 
    this.contactOwner = contactOwner; 
    this.updatedAt = LocalDateTime.now(); 
} 

public List<String> getContactIds() { return contactIds; }
public void setContactIds(List<String> contactIds) {
    this.contactIds = contactIds;
    this.updatedAt = LocalDateTime.now();
}

public void addContactId(String contactId) {
    if (this.contactIds == null) this.contactIds = new ArrayList<>();
    if (!this.contactIds.contains(contactId)) {
        this.contactIds.add(contactId);
        this.updatedAt = LocalDateTime.now();
    }
}

public void removeContactId(String contactId) {
    if (this.contactIds != null) {
        this.contactIds.remove(contactId);
        this.updatedAt = LocalDateTime.now();
    }
}
 
public Address getBillingAddress() { return billingAddress; } 
public void setBillingAddress(Address billingAddress) { 
    this.billingAddress = billingAddress; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public Address getShippingAddress() { return shippingAddress; } 
public void setShippingAddress(Address shippingAddress) { 
    this.shippingAddress = shippingAddress; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public boolean isSameAsBilling() { return sameAsBilling; } 
public void setSameAsBilling(boolean sameAsBilling) { 
    this.sameAsBilling = sameAsBilling; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public TaxIdType getTaxIdType() { return taxIdType; } 
public void setTaxIdType(TaxIdType taxIdType) { 
    this.taxIdType = taxIdType; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getTaxIdNumber() { return taxIdNumber; } 
public void setTaxIdNumber(String taxIdNumber) { 
    this.taxIdNumber = taxIdNumber; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public PaymentTerms getPaymentTerms() { return paymentTerms; } 
public void setPaymentTerms(PaymentTerms paymentTerms) { 
    this.paymentTerms = paymentTerms; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public CurrencyType getCurrency() { return currency; } 
public void setCurrency(CurrencyType currency) { 
    this.currency = currency; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
// 🔥 ACCESS CONTROL GETTERS & SETTERS 
public String getOrganizationId() { return organizationId; } 
public void setOrganizationId(String organizationId) { this.organizationId = organizationId; } 
 
public String getCreatedBy() { return createdBy; } 
public void setCreatedBy(String createdBy) { this.createdBy = createdBy; } 
 
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
 
// METADATA GETTERS & SETTERS 
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
