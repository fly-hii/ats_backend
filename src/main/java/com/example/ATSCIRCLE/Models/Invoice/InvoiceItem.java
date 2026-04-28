package com.example.ATSCIRCLE.Models.Invoice;

public class InvoiceItem {
    private String itemName;
     private String itemDescription;  // ✅ NEW: Item description
    private String hsnSac;
    private String itemImageUrl;     // ✅ NEW: S3 image URL
    private Integer quantity;
    private Double rate;
    private Double amount;
    private Double gstPercent;

    public InvoiceItem() {}

    public InvoiceItem(String itemName, String itemDescription,String hsnSac,String itemImageUrl, Integer quantity, 
                       Double rate, Double amount, Double gstPercent) {
        this.itemName = itemName;
this.itemDescription=itemDescription;
        this.hsnSac = hsnSac;
        this.itemImageUrl=itemImageUrl;
        this.quantity = quantity;
        this.rate = rate;
        this.amount = amount;
        this.gstPercent = gstPercent;
    }

    // Getters
    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public String getHsnSac() { return hsnSac; }
    public String getItemImageUrl() { return itemImageUrl; }
    public Integer getQuantity() { return quantity; }
    public Double getRate() { return rate; }
    public Double getAmount() { return amount; }
    public Double getGstPercent() { return gstPercent; }

    // Setters
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    public void setHsnSac(String hsnSac) { this.hsnSac = hsnSac; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setRate(Double rate) { this.rate = rate; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setGstPercent(Double gstPercent) { this.gstPercent = gstPercent; }
}