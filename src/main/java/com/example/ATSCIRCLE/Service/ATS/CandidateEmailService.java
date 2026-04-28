package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sends status-based notification emails to candidates.
 *
 * Triggered after every status update in ClientShareableLinkService.
 *
 * Statuses handled:
 *  - "rejected"            -> Sorry email with reason
 *  - "shortlisted"         -> Congratulations email
 *  - "interview scheduled" -> Email with frontend URL containing timeslots
 *  - "selected"            -> Congratulations / offer email
 *
 * All emails are routed through the organization's connected Gmail / Outlook
 * account (stored on the Users document identified by organizationId).
 * Falls back to SMTP if no provider is configured.
 */
@Service
public class CandidateEmailService {

    private static final Logger log = LoggerFactory.getLogger(CandidateEmailService.class);

    @Autowired
    private CEmailService emailService;

    /** Base URL of the candidate-facing frontend (e.g. https://app.atscircle.com) */
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // Called from ClientShareableLinkService after every save().
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatch the correct email based on the application's current status.
     *
     * @param application  freshly-saved CandidateApplication
     */
    public void sendStatusEmail(CandidateApplication application) {
        String email = application.getPrimaryEmail();
        if (email == null || email.isBlank()) {
            log.warn("No primary email for application {}; skipping notification.",
                    application.getApplicationId());
            return;
        }

        String status = application.getStatus();
        if (status == null) return;

        switch (status.toLowerCase().trim()) {

            case "rejected":
                sendRejectedEmail(application, email);
                break;

            case "shortlisted":
                sendShortlistedEmail(application, email);
                break;

            case "interview scheduled":
                sendInterviewScheduledEmail(application, email);
                break;

            case "slotassigned":
                sendSlotAssignedEmail(application, email);
                break;

            case "selected":
                sendSelectedEmail(application, email);
                break;

            default:
                log.info("No candidate email configured for status '{}' (appId: {}).",
                        status, application.getApplicationId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REJECTED
    // ─────────────────────────────────────────────────────────────────────────

    private void sendRejectedEmail(CandidateApplication app, String toEmail) {
        String candidateName = resolveName(app);
        String reason        = (app.getReason() != null && !app.getReason().isBlank())
                               ? app.getReason()
                               : "we have decided to move forward with other candidates at this time";

        String subject = "Update on Your Application - " + app.getApplicationId();

        String body = "Dear " + candidateName + ",\n\n"
                + "Thank you for taking the time to apply and for your interest in the position.\n\n"
                + "We are sorry to inform you that, after careful consideration, "
                + "your application has not been shortlisted for further evaluation.\n\n"
                + "Reason: " + reason + "\n\n"
                + "We truly appreciate the effort you put into your application and encourage you "
                + "to apply for future openings that match your profile.\n\n"
                + "We wish you all the best in your career journey.\n\n"
                + "Best regards,\n"
                + "The Recruitment Team";

        trySendEmail(app, toEmail, subject, body, "Rejected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHORTLISTED
    // ─────────────────────────────────────────────────────────────────────────

    private void sendShortlistedEmail(CandidateApplication app, String toEmail) {
        String candidateName = resolveName(app);

        String subject = "Congratulations! Your Profile Has Been Shortlisted - " + app.getApplicationId();

        String body = "Dear " + candidateName + ",\n\n"
                + "Congratulations!\n\n"
                + "We are pleased to inform you that your profile has been shortlisted "
                + "for the position you applied for.\n\n"
                + "Our team was impressed with your background and experience, "
                + "and we would like to move forward with your application.\n\n"
                + "You will receive further communication shortly regarding the next steps "
                + "in the selection process.\n\n"
                + "In the meantime, if you have any questions, please feel free to reach out "
                + "to your recruiter.\n\n"
                + "Best regards,\n"
                + "The Recruitment Team";

        trySendEmail(app, toEmail, subject, body, "Shortlisted");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERVIEW SCHEDULED
    // Sends a frontend URL where the candidate can view all proposed slots
    // and confirm one.  URL pattern: {baseUrl}/interview-slots/{applicationId}
    // ─────────────────────────────────────────────────────────────────────────

    private void sendInterviewScheduledEmail(CandidateApplication app, String toEmail) {
        String candidateName    = resolveName(app);
        String slotSelectionUrl = baseUrl + "/interview-slots/" + app.getId();

        // Render available timeslots in a readable list
        List<String> slots = app.getTimeslots();
        StringBuilder slotList = new StringBuilder();
        if (slots != null && !slots.isEmpty()) {
            for (int i = 0; i < slots.size(); i++) {
                slotList.append("   ").append(i + 1).append(". ").append(slots.get(i)).append("\n");
            }
        } else {
            slotList.append("   (Slots will be visible on the link below)\n");
        }

        String subject = "Interview Invitation - Please Select Your Slot - " + app.getApplicationId();

        String body = "Dear " + candidateName + ",\n\n"
                + "We are excited to move you to the next stage of our selection process - "
                + "the interview!\n\n"
                + "We have proposed the following interview time slots for your convenience:\n\n"
                + slotList
                + "\nPlease click the link below to confirm your preferred slot:\n\n"
                + "   " + slotSelectionUrl + "\n\n"
                + "Kindly confirm your slot at the earliest so we can schedule the interview "
                + "accordingly.\n\n"
                + "If none of the proposed slots work for you, please reply to this email "
                + "and we will do our best to accommodate you.\n\n"
                + "Best regards,\n"
                + "The Recruitment Team";

        trySendEmail(app, toEmail, subject, body, "Interview Scheduled");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SLOT ASSIGNED
    // Client has assigned specific slots — candidate must confirm one.
    // URL pattern: {baseUrl}/interview-slots/{applicationId}
    // ─────────────────────────────────────────────────────────────────────────

    private void sendSlotAssignedEmail(CandidateApplication app, String toEmail) {
        String candidateName    = resolveName(app);
        String slotSelectionUrl = baseUrl + "/interview-slots/" + app.getId();

        List<String> slots = app.getTimeslots();
        StringBuilder slotList = new StringBuilder();
        if (slots != null && !slots.isEmpty()) {
            for (int i = 0; i < slots.size(); i++) {
                slotList.append("   ").append(i + 1).append(". ").append(slots.get(i)).append("\n");
            }
        } else {
            slotList.append("   (Slots will be visible on the link below)\n");
        }

        // Include timezone if set
        String tzNote = "";
        if (app.getClientTimeZone() != null && !app.getClientTimeZone().isBlank()) {
            tzNote = "All slots are in timezone: " + app.getClientTimeZone() + "\n\n";
        }

        String subject = "Interview Slot Assigned - Please Confirm - " + app.getApplicationId();

        String body = "Dear " + candidateName + ",\n\n"
                + "Great news! Interview slots have been assigned for your application.\n\n"
                + "The following time slots have been proposed for your interview:\n\n"
                + slotList
                + "\n" + tzNote
                + "Please visit the link below to confirm your preferred slot:\n\n"
                + "   " + slotSelectionUrl + "\n\n"
                + "Kindly confirm at the earliest so we can finalise the interview schedule.\n\n"
                + "If none of the proposed slots are convenient, please reply to this email "
                + "and we will do our best to accommodate you.\n\n"
                + "Best regards,\n"
                + "The Recruitment Team";

        trySendEmail(app, toEmail, subject, body, "SlotAssigned");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELECTED
    // ─────────────────────────────────────────────────────────────────────────

    private void sendSelectedEmail(CandidateApplication app, String toEmail) {
        String candidateName = resolveName(app);

        String subject = "Congratulations! You Have Been Selected - " + app.getApplicationId();

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(candidateName).append(",\n\n");
        body.append("We are thrilled to inform you that you have been SELECTED for the position!\n\n");
        body.append("After a thorough evaluation process, the team is delighted to welcome you aboard.\n\n");

        if (app.getgivenCTC() != null && !app.getgivenCTC().isBlank()) {
            body.append("Offered CTC           : ").append(app.getgivenCTC()).append("\n");
        }
        if (app.getactualjoiningdate() != null && !app.getactualjoiningdate().isBlank()) {
            body.append("Expected Joining Date : ").append(app.getactualjoiningdate()).append("\n");
        }

        body.append("\nYour recruiter will be in touch shortly with the formal offer letter "
                + "and further onboarding details.\n\n");
        body.append("Please revert with your acceptance at the earliest so we can proceed.\n\n");
        body.append("Once again, congratulations and welcome to the team!\n\n");
        body.append("Best regards,\n");
        body.append("The Recruitment Team");

        trySendEmail(app, toEmail, subject, body.toString(), "Selected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Resolve a friendly display name from the application. */
    private String resolveName(CandidateApplication app) {
        if (app.getFullName() != null && !app.getFullName().isBlank())
            return app.getFullName().trim();

        StringBuilder sb = new StringBuilder();
        if (app.getFirstName() != null) sb.append(app.getFirstName().trim());
        if (app.getLastName()  != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(app.getLastName().trim());
        }
        return sb.length() > 0 ? sb.toString() : "Candidate";
    }

    /**
     * Fire-and-forget email with error logging.
     *
     * ✅ Passes application.getOrganizationId() so the email is sent FROM
     *    the organization's connected Gmail / Outlook account.
     *    Falls back to SMTP automatically if no provider is configured.
     */
    private void trySendEmail(CandidateApplication app,
                               String toEmail,
                               String subject,
                               String body,
                               String statusLabel) {
        try {
            // ✅ Use organizationId overload — routes via org's connected account
            emailService.sendEmail(app.getOrganizationId(), toEmail, subject, body);

            log.info("Candidate email sent via org={} status='{}' appId='{}' to='{}'",
                    app.getOrganizationId(), statusLabel, app.getApplicationId(), toEmail);

        } catch (Exception e) {
            log.error("Failed to send '{}' email for appId='{}' org='{}': {}",
                    statusLabel, app.getApplicationId(), app.getOrganizationId(),
                    e.getMessage(), e);
            // intentionally non-fatal - status is already saved
        }
    }
}