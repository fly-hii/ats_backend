package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.Models.ATS.OrganizationFieldConfig;
import com.example.ATSCIRCLE.Service.ATS.OrganizationFieldConfigService;
import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/field-configs")
public class OrganizationFieldConfigController {

    @Autowired
    private OrganizationFieldConfigService fieldConfigService;

    @Autowired
    private JwtService jwtService;

    // ✅ Helper method to extract organization context from JWT
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

    // Save or update field configuration for organization
   // Save or update field configuration for organization
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> saveOrUpdateFieldConfig(
            Authentication authentication,
            @RequestBody Map<String, List<String>> request) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            String userId = userContext.get("userId");
            
            List<String> selectedFields = request.get("selectedFields");
            if (selectedFields == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "selectedFields is required"));
            }
            
            OrganizationFieldConfig config = 
                    fieldConfigService.saveOrUpdateFieldConfig(organizationId, selectedFields, userId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error saving field configuration: " + e.getMessage()));
        }
    }

    // Get field configuration by organization ID (from token)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getFieldConfig(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            Optional<OrganizationFieldConfig> config = 
                    fieldConfigService.getFieldConfigByOrganization(organizationId);
            return config.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get selected fields for organization (from token)
    @GetMapping("/fields")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<String>> getSelectedFields(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<String> fields = fieldConfigService.getSelectedFieldsByOrganization(organizationId);
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Check if organization has field configuration (from token)
    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<Map<String, Boolean>> hasFieldConfig(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            boolean exists = fieldConfigService.hasFieldConfig(organizationId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Delete field configuration (from token)
    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> deleteFieldConfig(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            fieldConfigService.deleteFieldConfig(organizationId);
            return ResponseEntity.ok(Map.of("message", "Field configuration deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting field configuration: " + e.getMessage()));
        }
    }

    // Get all field configurations (Admin only - no token extraction needed)
    @GetMapping("/all")
    public ResponseEntity<List<OrganizationFieldConfig>> getAllFieldConfigs() {
        try {
            List<OrganizationFieldConfig> configs = fieldConfigService.getAllFieldConfigs();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Generate Excel template based on organization's selected fields
    @GetMapping("/template/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> downloadExcelTemplate(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");

            ByteArrayInputStream in = fieldConfigService.generateExcelTemplate(organizationId);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=candidate_application_template.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error generating template: " + e.getMessage()));
        }
    }
}