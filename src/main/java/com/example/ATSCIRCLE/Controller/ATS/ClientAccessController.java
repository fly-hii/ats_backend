package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Models.ATS.ClientShareableLink;
import com.example.ATSCIRCLE.Service.ATS.ClientShareableLinkService;
import com.example.ATSCIRCLE.Service.ATS.OrganizationFieldConfigService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/client-access")
public class ClientAccessController {

    @Autowired
    private ClientShareableLinkService linkService;

    @Autowired
    private OrganizationFieldConfigService fieldConfigService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{token}/validate
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{token}/validate")
    public ResponseEntity<?> validateLink(@PathVariable String token) {
        try {
            Optional<ClientShareableLink> linkOpt = linkService.validateAndGetLink(token);

            if (linkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Link is invalid or expired"));
            }

            ClientShareableLink link = linkOpt.get();
            return ResponseEntity.ok(Map.of(
                    "valid",            true,
                    "expiresAt",        link.getExpiresAt(),
                    "organizationId",   link.getOrganizationId(),
                    "clientId",         link.getClientId(),
                    "jobId",            link.getJobId(),
                    "applicationCount", link.getApplicationIds().size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error validating link"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{token}/applications
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{token}/applications")
    public ResponseEntity<?> getApplications(@PathVariable String token) {
        try {
            // FIX: validate ONCE here — accessCount incremented once
            Optional<ClientShareableLink> linkOpt = linkService.validateAndGetLink(token);
            if (linkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Link is invalid or expired"));
            }

            ClientShareableLink link = linkOpt.get();
            String organizationId = link.getOrganizationId();

            // FIX: pass token only for data fetch (no re-validation inside)
            List<CandidateApplication> applications = linkService.getApplicationsByToken(token);

            List<Map<String, Object>> filtered = applications.stream()
                    .map(app -> filterApplicationFields(app, organizationId))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("applications",    filtered);
            response.put("jobId",           link.getJobId());
            response.put("expiresAt",       link.getExpiresAt());
            response.put("accessCount",     link.getAccessCount());
            response.put("totalCandidates", filtered.size());
            response.put("clientEmail",     link.getClientEmail());
            response.put("token",           token);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching applications"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{token}/applications/{applicationId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{token}/applications/{applicationId}")
    public ResponseEntity<?> getApplicationById(@PathVariable String token,
                                                @PathVariable String applicationId) {
        try {
            Optional<ClientShareableLink> linkOpt = linkService.validateAndGetLink(token);
            if (linkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Link is invalid or expired"));
            }

            ClientShareableLink link = linkOpt.get();
            String organizationId = link.getOrganizationId();

            Optional<CandidateApplication> appOpt = linkService.getApplicationsByToken(token)
                    .stream().filter(a -> a.getId().equals(applicationId)).findFirst();

            if (appOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Application not found"));
            }

            return ResponseEntity.ok(filterApplicationFields(appOpt.get(), organizationId));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching application"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /{token}/applications/{applicationId}/status  — single update
    //
    // FIX: validateAndGetLink called ONCE. The resolved link object is passed
    //      into both updateClientTimeZoneAndSlots and updateApplicationStatusViaLink.
    //      accessCount is incremented exactly once per request.
    //
    // Body examples:
    //   { "status": "Shortlisted" }
    //   { "status": "Interview Scheduled", "timeslots": ["2026-03-01 10:00:00"], "clientTimeZone": "Asia/Kolkata" }
    //   { "status": "Selected", "givenCTC": "1200000" }
    //   { "status": "Rejected", "reason": "Not a fit" }
    // ─────────────────────────────────────────────────────────────────────────
@PatchMapping("/{token}/applications/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable String token,
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> payload) {
        try {
            Object statusObj = payload.get("status");
            if (statusObj == null || statusObj.toString().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "status is required"));
            }

            String status = statusObj.toString().trim();

         List<String> allowedStatuses = List.of(
    "Shortlisted", "Slotassigned", "Interview Scheduled", "Selected",
    "Rejected", "On Hold", "Under Review");

            if (!allowedStatuses.contains(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid status",
                                     "allowedStatuses", allowedStatuses));
            }

            // FIX: validate ONCE — accessCount incremented exactly once
            Optional<ClientShareableLink> linkOpt = linkService.validateAndGetLink(token);
            if (linkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Link is invalid or expired"));
            }
            ClientShareableLink link = linkOpt.get();

            // FIX: pass link object directly — no re-validation, no extra accessCount
            if (payload.containsKey("clientTimeZone")) {
                String clientTimeZone = (String) payload.get("clientTimeZone");
                linkService.updateClientTimeZoneAndSlots(link, clientTimeZone, payload);
            }

            // FIX: pass link object directly
            CandidateApplication updated =
                    linkService.updateApplicationStatusViaLink(link, applicationId, payload);

            Map<String, Object> result = new HashMap<>();
            result.put("message",       "Status updated successfully");
            result.put("applicationId", updated.getId());
            result.put("newStatus",     updated.getStatus());
            result.put("updatedAt",     updated.getUpdatedAt());

            if (updated.getgivenCTC() != null)
                result.put("givenCTC", updated.getgivenCTC());
            if (updated.getTimeslots() != null && !updated.getTimeslots().isEmpty())
                result.put("timeslots", updated.getTimeslots());
            if (updated.getReason() != null)
                result.put("reason", updated.getReason());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating status: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /{token}/applications/batch-update  — bulk update
    //
    // FIX: validateAndGetLink called ONCE. Link passed directly to service.
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{token}/applications/batch-update")
    public ResponseEntity<?> bulkUpdateStatuses(
            @PathVariable String token,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> applicationIds = (List<String>) request.get("applicationIds");

            if (applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "applicationIds array is required"));
            }

            Object statusObj = request.get("status");
            if (statusObj == null || statusObj.toString().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "status is required"));
            }

            // FIX: validate ONCE
            Optional<ClientShareableLink> linkOpt = linkService.validateAndGetLink(token);
            if (linkOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Link is invalid or expired"));
            }

            // FIX: pass link object directly
            List<CandidateApplication> updated =
                    linkService.updateMultipleStatusesViaLink(linkOpt.get(), applicationIds, request);

            List<Map<String, Object>> results = updated.stream().map(app -> {
                Map<String, Object> m = new HashMap<>();
                m.put("applicationId", app.getId());
                m.put("newStatus",     app.getStatus());
                m.put("success",       true);
                if (app.getgivenCTC() != null)     m.put("givenCTC", app.getgivenCTC());
                if (app.getReason() != null)        m.put("reason", app.getReason());
                if (app.getTimeslots() != null && !app.getTimeslots().isEmpty())
                    m.put("timeslots", app.getTimeslots());
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "message",      "Bulk update completed successfully",
                    "updatedCount", updated.size(),
                    "results",      results));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> filterApplicationFields(CandidateApplication application,
                                                         String organizationId) {
        List<String> selectedFields =
                fieldConfigService.getSelectedFieldsByOrganization(organizationId);

        if (selectedFields == null || selectedFields.isEmpty()) {
            return extractAllNonNullFields(application);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id",            application.getId());
        data.put("applicationId", application.getApplicationId());
        data.put("status",        application.getStatus());

        if (application.getReason() != null)
            data.put("reason", application.getReason());
        if (application.getTimeslots() != null && !application.getTimeslots().isEmpty())
            data.put("timeslots", application.getTimeslots());
        if (application.getgivenCTC() != null)
            data.put("givenCTC", application.getgivenCTC());

        Field[] fields = CandidateApplication.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (selectedFields.contains(fieldName)) {
                try {
                    Object value = field.get(application);
                    if (value != null) {
                        if (value instanceof List) {
                            if (!((List<?>) value).isEmpty()) data.put(fieldName, value);
                        } else {
                            data.put(fieldName, value);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

    private Map<String, Object> extractAllNonNullFields(CandidateApplication application) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Field field : CandidateApplication.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(application);
                if (value != null) {
                    if (value instanceof List) {
                        if (!((List<?>) value).isEmpty()) fields.put(field.getName(), value);
                    } else {
                        fields.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return fields;
    }
}