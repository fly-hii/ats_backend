package com.example.ATSCIRCLE.Controller.ATS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.ATSCIRCLE.Models.ATS.Jobs;
import com.example.ATSCIRCLE.Service.ATS.JobService;
import com.example.ATSCIRCLE.security.JwtService;

@RestController
@RequestMapping("/api/job-postings")
public class JobsController {

    @Autowired
    private JobService jobService;
    
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

    // -------------------- CREATE --------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> createJobPosting(@RequestBody Jobs jobPosting,
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            Jobs savedJobPosting = jobService.createJobPosting(
                jobPosting, 
                context.get("organizationId"), 
                context.get("userId")
            );
            return ResponseEntity.ok(savedJobPosting);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getAllJobPostings(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(jobService.getAllJobPostings(
            context.get("organizationId")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getJobPostingById(@PathVariable String id, 
                                               Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return jobService.getJobPostingById(id, context.get("organizationId"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/job-code/{jobCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getJobPostingByJobCode(@PathVariable String jobCode,
                                                    Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return jobService.getJobPostingByJobCode(jobCode, context.get("organizationId"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------- UPDATE --------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> updateJobPosting(@PathVariable String id,
                                              @RequestBody Jobs updatedJobPosting,
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            Jobs updated = jobService.updateJobPosting(
                id, 
                updatedJobPosting, 
                context.get("organizationId"),
                context.get("userId")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteJobPosting(@PathVariable String id, 
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            boolean deleted = jobService.deleteJobPosting(
                id, 
                context.get("organizationId")
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

    // -------------------- SEARCH & FILTER --------------------

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> getJobPostingsByStatus(@PathVariable String status,
                                                    Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            List<Jobs> jobPostings = jobService.getJobPostingsByStatus(
                status, context.get("organizationId"));
            return ResponseEntity.ok(jobPostings);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage()
            ));
        }
    }

   @GetMapping("/client/{clientId}")
@PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
public ResponseEntity<?> getJobPostingsByClient(@PathVariable("clientId") String clientId,
                                                Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        List<Jobs> jobPostings =
            jobService.getJobPostingsByClient(clientId, context.get("organizationId"));

        return ResponseEntity.ok(jobPostings);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "error", "Internal Server Error",
            "message", e.getMessage()
        ));
    }
}


    @PostMapping("/search/primary-skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> searchByPrimarySkills(@RequestBody List<String> skills,
                                                   Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            List<Jobs> jobPostings = jobService.searchByPrimarySkills(
                skills, context.get("organizationId"));
            return ResponseEntity.ok(jobPostings);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/search/secondary-skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATS')")
    public ResponseEntity<?> searchBySecondarySkills(@RequestBody List<String> skills,
                                                     Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            List<Jobs> jobPostings = jobService.searchBySecondarySkills(
                skills, context.get("organizationId"));
            return ResponseEntity.ok(jobPostings);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/by-user")
@PreAuthorize("hasAnyRole('ADMIN','ATS')")
public ResponseEntity<?> getJobPostingsByUser(Authentication authentication) {

    Map<String, String> context = extractUserContext(authentication);
    String orgId = context.get("organizationId");
    String userId = context.get("userId");

    return ResponseEntity.ok(
        jobService.getJobPostingsByCreatorOrAssignee(userId, orgId)
    );
}

@PutMapping("/update-assigned-to")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> updateAssignedTo(@RequestBody Map<String, Object> payload,
                                          Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        List<String> ids = (List<String>) payload.get("ids");
        String assignedTo = (String) payload.get("assignedTo");

        jobService.updateAssignedTo(ids, assignedTo, context.get("organizationId"));

        return ResponseEntity.ok(Map.of(
                "message", ids.size() + " jobs updated with new assignedTo: " + assignedTo
        ));

    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Update Error",
                "message", e.getMessage()
        ));
    }
}
   

}