package com.example.ATSCIRCLE.Models.Sales;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.ATSCIRCLE.Models.Sales.SalesEnums.AnnualRevenue;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.CompanyStage;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.CurrencyType;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.EmployeeCount;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.Industry;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LeadSource;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LeadStatus;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LegalType;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.PaymentTerms;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.RelationshipType;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.TaxIdType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Import shared enums
import static com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;

@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    // -------- Fields from Company (all existing fields) --------
    private String companyName;
    private String websiteUrl;
    private Industry industry;
    private RelationshipType relationshipType;
    private LeadStatus leadStatus;
    private LeadSource leadSource;
    private String linkedInUrl;
    private String createdBy;

    private CompanyStage companyStage;
    private LegalType legalType;
    private EmployeeCount employeeCount;
    private AnnualRevenue annualRevenue;
    private String primaryContact;
    private String jobTitle;
    private String email;
    private String phone;
    private String contactOwner;

    // ✅ Use shared Address class
    private Address billingAddress;
    private Address shippingAddress;
    private boolean sameAsBilling;

    private TaxIdType taxIdType = TaxIdType.GST;
    private String taxIdNumber;
    private String taxStatus;
    private PaymentTerms paymentTerms;
    private CurrencyType currency;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastActivity = LocalDateTime.now();
    private boolean doNotCall = false;
    private boolean doNotEmail = false;

    // -------- NEW Client-Specific Fields --------
    private String role = "CLIENT";
    private String clientCompanyLogoUrl;
    
    // Hiring Manager / Primary POC details
    private String hiringManager;
    private String pocName;
    private String pocJobTitle;
    private String pocEmail;
    private String pocPhone;
    
    // Agreements & Documents (S3 URLs)
    private String primaryAgreementUrl;
    private List<String> supportingDocumentUrls = new ArrayList<>();
    private String workOrderUrl;
    
    // Migration metadata
    private String migratedFromCompanyId;
    private LocalDateTime migratedAt;

    private String organizationId;

    // -------- Constructors --------
    public Client() {}

    // -------- Getters & Setters --------
    public String getId() { return id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public Industry getIndustry() { return industry; }
    public void setIndustry(Industry industry) { this.industry = industry; }

    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }

    public LeadStatus getLeadStatus() { return leadStatus; }
    public void setLeadStatus(LeadStatus leadStatus) { this.leadStatus = leadStatus; }

    public LeadSource getLeadSource() { return leadSource; }
    public void setLeadSource(LeadSource leadSource) { this.leadSource = leadSource; }

    public String getLinkedInUrl() { return linkedInUrl; }
    public void setLinkedInUrl(String linkedInUrl) { this.linkedInUrl = linkedInUrl; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public CompanyStage getCompanyStage() { return companyStage; }
    public void setCompanyStage(CompanyStage companyStage) { this.companyStage = companyStage; }

    public LegalType getLegalType() { return legalType; }
    public void setLegalType(LegalType legalType) { this.legalType = legalType; }

    public EmployeeCount getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(EmployeeCount employeeCount) { this.employeeCount = employeeCount; }

    public AnnualRevenue getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(AnnualRevenue annualRevenue) { this.annualRevenue = annualRevenue; }

    public String getPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(String primaryContact) { this.primaryContact = primaryContact; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getContactOwner() { return contactOwner; }
    public void setContactOwner(String contactOwner) { this.contactOwner = contactOwner; }

    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public boolean isSameAsBilling() { return sameAsBilling; }
    public void setSameAsBilling(boolean sameAsBilling) { this.sameAsBilling = sameAsBilling; }

    public String taxStatus(){return taxStatus;}
    public void settaxStatus(String taxStatus) { this.taxStatus = taxStatus; }

     public TaxIdType getTaxIdType() { return taxIdType; }
    public void setTaxIdType(TaxIdType taxIdType) { this.taxIdType = taxIdType; }

    public String getTaxIdNumber() { return taxIdNumber; }
    public void setTaxIdNumber(String taxIdNumber) { this.taxIdNumber = taxIdNumber; }

    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }

    public CurrencyType getCurrency() { return currency; }
    public void setCurrency(CurrencyType currency) { this.currency = currency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isDoNotCall() { return doNotCall; }
    public void setDoNotCall(boolean doNotCall) { this.doNotCall = doNotCall; }

    public boolean isDoNotEmail() { return doNotEmail; }
    public void setDoNotEmail(boolean doNotEmail) { this.doNotEmail = doNotEmail; }

    // NEW Client Fields Getters & Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getClientCompanyLogoUrl() { return clientCompanyLogoUrl; }
    public void setClientCompanyLogoUrl(String clientCompanyLogoUrl) { this.clientCompanyLogoUrl = clientCompanyLogoUrl; }

    public String getHiringManager() { return hiringManager; }
    public void setHiringManager(String hiringManager) { this.hiringManager = hiringManager; }

    public String getPocName() { return pocName; }
    public void setPocName(String pocName) { this.pocName = pocName; }

    public String getPocJobTitle() { return pocJobTitle; }
    public void setPocJobTitle(String pocJobTitle) { this.pocJobTitle = pocJobTitle; }

    public String getPocEmail() { return pocEmail; }
    public void setPocEmail(String pocEmail) { this.pocEmail = pocEmail; }

    public String getPocPhone() { return pocPhone; }
    public void setPocPhone(String pocPhone) { this.pocPhone = pocPhone; }

    public String getPrimaryAgreementUrl() { return primaryAgreementUrl; }
    public void setPrimaryAgreementUrl(String primaryAgreementUrl) { this.primaryAgreementUrl = primaryAgreementUrl; }

    public List<String> getSupportingDocumentUrls() { return supportingDocumentUrls; }
    public void setSupportingDocumentUrls(List<String> supportingDocumentUrls) { this.supportingDocumentUrls = supportingDocumentUrls; }

    public String getWorkOrderUrl() { return workOrderUrl; }
    public void setWorkOrderUrl(String workOrderUrl) { this.workOrderUrl = workOrderUrl; }

    public String getMigratedFromCompanyId() { return migratedFromCompanyId; }
    public void setMigratedFromCompanyId(String migratedFromCompanyId) { this.migratedFromCompanyId = migratedFromCompanyId; }

    public LocalDateTime getMigratedAt() { return migratedAt; }
    public void setMigratedAt(LocalDateTime migratedAt) { this.migratedAt = migratedAt; }

     public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}