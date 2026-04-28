package com.example.ATSCIRCLE.Controller.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.ItemMaster;
import com.example.ATSCIRCLE.Service.Invoice.ItemMasterService;
import com.example.ATSCIRCLE.Service.Invoice.S3UploadService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.example.ATSCIRCLE.security.JwtService;

@RestController
@RequestMapping("/api/items")
public class ItemMasterController {

    @Autowired
    private ItemMasterService itemMasterService;
    
    @Autowired
    private S3UploadService s3UploadService;
    
    @Autowired
    private JwtService jwtService;

    // Helper method to extract organization context from JWT
    private Map<String, String> extractUserContext(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        String organizationId = jwtService.extractOrganizationId(token);
        String type = jwtService.extractType(token);
        String userId = jwtService.extractId(token);
        
        return Map.of(
            "organizationId", organizationId,
            "type", type,
            "userId", userId
        );
    }

    /**
     * Create a single item with optional image upload
     * POST /api/items
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItem(
            Authentication authentication,
            @RequestParam("itemName") String itemName,
            @RequestParam("hsnOrSac") String hsnOrSac,
            @RequestParam(value = "itemDescription", required = false) String itemDescription,
            @RequestParam(value = "itemImage", required = false) MultipartFile itemImage) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            if (itemName == null || hsnOrSac == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "itemName and hsnOrSac are required"));
            }
            
            // Upload image to S3 if provided
            String imageUrl = null;
            if (itemImage != null && !itemImage.isEmpty()) {
                imageUrl = s3UploadService.uploadFile(itemImage, organizationId, "item-images");
            }
            
            ItemMaster item = itemMasterService.createItem(
                organizationId, itemName, itemDescription, hsnOrSac, imageUrl);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Item created successfully",
                "item", item
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create item: " + e.getMessage()));
        }
    }

    /**
     * Get all items for the authenticated organization
     * GET /api/items
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @GetMapping
    public ResponseEntity<?> getAllItems(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<ItemMaster> items = itemMasterService.getAllItems(organizationId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Items retrieved successfully",
                "count", items.size(),
                "items", items
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve items: " + e.getMessage()));
        }
    }

    /**
     * Get item by ID
     * GET /api/items/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(
            Authentication authentication,
            @PathVariable String id) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            return itemMasterService.getItemById(id)
                .map(item -> {
                    // Verify the item belongs to the user's organization
                    if (!item.getOrganizationId().equals(organizationId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied to this item"));
                    }
                    return ResponseEntity.ok(Map.of(
                        "message", "Item found",
                        "item", item
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found with id: " + id)));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve item: " + e.getMessage()));
        }
    }

    /**
     * Search items by name or HSN
     * GET /api/items/search?name={searchTerm}&hsn={hsn}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String hsn) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<ItemMaster> items;
            
            if (name != null && !name.isEmpty()) {
                items = itemMasterService.searchByName(organizationId, name);
            } else if (hsn != null && !hsn.isEmpty()) {
                items = itemMasterService.searchByHsn(organizationId, hsn);
            } else {
                items = itemMasterService.getAllItems(organizationId);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Search completed",
                "count", items.size(),
                "items", items
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Search failed: " + e.getMessage()));
        }
    }

     /**
     * Update an item with optional new image (Partial Update Support)
     * PUT /api/items/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateItem(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(value = "itemName", required = false) String itemName,
            @RequestParam(value = "hsnOrSac", required = false) String hsnOrSac,
            @RequestParam(value = "itemDescription", required = false) String itemDescription,
            @RequestParam(value = "itemImage", required = false) MultipartFile itemImage,
            @RequestParam(value = "removeImage", required = false, defaultValue = "false") Boolean removeImage) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            // Verify the item belongs to the user's organization
            return itemMasterService.getItemById(id)
                .map(existingItem -> {
                    if (!existingItem.getOrganizationId().equals(organizationId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied to this item"));
                    }
                    
                    try {
                        // Upload new image to S3 if provided
                        String imageUrl = null;
                        if (itemImage != null && !itemImage.isEmpty()) {
                            imageUrl = s3UploadService.uploadFile(itemImage, organizationId, "item-images");
                        } else if (removeImage) {
                            imageUrl = ""; // Empty string to clear the image
                        }
                        
                        ItemMaster item = itemMasterService.updateItemPartial(
                            id, itemName, itemDescription, hsnOrSac, imageUrl, removeImage);
                        
                        return ResponseEntity.ok(Map.of(
                            "message", "Item updated successfully",
                            "item", item
                        ));
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to update item: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found with id: " + id)));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update item: " + e.getMessage()));
        }
    }


    /**
     * Delete an item
     * DELETE /api/items/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(
            Authentication authentication,
            @PathVariable String id) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            // Verify the item belongs to the user's organization
            return itemMasterService.getItemById(id)
                .map(item -> {
                    if (!item.getOrganizationId().equals(organizationId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied to this item"));
                    }
                    
                    itemMasterService.deleteItem(id);
                    return ResponseEntity.ok(Map.of(
                        "message", "Item deleted successfully",
                        "id", id
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Item not found with id: " + id)));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete item: " + e.getMessage()));
        }
    }

    /**
     * Bulk upload items from Excel
     * POST /api/items/bulk-upload
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
            }
            
            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only .xlsx files are supported"));
            }
            
            List<ItemMaster> items = itemMasterService.bulkUploadFromExcel(organizationId, file);
            
            return ResponseEntity.ok(Map.of(
                "message", "Items uploaded successfully",
                "count", items.size(),
                "items", items
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Bulk upload failed: " + e.getMessage()));
        }
    }

    /**
     * Export items to Excel
     * GET /api/items/export
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @GetMapping("/export")
    public ResponseEntity<?> exportToExcel(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            byte[] excelData = itemMasterService.exportToExcel(organizationId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "items_" + organizationId + ".xlsx");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Export failed: " + e.getMessage()));
        }
    }

    /**
     * Get Excel template for bulk upload
     * GET /api/items/template
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    @GetMapping("/template")
    public ResponseEntity<?> downloadTemplate() {
        try {
            byte[] templateData = itemMasterService.generateExcelTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "items_template.xlsx");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(templateData);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate template: " + e.getMessage()));
        }
    }
}