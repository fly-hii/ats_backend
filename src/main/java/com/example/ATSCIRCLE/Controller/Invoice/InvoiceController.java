package com.example.ATSCIRCLE.Controller.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.Invoicemodel;
import com.example.ATSCIRCLE.Service.Invoice.InvoiceEmailService;
import com.example.ATSCIRCLE.Service.Invoice.InvoiceService;


import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.ATSCIRCLE.Service.Invoice.S3UploadService;
import com.example.ATSCIRCLE.security.JwtService;

@RestController
@RequestMapping("/api/placement-orders")
public class InvoiceController {

    @Autowired
    private InvoiceService placementOrderService;

    @Autowired
    private S3UploadService s3UploadService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceEmailService invoiceEmailService;
    
    @Autowired
      private JwtService jwtService;;

    /**
     * ✅ Helper method to extract user context from JWT
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

    /**
     * Create new placement order with files and user tracking
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> createPlacementOrder(
            @RequestParam("placementOrderData") String placementOrderData,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "documentTypes", required = false) String documentTypes,
            @RequestParam String organizationId,
            Authentication authentication) {
        try {
            // ✅ Extract user context from JWT
            Map<String, String> userContext = extractUserContext(authentication);
            String userId = userContext.get("userId");
            
            // Parse JSON data
            Invoicemodel placementOrder = objectMapper.readValue(placementOrderData, Invoicemodel.class);
            
            // ✅ Set createdBy field
            placementOrder.setCreatedBy(userId);
            
            // Handle file uploads if files are present
            if (files != null && files.length > 0) {
                List<Invoicemodel.AttachedDocument> documents = new ArrayList<>();
                
                // Parse document types
                String[] types = null;
                if (documentTypes != null && !documentTypes.isEmpty()) {
                    types = documentTypes.split(",");
                }
                
                // Upload each file
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isEmpty()) {
                        String docType = (types != null && i < types.length) ? types[i].trim() : "other";
                        
                        // Upload to S3 using existing service
                        String fileUrl = s3UploadService.uploadFile(files[i], organizationId, docType);
                        
                        // Add to documents list
                        Invoicemodel.AttachedDocument doc = new Invoicemodel.AttachedDocument(fileUrl, docType);
                        documents.add(doc);
                    }
                }
                
                // Add documents to placement order
                if (placementOrder.getDocuments() == null) {
                    placementOrder.setDocuments(documents);
                } else {
                    placementOrder.getDocuments().addAll(documents);
                }
            }
            
            // Create placement order
            Invoicemodel created = placementOrderService.createPlacementOrder(placementOrder, organizationId);
            
            // Send invoice email with file attachments if email address is provided
            try {
                if (created.getSendInvoiceMailTo() != null || created.getClientEmail() != null) {
                    invoiceEmailService.sendInvoiceEmail(created, files);
                }
            } catch (Exception e) {
                System.err.println("Failed to send invoice email: " + e.getMessage());
                // Don't fail the entire request if email fails
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating placement order: " + e.getMessage());
        }
    }

    /**
     * Update placement order with files and user tracking
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> updatePlacementOrder(
            @PathVariable String id,
            @RequestParam("placementOrderData") String placementOrderData,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "documentTypes", required = false) String documentTypes,
            @RequestParam(value = "deleteFileUrls", required = false) String deleteFileUrls,
            Authentication authentication) {
        try {
            // ✅ Extract user context from JWT
            Map<String, String> userContext = extractUserContext(authentication);
            String userId = userContext.get("userId");
            
            // Parse JSON data
            Invoicemodel updatedOrder = objectMapper.readValue(placementOrderData, Invoicemodel.class);
            
            // ✅ Set updatedBy field
            updatedOrder.setUpdatedBy(userId);
            
            // Get existing order
            Optional<Invoicemodel> existingOpt = placementOrderService.getPlacementOrderById(id);
            if (!existingOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Placement order not found with id: " + id);
            }
            
            Invoicemodel existing = existingOpt.get();
            
            // Handle file deletions
            if (deleteFileUrls != null && !deleteFileUrls.isEmpty()) {
                String[] urlsToDelete = deleteFileUrls.split(",");
                for (String fileUrl : urlsToDelete) {
                    String trimmedUrl = fileUrl.trim();
                    
                    // Remove from documents list
                    if (existing.getDocuments() != null) {
                        existing.getDocuments().removeIf(doc -> doc.getLink().equals(trimmedUrl));
                    }
                    
                    // Delete from S3
                    try {
                        s3UploadService.deleteFile(trimmedUrl);
                    } catch (Exception e) {
                        System.err.println("Error deleting file from S3: " + trimmedUrl);
                    }
                }
            }
            
            // Handle new file uploads
            if (files != null && files.length > 0) {
                List<Invoicemodel.AttachedDocument> newDocuments = new ArrayList<>();
                
                // Parse document types
                String[] types = null;
                if (documentTypes != null && !documentTypes.isEmpty()) {
                    types = documentTypes.split(",");
                }
                
                // Upload each file
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isEmpty()) {
                        String docType = (types != null && i < types.length) ? types[i].trim() : "other";
                        
                        // Upload to S3
                        String fileUrl = s3UploadService.uploadFile(
                            files[i], 
                            existing.getOrganizationId(), 
                            docType
                        );
                        
                        // Add to documents list
                        Invoicemodel.AttachedDocument doc = new Invoicemodel.AttachedDocument(fileUrl, docType);
                        newDocuments.add(doc);
                    }
                }
                
                // Add new documents to existing documents
                if (existing.getDocuments() == null) {
                    existing.setDocuments(newDocuments);
                } else {
                    existing.getDocuments().addAll(newDocuments);
                }
            }
            
            // Merge documents from updatedOrder (if any provided in JSON)
            if (updatedOrder.getDocuments() != null) {
                if (existing.getDocuments() == null) {
                    existing.setDocuments(updatedOrder.getDocuments());
                } else {
                    // Add only new documents that don't already exist
                    for (Invoicemodel.AttachedDocument doc : updatedOrder.getDocuments()) {
                        boolean exists = existing.getDocuments().stream()
                            .anyMatch(existingDoc -> existingDoc.getLink().equals(doc.getLink()));
                        if (!exists) {
                            existing.getDocuments().add(doc);
                        }
                    }
                }
            }
            
            // Update other fields from updatedOrder
            if (updatedOrder.getClientId() != null) existing.setClientId(updatedOrder.getClientId());
            if (updatedOrder.getClientName() != null) existing.setClientName(updatedOrder.getClientName());
            if (updatedOrder.getClientEmail() != null) existing.setClientEmail(updatedOrder.getClientEmail());
            if (updatedOrder.getPoNumber() != null) existing.setPoNumber(updatedOrder.getPoNumber());
            if (updatedOrder.getClientGSTIN() != null) existing.setClientGSTIN(updatedOrder.getClientGSTIN());
            if (updatedOrder.getGstStatus() != null) existing.setGstStatus(updatedOrder.getGstStatus());
            if (updatedOrder.getPaymentTerms() != null) existing.setPaymentTerms(updatedOrder.getPaymentTerms());
            if (updatedOrder.getBillingAddress() != null) existing.setBillingAddress(updatedOrder.getBillingAddress());
            if (updatedOrder.getSameAsBilling() != null) existing.setSameAsBilling(updatedOrder.getSameAsBilling());
            if (updatedOrder.getShippingAddress() != null) existing.setShippingAddress(updatedOrder.getShippingAddress());
            
            if (updatedOrder.getCandidateId() != null) existing.setCandidateId(updatedOrder.getCandidateId());
            if (updatedOrder.getCandidateName() != null) existing.setCandidateName(updatedOrder.getCandidateName());
            if (updatedOrder.getDesignation() != null) existing.setDesignation(updatedOrder.getDesignation());
            if (updatedOrder.getPlacementType() != null) existing.setPlacementType(updatedOrder.getPlacementType());
            if (updatedOrder.getCtc() != null) existing.setCtc(updatedOrder.getCtc());
            if (updatedOrder.getJoiningDate() != null) existing.setJoiningDate(updatedOrder.getJoiningDate());
            
            if (updatedOrder.getFeeModel() != null) existing.setFeeModel(updatedOrder.getFeeModel());
            if (updatedOrder.getFeePercentage() != null) existing.setFeePercentage(updatedOrder.getFeePercentage());
            if (updatedOrder.getFixedAmount() != null) existing.setFixedAmount(updatedOrder.getFixedAmount());
            if (updatedOrder.getCalculatedFee() != null) existing.setCalculatedFee(updatedOrder.getCalculatedFee());
            if (updatedOrder.getGuaranteeDays() != null) existing.setGuaranteeDays(updatedOrder.getGuaranteeDays());
            if (updatedOrder.getGuaranteeEndDate() != null) existing.setGuaranteeEndDate(updatedOrder.getGuaranteeEndDate());
            if (updatedOrder.getIfCandidateLeaves() != null) existing.setIfCandidateLeaves(updatedOrder.getIfCandidateLeaves());
            if (updatedOrder.getExitPenalty() != null) existing.setExitPenalty(updatedOrder.getExitPenalty());
            
            if (updatedOrder.getYourGSTIN() != null) existing.setYourGSTIN(updatedOrder.getYourGSTIN());
            if (updatedOrder.getYourPan() != null) existing.setYourPan(updatedOrder.getYourPan());
            if (updatedOrder.getAccountNumber() != null) existing.setAccountNumber(updatedOrder.getAccountNumber());
            if (updatedOrder.getIfscCode() != null) existing.setIfscCode(updatedOrder.getIfscCode());
            if (updatedOrder.getFromAddress() != null) existing.setFromAddress(updatedOrder.getFromAddress());
            
            if (updatedOrder.getTDSApplicable() != null) existing.setTDSApplicable(updatedOrder.getTDSApplicable());
            if (updatedOrder.getAutomaticPaymentReminders() != null) existing.setAutomaticPaymentReminders(updatedOrder.getAutomaticPaymentReminders());
            if (updatedOrder.getSendInvoiceMailTo() != null) existing.setSendInvoiceMailTo(updatedOrder.getSendInvoiceMailTo());
            
            // ✅ Set updatedBy field on existing object
            System.out.println("@@@@@@@"+userId+"@@@@@@@@@@@@");
            existing.setUpdatedBy(userId);
            
            // Update the order
            Invoicemodel updated = placementOrderService.updatePlacementOrder(id, existing);
            return ResponseEntity.ok(updated);
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error updating placement order: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating placement order: " + e.getMessage());
        }
    }

    /**
     * Generate presigned URL for document download
     */
    @GetMapping("/document-download-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getDocumentDownloadUrl(@RequestParam String fileUrl) {
        try {
            String presignedUrl = s3UploadService.generatePresignedUrl(fileUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", presignedUrl);
            response.put("expiresIn", "1 hour");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating download URL: " + e.getMessage());
        }
    }

    /**
     * Get all placement orders for an organization
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getAllPlacementOrders(@RequestParam String organizationId) {
        try {
            List<Invoicemodel> orders = placementOrderService.getAllPlacementOrders(organizationId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement orders: " + e.getMessage());
        }
    }

    /**
     * Get placement order by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getPlacementOrderById(@PathVariable String id) {
        try {
            Optional<Invoicemodel> order = placementOrderService.getPlacementOrderById(id);
            if (order.isPresent()) {
                return ResponseEntity.ok(order.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Placement order not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement order: " + e.getMessage());
        }
    }

    /**
     * Get placement orders by client
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getPlacementOrdersByClient(
            @PathVariable String clientId,
            @RequestParam String organizationId) {
        try {
            List<Invoicemodel> orders = placementOrderService.getPlacementOrdersByClient(organizationId, clientId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement orders: " + e.getMessage());
        }
    }

    /**
     * Get placement order by candidate
     */
    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> getPlacementOrderByCandidate(
            @PathVariable String candidateId,
            @RequestParam String organizationId) {
        try {
            Optional<Invoicemodel> order = placementOrderService.getPlacementOrderByCandidate(organizationId, candidateId);
            if (order.isPresent()) {
                return ResponseEntity.ok(order.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Placement order not found for candidate: " + candidateId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement order: " + e.getMessage());
        }
    }

    /**
     * Get placement orders by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<?> getPlacementOrdersByDateRange(
            @RequestParam String organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Invoicemodel> orders = placementOrderService.getPlacementOrdersByDateRange(organizationId, startDate, endDate);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement orders: " + e.getMessage());
        }
    }

    /**
     * Get placement orders by placement type
     */
    @GetMapping("/type/{placementType}")
    public ResponseEntity<?> getPlacementOrdersByType(
            @PathVariable String placementType,
            @RequestParam String organizationId) {
        try {
            List<Invoicemodel> orders = placementOrderService.getPlacementOrdersByType(organizationId, placementType);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching placement orders: " + e.getMessage());
        }
    }

    /**
     * Delete placement order and all associated documents
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> deletePlacementOrder(@PathVariable String id) {
        try {
            // Get the order first to delete associated files
            Optional<Invoicemodel> orderOpt = placementOrderService.getPlacementOrderById(id);
            
            if (orderOpt.isPresent()) {
                Invoicemodel order = orderOpt.get();
                
                // Delete all associated documents from S3
                if (order.getDocuments() != null) {
                    for (Invoicemodel.AttachedDocument doc : order.getDocuments()) {
                        try {
                            s3UploadService.deleteFile(doc.getLink());
                        } catch (Exception e) {
                            // Log error but continue deletion
                            System.err.println("Error deleting file: " + doc.getLink());
                        }
                    }
                }
            }
            
            placementOrderService.deletePlacementOrder(id);
            return ResponseEntity.ok("Placement order and associated documents deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting placement order: " + e.getMessage());
        }
    }
    
    /**
     * Resend invoice email
     */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVOICE')")
    public ResponseEntity<?> resendInvoiceEmail(
            @PathVariable String id,
            @RequestParam(value = "files", required = false) MultipartFile[] newFiles) {
        try {
            // Get the placement order by ID
            Optional<Invoicemodel> orderOpt = placementOrderService.getPlacementOrderById(id);
            
            if (!orderOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "success", false,
                            "message", "Placement order not found with id: " + id
                        ));
            }
            
            Invoicemodel invoice = orderOpt.get();
            
            // Validate email address
            String toEmail = invoice.getSendInvoiceMailTo() != null ? 
                             invoice.getSendInvoiceMailTo() : 
                             invoice.getClientEmail();
            
            if (toEmail == null || toEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "message", "No email address found to send invoice"
                        ));
            }
            
            // Get existing document URLs from invoice
            List<String> existingDocUrls = new ArrayList<>();
            if (invoice.getDocuments() != null && !invoice.getDocuments().isEmpty()) {
                for (Invoicemodel.AttachedDocument doc : invoice.getDocuments()) {
                    if (doc.getLink() != null && !doc.getLink().isEmpty()) {
                        existingDocUrls.add(doc.getLink());
                    }
                }
            }
            
            // Download existing attachments from S3 and convert to MultipartFile
            List<MultipartFile> allFiles = new ArrayList<>();
            int existingFilesCount = 0;
            
            for (String fileUrl : existingDocUrls) {
                try {
                    MultipartFile file = s3UploadService.downloadFileAsMultipart(fileUrl);
                    if (file != null) {
                        allFiles.add(file);
                        existingFilesCount++;
                        System.out.println("✅ Downloaded existing attachment: " + file.getOriginalFilename());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to download file from S3: " + fileUrl + " - " + e.getMessage());
                    // Continue with other files even if one fails
                }
            }
            
            // Add new files if provided
            int newFilesCount = 0;
            if (newFiles != null && newFiles.length > 0) {
                for (MultipartFile file : newFiles) {
                    if (!file.isEmpty()) {
                        allFiles.add(file);
                        newFilesCount++;
                    }
                }
            }
            
            // Convert list to array
            MultipartFile[] filesArray = allFiles.toArray(new MultipartFile[0]);
            
            // Send invoice email with all attachments
            invoiceEmailService.sendInvoiceEmail(invoice, filesArray);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Invoice email sent successfully",
                "sentTo", toEmail,
                "totalAttachments", allFiles.size(),
                "existingAttachments", existingFilesCount,
                "newAttachments", newFilesCount,
                "invoiceId", id,
                "candidateName", invoice.getCandidateName() != null ? invoice.getCandidateName() : "N/A"
            ));
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to send invoice email: " + e.getMessage()
                    ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Unexpected error sending invoice email: " + e.getMessage()
                    ));
        }
    }
}