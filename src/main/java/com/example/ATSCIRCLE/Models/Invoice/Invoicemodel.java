package com.example.ATSCIRCLE.Models.Invoice;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "Invoicemodel")
public class Invoicemodel {

    @Id
    private String id;

    private String organizationId;

    // ---------- Section 1: Client / Billing & Shipping ----------
    private String clientId;  // Reference to Client collection
    private String clientName;
    private String clientEmail;
    private String poNumber;
    private String clientGSTIN;
    private String gstStatus;
    private String paymentTerms;
    
    // Client Address (from Client model)
    private Address billingAddress;
    private Boolean sameAsBilling;
    private Address shippingAddress;

    // ---------- Section 2: Placement Details ----------
    private String candidateId;  // Reference to CandidateApplication
    private String candidateName;
    private String designation;
    private String placementType;
    private Double ctc;
    private LocalDate joiningDate;

    // ---------- Section 3: Fee Structure ----------
    private String feeModel;
    private Double feePercentage;
    private Double fixedAmount;
    private Double calculatedFee;

    private Integer guaranteeDays;
    private LocalDate guaranteeEndDate;

    private String ifCandidateLeaves;
    private String exitPenalty;

    // ---------- Section 4: Your Company Details (from Users table) ----------
    private String yourGSTIN;
    private String yourPan;
    private String accountNumber;
    private String ifscCode;
    
    // From Address (Your company address from Users table)
    private Address fromAddress;

    // ---------- Section 5: Compliance & Taxes ----------
    private Boolean isTDSApplicable;

    // Documents
    private List<AttachedDocument> documents = new ArrayList<>();

    private Boolean automaticPaymentReminders;
    private String sendInvoiceMailTo;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // ---------- Constructors ----------
    public Invoicemodel() {
        this.documents = new ArrayList<>();
    }

    // ---------- Embedded Classes ----------
    public static class Address {
        private String streetLine1;
        private String streetLine2;
        private String city;
        private String state;
        private String country;
        private String zipcode;

        public Address() {}

        public Address(String streetLine1, String streetLine2, String city, String state, String country, String zipcode) {
            this.streetLine1 = streetLine1;
            this.streetLine2 = streetLine2;
            this.city = city;
            this.state = state;
            this.country = country;
            this.zipcode = zipcode;
        }

        // Getters & Setters
        public String getStreetLine1() { return streetLine1; }
        public void setStreetLine1(String streetLine1) { this.streetLine1 = streetLine1; }

        public String getStreetLine2() { return streetLine2; }
        public void setStreetLine2(String streetLine2) { this.streetLine2 = streetLine2; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getZipcode() { return zipcode; }
        public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    }

    public static class AttachedDocument {
        private String link;
        private String documentType;

        public AttachedDocument() {}

        public AttachedDocument(String link, String documentType) {
            this.link = link;
            this.documentType = documentType;
        }

        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }

        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
    }

    // ---------- Helper Methods ----------
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- Getters & Setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public String getClientGSTIN() { return clientGSTIN; }
    public void setClientGSTIN(String clientGSTIN) { this.clientGSTIN = clientGSTIN; }

    public String getGstStatus() { return gstStatus; }
    public void setGstStatus(String gstStatus) { this.gstStatus = gstStatus; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public Address getBillingAddress() { return billingAddress; }
    public void setBillingAddress(Address billingAddress) { this.billingAddress = billingAddress; }

    public Boolean getSameAsBilling() { return sameAsBilling; }
    public void setSameAsBilling(Boolean sameAsBilling) { this.sameAsBilling = sameAsBilling; }

    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPlacementType() { return placementType; }
    public void setPlacementType(String placementType) { this.placementType = placementType; }

    public Double getCtc() { return ctc; }
    public void setCtc(Double ctc) { this.ctc = ctc; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public String getFeeModel() { return feeModel; }
    public void setFeeModel(String feeModel) { this.feeModel = feeModel; }

    public Double getFeePercentage() { return feePercentage; }
    public void setFeePercentage(Double feePercentage) { this.feePercentage = feePercentage; }

    public Double getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(Double fixedAmount) { this.fixedAmount = fixedAmount; }

    public Double getCalculatedFee() { return calculatedFee; }
    public void setCalculatedFee(Double calculatedFee) { this.calculatedFee = calculatedFee; }

    public Integer getGuaranteeDays() { return guaranteeDays; }
    public void setGuaranteeDays(Integer guaranteeDays) { this.guaranteeDays = guaranteeDays; }

    public LocalDate getGuaranteeEndDate() { return guaranteeEndDate; }
    public void setGuaranteeEndDate(LocalDate guaranteeEndDate) { this.guaranteeEndDate = guaranteeEndDate; }

    public String getIfCandidateLeaves() { return ifCandidateLeaves; }
    public void setIfCandidateLeaves(String ifCandidateLeaves) { this.ifCandidateLeaves = ifCandidateLeaves; }

    public String getExitPenalty() { return exitPenalty; }
    public void setExitPenalty(String exitPenalty) { this.exitPenalty = exitPenalty; }

    public String getYourGSTIN() { return yourGSTIN; }
    public void setYourGSTIN(String yourGSTIN) { this.yourGSTIN = yourGSTIN; }

    public String getYourPan() { return yourPan; }
    public void setYourPan(String yourPan) { this.yourPan = yourPan; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public Address getFromAddress() { return fromAddress; }
    public void setFromAddress(Address fromAddress) { this.fromAddress = fromAddress; }

    public Boolean getTDSApplicable() { return isTDSApplicable; }
    public void setTDSApplicable(Boolean TDSApplicable) { isTDSApplicable = TDSApplicable; }

    public List<AttachedDocument> getDocuments() { return documents; }
    public void setDocuments(List<AttachedDocument> documents) { this.documents = documents; }

    public Boolean getAutomaticPaymentReminders() { return automaticPaymentReminders; }
    public void setAutomaticPaymentReminders(Boolean automaticPaymentReminders) { this.automaticPaymentReminders = automaticPaymentReminders; }

    public String getSendInvoiceMailTo() { return sendInvoiceMailTo; }
    public void setSendInvoiceMailTo(String sendInvoiceMailTo) { this.sendInvoiceMailTo = sendInvoiceMailTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}