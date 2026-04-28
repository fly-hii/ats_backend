package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Service.ATS.InterviewSlotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public (no-auth) controller that the candidate uses to:
 *   1. View available interview slots          GET  /api/interview-slots/{applicationId}
 *   2. Confirm / select a slot                 POST /api/interview-slots/{applicationId}/select
 *
 * The frontend URL sent in the email:
 *   {baseUrl}/interview-slots/{applicationId}
 * That React page calls these two endpoints.
 */
@RestController
@RequestMapping("/api/interview-slots")
public class InterviewSlotController {

    @Autowired
    private InterviewSlotService interviewSlotService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{applicationId}
    // Returns available slots, converted to the candidate's timezone if provided.
    //
    // Query param (optional):  ?timezone=Asia/Kolkata
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getSlots(
            @PathVariable String applicationId,
            @RequestParam(required = false) String timezone) {
        try {
            Map<String, Object> slotData =
                    interviewSlotService.getSlotsForCandidate(applicationId, timezone);
            return ResponseEntity.ok(slotData);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching slots: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /{applicationId}/select
    // Candidate submits their chosen slot.
    //
    // Request body:
    // {
    //   "selectedSlot"  : "2026-03-15 10:00:00",   // the slot string the candidate picked
    //   "userTimeZone"  : "Asia/Kolkata"            // optional – candidate's local timezone
    // }
    //
    // On success the selectedSlot is stored on the CandidateApplication.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{applicationId}/select")
    public ResponseEntity<?> selectSlot(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> body) {
        try {
            Object slotObj = body.get("selectedSlot");
            if (slotObj == null || slotObj.toString().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "selectedSlot is required"));
            }

            String selectedSlot  = slotObj.toString().trim();
            String userTimeZone  = body.containsKey("userTimeZone")
                                   ? body.get("userTimeZone").toString().trim()
                                   : null;

            CandidateApplication updated =
                    interviewSlotService.confirmSlot(applicationId, selectedSlot, userTimeZone);

            return ResponseEntity.ok(Map.of(
                    "message",      "Slot confirmed successfully",
                    "applicationId", updated.getId(),
                    "selectedSlot",  updated.getSelectedSlot(),
                    "userTimeZone",  updated.getUserTimeZone() != null ? updated.getUserTimeZone() : ""
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error confirming slot: " + e.getMessage()));
        }
    }
}