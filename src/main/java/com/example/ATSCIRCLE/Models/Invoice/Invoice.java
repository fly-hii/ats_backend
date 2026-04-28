package com.example.ATSCIRCLE.Models.Invoice;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Document(collection = "invoices")
public class Invoice {
    @Id
    private String id;
    
    @Indexed
    private String organizationId;
    
    // ---------- Invoice ----------
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String customerPO;
    
    // ---------- Supplier ----------
    private String legalBusinessName;
    private String gstNumber;
    private String supplierGstStatus;
    private Double defaultGstRate;
    
    // ---------- Supplier Address ----------
    private String streetLine1;
    private String streetLine2;
    private String city;
    private String state;
    private String country;
    private String zipcode;
    
    // ---------- Bank ----------
    private String accountNo;
    private String ifscCode;
    private String accountName;
    private String bankName;
    
    // ---------- Customer ----------
    private String customerName;
    private String customerGSTIN;
    private String sendToEmail;
    
    // ---------- ITEMS ----------
    private List<InvoiceItem> items;
    
    // ---------- PAYMENT ----------
    private String paymentTerms;
    private Boolean isTdsApplicable;
    private Double advanceReceived;
    private String preferredPaymentMethod;
    private String paymentLink;
    private String upiId;
    private String upiNote;
    private String upiQrCodeUrl;
    
    // ---------- NOTES ----------
    private String additionalNotes;
    private String authorisedSignatoryName;
    private String designation;
    
    // ---------- TOTALS ----------
    private Double subTotal;
    private Double gstAmount;
    private Double tdsAmount;
    private Double netPayable;
    
    // ---------- REMINDERS ----------
    private List<LocalDate> reminders;
    
    // ✅ NEW FIELDS ----------
    private String ewayBillNumber;      // E-Way Bill Number
    private String arnNumber;           // ARN (Acknowledgement Reference Number)
    private String fssaiNumber;         // FSSAI License Number
    private String packingSlipNumber;   // Packing Slip Number
    private String recordPaymentDetails; // Payment recording details/notes
    
    // ---------- METADATA ----------
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Invoice() {
        this.items = new ArrayList<>();
        this.reminders = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Invoice(String id, String organizationId, String invoiceNumber, LocalDate invoiceDate, 
                   String customerPO, String legalBusinessName, String gstNumber, String supplierGstStatus, 
                   Double defaultGstRate, String streetLine1, String streetLine2, String city, String state, 
                   String country, String zipcode, String accountNo, String ifscCode, String accountName, 
                   String bankName, String customerName, String customerGSTIN, String sendToEmail,
                   List<InvoiceItem> items, String paymentTerms, Boolean isTdsApplicable, Double advanceReceived, 
                   String preferredPaymentMethod, String paymentLink, String upiId, String upiNote, 
                   String upiQrCodeUrl, String additionalNotes, String authorisedSignatoryName, 
                   String designation, Double subTotal, Double gstAmount, Double tdsAmount, 
                   Double netPayable, List<LocalDate> reminders, String eWayBillNumber, String arnNumber,
                   String fssaiNumber, String packingSlipNumber, String recordPaymentDetails,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.customerPO = customerPO;
        this.legalBusinessName = legalBusinessName;
        this.gstNumber = gstNumber;
        this.supplierGstStatus = supplierGstStatus;
        this.defaultGstRate = defaultGstRate;
        this.streetLine1 = streetLine1;
        this.streetLine2 = streetLine2;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipcode = zipcode;
        this.accountNo = accountNo;
        this.ifscCode = ifscCode;
        this.accountName = accountName;
        this.bankName = bankName;
        this.customerName = customerName;
        this.customerGSTIN = customerGSTIN;
        this.sendToEmail = sendToEmail;
        this.items = items;
        this.paymentTerms = paymentTerms;
        this.isTdsApplicable = isTdsApplicable;
        this.advanceReceived = advanceReceived;
        this.preferredPaymentMethod = preferredPaymentMethod;
        this.paymentLink = paymentLink;
        this.upiId = upiId;
        this.upiNote = upiNote;
        this.upiQrCodeUrl = upiQrCodeUrl;
        this.additionalNotes = additionalNotes;
        this.authorisedSignatoryName = authorisedSignatoryName;
        this.designation = designation;
        this.subTotal = subTotal;
        this.gstAmount = gstAmount;
        this.tdsAmount = tdsAmount;
        this.netPayable = netPayable;
        this.reminders = reminders;
        this.ewayBillNumber = eWayBillNumber;
        this.arnNumber = arnNumber;
        this.fssaiNumber = fssaiNumber;
        this.packingSlipNumber = packingSlipNumber;
        this.recordPaymentDetails = recordPaymentDetails;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void generateInvoiceNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = generateRandomString(4);
        this.invoiceNumber = "INV-" + date + "-" + randomPart;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Getters
    public String getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public String getCustomerPO() { return customerPO; }
    public String getLegalBusinessName() { return legalBusinessName; }
    public String getGstNumber() { return gstNumber; }
    public String getSupplierGstStatus() { return supplierGstStatus; }
    public Double getDefaultGstRate() { return defaultGstRate; }
    public String getStreetLine1() { return streetLine1; }
    public String getStreetLine2() { return streetLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getZipcode() { return zipcode; }
    public String getAccountNo() { return accountNo; }
    public String getIfscCode() { return ifscCode; }
    public String getAccountName() { return accountName; }
    public String getBankName() { return bankName; }
    public String getCustomerName() { return customerName; }
    public String getCustomerGSTIN() { return customerGSTIN; }
    public String getSendToEmail() { return sendToEmail; }
    public List<InvoiceItem> getItems() { return items; }
    public String getPaymentTerms() { return paymentTerms; }
    public Boolean getIsTdsApplicable() { return isTdsApplicable; }
    public Double getAdvanceReceived() { return advanceReceived; }
    public String getPreferredPaymentMethod() { return preferredPaymentMethod; }
    public String getPaymentLink() { return paymentLink; }
    public String getUpiId() { return upiId; }
    public String getUpiNote() { return upiNote; }
    public String getUpiQrCodeUrl() { return upiQrCodeUrl; }
    public String getAdditionalNotes() { return additionalNotes; }
    public String getAuthorisedSignatoryName() { return authorisedSignatoryName; }
    public String getDesignation() { return designation; }
    public Double getSubTotal() { return subTotal; }
    public Double getGstAmount() { return gstAmount; }
    public Double getTdsAmount() { return tdsAmount; }
    public Double getNetPayable() { return netPayable; }
    public List<LocalDate> getReminders() { return reminders; }
    public String getEwayBillNumber() { return ewayBillNumber; }
    public String getArnNumber() { return arnNumber; }
    public String getFssaiNumber() { return fssaiNumber; }
    public String getPackingSlipNumber() { return packingSlipNumber; }
    public String getRecordPaymentDetails() { return recordPaymentDetails; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public void setCustomerPO(String customerPO) { this.customerPO = customerPO; }
    public void setLegalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
    public void setSupplierGstStatus(String supplierGstStatus) { this.supplierGstStatus = supplierGstStatus; }
    public void setDefaultGstRate(Double defaultGstRate) { this.defaultGstRate = defaultGstRate; }
    public void setStreetLine1(String streetLine1) { this.streetLine1 = streetLine1; }
    public void setStreetLine2(String streetLine2) { this.streetLine2 = streetLine2; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setCountry(String country) { this.country = country; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerGSTIN(String customerGSTIN) { this.customerGSTIN = customerGSTIN; }
    public void setSendToEmail(String sendToEmail) { this.sendToEmail = sendToEmail; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public void setIsTdsApplicable(Boolean isTdsApplicable) { this.isTdsApplicable = isTdsApplicable; }
    public void setAdvanceReceived(Double advanceReceived) { this.advanceReceived = advanceReceived; }
    public void setPreferredPaymentMethod(String preferredPaymentMethod) { this.preferredPaymentMethod = preferredPaymentMethod; }
    public void setPaymentLink(String paymentLink) { this.paymentLink = paymentLink; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public void setUpiNote(String upiNote) { this.upiNote = upiNote; }
    public void setUpiQrCodeUrl(String upiQrCodeUrl) { this.upiQrCodeUrl = upiQrCodeUrl; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public void setAuthorisedSignatoryName(String authorisedSignatoryName) { this.authorisedSignatoryName = authorisedSignatoryName; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }
    public void setGstAmount(Double gstAmount) { this.gstAmount = gstAmount; }
    public void setTdsAmount(Double tdsAmount) { this.tdsAmount = tdsAmount; }
    public void setNetPayable(Double netPayable) { this.netPayable = netPayable; }
    public void setReminders(List<LocalDate> reminders) { this.reminders = reminders; }
    public void setEwayBillNumber(String eWayBillNumber) { this.ewayBillNumber = eWayBillNumber; }
    public void setArnNumber(String arnNumber) { this.arnNumber = arnNumber; }
    public void setFssaiNumber(String fssaiNumber) { this.fssaiNumber = fssaiNumber; }
    public void setPackingSlipNumber(String packingSlipNumber) { this.packingSlipNumber = packingSlipNumber; }
    public void setRecordPaymentDetails(String recordPaymentDetails) { this.recordPaymentDetails = recordPaymentDetails; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}