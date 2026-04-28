package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;

import com.example.ATSCIRCLE.Service.ATS.CandidateApplicationService;
import com.example.ATSCIRCLE.Service.ATS.OrganizationFieldConfigService;
import com.example.ATSCIRCLE.Service.ATS.InterviewScheduleService;
import com.example.ATSCIRCLE.DTO.ScheduleMeetDTO;
import com.example.ATSCIRCLE.DTO.ConvertSlotsDTO;
import com.example.ATSCIRCLE.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;

import com.example.ATSCIRCLE.Service.ATS.S3FileUploadService;


@RestController
@RequestMapping("/api/applications")
public class CandidatepplicationController {

    @Autowired
    private CandidateApplicationService applicationService;

    @Autowired
    private OrganizationFieldConfigService fieldConfigService;
    
    @Autowired
    private InterviewScheduleService interviewScheduleService;

  
@Autowired
private S3FileUploadService s3FileUploadService;

    @Autowired
    private ObjectMapper objectMapper;

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

    // Helper method to filter application based on organization's selected fields
    private Map<String, Object> filterApplicationFields(CandidateApplication application, String organizationId) {
        List<String> selectedFields = fieldConfigService.getSelectedFieldsByOrganization(organizationId);
        
        if (selectedFields == null || selectedFields.isEmpty()) {
            return extractAllNonNullFields(application);
        }
        
        Map<String, Object> filteredData = new LinkedHashMap<>();
        Field[] fields = CandidateApplication.class.getDeclaredFields();
        
        // Always include core fields
        filteredData.put("id", application.getId());
        filteredData.put("organizationId", application.getOrganizationId());
        filteredData.put("applicationId", application.getApplicationId());
        filteredData.put("jobIds", application.getJobIds());
        filteredData.put("clientIds", application.getClientIds());
        filteredData.put("status", application.getStatus());
        filteredData.put("givenCTC", application.getgivenCTC());
        filteredData.put("reason", application.getReason());
        filteredData.put("actualjoiningdate", application.getactualjoiningdate());
        filteredData.put("selectedslot", application.getSelectedSlot());
        filteredData.put("timeslots", application.getTimeslots());
        filteredData.put("createdAt", application.getCreatedAt());
        filteredData.put("updatedAt", application.getUpdatedAt());
        filteredData.put("createdBy", application.getCreatedBy());
        filteredData.put("updatedBy", application.getUpdatedBy());
        
        // Add only selected fields
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            
            if (selectedFields.contains(fieldName)) {
                try {
                    Object value = field.get(application);
                    if (value != null) {
                        if (value instanceof List) {
                            if (!((List<?>) value).isEmpty()) {
                                filteredData.put(fieldName, value);
                            }
                        } else {
                            filteredData.put(fieldName, value);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return filteredData;
    }

    // Helper method to extract all non-null fields
    private Map<String, Object> extractAllNonNullFields(CandidateApplication application) {
        Map<String, Object> filledFields = new LinkedHashMap<>();
        Field[] fields = CandidateApplication.class.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(application);
                if (value != null) {
                    if (value instanceof List) {
                        if (!((List<?>) value).isEmpty()) {
                            filledFields.put(field.getName(), value);
                        }
                    } else {
                        filledFields.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        return filledFields;
    }

    // Get all available field names
    @GetMapping("/fields/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<String>> getAllAvailableFields() {
        try {
            List<String> fields = applicationService.getAllAvailableFields();
            return ResponseEntity.ok(fields);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create new application (Organization ID from token)
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
public ResponseEntity createApplication(
        Authentication authentication,
      @RequestPart(value = "data") String dataJson,  // Changed from @RequestParam to @RequestPart
        @RequestPart(value = "resume", required = false) MultipartFile resume,
        @RequestPart(value = "coverLetter", required = false) MultipartFile coverLetter,
        @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto,
        @RequestPart(value = "professionalLicenses", required = false) MultipartFile professionalLicenses,
        @RequestPart(value = "certifications", required = false) MultipartFile certifications,
        @RequestPart(value = "supportingDocuments", required = false) MultipartFile supportingDocuments,
        @RequestPart(value = "areaCertifications", required = false) MultipartFile areaCertifications){
    try {
        Map<String, String> userContext = extractUserContext(authentication);
        String organizationId = userContext.get("organizationId");
        String userId = userContext.get("userId");

        // Parse JSON data
        Map<String, Object> requestBody = objectMapper.readValue(dataJson, Map.class);

        // Extract single jobId and clientId from request
        String jobId = (String) requestBody.get("jobId");
        String clientId = (String) requestBody.get("clientId");

        // Validate required fields
        if (jobId == null || jobId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "jobId is required"));
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "clientId is required"));
        }

        // Get email from request
        String email = (String) requestBody.get("primaryEmail");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "primaryEmail is required"));
        }

        // Upload files to S3 if provided
        if (resume != null && !resume.isEmpty()) {
            String resumeUrl = s3FileUploadService.uploadResume(resume, organizationId);
            requestBody.put("resumeUrl", resumeUrl);
        }
        if (coverLetter != null && !coverLetter.isEmpty()) {
            String coverLetterUrl = s3FileUploadService.uploadCoverLetter(coverLetter, organizationId);
            requestBody.put("coverLetterUrl", coverLetterUrl);
        }
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String profilePhotoUrl = s3FileUploadService.uploadProfilePhoto(profilePhoto, organizationId);
            requestBody.put("profilePhotoUrl", profilePhotoUrl);
        }
        if (professionalLicenses != null && !professionalLicenses.isEmpty()) {
            String licensesUrl = s3FileUploadService.uploadProfessionalLicenses(professionalLicenses, organizationId);
            requestBody.put("professionalLicenses", licensesUrl);
        }
        if (certifications != null && !certifications.isEmpty()) {
            String certificationsUrl = s3FileUploadService.uploadCertifications(certifications, organizationId);
            requestBody.put("certificationsUpload", certificationsUrl);
        }
        if (supportingDocuments != null && !supportingDocuments.isEmpty()) {
            String supportingDocsUrl = s3FileUploadService.uploadSupportingDocuments(supportingDocuments, organizationId);
            requestBody.put("supportingDocuments", supportingDocsUrl);
        }
        if (areaCertifications != null && !areaCertifications.isEmpty()) {
            String areaCertUrl = s3FileUploadService.uploadAreaProfessionalCertifications(areaCertifications, organizationId);
            requestBody.put("areaProfessionalCertifications", areaCertUrl);
        }


        Optional<CandidateApplication> existingApp = applicationService.getApplicationByEmail(email);

        if (existingApp.isPresent()) {
            CandidateApplication existing = existingApp.get();

            // Check if same client already exists (regardless of job)
            if (existing.getClientIds() != null && existing.getClientIds().contains(clientId)) {
                // Check if same job also exists with this client
                for (int i = 0; i < existing.getClientIds().size(); i++) {
                    if (existing.getClientIds().get(i).equals(clientId)) {
                        if (existing.getJobIds().get(i).equals(jobId)) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "Candidate has already applied for this job with this client"));
                        } else {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "Candidate has already applied to this client. Multiple job applications to the same client are not allowed"));
                        }
                    }
                }
            }

            // Append new job and client to existing application (only if different client)
            CandidateApplication updated = applicationService.appendJobToApplication(
                    existing.getId(), jobId, clientId, userId);

            // Filter response based on organization config
            Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
            return ResponseEntity.ok(Map.of(
                    "message", "Job added to existing application",
                    "application", filteredResponse
            ));
        }

        // Create new application if email doesn't exist
        CandidateApplication newApplication = applicationService.createApplicationFromMap(
                requestBody, organizationId, jobId, clientId, userId);

        // Filter response based on organization config
        Map<String, Object> filteredResponse = filterApplicationFields(newApplication, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(filteredResponse);

    } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "File upload error: " + e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error creating application: " + e.getMessage()));
    }
}
@PutMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
public ResponseEntity updateApplication(
        Authentication authentication,
        @PathVariable String id,
        @RequestBody Map<String, Object> updates) {  // ✅ Changed to @RequestBody
    try {
        Map<String, String> userContext = extractUserContext(authentication);
        String organizationId = userContext.get("organizationId");
        String userId = userContext.get("userId");

        // Get existing application
        Optional<CandidateApplication> existingApp = applicationService.getApplicationById(id);
        if (!existingApp.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Application not found"));
        }

        // Update the application
        CandidateApplication updated = applicationService.updateApplicationPartial(id, updates, userId);

        // Filter response based on organization config
        Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
        return ResponseEntity.ok(filteredResponse);

    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error updating application: " + e.getMessage()));
    }

}
    // Get application by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getApplicationById(
            Authentication authentication,
            @PathVariable String id) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            Optional<CandidateApplication> application = applicationService.getApplicationById(id);
            
            if (application.isPresent()) {
                Map<String, Object> filteredResponse = filterApplicationFields(application.get(), organizationId);
                return ResponseEntity.ok(filteredResponse);
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get application by application ID
    @GetMapping("/application-id/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getApplicationByApplicationId(
            Authentication authentication,
            @PathVariable String applicationId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            Optional<CandidateApplication> application = 
                    applicationService.getApplicationByApplicationId(applicationId);
            
            if (application.isPresent()) {
                Map<String, Object> filteredResponse = filterApplicationFields(application.get(), organizationId);
                return ResponseEntity.ok(filteredResponse);
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all applications
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getAllApplications(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationId(organizationId);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get applications by job ID
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getApplicationsByJob(
            Authentication authentication,
            @PathVariable String jobId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationAndJob(organizationId, jobId);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get applications by client ID
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getApplicationsByClient(
            Authentication authentication,
            @PathVariable String clientId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationAndClient(organizationId, clientId);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get applications by client and job
    @GetMapping("/client/{clientId}/job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getApplicationsByClientAndJob(
            Authentication authentication,
            @PathVariable String clientId, 
            @PathVariable String jobId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationClientAndJob(
                        organizationId, clientId, jobId);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get applications by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getApplicationsByStatus(
            Authentication authentication,
            @PathVariable String status) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationAndStatus(organizationId, status);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get applications by job and status
    @GetMapping("/job/{jobId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<List<Map<String, Object>>> getApplicationsByJobAndStatus(
            Authentication authentication,
            @PathVariable String jobId, 
            @PathVariable String status) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            List<CandidateApplication> applications = 
                    applicationService.getApplicationsByOrganizationJobAndStatus(
                        organizationId, jobId, status);
            
            List<Map<String, Object>> filteredApplications = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredApplications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update application status
    @PatchMapping("/{id}/status")
@PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
public ResponseEntity<?> updateApplicationStatus(
        Authentication authentication,
        @PathVariable String id,
        @RequestBody Map<String, Object> statusPayload) {   // <── now accepts full payload
    try {
        Map<String, String> userContext = extractUserContext(authentication);
        String organizationId = userContext.get("organizationId");
        String userId = userContext.get("userId");

        // Validate status field
        Object statusObj = statusPayload.get("status");
        if (statusObj == null || statusObj.toString().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "status is required"));
        }

        String status = statusObj.toString().trim();

        // Delegate to service — validation of conditional fields happens there
        CandidateApplication updated = applicationService.updateApplicationStatus(
                id, status, statusPayload, userId);

        Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
        return ResponseEntity.ok(filteredResponse);

    } catch (IllegalArgumentException e) {
        // Missing required field for the given status
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error updating status: " + e.getMessage()));
    }
}

    // Delete application
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteApplication(@PathVariable String id) {
        try {
            applicationService.deleteApplication(id);
            return ResponseEntity.ok(Map.of("message", "Application deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting application: " + e.getMessage()));
        }
    }

    // Count applications
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<Map<String, Long>> countApplications(Authentication authentication) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            
            Long count = applicationService.countApplicationsByOrganization(organizationId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Count applications by job
    @GetMapping("/job/{jobId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<Map<String, Long>> countApplicationsByJob(@PathVariable String jobId) {
        try {
            Long count = applicationService.countApplicationsByJob(jobId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Count applications by status
    @GetMapping("/status/{status}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<Map<String, Long>> countApplicationsByStatus(@PathVariable String status) {
        try {
            Long count = applicationService.countApplicationsByStatus(status);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Check if email exists
    @GetMapping("/email/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = applicationService.isEmailExists(email);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Add job to existing application
    @PostMapping("/{applicationId}/add-job")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> addJobToApplication(
            Authentication authentication,
            @PathVariable String applicationId,
            @RequestBody Map<String, String> jobDetails) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String userId = userContext.get("userId");
            
            String jobId = jobDetails.get("jobId");
            String clientId = jobDetails.get("clientId");
            
            if (jobId == null || clientId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "jobId and clientId are required"));
            }
            
            Optional<CandidateApplication> existingApp = applicationService.getApplicationById(applicationId);
            
            if (!existingApp.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Application not found"));
            }
            
            CandidateApplication application = existingApp.get();
            
            if (application.getClientIds() != null && application.getClientIds().contains(clientId)) {
                for (int i = 0; i < application.getClientIds().size(); i++) {
                    if (application.getClientIds().get(i).equals(clientId)) {
                        if (application.getJobIds().get(i).equals(jobId)) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "Candidate has already applied for this job with this client"));
                        } else {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "Candidate has already applied to this client. Multiple job applications to the same client are not allowed"));
                        }
                    }
                }
            }
            
            CandidateApplication updated = 
                    applicationService.addJobToApplication(applicationId, jobId, clientId, userId);
            
            Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
            return ResponseEntity.ok(filteredResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error adding job: " + e.getMessage()));
        }
    }

    // Remove job from application (Continuation)
    @DeleteMapping("/{applicationId}/remove-job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> removeJobFromApplication(
            Authentication authentication,
            @PathVariable String applicationId,
            @PathVariable String jobId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String userId = userContext.get("userId");
            
            CandidateApplication updated = 
                    applicationService.removeJobFromApplication(applicationId, jobId, userId);
            
            Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
            return ResponseEntity.ok(filteredResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error removing job: " + e.getMessage()));
        }
    }

    // Add multiple jobs to application
    @PostMapping("/{applicationId}/add-multiple-jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> addMultipleJobsToApplication(
            Authentication authentication,
            @PathVariable String applicationId,
            @RequestBody Map<String, List<String>> jobDetails) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String userId = userContext.get("userId");
            
            List<String> jobIds = jobDetails.get("jobIds");
            List<String> clientIds = jobDetails.get("clientIds");
            
            if (jobIds == null || clientIds == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "jobIds and clientIds are required"));
            }
            
            if (jobIds.size() != clientIds.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "jobIds and clientIds must have same size"));
            }
            
            CandidateApplication updated = 
                    applicationService.addMultipleJobsToApplication(applicationId, jobIds, clientIds, userId);
            
            Map<String, Object> filteredResponse = filterApplicationFields(updated, organizationId);
            return ResponseEntity.ok(filteredResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error adding jobs: " + e.getMessage()));
        }
    }

    // Bulk import applications from file (NO VALIDATIONS)
    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> bulkImportApplications(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") String jobId,
            @RequestParam("clientId") String clientId) {
        try {
            Map<String, String> userContext = extractUserContext(authentication);
            String organizationId = userContext.get("organizationId");
            String userId = userContext.get("userId");
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "File is empty"));
            }
            
            // Validate jobId and clientId
            if (jobId == null || jobId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "jobId is required"));
            }
            
            if (clientId == null || clientId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "clientId is required"));
            }
            
            // Check file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Only CSV and Excel files are supported"));
            }
            
            // Process bulk import
            Map<String, Object> result = applicationService.bulkImportApplicationsFromFile(
                    file, organizationId, jobId, clientId, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error during bulk import: " + e.getMessage()));
        }
    }

  
    @GetMapping("/{applicationId}/convert-slots")
    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('ATS')")
    public ResponseEntity<?> convertSlotsToUserTimeZone(
            @PathVariable String applicationId,
            @RequestParam String userTimeZone,
            Authentication authentication) {
        try {
            // Validate timezone
            if (userTimeZone == null || userTimeZone.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "userTimeZone parameter is required"));
            }
            
            List<String> convertedSlots = interviewScheduleService.getConvertedSlots(
                applicationId,
                userTimeZone
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Slots converted successfully",
                "original_timezone", "Client timezone",
                "target_timezone", userTimeZone,
                "slots", convertedSlots
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error converting slots: " + e.getMessage()
                    ));
        }
    }

  
    @PostMapping("/schedule-meet")
    @PreAuthorize("hasAuthority('ATS')")
    public ResponseEntity<?> scheduleInterviewMeeting(
            @RequestBody ScheduleMeetDTO scheduleMeetDTO,
            Authentication authentication) {
        try {
            // Extract user ID from JWT token
            String token = (String) authentication.getCredentials();
            String userId = jwtService.extractId(token);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "User not authenticated"));
            }
            
            // Validate required fields
            if (scheduleMeetDTO.getCandidateApplicationId() == null || 
                scheduleMeetDTO.getSelectedSlot() == null ||
                scheduleMeetDTO.getUserTimeZone() == null ||
                scheduleMeetDTO.getJobId() == null ||
                scheduleMeetDTO.getCandidateEmail() == null ||
                scheduleMeetDTO.getToEmail() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Missing required fields"));
            }
            
            String result = interviewScheduleService.scheduleInterviewMeeting(userId, scheduleMeetDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result
            ));
        } catch (Exception e) {
            System.err.println("Error scheduling interview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error scheduling interview: " + e.getMessage()
                    ));
        }
    }
}