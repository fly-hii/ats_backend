package com.example.ATSCIRCLE.Controller.Sales;

import com.example.ATSCIRCLE.Models.Sales.EmailLog;
import com.example.ATSCIRCLE.Service.Sales.BulkEmailService;
import com.example.ATSCIRCLE.Service.Sales.ExcelEmailHelper;
import com.example.ATSCIRCLE.DTO.EmailRecipient;
import com.example.ATSCIRCLE.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/bulk-email")
public class BulkEmailController {

    @Autowired
    private BulkEmailService emailService;

    @Autowired
    private JwtService jwtService;

    // ================================================================================
    //                    ENTITY TYPES ENUM
    // ================================================================================

    public enum EntityType {
        CONTACT,
        COMPANY,
        CLIENT
    }

    // ================================================================================
    //                    SEND EMAIL BY IDs
    // ================================================================================

    /**
     * Send bulk email to CONTACT / COMPANY / CLIENT by their IDs
     *
     * Request params:
     *   entityType  - CONTACT | COMPANY | CLIENT
     *   entityIds   - list of IDs from the respective table
     *   subject     - email subject
     *   body        - email body (HTML allowed)
     *   attachments - (optional) files to attach
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM', 'EMPLOYEE', 'ATS')")
    public ResponseEntity<?> sendBulkEmail(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityIds") List<String> entityIds,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            Authentication authentication) {

        try {
            // Validate inputs
            ErrorResponse validationError = validateSendEmailInput(entityType, entityIds, subject, body);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            Map<String, String> context = extractUserContext(authentication);

            EntityType entityTypeEnum;
            try {
                entityTypeEnum = EntityType.valueOf(entityType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "Invalid Entity Type",
                        "entityType must be one of: CONTACT, COMPANY, CLIENT",
                        400
                ));
            }

            EmailLog emailLog;
            switch (entityTypeEnum) {
                case CONTACT:
                    emailLog = emailService.sendEmailToContacts(
                            context.get("organizationId"),
                            context.get("userId"),
                            entityIds, subject, body, attachments);
                    break;

                case COMPANY:
                    emailLog = emailService.sendEmailToCompanies(
                            context.get("organizationId"),
                            context.get("userId"),
                            entityIds, subject, body, attachments);
                    break;

                case CLIENT:
                    emailLog = emailService.sendEmailToClients(
                            context.get("organizationId"),
                            context.get("userId"),
                            entityIds, subject, body, attachments);
                    break;

                default:
                    return ResponseEntity.badRequest().body(new ErrorResponse(
                            "Invalid Entity Type",
                            "entityType must be one of: CONTACT, COMPANY, CLIENT",
                            400
                    ));
            }

            return ResponseEntity.ok(buildSuccessResponse(emailLog, entityTypeEnum));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Email Send Error", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "Unexpected Error", e.getMessage(), 500));
        }
    }

    // ================================================================================
    //                    SEND EMAIL VIA EXCEL
    // ================================================================================

    /**
     * Send bulk email via Excel file for CONTACT / COMPANY / CLIENT
     *
     * Excel columns expected: email, fullname (or firstname + lastname), companyname
     *
     * Request params:
     *   entityType  - CONTACT | COMPANY | CLIENT
     *   excelFile   - .xlsx / .xls file with recipient data
     *   subject     - email subject
     *   body        - email body (HTML allowed)
     *   attachments - (optional) files to attach
     */
    @PostMapping("/send-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM', 'EMPLOYEE', 'ATS')")
    public ResponseEntity<?> sendBulkEmailViaExcel(
            @RequestParam("entityType") String entityType,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            Authentication authentication) {

        try {
            // Validate inputs
            ErrorResponse validationError = validateExcelEmailInput(entityType, excelFile, subject, body);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            Map<String, String> context = extractUserContext(authentication);

            // Parse Excel file
            List<EmailRecipient> recipients;
            try {
                recipients = ExcelEmailHelper.parseExcelForEmails(excelFile.getInputStream());
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "File Processing Error",
                        "Failed to read Excel file: " + e.getMessage(),
                        400
                ));
            }

            if (recipients == null || recipients.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "Excel Parsing Error",
                        "No valid email recipients found in Excel file. Ensure 'email' column exists.",
                        400
                ));
            }

            EntityType entityTypeEnum;
            try {
                entityTypeEnum = EntityType.valueOf(entityType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "Invalid Entity Type",
                        "entityType must be one of: CONTACT, COMPANY, CLIENT",
                        400
                ));
            }

            EmailLog emailLog;
            switch (entityTypeEnum) {
                case CONTACT:
                    // Contact Excel -> use fullName from Excel as greeting
                    emailLog = emailService.sendEmailToDirectRecipients(
                            context.get("organizationId"),
                            context.get("userId"),
                            recipients, subject, body, attachments);
                    break;

                case COMPANY:
                    emailLog = emailService.sendEmailToCompaniesViaExcel(
                            context.get("organizationId"),
                            context.get("userId"),
                            recipients, subject, body, attachments);
                    break;

                case CLIENT:
                    emailLog = emailService.sendEmailToClientsViaExcel(
                            context.get("organizationId"),
                            context.get("userId"),
                            recipients, subject, body, attachments);
                    break;

                default:
                    return ResponseEntity.badRequest().body(new ErrorResponse(
                            "Invalid Entity Type",
                            "entityType must be one of: CONTACT, COMPANY, CLIENT",
                            400
                    ));
            }

            return ResponseEntity.ok(buildSuccessResponse(emailLog, entityTypeEnum));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Email Send Error", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "Unexpected Error", e.getMessage(), 500));
        }
    }

    // ================================================================================
    //                    GET EMAIL LOGS
    // ================================================================================

    /**
     * Get all email logs for the organization
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM', 'EMPLOYEE', 'ATS')")
    public ResponseEntity<?> getEmailLogs(Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            List<EmailLog> logs = emailService.getEmailLogs(context.get("organizationId"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", logs.size(),
                    "logs", logs
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Error", "Failed to retrieve email logs: " + e.getMessage(), 400));
        }
    }

    /**
     * Get email logs filtered by status
     * Status values: PENDING, SUCCESS, PARTIAL_FAILED, FAILED
     */
    @GetMapping("/logs/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM', 'EMPLOYEE', 'ATS')")
    public ResponseEntity<?> getEmailLogsByStatus(
            @PathVariable String status,
            Authentication authentication) {

        try {
            Map<String, String> context = extractUserContext(authentication);

            EmailLog.EmailStatus emailStatus;
            try {
                emailStatus = EmailLog.EmailStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "Invalid Status",
                        "Status must be one of: PENDING, SUCCESS, PARTIAL_FAILED, FAILED",
                        400
                ));
            }

            List<EmailLog> logs = emailService.getEmailLogsByStatus(
                    context.get("organizationId"), emailStatus);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", status.toUpperCase(),
                    "total", logs.size(),
                    "logs", logs
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Error", "Failed to retrieve email logs: " + e.getMessage(), 400));
        }
    }

    /**
     * Get a specific email log by ID
     */
    @GetMapping("/logs/{emailLogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM', 'EMPLOYEE', 'ATS')")
    public ResponseEntity<?> getEmailLogById(@PathVariable String emailLogId) {
        try {
            return emailService.getEmailLogById(emailLogId)
                    .map(log -> ResponseEntity.ok((Object) log))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Error", "Failed to retrieve email log: " + e.getMessage(), 400));
        }
    }

    // ================================================================================
    //                    HELPER METHODS
    // ================================================================================

    /**
     * Extract organizationId and userId from JWT token
     */
    private Map<String, String> extractUserContext(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        String organizationId = jwtService.extractOrganizationId(token);
        String userId = jwtService.extractId(token);

        return Map.of(
                "organizationId", organizationId,
                "userId", userId
        );
    }

    /**
     * Build a consistent success response with email log details
     */
    private Map<String, Object> buildSuccessResponse(EmailLog emailLog, EntityType entityType) {
        List<String> failedEmails = emailLog.getFailedEmails() != null
                ? emailLog.getFailedEmails()
                : new ArrayList<>();

        int totalRecipients = emailLog.getRecipientEmails() != null
                ? emailLog.getRecipientEmails().size()
                : 0;

        int successCount = totalRecipients - failedEmails.size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("emailLogId", emailLog.getId());
        response.put("entityType", entityType.toString());
        response.put("status", emailLog.getStatus().toString());
        response.put("totalRecipients", totalRecipients);
        response.put("successCount", successCount);
        response.put("failedCount", failedEmails.size());
        response.put("failedEmails", failedEmails);
        response.put("attachmentUrls", emailLog.getAttachmentUrls() != null
                ? emailLog.getAttachmentUrls()
                : new ArrayList<>());
        response.put("sentAt", emailLog.getSentAt() != null
                ? emailLog.getSentAt().toString()
                : null);

        return response;
    }

    /**
     * Validate send-by-IDs input
     */
    private ErrorResponse validateSendEmailInput(String entityType, List<String> entityIds,
                                                  String subject, String body) {
        if (entityType == null || entityType.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "entityType is required. Must be: CONTACT, COMPANY, or CLIENT", 400);
        }
        if (entityIds == null || entityIds.isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "At least one entity ID is required", 400);
        }
        if (subject == null || subject.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "Subject is required and cannot be empty", 400);
        }
        if (body == null || body.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "Email body is required and cannot be empty", 400);
        }
        return null;
    }

    /**
     * Validate send-via-Excel input
     */
    private ErrorResponse validateExcelEmailInput(String entityType, MultipartFile excelFile,
                                                   String subject, String body) {
        if (entityType == null || entityType.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "entityType is required. Must be: CONTACT, COMPANY, or CLIENT", 400);
        }
        if (excelFile == null || excelFile.isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "Excel file is required", 400);
        }
        if (!isValidExcelFile(excelFile)) {
            return new ErrorResponse("Validation Error",
                    "Invalid file type. Only .xlsx and .xls files are supported", 400);
        }
        if (subject == null || subject.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "Subject is required and cannot be empty", 400);
        }
        if (body == null || body.trim().isEmpty()) {
            return new ErrorResponse("Validation Error",
                    "Email body is required and cannot be empty", 400);
        }
        return null;
    }

    /**
     * Check if uploaded file is a valid Excel file
     */
    private boolean isValidExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    // ================================================================================
    //                    ERROR RESPONSE CLASS
    // ================================================================================

    public static class ErrorResponse {
        private final boolean success = false;
        private String error;
        private String message;
        private int status;

        public ErrorResponse(String error, String message, int status) {
            this.error = error;
            this.message = message;
            this.status = status;
        }

        public boolean isSuccess()    { return success; }
        public String getError()      { return error; }
        public String getMessage()    { return message; }
        public int getStatus()        { return status; }

        public void setError(String error)     { this.error = error; }
        public void setMessage(String message) { this.message = message; }
        public void setStatus(int status)      { this.status = status; }
    }
}