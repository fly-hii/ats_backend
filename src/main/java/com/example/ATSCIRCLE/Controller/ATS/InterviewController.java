package com.example.ATSCIRCLE.Controller.ATS;

import com.example.ATSCIRCLE.DTO.ScheduleMeetDTO;
import com.example.ATSCIRCLE.Service.ATS.InterviewScheduleService;
import com.example.ATSCIRCLE.Util.TimeZoneUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    @Autowired
    private InterviewScheduleService interviewScheduleService;

    @GetMapping("/timezones")
    public ResponseEntity<?> getAllTimezones() {
        List<String> timezones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        timezones.sort(String::compareTo);
        return ResponseEntity.ok(Map.of(
            "totalCount", timezones.size(),
            "timezones",  timezones
        ));
    }

    @GetMapping("/{candidateApplicationId}/converted-slots")
    public ResponseEntity<?> getConvertedSlots(
            @PathVariable String candidateApplicationId,
            @RequestParam String userTimeZone,
            @RequestHeader("X-User-Id") String userId) {

        if (userTimeZone == null || userTimeZone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "userTimeZone is required.",
                "action",  "User must select their timezone from the dropdown first.",
                "example", "?userTimeZone=Asia/Kolkata"
            ));
        }

        if (!TimeZoneUtil.isValidTimeZone(userTimeZone)) {
            return ResponseEntity.badRequest().body(Map.of(
                "message",       "Invalid timezone: " + userTimeZone,
                "action",        "Pick a valid IANA timezone from GET /api/interviews/timezones",
                "validExamples", List.of("Asia/Kolkata", "America/New_York", "Europe/London", "UTC")
            ));
        }

        try {
            List<String> convertedSlots =
                    interviewScheduleService.getConvertedSlots(candidateApplicationId, userTimeZone);

            return ResponseEntity.ok(Map.of(
                "candidateApplicationId", candidateApplicationId,
                "userTimeZone",           userTimeZone,
                "convertedSlots",         convertedSlots,
                "totalSlots",             convertedSlots.size(),
                "nextStep",               "User picks one slot from convertedSlots, then POST /api/interviews/schedule"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleInterview(
            @RequestBody ScheduleMeetDTO scheduleMeetDTO,
            @RequestHeader("X-User-Id") String userId) {

        // Validate all required fields with clear error messages
        if (isBlank(scheduleMeetDTO.getCandidateApplicationId()))
            return bad("candidateApplicationId is required");

        if (isBlank(scheduleMeetDTO.getUserTimeZone()))
            return bad("userTimeZone is required — must be the same timezone the user selected in the dropdown.");

        if (!TimeZoneUtil.isValidTimeZone(scheduleMeetDTO.getUserTimeZone()))
            return bad("Invalid userTimeZone: " + scheduleMeetDTO.getUserTimeZone() +
                       ". Must be a valid IANA string. Get list from GET /api/interviews/timezones");

        if (isBlank(scheduleMeetDTO.getSelectedSlot()))
            return bad("selectedSlot is required. " +
                       "Call GET /api/interviews/{appId}/converted-slots?userTimeZone=... first, then pick one slot.");

        if (isBlank(scheduleMeetDTO.getJobId()))
            return bad("jobId is required");

        if (isBlank(scheduleMeetDTO.getToEmail()))
            return bad("toEmail (candidate email) is required");

        if (isBlank(scheduleMeetDTO.getCandidateName()))
            return bad("candidateName is required");

        try {
            String result = interviewScheduleService.scheduleInterviewMeeting(userId, scheduleMeetDTO);
            String meetLink = extractMeetLink(result);

            return ResponseEntity.ok(Map.of(
                "message",       "Interview scheduled successfully!",
                "meetLink",      meetLink != null ? meetLink : result,
                "scheduledSlot", scheduleMeetDTO.getSelectedSlot(),
                "timezone",      scheduleMeetDTO.getUserTimeZone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error scheduling interview: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String extractMeetLink(String result) {
        if (result != null && result.contains("Meet link: "))
            return result.substring(result.indexOf("Meet link: ") + "Meet link: ".length()).trim();
        return result;
    }
}