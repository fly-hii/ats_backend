package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Models.ATS.ClientShareableLink;
import com.example.ATSCIRCLE.Service.ATS.ClientShareableLinkService;
import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Authenticated controller — handles all recruiter-side operations:
 *   - creating shareable links
 *   - managing (revoke / extend / list) links
 *   - sending emails (two separate endpoints)
 */
@RestController
@RequestMapping("/api/shareable-links")
public class ClientShareableLinkController {

    @Autowired
    private ClientShareableLinkService linkService;

    @Autowired
    private JwtService jwtService;

    // ── JWT helper ─────────────────────────────────────────────────────────
    private Map<String, String> extractUserContext(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return Map.of(
                "organizationId", jwtService.extractOrganizationId(token),
                "userId",         jwtService.extractId(token),
                "type",           jwtService.extractType(token)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/shareable-links
    // Create a new shareable link (does NOT send any email)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> createShareableLink(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, String> ctx = extractUserContext(authentication);
            String organizationId = ctx.get("organizationId");

            String clientId    = (String) request.get("clientId");
            String jobId       = (String) request.get("jobId");
            String clientEmail = (String) request.get("clientEmail");

            @SuppressWarnings("unchecked")
            List<String> applicationIds = (List<String>) request.get("applicationIds");

            Integer expiryHours = request.containsKey("expiryHours")
                    ? ((Number) request.get("expiryHours")).intValue()
                    : null;

            if (clientId == null || jobId == null || clientEmail == null
                    || applicationIds == null || applicationIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message",
                                "clientId, jobId, clientEmail and applicationIds are required"));
            }

            ClientShareableLink link = linkService.createShareableLink(
                    organizationId, clientId, jobId, clientEmail, applicationIds, expiryHours);

            String shareableUrl = linkService.generateShareableUrl(link.getToken());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message",       "Shareable link created successfully",
                    "linkId",        link.getId(),
                    "token",         link.getToken(),
                    "shareableUrl",  shareableUrl,
                    "expiresAt",     link.getExpiresAt(),
                    "clientEmail",   link.getClientEmail(),
                    "candidateCount", applicationIds.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating link: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMAIL ENDPOINT 1
    // POST /api/shareable-links/{linkId}/send-link
    //
    // Sends an email to the client containing ONLY the portal URL.
    // The client clicks the link to open the browser review portal.
    // No Excel file is attached.
    //
    // Request body: (empty — uses clientEmail stored on the link)
    // ═════════════════════════════════════════════════════════════════════════
    @PostMapping("/{linkId}/send-link")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> sendShareableLinkEmail(
            Authentication authentication,
            @PathVariable String linkId) {
        try {
            linkService.sendShareableLinkEmail(linkId);

            return ResponseEntity.ok(Map.of(
                    "message", "Portal link email sent successfully",
                    "linkId",  linkId,
                    "type",    "link-only"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send email: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMAIL ENDPOINT 2
    // POST /api/shareable-links/{linkId}/send-attachment
    //
    // Sends an email to the client with an Excel file of candidates attached.
    // No portal URL is included in the email body.
    // The Excel is built on-the-fly from all applicationIds on the link.
    //
    // Request body: (empty — uses clientEmail stored on the link)
    // ═════════════════════════════════════════════════════════════════════════
    @PostMapping("/{linkId}/send-attachment")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> sendCandidateListAttachmentEmail(
            Authentication authentication,
            @PathVariable String linkId) {
        try {
            linkService.sendCandidateListAttachmentEmail(linkId);

            return ResponseEntity.ok(Map.of(
                    "message", "Candidate list attachment email sent successfully",
                    "linkId",  linkId,
                    "type",    "attachment-only"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send email: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/shareable-links  — list all links for this organisation
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getAllLinks(Authentication authentication) {
        try {
            String organizationId = extractUserContext(authentication).get("organizationId");
            List<ClientShareableLink> links = linkService.getLinksByOrganization(organizationId);
            return ResponseEntity.ok(links);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching links: " + e.getMessage()));
        }
    }

    // GET /api/shareable-links/active
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getActiveLinks(Authentication authentication) {
        try {
            String organizationId = extractUserContext(authentication).get("organizationId");
            List<ClientShareableLink> links =
                    linkService.getActiveLinksByOrganization(organizationId);
            return ResponseEntity.ok(links);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching active links: " + e.getMessage()));
        }
    }

    // GET /api/shareable-links/job/{jobId}
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getLinksByJob(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(linkService.getLinksByJob(jobId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // GET /api/shareable-links/client/{clientId}
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getLinksByClient(@PathVariable String clientId) {
        try {
            return ResponseEntity.ok(linkService.getLinksByClient(clientId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/shareable-links/{linkId}/revoke
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{linkId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> revokeLink(@PathVariable String linkId) {
        try {
            linkService.revokeLink(linkId);
            return ResponseEntity.ok(Map.of("message", "Link revoked successfully", "linkId", linkId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error revoking link: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/shareable-links/{linkId}/extend
    // Body: { "hours": 24 }
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{linkId}/extend")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> extendLinkExpiry(
            @PathVariable String linkId,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer hours = body.get("hours");
            if (hours == null || hours <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "hours must be a positive integer"));
            }
            ClientShareableLink updated = linkService.extendLinkExpiry(linkId, hours);
            return ResponseEntity.ok(Map.of(
                    "message",    "Expiry extended by " + hours + " hour(s)",
                    "linkId",     updated.getId(),
                    "newExpiresAt", updated.getExpiresAt()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error extending link: " + e.getMessage()));
        }
    }
    @DeleteMapping("/cleanup/expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> cleanupExpiredLinks() {
        try {
            linkService.cleanupExpiredLinks();
            return ResponseEntity.ok(Map.of(
                    "message", "Expired links marked as expired successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Cleanup failed: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/shareable-links/cleanup/old?daysOld=30
    //
    // Permanently deletes all links whose expiresAt is older than N days.
    // Default: 30 days if not provided.
    // Use with caution — this is a hard delete from the database.
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/cleanup/old")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> deleteOldLinks(
            @RequestParam(value = "daysOld", defaultValue = "30") int daysOld) {
        try {
            if (daysOld <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "daysOld must be a positive integer"));
            }
            linkService.deleteOldLinks(daysOld);
            return ResponseEntity.ok(Map.of(
                    "message", "Links older than " + daysOld + " day(s) permanently deleted",
                    "daysOld", daysOld
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Delete failed: " + e.getMessage()));
        }
    }
}