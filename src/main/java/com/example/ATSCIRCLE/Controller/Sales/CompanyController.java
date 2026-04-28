package com.example.ATSCIRCLE.Controller.Sales;

import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Contact;
import com.example.ATSCIRCLE.Models.Sales.EmailLog;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.Industry;

import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LeadStatus;

import com.example.ATSCIRCLE.Service.Sales.CompanyService;
import com.example.ATSCIRCLE.Service.Sales.ExcelEmailHelper;
import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;



import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;
    
    
    @Autowired
    private JwtService jwtService;

    // ✅ Helper method to extract organization context from JWT
    private Map<String, String> extractUserContext(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        String organizationId = jwtService.extractOrganizationId(token);
        String type = jwtService.extractType(token);  // This will be "EMPLOYEE" or organization type
        String userId = jwtService.extractId(token);
        
        return Map.of(
            "organizationId", organizationId,
            "type", type,
            "userId", userId
        );
    }
    
    // ✅ Helper to check CRM access - Only checks role from Spring Security
    private boolean hasCRMAccess(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                            auth.getAuthority().equals("ROLE_CRM"));
    }

    // -------------------- CREATE --------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> createCompany(@RequestBody Company company,
                                           Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            Company savedCompany = companyService.createCompany(
                company, 
                context.get("organizationId"), 
                context.get("userId")
            );
            return ResponseEntity.ok(savedCompany);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation Error",
                "message", e.getMessage(),
                "status", 400
            ));
        }
    }

    // -------------------- READ --------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getAllCompanies(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(companyService.getAllCompanies(context.get("organizationId")));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getAllCompaniesPaged(Pageable pageable, Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(companyService.getAllCompanies(context.get("organizationId"), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getCompanyById(@PathVariable String id, Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return companyService.getCompanyById(id, context.get("organizationId"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------- UPDATE --------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> updateCompany(@PathVariable String id,
                                           @RequestBody Company updatedCompany,
                                           Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            Company updated = companyService.updateCompany(
                id, 
                updatedCompany, 
                context.get("organizationId"),
                context.get("type")
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage(),
                "status", 400
            ));
        }
    }

    // -------------------- DELETE (ADMIN ONLY) --------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")

    public ResponseEntity<?> deleteCompany(@PathVariable String id, Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            boolean deleted = companyService.deleteCompany(
                id, 
                context.get("organizationId"),
                context.get("type")
            );
            
            return deleted 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Access Denied",
                "message", e.getMessage(),
                "status", 403
            ));
        }
    }

    
 @GetMapping("/by-user/{userId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> getCompaniesByUser(
        @PathVariable String userId,
        Authentication authentication
) {
    Map<String, String> context = extractUserContext(authentication);
    String orgId = context.get("organizationId");

    return ResponseEntity.ok(
        companyService.getCompaniesByCreatorOrAssignee(userId, orgId)
    );
}


   

    // -------------------- STATISTICS --------------------

    @GetMapping("/stats/industry/{industry}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getCountByIndustry(@PathVariable Industry industry, 
                                                 Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(companyService.getCompanyCountByIndustry(
            industry, context.get("organizationId")));
    }

    @GetMapping("/stats/lead-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getCountByLeadStatus(@PathVariable LeadStatus status, 
                                                   Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(companyService.getCompanyCountByLeadStatus(
            status, context.get("organizationId")));
    }

    @GetMapping("/stats/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getTotalCount(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(companyService.getTotalCompanyCount(
            context.get("organizationId")));
    }

    
    // -------------------- ADMIN OPERATIONS --------------------

    @GetMapping("/columns")
    // @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<List<String>> getColumns() {
        return ResponseEntity.ok(companyService.getAllColumns());
    }

    @PostMapping("/generate-template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> generateTemplate(@RequestBody List<String> selectedColumns) 
            throws IOException {
        ByteArrayInputStream in = companyService.generateExcelTemplate(selectedColumns);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=company_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file, 
                                          Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            companyService.importExcel(file, context.get("organizationId"), context.get("userId"));
            return ResponseEntity.ok(Map.of("message", "Data imported successfully!"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Import Error",
                "message", e.getMessage()
            ));
        }
    }

@DeleteMapping("/deletebulk")
@PreAuthorize("hasAnyRole('ADMIN')") // Recommended
public ResponseEntity<?> deleteCompanies(
        @RequestBody List<String> ids,
        Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        companyService.deleteCompaniesByIds(
            ids,
            context.get("organizationId"),
            context.get("type")
        );

        return ResponseEntity.ok(Map.of(
            "message", ids.size() + " companies deleted successfully."
        ));
    } catch (RuntimeException e) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Delete Error",
            "message", e.getMessage(),
            "status", 403
        ));
    }
}

    
    @PutMapping("/update-contact-owner")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> updateContactOwner(@RequestBody Map<String, Object> payload,
                                                 Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            List<String> ids = (List<String>) payload.get("ids");
            String contactOwner = (String) payload.get("contactOwner");

            companyService.updateContactOwner(ids, contactOwner, context.get("organizationId"));
            return ResponseEntity.ok(Map.of(
                "message", ids.size() + " companies updated with new contact owner: " + contactOwner
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
            ));
        }
    }
    
  @PutMapping("/update-assigned-to")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> updateAssignedTo(@RequestBody Map<String, Object> payload,
                                          Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        List<String> ids = (List<String>) payload.get("ids");
        String assignedTo = (String) payload.get("assignedTo");

        companyService.updateAssignedTo(ids, assignedTo, context.get("organizationId"));

        return ResponseEntity.ok(Map.of(
                "message", ids.size() + " companies updated with new assignedTo user: " + assignedTo
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
        ));
    }
}


    @PutMapping("/update-bulk-field")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> updateBulkField(@RequestBody Map<String, Object> request,
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            List<String> ids = (List<String>) request.get("ids");
            String fieldName = (String) request.get("fieldName");
            Object fieldValue = request.get("fieldValue");

            if (ids == null || ids.isEmpty() || fieldName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Request",
                    "message", "Provide ids, fieldName, and fieldValue."
                ));
            }

            companyService.updateCompaniesField(ids, fieldName, fieldValue, context.get("organizationId"));
            return ResponseEntity.ok(Map.of(
                "message", "Updated field '" + fieldName + "' for " + ids.size() + " companies."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
            ));
        }
    }


    @PutMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> updateNotes(
        @PathVariable String id,
        @RequestBody Map<String, String> body,
        Authentication authentication) {

    Map<String, String> context = extractUserContext(authentication);

    String notes = body.get("notes");

    Optional<Company> updated = companyService.updateNotes(
            id,
            notes,
            context.get("organizationId")   // ✅ from token
    );

    if (updated.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(updated.get());
}
    
 @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getNotes(
        @PathVariable String id,
        Authentication authentication) {
 
    Map<String, String> context = extractUserContext(authentication);
 
    Optional<String> notes = companyService.getNotes(
            id,
            context.get("organizationId")   // ✅ from token
    );
 
    if (notes.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
 
    return ResponseEntity.ok(Map.of("notes", notes.get()));
}

// ---- GET linked contacts of a company ----
@GetMapping("/{id}/contacts")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> getLinkedContacts(@PathVariable String id,
                                            Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        List<Contact> contacts = companyService.getLinkedContacts(id, context.get("organizationId"));
        return ResponseEntity.ok(contacts);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Error", "message", e.getMessage()));
    }
}

// ---- Link a contact to a company ----
@PostMapping("/{id}/contacts/{contactId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> linkContact(@PathVariable String id,
                                      @PathVariable String contactId,
                                      Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        companyService.linkContactToCompany(id, contactId, context.get("organizationId"));
        return ResponseEntity.ok(Map.of("message", "Contact linked successfully"));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Link Error", "message", e.getMessage()));
    }
}

// ---- Unlink a contact from a company ----
@DeleteMapping("/{id}/contacts/{contactId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> unlinkContact(@PathVariable String id,
                                        @PathVariable String contactId,
                                        Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        companyService.unlinkContactFromCompany(id, contactId, context.get("organizationId"));
        return ResponseEntity.ok(Map.of("message", "Contact unlinked successfully"));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Unlink Error", "message", e.getMessage()));
    }
}

}