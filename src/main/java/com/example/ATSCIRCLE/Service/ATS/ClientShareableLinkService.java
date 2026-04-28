// package com.example.ATSCIRCLE.Service.ATS;

// import com.example.ATSCIRCLE.Models.ATS.ClientShareableLink;
// import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
// import com.example.ATSCIRCLE.Repository.ClientShareableLinkRepository;
// import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;

// import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.lang.reflect.Method;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;

// @Service
// public class ClientShareableLinkService {

//     private static final Logger log = LoggerFactory.getLogger(ClientShareableLinkService.class);

//     @Autowired
//     private ClientShareableLinkRepository linkRepository;

//     @Autowired
//     private Candidateapplicationrepository applicationRepository;

//     @Autowired
//     private OrganizationFieldConfigService organizationFieldConfigService;

//     @Autowired
//     private CEmailService emailService;

//      @Autowired
//     private CandidateEmailService candidateEmailService;

//     @Value("${app.base-url:http://localhost:3000}")
//     private String baseUrl;

//     // ─────────────────────────────────────────────────────────────────────────
//     // Create shareable link
//     // ─────────────────────────────────────────────────────────────────────────
//     @Transactional
//     public ClientShareableLink createShareableLink(String organizationId,
//                                                    String clientId,
//                                                    String jobId,
//                                                    String clientEmail,
//                                                    List<String> applicationIds,
//                                                    Integer expiryHours) {
//         ClientShareableLink link = new ClientShareableLink();
//         link.setOrganizationId(organizationId);
//         link.setClientId(clientId);
//         link.setJobId(jobId);
//         link.setClientEmail(clientEmail);
//         link.setApplicationIds(applicationIds);
//         link.prePersist();

//         if (expiryHours != null && expiryHours > 0) {
//             link.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));
//         }

//         return linkRepository.save(link);
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // FIX: validateAndGetLink — increments accessCount ONCE per call.
//     // All internal service methods that already have the link object should
//     // NOT call this again. Only the controller entry-point calls this.
//     // ─────────────────────────────────────────────────────────────────────────
//     public Optional<ClientShareableLink> validateAndGetLink(String token) {
//         Optional<ClientShareableLink> linkOpt = linkRepository.findByToken(token);

//         if (linkOpt.isPresent()) {
//             ClientShareableLink link = linkOpt.get();
//             if (link.isLinkValid()) {
//                 link.incrementAccessCount();
//                 linkRepository.save(link);
//                 return Optional.of(link);
//             }
//         }

//         return Optional.empty();
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // FIX: Internal-only validation — does NOT increment accessCount.
//     // Used by service methods that are called after the controller already
//     // validated the token.
//     // ─────────────────────────────────────────────────────────────────────────
//     private ClientShareableLink getValidatedLinkInternal(String token) {
//         return linkRepository.findByToken(token)
//                 .filter(ClientShareableLink::isLinkValid)
//                 .orElseThrow(() -> new RuntimeException("Invalid or expired link"));
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Fetch applications belonging to a valid token
//     // FIX: uses getValidatedLinkInternal — no extra accessCount increment
//     // ─────────────────────────────────────────────────────────────────────────
//     public List<CandidateApplication> getApplicationsByToken(String token) {
//         ClientShareableLink link = getValidatedLinkInternal(token);
//         return applicationRepository.findAllById(link.getApplicationIds());
//     }

//    @Transactional
//     public CandidateApplication updateApplicationStatusViaLink(ClientShareableLink link,
//                                                                String applicationId,
//                                                                Map<String, Object> payload) {
//         if (!link.getApplicationIds().contains(applicationId)) {
//             throw new RuntimeException("Application not accessible via this link");
//         }

//         CandidateApplication application = applicationRepository.findById(applicationId)
//                 .orElseThrow(() -> new RuntimeException("Application not found"));

//         applyStatusPayload(application, payload);
//         application.preUpdate();

//         CandidateApplication saved = applicationRepository.save(application);

//         // ── NEW: notify candidate by email ──
//         candidateEmailService.sendStatusEmail(saved);

//         return saved;
//     }


// // ── 3. updateMultipleStatusesViaLink — fire email for each updated app ────────
// //    Replace the existing method body with this version:

//     @Transactional
//     public List<CandidateApplication> updateMultipleStatusesViaLink(ClientShareableLink link,
//                                                                      List<String> applicationIds,
//                                                                      Map<String, Object> payload) {
//         for (String appId : applicationIds) {
//             if (!link.getApplicationIds().contains(appId)) {
//                 throw new RuntimeException("Application " + appId + " not accessible via this link");
//             }
//         }

//         List<CandidateApplication> applications = applicationRepository.findAllById(applicationIds);
//         if (applications.isEmpty()) throw new RuntimeException("No applications found");

//         for (CandidateApplication app : applications) {
//             applyStatusPayload(app, payload);
//             app.preUpdate();
//         }

//         List<CandidateApplication> savedList = applicationRepository.saveAll(applications);

//         // ── NEW: notify each candidate by email ──
//         for (CandidateApplication saved : savedList) {
//             candidateEmailService.sendStatusEmail(saved);
//         }

//         return savedList;
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // FIX: updateClientTimeZoneAndSlots — accepts link object directly
//     // No token re-validation, no extra accessCount increment
//     // ─────────────────────────────────────────────────────────────────────────
//     @Transactional
//     public void updateClientTimeZoneAndSlots(ClientShareableLink link,
//                                              String clientTimeZone,
//                                              Map<String, Object> payload) {
//         link.setClientTimeZone(clientTimeZone);

//         @SuppressWarnings("unchecked")
//         List<String> timeSlots = (List<String>) payload.get("timeslots");
//         if (timeSlots != null && !timeSlots.isEmpty()) {
//             link.setTimeSlots(timeSlots);
//         }

//         linkRepository.save(link);
//         log.info("Updated ClientShareableLink {} with timezone: {} and {} slots",
//                 link.getId(), clientTimeZone, timeSlots != null ? timeSlots.size() : 0);
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Status payload applicator
//     // ─────────────────────────────────────────────────────────────────────────
//     private void applyStatusPayload(CandidateApplication application, Map<String, Object> payload) {
//     if (payload == null || !payload.containsKey("status")) {
//         throw new IllegalArgumentException("status is required");
//     }

//     String status = payload.get("status").toString().trim();

//     switch (status.toLowerCase()) {

//         case "shortlisted":
//             application.setStatus(status);
//             application.setTimeslots(new ArrayList<>());
//             application.setReason(extractString(payload, "reason"));
//             break;

//         // ADD THIS CASE:
//         case "slotassigned":
//             Object tsObjSA = payload.get("timeslots");
//             if (!(tsObjSA instanceof List) || ((List<?>) tsObjSA).isEmpty()) {
//                 throw new IllegalArgumentException(
//                     "timeslots must be a non-empty list when status is 'Slotassigned'");
//             }
//             @SuppressWarnings("unchecked")
//             List<String> saTimeslots = (List<String>) tsObjSA;
//             application.setStatus(status);
//             application.setTimeslots(saTimeslots);
//             if (payload.containsKey("clientTimeZone")) {
//                 application.setClientTimeZone(extractString(payload, "clientTimeZone"));
//             }
//             application.setReason(extractString(payload, "reason"));
//             break;

//         case "interview scheduled":
//             Object tsObj = payload.get("timeslots");
//             if (!(tsObj instanceof List) || ((List<?>) tsObj).isEmpty()) {
//                 throw new IllegalArgumentException(
//                     "timeslots must be a non-empty list when status is 'Interview Scheduled'");
//             }
//             @SuppressWarnings("unchecked")
//             List<String> timeslots = (List<String>) tsObj;
//             application.setStatus(status);
//             application.setTimeslots(timeslots);
//             if (payload.containsKey("clientTimeZone")) {
//                 application.setClientTimeZone(extractString(payload, "clientTimeZone"));
//             }
//             application.setReason(extractString(payload, "reason"));
//             break;

//         case "selected":
//             String givenCTC = extractString(payload, "givenCTC");
//             if (givenCTC == null || givenCTC.isBlank()) {
//                 throw new IllegalArgumentException("givenCTC is required when status is 'Selected'");
//             }
//             application.setStatus(status);
//             application.setgivenCTC(givenCTC);
//             application.setReason(extractString(payload, "reason"));
//             String joiningDate = extractString(payload, "actualjoiningdate");
//             if (joiningDate != null && !joiningDate.isBlank()) {
//                 application.setactualjoiningdate(joiningDate);
//             }
//             break;

//         default:
//             application.setStatus(status);
//             String reason = extractString(payload, "reason");
//             if (reason != null) application.setReason(reason);
//             break;
//     }
// }

//     private String extractString(Map<String, Object> payload, String key) {
//         Object val = payload.get(key);
//         return (val instanceof String) ? ((String) val).trim() : null;
//     }

//     // ═════════════════════════════════════════════════════════════════════════
//     // EMAIL SENDING
//     // ═════════════════════════════════════════════════════════════════════════

//     // public void sendShareableLinkEmail(String linkId) {
//     //     ClientShareableLink link = linkRepository.findById(linkId)
//     //             .orElseThrow(() -> new RuntimeException("Link not found with id: " + linkId));

//     //     if (!link.isLinkValid()) {
//     //         throw new RuntimeException("Cannot send an expired or revoked link");
//     //     }

//     //     String shareableUrl    = generateShareableUrl(link.getToken());
//     //     String formattedExpiry = link.getExpiresAt()
//     //             .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

//     //     String subject = "Candidate Review Portal – Action Required";
//     //     String body = "Dear Client,\n\n"
//     //             + "We have shared candidates for your review. Please use the link below to view "
//     //             + "profiles and update statuses (Shortlist / Schedule Interview / Select / Reject):\n\n"
//     //             + "    " + shareableUrl + "\n\n"
//     //             + "This link expires on: " + formattedExpiry + "\n\n"
//     //             + "If you face any issues accessing the portal, please contact your recruiter.\n\n"
//     //             + "Best regards,\nATS Circle Team";

//     //     emailService.sendEmail(link.getClientEmail(), subject, body);
//     // }

//     // public void sendCandidateListAttachmentEmail(String linkId) {
//     //     ClientShareableLink link = linkRepository.findById(linkId)
//     //             .orElseThrow(() -> new RuntimeException("Link not found"));

//     //     if (!link.isLinkValid()) {
//     //         throw new RuntimeException("Cannot send an expired or revoked link");
//     //     }

//     //     List<CandidateApplication> applications =
//     //             applicationRepository.findAllById(link.getApplicationIds());

//     //     log.info("Applications fetched for linkId {}: {}", linkId, applications.size());

//     //     if (applications.isEmpty()) {
//     //         throw new RuntimeException(
//     //                 "No applications found for this link. Check if applicationIds are saved correctly.");
//     //     }

//     //     List<String> selectedFields =
//     //             organizationFieldConfigService.getSelectedFieldsByOrganization(link.getOrganizationId());

//     //     log.info("Selected fields from config for org {}: {}", link.getOrganizationId(), selectedFields);

//     //     if (selectedFields.isEmpty()) {
//     //         throw new RuntimeException("Organization fields not configured");
//     //     }

//     //     String formattedExpiry = link.getExpiresAt()
//     //             .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

//     //     String subject = "Candidate List – Action Required";

//     //     String body = "Dear Client,\n\n"
//     //             + "Please find the attached Excel file containing the candidate list for your review.\n\n"
//     //             + "Kindly go through the profiles and share your feedback "
//     //             + "(Shortlist / Schedule Interview / Select / Reject) at the earliest.\n\n"
//     //             + "Details:\n"
//     //             + "  • Total Candidates : " + applications.size() + "\n"
//     //             + "If you have any questions or need further information about any candidate, "
//     //             + "please do not hesitate to contact your recruiter.\n\n"
//     //             + "Best regards,\n"
//     //             + "ATS Circle Team";

//     //     byte[] excelBytes = buildDynamicCandidateExcel(applications, selectedFields);

//     //     emailService.sendEmailWithBytesAttachment(
//     //             link.getClientEmail(),
//     //             subject,
//     //             body,
//     //             excelBytes,
//     //             "candidate_list.xlsx"
//     //     );
//     // }
// // ─── In ClientShareableLinkService ───────────────────────────────────────────
// // Only these two methods change. Everything else stays exactly the same.
// // Pass link.getOrganizationId() as the first argument to emailService calls.

//     public void sendShareableLinkEmail(String linkId) {
//         ClientShareableLink link = linkRepository.findById(linkId)
//                 .orElseThrow(() -> new RuntimeException("Link not found with id: " + linkId));

//         if (!link.isLinkValid()) {
//             throw new RuntimeException("Cannot send an expired or revoked link");
//         }

//         String shareableUrl    = generateShareableUrl(link.getToken());
//         String formattedExpiry = link.getExpiresAt()
//                 .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

//         String subject = "Candidate Review Portal – Action Required";
//         String body = "Dear Client,\n\n"
//                 + "We have shared candidates for your review. Please use the link below to view "
//                 + "profiles and update statuses (Shortlist / Schedule Interview / Select / Reject):\n\n"
//                 + "    " + shareableUrl + "\n\n"
//                 + "This link expires on: " + formattedExpiry + "\n\n"
//                 + "If you face any issues accessing the portal, please contact your recruiter.\n\n"
//                 + "Best regards,\nATS Circle Team";

//         // ✅ Pass organizationId → email goes FROM the org's connected Gmail/Outlook
//         emailService.sendEmail(link.getOrganizationId(), link.getClientEmail(), subject, body);
//     }

//     public void sendCandidateListAttachmentEmail(String linkId) {
//         ClientShareableLink link = linkRepository.findById(linkId)
//                 .orElseThrow(() -> new RuntimeException("Link not found"));

//         if (!link.isLinkValid()) {
//             throw new RuntimeException("Cannot send an expired or revoked link");
//         }

//         List<CandidateApplication> applications =
//                 applicationRepository.findAllById(link.getApplicationIds());

//         log.info("Applications fetched for linkId {}: {}", linkId, applications.size());

//         if (applications.isEmpty()) {
//             throw new RuntimeException(
//                     "No applications found for this link. Check if applicationIds are saved correctly.");
//         }

//         List<String> selectedFields =
//                 organizationFieldConfigService.getSelectedFieldsByOrganization(link.getOrganizationId());

//         log.info("Selected fields from config for org {}: {}", link.getOrganizationId(), selectedFields);

//         if (selectedFields.isEmpty()) {
//             throw new RuntimeException("Organization fields not configured");
//         }

//         String subject = "Candidate List – Action Required";
//         String body = "Dear Client,\n\n"
//                 + "Please find the attached Excel file containing the candidate list for your review.\n\n"
//                 + "Kindly go through the profiles and share your feedback "
//                 + "(Shortlist / Schedule Interview / Select / Reject) at the earliest.\n\n"
//                 + "Details:\n"
//                 + "  • Total Candidates : " + applications.size() + "\n"
//                 + "If you have any questions or need further information about any candidate, "
//                 + "please do not hesitate to contact your recruiter.\n\n"
//                 + "Best regards,\n"
//                 + "ATS Circle Team";

//         byte[] excelBytes = buildDynamicCandidateExcel(applications, selectedFields);

//         // ✅ Pass organizationId → attachment also goes FROM the org's connected Gmail/Outlook
//         emailService.sendEmailWithBytesAttachment(
//                 link.getOrganizationId(),
//                 link.getClientEmail(),
//                 subject,
//                 body,
//                 excelBytes,
//                 "candidate_list.xlsx"
//         );
//     }
//     // ═════════════════════════════════════════════════════════════════════════
//     // DYNAMIC EXCEL BUILDER
//     // ═════════════════════════════════════════════════════════════════════════
//     private byte[] buildDynamicCandidateExcel(List<CandidateApplication> applications,
//                                               List<String> selectedFields) {
//         log.info("Building Excel — applications: {}, fields: {}", applications.size(), selectedFields);

//         try (Workbook workbook = new XSSFWorkbook();
//              ByteArrayOutputStream out = new ByteArrayOutputStream()) {

//             Sheet sheet = workbook.createSheet("Candidates");

//             CellStyle headerStyle = workbook.createCellStyle();
//             Font headerFont = workbook.createFont();
//             headerFont.setBold(true);
//             headerStyle.setFont(headerFont);

//             Row headerRow = sheet.createRow(0);
//             for (int i = 0; i < selectedFields.size(); i++) {
//                 Cell cell = headerRow.createCell(i);
//                 cell.setCellValue(selectedFields.get(i));
//                 cell.setCellStyle(headerStyle);
//             }

//             int rowNum = 1;
//             for (CandidateApplication app : applications) {
//                 Row row = sheet.createRow(rowNum++);
//                 for (int col = 0; col < selectedFields.size(); col++) {
//                     String value = extractFieldValue(app, selectedFields.get(col));
//                     row.createCell(col).setCellValue(value);
//                 }
//             }

//             for (int i = 0; i < selectedFields.size(); i++) {
//                 sheet.autoSizeColumn(i);
//             }

//             workbook.write(out);
//             return out.toByteArray();

//         } catch (IOException e) {
//             throw new RuntimeException("Excel build failed", e);
//         }
//     }

//     private String extractFieldValue(CandidateApplication app, String fieldName) {
//         if (fieldName == null || fieldName.isBlank()) return "";
//         String trimmed = fieldName.trim();
//         Method method = resolveMethod("get" + Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1));
//         if (method == null) method = resolveMethod("get" + trimmed);
//         if (method == null) { log.warn("No getter found for field [{}]", fieldName); return ""; }
//         try { return convertToString(method.invoke(app)); }
//         catch (Exception e) { log.error("Error invoking {} for field [{}]: {}", method.getName(), fieldName, e.getMessage()); return ""; }
//     }

//     private Method resolveMethod(String methodName) {
//         try { return CandidateApplication.class.getMethod(methodName); }
//         catch (NoSuchMethodException e) { return null; }
//     }

//     private String convertToString(Object value) {
//         if (value == null) return "";
//         if (value instanceof Collection<?>) {
//             Collection<?> col = (Collection<?>) value;
//             if (col.isEmpty()) return "";
//             StringBuilder sb = new StringBuilder();
//             for (Object item : col) { if (sb.length() > 0) sb.append(", "); sb.append(item != null ? item.toString() : ""); }
//             return sb.toString();
//         }
//         if (value instanceof Map<?, ?>) { Map<?, ?> map = (Map<?, ?>) value; return map.isEmpty() ? "" : map.toString(); }
//         return value.toString().trim();
//     }

//     // ─────────────────────────────────────────────────────────────────────────
//     // Admin helpers
//     // ─────────────────────────────────────────────────────────────────────────
//     public String generateShareableUrl(String token) {
//         return baseUrl + "/clientaccesspage/" + token;
//     }

//     @Transactional
//     public void revokeLink(String linkId) {
//         linkRepository.findById(linkId).ifPresent(link -> { link.revokeLink(); linkRepository.save(link); });
//     }

//     @Transactional
//     public ClientShareableLink extendLinkExpiry(String linkId, int hours) {
//         return linkRepository.findById(linkId).map(link -> { link.extendExpiry(hours); return linkRepository.save(link); })
//                 .orElseThrow(() -> new RuntimeException("Link not found"));
//     }

//     public List<ClientShareableLink> getLinksByOrganization(String organizationId) {
//         return linkRepository.findByOrganizationId(organizationId);
//     }

//     public List<ClientShareableLink> getActiveLinksByOrganization(String organizationId) {
//         return linkRepository.findByOrganizationIdAndIsExpiredAndIsRevoked(organizationId, false, false);
//     }

//     public List<ClientShareableLink> getLinksByClient(String clientId) {
//         return linkRepository.findByClientId(clientId);
//     }

//     public List<ClientShareableLink> getLinksByJob(String jobId) {
//         return linkRepository.findByJobId(jobId);
//     }

//     @Transactional
//     public void cleanupExpiredLinks() {
//         linkRepository.findByExpiresAtBefore(LocalDateTime.now()).stream()
//                 .filter(l -> !l.getIsExpired())
//                 .forEach(l -> { l.setIsExpired(true); linkRepository.save(l); });
//     }

//     @Transactional
//     public void deleteOldLinks(int daysOld) {
//         LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
//         linkRepository.deleteAll(linkRepository.findByExpiresAtBefore(cutoff));
//     }

//     @Transactional
//     public ClientShareableLink updateLink(ClientShareableLink link) {
//         return linkRepository.save(link);
//     }
// }

package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.ClientShareableLink;
import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Repository.ClientShareableLinkRepository;
import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClientShareableLinkService {

    private static final Logger log = LoggerFactory.getLogger(ClientShareableLinkService.class);

    @Autowired
    private ClientShareableLinkRepository linkRepository;

    @Autowired
    private Candidateapplicationrepository applicationRepository;

    @Autowired
    private OrganizationFieldConfigService organizationFieldConfigService;

    @Autowired
    private CEmailService emailService;

    @Autowired
    private CandidateEmailService candidateEmailService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // Create shareable link
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public ClientShareableLink createShareableLink(String organizationId,
                                                   String clientId,
                                                   String jobId,
                                                   String clientEmail,
                                                   List<String> applicationIds,
                                                   Integer expiryHours) {
        ClientShareableLink link = new ClientShareableLink();
        link.setOrganizationId(organizationId);
        link.setClientId(clientId);
        link.setJobId(jobId);
        link.setClientEmail(clientEmail);
        link.setApplicationIds(applicationIds);
        link.prePersist();

        if (expiryHours != null && expiryHours > 0) {
            link.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));
        }

        return linkRepository.save(link);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateAndGetLink — increments accessCount ONCE per call (controller only)
    // ─────────────────────────────────────────────────────────────────────────
    public Optional<ClientShareableLink> validateAndGetLink(String token) {
        Optional<ClientShareableLink> linkOpt = linkRepository.findByToken(token);

        if (linkOpt.isPresent()) {
            ClientShareableLink link = linkOpt.get();
            if (link.isLinkValid()) {
                link.incrementAccessCount();
                linkRepository.save(link);
                return Optional.of(link);
            }
        }

        return Optional.empty();
    }

    // Internal-only validation — does NOT increment accessCount
    private ClientShareableLink getValidatedLinkInternal(String token) {
        return linkRepository.findByToken(token)
                .filter(ClientShareableLink::isLinkValid)
                .orElseThrow(() -> new RuntimeException("Invalid or expired link"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch applications belonging to a valid token
    // ─────────────────────────────────────────────────────────────────────────
    public List<CandidateApplication> getApplicationsByToken(String token) {
        ClientShareableLink link = getValidatedLinkInternal(token);
        return applicationRepository.findAllById(link.getApplicationIds());
    }

    @Transactional
    public CandidateApplication updateApplicationStatusViaLink(ClientShareableLink link,
                                                               String applicationId,
                                                               Map<String, Object> payload) {
        if (!link.getApplicationIds().contains(applicationId)) {
            throw new RuntimeException("Application not accessible via this link");
        }

        CandidateApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        applyStatusPayload(application, payload);
        application.preUpdate();

        CandidateApplication saved = applicationRepository.save(application);
        candidateEmailService.sendStatusEmail(saved);

        return saved;
    }

    @Transactional
    public List<CandidateApplication> updateMultipleStatusesViaLink(ClientShareableLink link,
                                                                     List<String> applicationIds,
                                                                     Map<String, Object> payload) {
        for (String appId : applicationIds) {
            if (!link.getApplicationIds().contains(appId)) {
                throw new RuntimeException("Application " + appId + " not accessible via this link");
            }
        }

        List<CandidateApplication> applications = applicationRepository.findAllById(applicationIds);
        if (applications.isEmpty()) throw new RuntimeException("No applications found");

        for (CandidateApplication app : applications) {
            applyStatusPayload(app, payload);
            app.preUpdate();
        }

        List<CandidateApplication> savedList = applicationRepository.saveAll(applications);

        for (CandidateApplication saved : savedList) {
            candidateEmailService.sendStatusEmail(saved);
        }

        return savedList;
    }

    @Transactional
    public void updateClientTimeZoneAndSlots(ClientShareableLink link,
                                             String clientTimeZone,
                                             Map<String, Object> payload) {
        link.setClientTimeZone(clientTimeZone);

        @SuppressWarnings("unchecked")
        List<String> timeSlots = (List<String>) payload.get("timeslots");
        if (timeSlots != null && !timeSlots.isEmpty()) {
            link.setTimeSlots(timeSlots);
        }

        linkRepository.save(link);
        log.info("Updated ClientShareableLink {} with timezone: {} and {} slots",
                link.getId(), clientTimeZone, timeSlots != null ? timeSlots.size() : 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status payload applicator
    // ─────────────────────────────────────────────────────────────────────────
    private void applyStatusPayload(CandidateApplication application, Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("status")) {
            throw new IllegalArgumentException("status is required");
        }

        String status = payload.get("status").toString().trim();

        switch (status.toLowerCase()) {

            case "shortlisted":
                application.setStatus(status);
                application.setTimeslots(new ArrayList<>());
                application.setReason(extractString(payload, "reason"));
                break;

            case "slotassigned":
                Object tsObjSA = payload.get("timeslots");
                if (!(tsObjSA instanceof List) || ((List<?>) tsObjSA).isEmpty()) {
                    throw new IllegalArgumentException(
                            "timeslots must be a non-empty list when status is 'Slotassigned'");
                }
                @SuppressWarnings("unchecked")
                List<String> saTimeslots = (List<String>) tsObjSA;
                application.setStatus(status);
                application.setTimeslots(saTimeslots);
                if (payload.containsKey("clientTimeZone")) {
                    application.setClientTimeZone(extractString(payload, "clientTimeZone"));
                }
                application.setReason(extractString(payload, "reason"));
                break;

            case "interview scheduled":
                Object tsObj = payload.get("timeslots");
                if (!(tsObj instanceof List) || ((List<?>) tsObj).isEmpty()) {
                    throw new IllegalArgumentException(
                            "timeslots must be a non-empty list when status is 'Interview Scheduled'");
                }
                @SuppressWarnings("unchecked")
                List<String> timeslots = (List<String>) tsObj;
                application.setStatus(status);
                application.setTimeslots(timeslots);
                if (payload.containsKey("clientTimeZone")) {
                    application.setClientTimeZone(extractString(payload, "clientTimeZone"));
                }
                application.setReason(extractString(payload, "reason"));
                break;

            case "selected":
                String givenCTC = extractString(payload, "givenCTC");
                if (givenCTC == null || givenCTC.isBlank()) {
                    throw new IllegalArgumentException("givenCTC is required when status is 'Selected'");
                }
                application.setStatus(status);
                application.setgivenCTC(givenCTC);
                application.setReason(extractString(payload, "reason"));
                String joiningDate = extractString(payload, "actualjoiningdate");
                if (joiningDate != null && !joiningDate.isBlank()) {
                    application.setactualjoiningdate(joiningDate);
                }
                break;

            default:
                application.setStatus(status);
                String reason = extractString(payload, "reason");
                if (reason != null) application.setReason(reason);
                break;
        }
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        return (val instanceof String) ? ((String) val).trim() : null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMAIL — sends FROM the org's connected Gmail / Outlook account
    // ═════════════════════════════════════════════════════════════════════════

    public void sendShareableLinkEmail(String linkId) {
        ClientShareableLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found with id: " + linkId));

        if (!link.isLinkValid()) {
            throw new RuntimeException("Cannot send an expired or revoked link");
        }

        String shareableUrl    = generateShareableUrl(link.getToken());
        String formattedExpiry = link.getExpiresAt()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        // ASCII hyphen used — avoids any encoding risk in subject
        String subject = "Candidate Review Portal - Action Required";
        String body = "Dear Client,\n\n"
                + "We have shared candidates for your review. Please use the link below to view "
                + "profiles and update statuses (Shortlist / Schedule Interview / Select / Reject):\n\n"
                + "    " + shareableUrl + "\n\n"
                + "This link expires on: " + formattedExpiry + "\n\n"
                + "If you face any issues accessing the portal, please contact your recruiter.\n\n"
                + "Best regards,\nATS Circle Team";

        // Pass organizationId so email is sent FROM the org's connected account
        emailService.sendEmail(link.getOrganizationId(), link.getClientEmail(), subject, body);
    }

    public void sendCandidateListAttachmentEmail(String linkId) {
        ClientShareableLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        if (!link.isLinkValid()) {
            throw new RuntimeException("Cannot send an expired or revoked link");
        }

        List<CandidateApplication> applications =
                applicationRepository.findAllById(link.getApplicationIds());

        log.info("Applications fetched for linkId {}: {}", linkId, applications.size());

        if (applications.isEmpty()) {
            throw new RuntimeException(
                    "No applications found for this link. Check if applicationIds are saved correctly.");
        }

        List<String> selectedFields =
                organizationFieldConfigService.getSelectedFieldsByOrganization(link.getOrganizationId());

        log.info("Selected fields from config for org {}: {}", link.getOrganizationId(), selectedFields);

        if (selectedFields.isEmpty()) {
            throw new RuntimeException("Organization fields not configured");
        }

        // ASCII hyphen used — avoids any encoding risk in subject
        String subject = "Candidate List - Action Required";
        String body = "Dear Client,\n\n"
                + "Please find the attached Excel file containing the candidate list for your review.\n\n"
                + "Kindly go through the profiles and share your feedback "
                + "(Shortlist / Schedule Interview / Select / Reject) at the earliest.\n\n"
                + "Details:\n"
                + "  - Total Candidates: " + applications.size() + "\n\n"
                + "If you have any questions or need further information about any candidate, "
                + "please do not hesitate to contact your recruiter.\n\n"
                + "Best regards,\n"
                + "ATS Circle Team";

        byte[] excelBytes = buildDynamicCandidateExcel(applications, selectedFields);

        // Pass organizationId so attachment email is also sent FROM the org's connected account
        emailService.sendEmailWithBytesAttachment(
                link.getOrganizationId(),
                link.getClientEmail(),
                subject,
                body,
                excelBytes,
                "candidate_list.xlsx"
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DYNAMIC EXCEL BUILDER
    // ═════════════════════════════════════════════════════════════════════════
    private byte[] buildDynamicCandidateExcel(List<CandidateApplication> applications,
                                              List<String> selectedFields) {
        log.info("Building Excel - applications: {}, fields: {}", applications.size(), selectedFields);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Candidates");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < selectedFields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(selectedFields.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (CandidateApplication app : applications) {
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < selectedFields.size(); col++) {
                    String value = extractFieldValue(app, selectedFields.get(col));
                    row.createCell(col).setCellValue(value);
                }
            }

            for (int i = 0; i < selectedFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Excel build failed", e);
        }
    }

    private String extractFieldValue(CandidateApplication app, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) return "";
        String trimmed = fieldName.trim();
        Method method = resolveMethod("get" + Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1));
        if (method == null) method = resolveMethod("get" + trimmed);
        if (method == null) { log.warn("No getter found for field [{}]", fieldName); return ""; }
        try { return convertToString(method.invoke(app)); }
        catch (Exception e) { log.error("Error invoking {} for field [{}]: {}", method.getName(), fieldName, e.getMessage()); return ""; }
    }

    private Method resolveMethod(String methodName) {
        try { return CandidateApplication.class.getMethod(methodName); }
        catch (NoSuchMethodException e) { return null; }
    }

    private String convertToString(Object value) {
        if (value == null) return "";
        if (value instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) value;
            if (col.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Object item : col) { if (sb.length() > 0) sb.append(", "); sb.append(item != null ? item.toString() : ""); }
            return sb.toString();
        }
        if (value instanceof Map<?, ?>) { Map<?, ?> map = (Map<?, ?>) value; return map.isEmpty() ? "" : map.toString(); }
        return value.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin helpers
    // ─────────────────────────────────────────────────────────────────────────
    public String generateShareableUrl(String token) {
        return baseUrl + "/clientaccesspage/" + token;
    }

    @Transactional
    public void revokeLink(String linkId) {
        linkRepository.findById(linkId).ifPresent(link -> { link.revokeLink(); linkRepository.save(link); });
    }

    @Transactional
    public ClientShareableLink extendLinkExpiry(String linkId, int hours) {
        return linkRepository.findById(linkId).map(link -> { link.extendExpiry(hours); return linkRepository.save(link); })
                .orElseThrow(() -> new RuntimeException("Link not found"));
    }

    public List<ClientShareableLink> getLinksByOrganization(String organizationId) {
        return linkRepository.findByOrganizationId(organizationId);
    }

    public List<ClientShareableLink> getActiveLinksByOrganization(String organizationId) {
        return linkRepository.findByOrganizationIdAndIsExpiredAndIsRevoked(organizationId, false, false);
    }

    public List<ClientShareableLink> getLinksByClient(String clientId) {
        return linkRepository.findByClientId(clientId);
    }

    public List<ClientShareableLink> getLinksByJob(String jobId) {
        return linkRepository.findByJobId(jobId);
    }

    @Transactional
    public void cleanupExpiredLinks() {
        linkRepository.findByExpiresAtBefore(LocalDateTime.now()).stream()
                .filter(l -> !l.getIsExpired())
                .forEach(l -> { l.setIsExpired(true); linkRepository.save(l); });
    }

    @Transactional
    public void deleteOldLinks(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        linkRepository.deleteAll(linkRepository.findByExpiresAtBefore(cutoff));
    }

    @Transactional
    public ClientShareableLink updateLink(ClientShareableLink link) {
        return linkRepository.save(link);
    }
}