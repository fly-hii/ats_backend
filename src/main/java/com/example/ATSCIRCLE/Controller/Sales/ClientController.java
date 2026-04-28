package com.example.ATSCIRCLE.Controller.Sales;

import com.example.ATSCIRCLE.Models.Sales.Client;
import com.example.ATSCIRCLE.Service.Sales.ClientService;
import com.example.ATSCIRCLE.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private JwtService jwtService;

    // Helper method to extract organizationId from JWT
    private String getOrganizationId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractOrganizationId(token);
        }
        throw new RuntimeException("Invalid or missing authorization token");
    }

    // Helper method to extract current user email from JWT
    private String getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractUsername(token);
        }
        throw new RuntimeException("Invalid or missing authorization token");
    }

    // -------------------- MIGRATION ENDPOINT --------------------
    
    @PostMapping("/migrate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> migrateCompanyToClient(
            HttpServletRequest request,
            @RequestParam String companyId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String hiringManager,
            @RequestParam(required = false) String pocName,
            @RequestParam(required = false) String pocJobTitle,
            @RequestParam(required = false) String pocEmail,
            @RequestParam(required = false) String pocPhone,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile primaryAgreement,
            @RequestParam(required = false) MultipartFile workOrder,
            @RequestParam(required = false) List<MultipartFile> supportingDocs
    ) {
        try {
            String organizationId = getOrganizationId(request);
            String currentUser = getCurrentUser(request);
            
            Client client = clientService.migrateCompanyToClient(
                companyId, role, hiringManager, pocName, pocJobTitle, 
                pocEmail, pocPhone, logo, primaryAgreement, workOrder, 
                supportingDocs, organizationId, currentUser
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Company migrated to Client successfully",
                "client", client
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Migration Error",
                "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "File Upload Error",
                "message", "Failed to upload files: " + e.getMessage()
            ));
        }
    }

    // -------------------- CREATE --------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<?> createClient(
            HttpServletRequest request,
            @RequestParam String companyName,
            @RequestParam(required = false) String websiteUrl,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(required = false) String leadStatus,
            @RequestParam(required = false) String leadSource,
            @RequestParam(required = false) String linkedInUrl,
            @RequestParam(required = false) String companyStage,
            @RequestParam(required = false) String legalType,
            @RequestParam(required = false) String employeeCount,
            @RequestParam(required = false) String annualRevenue,
            @RequestParam(required = false) String primaryContact,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String contactOwner,
            @RequestParam(required = false) String billingStreet,
            @RequestParam(required = false) String billingCity,
            @RequestParam(required = false) String billingState,
            @RequestParam(required = false) String billingCountry,
            @RequestParam(required = false) String billingZipCode,
            @RequestParam(required = false) String shippingStreet,
            @RequestParam(required = false) String shippingCity,
            @RequestParam(required = false) String shippingState,
            @RequestParam(required = false) String shippingCountry,
            @RequestParam(required = false) String shippingZipCode,
            @RequestParam(required = false) Boolean sameAsBilling,
            @RequestParam(required = false) String taxIdType,
            @RequestParam(required = false) String taxIdNumber,
            @RequestParam(required = false) String paymentTerms,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String hiringManager,
            @RequestParam(required = false) String pocName,
            @RequestParam(required = false) String pocJobTitle,
            @RequestParam(required = false) String pocEmail,
            @RequestParam(required = false) String pocPhone,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile primaryAgreement,
            @RequestParam(required = false) MultipartFile workOrder,
            @RequestParam(required = false) List<MultipartFile> supportingDocs,
            @RequestParam(required = false) Boolean doNotCall,
            @RequestParam(required = false) Boolean doNotEmail
    ) {
        try {
            String organizationId = getOrganizationId(request);
            String currentUser = getCurrentUser(request);
            
            Client savedClient = clientService.createClientWithFiles(
                companyName, websiteUrl, industry, relationshipType, leadStatus, leadSource,
                linkedInUrl, companyStage, legalType, employeeCount, annualRevenue,
                primaryContact, jobTitle, email, phone, contactOwner,
                billingStreet, billingCity, billingState, billingCountry, billingZipCode,
                shippingStreet, shippingCity, shippingState, shippingCountry, shippingZipCode,
                sameAsBilling, taxIdType, taxIdNumber, paymentTerms, currency,
                role, hiringManager, pocName, pocJobTitle, pocEmail, pocPhone,
                logo, primaryAgreement, workOrder, supportingDocs,
                doNotCall, doNotEmail, currentUser, organizationId
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Client created successfully",
                "client", savedClient
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "File Upload Error",
                "message", e.getMessage()
            ));
        }
    }

    // -------------------- READ --------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<List<Client>> getAllClients(HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return ResponseEntity.ok(clientService.getAllClientsByOrganization(organizationId));
    }

    @GetMapping("/my-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<List<Client>> getMyClients(HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        String currentUser = getCurrentUser(request);
        return ResponseEntity.ok(clientService.getClientsByCreatedByAndOrganization(currentUser, organizationId));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<List<Client>> getClientsAssignedToMe(HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        String currentUser = getCurrentUser(request);
        return ResponseEntity.ok(clientService.getClientsAssignedToUser(currentUser, organizationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<Client> getClientById(@PathVariable String id, HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return clientService.getClientByIdAndOrganization(id, organizationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------- UPDATE --------------------

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<?> updateClientWithFiles(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String websiteUrl,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(required = false) String leadStatus,
            @RequestParam(required = false) String leadSource,
            @RequestParam(required = false) String linkedInUrl,
            @RequestParam(required = false) String companyStage,
            @RequestParam(required = false) String legalType,
            @RequestParam(required = false) String employeeCount,
            @RequestParam(required = false) String annualRevenue,
            @RequestParam(required = false) String primaryContact,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String contactOwner,
            @RequestParam(required = false) String billingStreet,
            @RequestParam(required = false) String billingCity,
            @RequestParam(required = false) String billingState,
            @RequestParam(required = false) String billingCountry,
            @RequestParam(required = false) String billingZipCode,
            @RequestParam(required = false) String shippingStreet,
            @RequestParam(required = false) String shippingCity,
            @RequestParam(required = false) String shippingState,
            @RequestParam(required = false) String shippingCountry,
            @RequestParam(required = false) String shippingZipCode,
            @RequestParam(required = false) Boolean sameAsBilling,
            @RequestParam(required = false) String taxIdType,
            @RequestParam(required = false) String taxIdNumber,
            @RequestParam(required = false) String paymentTerms,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String hiringManager,
            @RequestParam(required = false) String pocName,
            @RequestParam(required = false) String pocJobTitle,
            @RequestParam(required = false) String pocEmail,
            @RequestParam(required = false) String pocPhone,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile primaryAgreement,
            @RequestParam(required = false) MultipartFile workOrder,
            @RequestParam(required = false) List<MultipartFile> supportingDocs,
            @RequestParam(required = false) Boolean doNotCall,
            @RequestParam(required = false) Boolean doNotEmail,
            @RequestParam(required = false) Boolean deleteLogo,
            @RequestParam(required = false) Boolean deletePrimaryAgreement,
            @RequestParam(required = false) Boolean deleteWorkOrder
    ) {
        try {
            String organizationId = getOrganizationId(request);
            
            Client updated = clientService.updateClientWithFiles(
                id, companyName, websiteUrl, industry, relationshipType, leadStatus, leadSource,
                linkedInUrl, companyStage, legalType, employeeCount, annualRevenue,
                primaryContact, jobTitle, email, phone, contactOwner,
                billingStreet, billingCity, billingState, billingCountry, billingZipCode,
                shippingStreet, shippingCity, shippingState, shippingCountry, shippingZipCode,
                sameAsBilling, taxIdType, taxIdNumber, paymentTerms, currency,
                role, hiringManager, pocName, pocJobTitle, pocEmail, pocPhone,
                logo, primaryAgreement, workOrder, supportingDocs,
                doNotCall, doNotEmail, deleteLogo, deletePrimaryAgreement, deleteWorkOrder,
                organizationId
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Client updated successfully",
                "client", updated
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "File Upload Error",
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<?> updateClient(
            @PathVariable String id, 
            @RequestBody Client updatedClient,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            Client updated = clientService.updateClient(id, updatedClient, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Client updated successfully",
                "client", updated
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
            ));
        }
    }

    // -------------------- DELETE --------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable String id, HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return clientService.deleteClient(id, organizationId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // -------------------- BULK OPERATIONS --------------------

    @DeleteMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkDeleteClients(@RequestBody List<String> ids, HttpServletRequest request) {
        try {
            String organizationId = getOrganizationId(request);
            clientService.deleteClientsByIds(ids, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Clients deleted successfully",
                "deletedCount", ids.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bulk Delete Error",
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/bulk-update/contact-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkUpdateContactOwner(
            @RequestBody Map<String, Object> req,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) req.get("ids");
            String contactOwner = (String) req.get("contactOwner");
            
            clientService.updateContactOwner(ids, contactOwner, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Contact owner updated successfully",
                "updatedCount", ids.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bulk Update Error",
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/bulk-update/field")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkUpdateField(
            @RequestBody Map<String, Object> req,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) req.get("ids");
            String fieldName = (String) req.get("fieldName");
            Object fieldValue = req.get("fieldValue");
            
            clientService.updateClientsField(ids, fieldName, fieldValue, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Field updated successfully",
                "updatedCount", ids.size(),
                "field", fieldName
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bulk Update Error",
                "message", e.getMessage()
            ));
        }
    }

    // -------------------- EXCEL OPERATIONS --------------------

    @GetMapping("/excel/columns")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<String>> getAllColumns() {
        return ResponseEntity.ok(clientService.getAllColumns());
    }

    @PostMapping("/excel/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<InputStreamResource> generateExcelTemplate(
            @RequestBody List<String> selectedColumns
    ) {
        try {
            ByteArrayInputStream in = clientService.generateExcelTemplate(selectedColumns);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=client_template.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/excel/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            String currentUser = getCurrentUser(request);
            
            clientService.importExcel(file, currentUser, organizationId);
            return ResponseEntity.ok(Map.of("message", "Clients imported successfully"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Import Error",
                "message", "Failed to parse Excel file: " + e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Import Error",
                "message", e.getMessage()
            ));
        }
    }

    // -------------------- FILE UPLOADS --------------------

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<?> uploadLogo(
            @PathVariable String id,
            @RequestParam("file") MultipartFile logo,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            Client client = clientService.uploadClientLogo(id, logo, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Logo uploaded successfully",
                "logoUrl", client.getClientCompanyLogoUrl()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Upload Error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> uploadDocuments(
            @PathVariable String id,
            @RequestParam("files") List<MultipartFile> documents,
            HttpServletRequest request
    ) {
        try {
            String organizationId = getOrganizationId(request);
            Client client = clientService.uploadSupportingDocuments(id, documents, organizationId);
            return ResponseEntity.ok(Map.of(
                "message", "Documents uploaded successfully",
                "documentUrls", client.getSupportingDocumentUrls()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Upload Error",
                "message", e.getMessage()
            ));
        }
    }

    // -------------------- SEARCH / FILTERS --------------------

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<List<Client>> searchClients(
            @RequestParam String name,
            HttpServletRequest request
    ) {
        String organizationId = getOrganizationId(request);
        return ResponseEntity.ok(clientService.searchClientsByName(name, organizationId));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<Client> findByEmail(@PathVariable String email, HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return clientService.findByEmail(email, organizationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<List<Client>> getByRole(@PathVariable String role, HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return ResponseEntity.ok(clientService.getClientsByRole(role, organizationId));
    }

    // -------------------- STATISTICS --------------------

    @GetMapping("/stats/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<Long> getTotalCount(HttpServletRequest request) {
        String organizationId = getOrganizationId(request);
        return ResponseEntity.ok(clientService.getTotalClientCount(organizationId));
    }

    @GetMapping("/check-migration/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS','CRM')")
    public ResponseEntity<Boolean> isCompanyMigrated(
            @PathVariable String companyId,
            HttpServletRequest request
    ) {

    // ================================================================================
    //              NOTE: Email sending endpoints moved to BulkEmailController
    //     Use /api/bulk-email/send or /api/bulk-email/send-excel instead
    // ================================================================================/
        String organizationId = getOrganizationId(request);
        return ResponseEntity.ok(clientService.isCompanyAlreadyMigrated(companyId, organizationId));
    }
}