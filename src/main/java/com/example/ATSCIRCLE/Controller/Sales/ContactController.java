package com.example.ATSCIRCLE.Controller.Sales;

import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.example.ATSCIRCLE.Models.Sales.Contact;
import com.example.ATSCIRCLE.Service.Sales.ContactService;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @Autowired
    private JwtService jwtService;

    // ---------------------------------------------------------
    // Extract Context
    // ---------------------------------------------------------
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

    // ---------------------------------------------------------
    // Create Contact
    // ---------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> createContact(@RequestBody Contact contact,
                                           Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);

        try {
            Contact saved = contactService.createContact(
                    contact,
                    context.get("organizationId"),
                    context.get("userId"),
                    context.get("type")
            );

            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation Error",
                    "message", e.getMessage(),
                    "status", 400
            ));
        }
    }

    // ---------------------------------------------------------
    // Get All Contacts
    // ---------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getAllContacts(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);

        return ResponseEntity.ok(
                contactService.getAllContacts(
                        context.get("organizationId"),
                        context.get("type"),
                        context.get("userId")
                )
        );
    }

    // ---------------------------------------------------------
    // Get Contact by ID
    // ---------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getContactById(@PathVariable String id,
                                            Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);

        Optional<Contact> contact = contactService.getContactById(
                id,
                context.get("organizationId"),
                context.get("type"),
                context.get("userId")
        );

        return contact.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------
    // Update Contact
    // ---------------------------------------------------------
    @PutMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> updateContact(@PathVariable String id,
                                       @RequestBody Contact contact,
                                       Authentication authentication) {
    Map<String, String> context = extractUserContext(authentication);

    try {
        Contact updated = contactService.updateContact(
                id,
                contact,
                context.get("organizationId"),
                context.get("type"),      // userType
                context.get("userId")     // userEmail or user unique id
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


    // ---------------------------------------------------------
    // Delete Contact
    // ---------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteContact(@PathVariable String id,
                                           Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);

        try {
            contactService.deleteContact(
                    id,
                    context.get("organizationId"),
                    context.get("type")
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Contact deleted successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Delete Error",
                    "message", e.getMessage(),
                    "status", 400
            ));
        }
    }

     // ================================================
    //      GET CONTACTS FOR LOGGED-IN USER
    // ================================================
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getMyContacts(Authentication authentication) {

        Map<String, String> ctx = extractUserContext(authentication);

        String organizationId = ctx.get("organizationId");
        String userId = ctx.get("userId");

        List<Contact> contacts = contactService.getUserContacts(organizationId, userId);

        return ResponseEntity.ok(contacts);
    }

   

    // -------------------- GET ALL CONTACT COLUMNS --------------------
    @GetMapping("/columns")
     @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<List<String>> getColumns() {
        return ResponseEntity.ok(contactService.getAllColumns());
    }

    // -------------------- GENERATE EXCEL TEMPLATE --------------------
    @PostMapping("/generate-template")
   @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<InputStreamResource> generateTemplate(
            @RequestBody List<String> selectedColumns
    ) throws IOException {

        ByteArrayInputStream in = contactService.generateExcelTemplate(selectedColumns);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=contacts_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(in));
    }

    // -------------------- IMPORT CONTACTS FROM EXCEL --------------------
    @PostMapping("/import")
   @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file,
                                         Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);

            contactService.importExcel(
                    file,
                    context.get("organizationId"),
                    context.get("userId")
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Contacts imported successfully!"
            ));

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Import Error",
                    "message", e.getMessage()
            ));
        }
    }

    // -------------------- BULK DELETE CONTACTS --------------------
    @DeleteMapping("/delete")
@PreAuthorize("hasAnyRole('ADMIN')")
public ResponseEntity<?> deleteContacts(@RequestBody List<String> ids,
                                        Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        // Extract role directly from Spring Security instead of JWT type claim
        String userType = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(a -> a.equals("ADMIN") || a.equals("CRM"))
                .findFirst()
                .orElse("UNKNOWN");

        contactService.deleteContactsByIds(
                ids,
                context.get("organizationId"),
                userType  // now correctly "ADMIN" instead of null
        );

        return ResponseEntity.ok(Map.of(
                "message", ids.size() + " contacts deleted successfully."
        ));

    } catch (RuntimeException e) {
        return ResponseEntity.status(403).body(Map.of(
                "error", "Delete Error",
                "message", e.getMessage(),
                "status", 403
        ));
    }
}

    

    // -------------------- BULK UPDATE ASSIGNED TO --------------------
    @PutMapping("/update-assigned-to")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAssignedTo(@RequestBody Map<String, Object> payload,
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);

            List<String> ids = (List<String>) payload.get("ids");
            String assignedTo = (String) payload.get("assignedTo");

            contactService.updateAssignedTo(ids, assignedTo, context.get("organizationId"));

            return ResponseEntity.ok(Map.of(
                    "message", ids.size() + " contacts updated with new assignedTo: " + assignedTo
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Update Error",
                    "message", e.getMessage()
            ));
        }
    }

    // -------------------- BULK UPDATE ANY FIELD --------------------
    @PutMapping("/update-field")
    @PreAuthorize("hasRole('ADMIN')")
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

            contactService.updateContactsField(
                    ids,
                    fieldName,
                    fieldValue,
                    context.get("organizationId"),
                    context.get("type"),
                    context.get("userId")
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Updated field '" + fieldName + "' for " + ids.size() + " contacts."
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

    Optional<Contact> updated = contactService.updateNotes(
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

    Optional<String> notes = contactService.getNotes(
            id,
            context.get("organizationId")   // ✅ from token
    );

    if (notes.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(Map.of("notes", notes.get()));
}

   // ---- GET associated company of a contact ----
@GetMapping("/{id}/company")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> getAssociatedCompany(@PathVariable String id,
                                               Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        return contactService.getAssociatedCompany(id, context.get("organizationId"))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Error", "message", e.getMessage()));
    }
}
}