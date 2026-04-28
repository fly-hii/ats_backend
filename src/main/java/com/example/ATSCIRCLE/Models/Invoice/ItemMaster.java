package com.example.ATSCIRCLE.Models.Invoice;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "items")
@CompoundIndex(name = "org_item_hsn_unique", 
               def = "{'organizationId': 1, 'itemName': 1, 'hsnSac': 1}", 
               unique = true)
public class ItemMaster {
    @Id
    private String id;
    
    @Indexed
    private String organizationId;
    
    private String itemName;
    private String itemDescription;  // ✅ NEW: Item description
    private String hsnSac;
    private String itemImageUrl;     // ✅ NEW: S3 image URL
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public ItemMaster() {
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
    }

    public ItemMaster(String id, String organizationId, String itemName, String itemDescription,
                      String hsnSac, String itemImageUrl, LocalDateTime createdAt, 
                      LocalDateTime lastUsedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.hsnSac = hsnSac;
        this.itemImageUrl = itemImageUrl;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    // Getters
    public String getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public String getHsnSac() { return hsnSac; }
    public String getItemImageUrl() { return itemImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    public void setHsnSac(String hsnSac) { this.hsnSac = hsnSac; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}