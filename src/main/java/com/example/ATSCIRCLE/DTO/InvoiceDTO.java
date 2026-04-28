package com.example.ATSCIRCLE.DTO;

import java.time.LocalDate;
import java.util.List;

public class InvoiceDTO {
    // Invoice details
    // private String organizationId;
    // private String invoiceNumber;
    private LocalDate invoiceDate;
    private String customerPO;
    
    // Supplier details
    private String legalBusinessName;
    private String gstNumber;
    private String supplierGstStatus;
    private Double defaultGstRate;
    
    // Supplier address
    private String streetLine1;
    private String streetLine2;
    private String city;
    private String state;
    private String country;
    private String zipcode;
    
    // Bank details
    private String accountNo;
    private String ifscCode;
    private String accountName;
    private String bankName;
    
    // Customer details
    private String customerName;
    private String customerGSTIN;
    private String customerAddress;
    private String customerState;
    private String sendToEmail;  // NEW FIELD - Email to send invoice to
    
    // Items
    private List<InvoiceItemDTO> items;
     // ✅ NEW FIELDS ----------
    private String ewayBillNumber;      // E-Way Bill Number
    private String arnNumber;           // ARN (Acknowledgement Reference Number)
    private String fssaiNumber;         // FSSAI License Number
    private String packingSlipNumber;   // Packing Slip Number
    private String recordPaymentDetails; // Payment recording details/notes
    
    // Payment details
    private String paymentTerms;
    private Boolean isTdsApplicable;
    private Double customTdsRate;
    private Double advanceReceived;
    private String preferredPaymentMethod;
    private String paymentLink;
    private String upiId;
    private String upiNote;
    private String upiQrCodeUrl;
    
    // Notes
    private String additionalNotes;
    private String authorisedSignatoryName;
    private String designation;

    // ---------- REMINDERS ----------
    private List<LocalDate> reminders;
    
    // Reminder settings
    private Boolean sendOnInvoiceDate;
    private Boolean sendOnDueDate;
    private Boolean sendOnOverdue;


    // Inner class for invoice items
    public static class InvoiceItemDTO {
        private String itemName;
         private String itemDescription;  // ✅ NEW: Item description
        private String itemImageUrl;   
        private String hsnOrSac;
        private Integer quantity;
        private Double rate;
        private Double gstPercent;

        // Getters and setters
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public String getHsnOrSac() { return hsnOrSac; }
        public void setHsnOrSac(String hsnOrSac) { this.hsnOrSac = hsnOrSac; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getRate() { return rate; }
        public void setRate(Double rate) { this.rate = rate; }

        public Double getGstPercent() { return gstPercent; }
        public void setGstPercent(Double gstPercent) { this.gstPercent = gstPercent; }

        public String getItemDescription() { return itemDescription; }
        public String getItemImageUrl() { return itemImageUrl; }

         public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
         public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }
    }

    // Getters and setters
    // public String getOrganizationId() { return organizationId; }
    // public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    // public String getInvoiceNumber() { return invoiceNumber; }
    // public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getCustomerPO() { return customerPO; }
    public void setCustomerPO(String customerPO) { this.customerPO = customerPO; }

    public String getLegalBusinessName() { return legalBusinessName; }
    public void setLegalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public String getSupplierGstStatus() { return supplierGstStatus; }
    public void setSupplierGstStatus(String supplierGstStatus) { this.supplierGstStatus = supplierGstStatus; }

    public Double getDefaultGstRate() { return defaultGstRate; }
    public void setDefaultGstRate(Double defaultGstRate) { this.defaultGstRate = defaultGstRate; }

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

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerGSTIN() { return customerGSTIN; }
    public void setCustomerGSTIN(String customerGSTIN) { this.customerGSTIN = customerGSTIN; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerState() { return customerState; }
    public void setCustomerState(String customerState) { this.customerState = customerState; }

    public String getSendToEmail() { return sendToEmail; }
    public void setSendToEmail(String sendToEmail) { this.sendToEmail = sendToEmail; }

    public List<InvoiceItemDTO> getItems() { return items; }
    public void setItems(List<InvoiceItemDTO> items) { this.items = items; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public Boolean getIsTdsApplicable() { return isTdsApplicable; }
    public void setIsTdsApplicable(Boolean isTdsApplicable) { this.isTdsApplicable = isTdsApplicable; }

    public Double getCustomTdsRate() { return customTdsRate; }
    public void setCustomTdsRate(Double customTdsRate) { this.customTdsRate = customTdsRate; }

    public Double getAdvanceReceived() { return advanceReceived; }
    public void setAdvanceReceived(Double advanceReceived) { this.advanceReceived = advanceReceived; }

    public String getPreferredPaymentMethod() { return preferredPaymentMethod; }
    public void setPreferredPaymentMethod(String preferredPaymentMethod) { this.preferredPaymentMethod = preferredPaymentMethod; }

    public String getPaymentLink() { return paymentLink; }
    public void setPaymentLink(String paymentLink) { this.paymentLink = paymentLink; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getUpiNote() { return upiNote; }
    public void setUpiNote(String upiNote) { this.upiNote = upiNote; }

    public String getUpiQrCodeUrl() { return upiQrCodeUrl; }
    public void setUpiQrCodeUrl(String upiQrCodeUrl) { this.upiQrCodeUrl = upiQrCodeUrl; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public String getAuthorisedSignatoryName() { return authorisedSignatoryName; }
    public void setAuthorisedSignatoryName(String authorisedSignatoryName) { this.authorisedSignatoryName = authorisedSignatoryName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Boolean getSendOnInvoiceDate() { return sendOnInvoiceDate; }
    public void setSendOnInvoiceDate(Boolean sendOnInvoiceDate) { this.sendOnInvoiceDate = sendOnInvoiceDate; }

    public Boolean getSendOnDueDate() { return sendOnDueDate; }
    public void setSendOnDueDate(Boolean sendOnDueDate) { this.sendOnDueDate = sendOnDueDate; }

    public Boolean getSendOnOverdue() { return sendOnOverdue; }
    public void setSendOnOverdue(Boolean sendOnOverdue) { this.sendOnOverdue = sendOnOverdue; }
     public String getEwayBillNumber() { return ewayBillNumber; }
    public String getArnNumber() { return arnNumber; }
    public String getFssaiNumber() { return fssaiNumber; }
    public String getPackingSlipNumber() { return packingSlipNumber; }
    public String getRecordPaymentDetails() { return recordPaymentDetails; }
     public void setEwayBillNumber(String eWayBillNumber) { this.ewayBillNumber = eWayBillNumber; }
    public void setArnNumber(String arnNumber) { this.arnNumber = arnNumber; }
    public void setFssaiNumber(String fssaiNumber) { this.fssaiNumber = fssaiNumber; }
    public void setPackingSlipNumber(String packingSlipNumber) { this.packingSlipNumber = packingSlipNumber; }
    public void setRecordPaymentDetails(String recordPaymentDetails) { this.recordPaymentDetails = recordPaymentDetails; }

     public List<LocalDate> getReminders() {
        return reminders;
    }
     public void setReminders(List<LocalDate> reminders) {
        this.reminders = reminders;
    }
}