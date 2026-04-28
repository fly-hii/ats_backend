package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles interview slot retrieval and candidate slot confirmation.
 *
 * Slot flow:
 *  1. Client sets status = "Interview Scheduled" with timeslots[] + clientTimeZone
 *     → stored on CandidateApplication
 *  2. Candidate opens frontend URL (/interview-slots/{applicationId})
 *     → frontend calls GET /api/interview-slots/{applicationId}?timezone=<tz>
 *     → this service converts slots from clientTimeZone → candidate's timezone
 *  3. Candidate picks a slot
 *     → frontend calls POST /api/interview-slots/{applicationId}/select
 *     → selectedSlot + userTimeZone are stored on the application
 */
@Service
public class InterviewSlotService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSlotService.class);

    /** Expected format for slot strings stored/sent: "yyyy-MM-dd HH:mm:ss" */
    private static final DateTimeFormatter SLOT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Human-readable format shown to the candidate */
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  hh:mm a z");

    @Autowired
    private Candidateapplicationrepository applicationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET  — fetch & convert slots for the candidate
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns available timeslots for a candidate, optionally converted to
     * their local timezone.
     *
     * @param applicationId  MongoDB _id of the CandidateApplication
     * @param candidateZone  IANA timezone string from the browser (nullable)
     * @return map with slots, metadata, and alreadySelected flag
     */
    public Map<String, Object> getSlotsForCandidate(String applicationId, String candidateZone) {
        CandidateApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        if (!"interview scheduled".equalsIgnoreCase(app.getStatus()) 
    && !"slotassigned".equalsIgnoreCase(app.getStatus())) {
            throw new RuntimeException("No interview has been scheduled for this application.");
        }

        List<String> rawSlots    = app.getTimeslots();
        String       clientTz    = app.getClientTimeZone();   // timezone used when storing slots
        String       selectedSlot = app.getSelectedSlot();

        List<Map<String, String>> slotList = new ArrayList<>();

        for (String raw : rawSlots) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("original", raw);                           // always include raw value

            if (candidateZone != null && !candidateZone.isBlank() && clientTz != null) {
                String converted = convertSlot(raw, clientTz, candidateZone);
                entry.put("display",  converted);
                entry.put("timezone", candidateZone);
            } else {
                entry.put("display",  raw);
                entry.put("timezone", clientTz != null ? clientTz : "UTC");
            }

            slotList.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId",  applicationId);
        result.put("candidateName",  resolveName(app));
        result.put("slots",          slotList);
        result.put("alreadySelected", selectedSlot != null && !selectedSlot.isBlank());
        if (selectedSlot != null) result.put("selectedSlot", selectedSlot);
        result.put("userTimeZone",   app.getUserTimeZone());
        result.put("status",         app.getStatus());

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST — candidate confirms their chosen slot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stores the candidate's selected slot on the application.
     *
     * @param applicationId  MongoDB _id
     * @param selectedSlot   The slot string the candidate picked (must be in the
     *                       application's timeslots list OR its converted equivalent)
     * @param userTimeZone   Candidate's local timezone (optional, stored for records)
     */
    @Transactional
    public CandidateApplication confirmSlot(String applicationId,
                                            String selectedSlot,
                                            String userTimeZone) {
        CandidateApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

       if (!"interview scheduled".equalsIgnoreCase(app.getStatus()) 
    && !"slotassigned".equalsIgnoreCase(app.getStatus())) {
            throw new RuntimeException("This application is not in 'Interview Scheduled' status.");
        }

        if (app.getSelectedSlot() != null && !app.getSelectedSlot().isBlank()) {
            throw new IllegalArgumentException(
                    "You have already confirmed a slot: " + app.getSelectedSlot());
        }

        // Validate — the submitted slot must match one of the stored raw slots
        // OR match a converted slot (we do a loose string match)
        List<String> validSlots  = app.getTimeslots();
        boolean      slotIsValid = validSlots != null && validSlots.stream()
                .anyMatch(s -> s.equalsIgnoreCase(selectedSlot));

        // If not a direct match, try to see if it converts back to one of the stored slots
        if (!slotIsValid && app.getClientTimeZone() != null && userTimeZone != null) {
            for (String raw : validSlots) {
                String converted = convertSlot(raw, app.getClientTimeZone(), userTimeZone);
                if (converted.equalsIgnoreCase(selectedSlot)) {
                    slotIsValid = true;
                    break;
                }
            }
        }

        if (!slotIsValid) {
            throw new IllegalArgumentException(
                    "Invalid slot selected. Please choose from the available options.");
        }

        // Persist
        app.setSelectedSlot(selectedSlot);
        if (userTimeZone != null && !userTimeZone.isBlank()) {
            app.setUserTimeZone(userTimeZone);
        }

        // Also update convertedSlots list for reference
        if (userTimeZone != null && app.getClientTimeZone() != null && validSlots != null) {
            List<String> convertedList = new ArrayList<>();
            for (String raw : validSlots) {
                convertedList.add(convertSlot(raw, app.getClientTimeZone(), userTimeZone));
            }
            app.setConvertedSlots(convertedList);
        }

        app.preUpdate();
        CandidateApplication saved = applicationRepository.save(app);
        log.info("Candidate {} confirmed slot '{}' (tz: {})",
                applicationId, selectedSlot, userTimeZone);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convert a slot string from one IANA timezone to another.
     * Returns the original string on any parse error.
     */
    private String convertSlot(String slotStr, String fromTz, String toTz) {
        try {
            ZoneId from   = ZoneId.of(fromTz);
            ZoneId to     = ZoneId.of(toTz);
            // Parse assuming the stored slot is in "fromTz"
            ZonedDateTime zdt = ZonedDateTime.of(
                    java.time.LocalDateTime.parse(slotStr, SLOT_FORMATTER), from);
            ZonedDateTime converted = zdt.withZoneSameInstant(to);
            return converted.format(DISPLAY_FORMATTER);
        } catch (DateTimeParseException | java.time.zone.ZoneRulesException e) {
            log.warn("Could not convert slot '{}' from '{}' to '{}': {}",
                    slotStr, fromTz, toTz, e.getMessage());
            return slotStr;   // fallback: return as-is
        }
    }

    private String resolveName(CandidateApplication app) {
        if (app.getFullName() != null && !app.getFullName().isBlank())
            return app.getFullName().trim();
        StringBuilder sb = new StringBuilder();
        if (app.getFirstName() != null) sb.append(app.getFirstName().trim());
        if (app.getLastName()  != null) { if (sb.length() > 0) sb.append(" "); sb.append(app.getLastName().trim()); }
        return sb.length() > 0 ? sb.toString() : "Candidate";
    }
}
