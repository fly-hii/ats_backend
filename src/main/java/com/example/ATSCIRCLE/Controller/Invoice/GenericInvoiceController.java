package com.example.ATSCIRCLE.Controller.Invoice;

import com.example.ATSCIRCLE.DTO.InvoiceDTO;
import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import com.example.ATSCIRCLE.Service.Invoice.GenericInvoiceservice;
import com.example.ATSCIRCLE.Service.Invoice.S3UploadService;
import com.example.ATSCIRCLE.Service.Invoice.ItemMasterService;
import com.example.ATSCIRCLE.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.ATSCIRCLE.Service.Invoice.GenericInvoiceReminderScheduler;
import com.example.ATSCIRCLE.Models.Invoice.ItemMaster;

@RestController
@RequestMapping("/api/invoices")
public class GenericInvoiceController {

    @Autowired
    private GenericInvoiceservice invoiceService;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private GenericInvoiceReminderScheduler schedular;
    
    @Autowired
    private S3UploadService s3UploadService;
    
    @Autowired
    private ItemMasterService itemMasterService;
    
    /**
     * Create invoice with item images support
     * POST /api/invoices
     * Uses multipart/form-data to support image uploads
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> createInvoice(
            @RequestPart("invoice") String invoiceDtoJson,
            @RequestPart(value = "itemImages", required = false) MultipartFile[] itemImages,
            Authentication authentication) {
        try {
            // Extract user context from JWT
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String role = userContext.get("type");
            
            // Parse JSON to InvoiceDTO
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            InvoiceDTO invoiceDTO = objectMapper.readValue(invoiceDtoJson, InvoiceDTO.class);
            
            // Validate required fields
            if (invoiceDTO.getInvoiceDate() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "invoiceDate is required"));
            }
            
            if (invoiceDTO.getCustomerName() == null || invoiceDTO.getCustomerName().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "customerName is required"));
            }
            
            if (invoiceDTO.getItems() == null || invoiceDTO.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least one item is required"));
            }
            
            // Process item images and fetch from ItemMaster
            if (itemImages != null && itemImages.length > 0) {
                processItemImages(invoiceDTO, itemImages, organizationId);
            } else {
                // Fetch images from ItemMaster if not provided
                fetchItemImagesFromMaster(invoiceDTO, organizationId);
            }
            
            // Create invoice with organizationId from token
            Invoice invoice = invoiceService.createInvoice(invoiceDTO, organizationId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Invoice created successfully and email sent",
                "invoice", invoice,
                "invoiceNumber", invoice.getInvoiceNumber(),
                "organizationId", invoice.getOrganizationId(),
                "createdBy", userContext.get("userId"),
                "role", role
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create invoice: " + e.getMessage()));
        }
    }
    
    /**
     * Process uploaded item images and update DTO
     */
    private void processItemImages(InvoiceDTO invoiceDTO, MultipartFile[] itemImages, 
                                   String organizationId) {
        List<InvoiceDTO.InvoiceItemDTO> items = invoiceDTO.getItems();
        
        // Upload images to S3
        for (int i = 0; i < Math.min(items.size(), itemImages.length); i++) {
            if (itemImages[i] != null && !itemImages[i].isEmpty()) {
                try {
                    String imageUrl = s3UploadService.uploadFile(
                        itemImages[i], organizationId, "item-images");
                    items.get(i).setItemImageUrl(imageUrl);
                    
                    System.out.println("✅ Uploaded image for item " + (i+1) + ": " + imageUrl);
                } catch (Exception e) {
                    System.err.println("❌ Failed to upload image for item " + (i+1) + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Fetch item images and descriptions from ItemMaster
     */
    private void fetchItemImagesFromMaster(InvoiceDTO invoiceDTO, String organizationId) {
        List<InvoiceDTO.InvoiceItemDTO> items = invoiceDTO.getItems();
        
        for (InvoiceDTO.InvoiceItemDTO item : items) {
            // Fetch from ItemMaster using name and HSN
            Optional<ItemMaster> itemMaster = itemMasterService.getItemByNameAndHsn(
                organizationId, item.getItemName(), item.getHsnOrSac());
            
            if (itemMaster.isPresent()) {
                ItemMaster master = itemMaster.get();
                
                // Set image URL if available
                if (master.getItemImageUrl() != null && !master.getItemImageUrl().isEmpty()) {
                    item.setItemImageUrl(master.getItemImageUrl());
                }
                
                // Set description if available and not already set
                if (item.getItemDescription() == null || item.getItemDescription().isEmpty()) {
                    if (master.getItemDescription() != null) {
                        item.setItemDescription(master.getItemDescription());
                    }
                }
                
                System.out.println("✅ Fetched image from ItemMaster for: " + item.getItemName());
            }
        }
    }

    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getAllInvoicesForUser(Authentication authentication) {
        try {
            // Extract organization from JWT
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<Invoice> invoices = invoiceService.getAllInvoices(organizationId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invoices retrieved successfully",
                "organizationId", organizationId,
                "count", invoices.size(),
                "invoices", invoices
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve invoices: " + e.getMessage()));
        }
    }

    
    @GetMapping("/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getAllInvoices(
            @PathVariable String organizationId,
            Authentication authentication) {
        try {
            // Extract user context from JWT
            Map<String, String> userContext = extractUserContext(authentication);
            String userOrgId = userContext.get("organizationId");
            String role = userContext.get("type");
            
            // Security check: Non-admin users can only view their own organization's invoices
            if (!"ADMIN".equals(role) && !userOrgId.equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. You can only view invoices for your organization"));
            }
            
            List<Invoice> invoices = invoiceService.getAllInvoices(organizationId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invoices retrieved successfully",
                "count", invoices.size(),
                "invoices", invoices
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve invoices: " + e.getMessage()));
        }
    }

    /**
     * Get invoice by ID
     * GET /api/invoices/invoice/{id}
     */
    @GetMapping("/invoice/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getInvoiceById(
            @PathVariable String id,
            Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String role = userContext.get("type");
            
            return invoiceService.getInvoiceById(id)
                .map(invoice -> {
                    // Security check: Non-admin users can only view their own organization's invoices
                    if (!"ADMIN".equals(role) && !organizationId.equals(invoice.getOrganizationId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied to this invoice"));
                    }
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Invoice found",
                        "invoice", invoice
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invoice not found with id: " + id)));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve invoice: " + e.getMessage()));
        }
    }

    /**
     * Search invoices by customer name
     * GET /api/invoices/search?customer={customerName}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> searchInvoicesForUser(
            @RequestParam String customer,
            Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<Invoice> invoices = invoiceService.searchByCustomer(organizationId, customer);
            
            return ResponseEntity.ok(Map.of(
                "message", "Search completed",
                "searchTerm", customer,
                "count", invoices.size(),
                "invoices", invoices
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Search failed: " + e.getMessage()));
        }
    }

    /**
     * Get invoices by date range
     * GET /api/invoices/date-range?start=2025-01-01&end=2025-01-31
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getInvoicesByDateRangeForUser(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<Invoice> invoices = invoiceService.getInvoicesByDateRange(
                organizationId, start, end);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invoices retrieved successfully",
                "startDate", start,
                "endDate", end,
                "count", invoices.size(),
                "invoices", invoices
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve invoices: " + e.getMessage()));
        }
    }

    /**
     * Update invoice with item images support
     * PUT /api/invoices/{id}
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> updateInvoice(
            @PathVariable String id,
            @RequestPart("invoice") String invoiceDtoJson,
            @RequestPart(value = "itemImages", required = false) MultipartFile[] itemImages,
            Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String role = userContext.get("type");
            
            // Parse JSON to InvoiceDTO
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            InvoiceDTO invoiceDTO = objectMapper.readValue(invoiceDtoJson, InvoiceDTO.class);
            
            // Get the existing invoice to check ownership
            Invoice existingInvoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
            
            // Security check
            if (!"ADMIN".equals(role) && !organizationId.equals(existingInvoice.getOrganizationId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. You can only update invoices for your organization"));
            }
            
            // Process item images if provided
            if (itemImages != null && itemImages.length > 0) {
                processItemImages(invoiceDTO, itemImages, organizationId);
            } else {
                // Fetch from ItemMaster if not provided
                fetchItemImagesFromMaster(invoiceDTO, organizationId);
            }
            
            Invoice invoice = invoiceService.updateInvoice(id, invoiceDTO);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invoice updated successfully",
                "invoice", invoice,
                "updatedBy", userContext.get("userId")
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update invoice: " + e.getMessage()));
        }
    }

    /**
     * Delete invoice
     * DELETE /api/invoices/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> deleteInvoice(
            @PathVariable String id,
            Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String role = userContext.get("type");
            
            // Get the existing invoice to check ownership
            Invoice existingInvoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
            
            // Security check
            if (!"ADMIN".equals(role) && !organizationId.equals(existingInvoice.getOrganizationId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. You can only delete invoices for your organization"));
            }
            
            invoiceService.deleteInvoice(id);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invoice deleted successfully",
                "id", id,
                "deletedBy", userContext.get("userId")
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete invoice: " + e.getMessage()));
        }
    }

    /**
     * Get invoice statistics
     * GET /api/invoices/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getInvoiceStatsForUser(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<Invoice> invoices = invoiceService.getAllInvoices(organizationId);
            
            double totalAmount = invoices.stream()
                .mapToDouble(Invoice::getNetPayable)
                .sum();
            
            double totalGst = invoices.stream()
                .mapToDouble(inv -> inv.getGstAmount() != null ? inv.getGstAmount() : 0.0)
                .sum();
            
            double totalTds = invoices.stream()
                .mapToDouble(inv -> inv.getTdsAmount() != null ? inv.getTdsAmount() : 0.0)
                .sum();
            
            return ResponseEntity.ok(Map.of(
                "message", "Statistics calculated",
                "organizationId", organizationId,
                "totalInvoices", invoices.size(),
                "totalAmount", totalAmount,
                "totalGst", totalGst,
                "totalTds", totalTds
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to calculate stats: " + e.getMessage()));
        }
    }

    /**
     * Helper method to extract organization context from JWT
     */
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

    @PostMapping("/invoice-reminders")
    public String trigger() {
        schedular.triggerManualReminders();
        return "Invoice reminders triggered manually";
    }
}