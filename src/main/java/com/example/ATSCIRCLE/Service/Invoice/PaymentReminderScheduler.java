package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.Invoicemodel;
import com.example.ATSCIRCLE.Repository.InvoiceRepository;
import com.example.ATSCIRCLE.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PaymentReminderScheduler {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    /**
     * Runs every day at 9:00 AM to check for payment reminders.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkAndSendPaymentReminders() {
        LocalDate today       = LocalDate.now();
        List<Invoicemodel> all = invoiceRepository.findAll();

        for (Invoicemodel invoice : all) {
            if (invoice.getAutomaticPaymentReminders() == null
                    || !invoice.getAutomaticPaymentReminders()) continue;

            if (invoice.getJoiningDate() == null) continue;

            long daysSinceJoining = ChronoUnit.DAYS.between(invoice.getJoiningDate(), today);

            if (daysSinceJoining == 30 || daysSinceJoining == 60 || daysSinceJoining == 90) {
                sendPaymentReminder(invoice, (int) daysSinceJoining);
            }
        }
    }

    private void sendPaymentReminder(Invoicemodel invoice, int days) {
        String message = createReminderMessage(invoice, days);

        // 1. WebSocket notification
        sendWebSocketNotification(invoice, message, days);

        // 2. Email notification — routed via org's connected account
        if (invoice.getSendInvoiceMailTo() != null
                && !invoice.getSendInvoiceMailTo().isEmpty()) {
            sendEmailNotification(invoice, message, days);
        }

        System.out.println("Payment reminder sent for Invoice ID: "
                + invoice.getId() + " - " + days + " days overdue");
    }

    private String createReminderMessage(Invoicemodel invoice, int days) {
        return String.format(
            "Payment Reminder: Invoice for %s (%s) is %d days old. "
            + "Placement Fee: Rs.%.2f. Please process payment at the earliest.",
            invoice.getCandidateName(),
            invoice.getDesignation(),
            days,
            invoice.getCalculatedFee()
        );
    }

    private void sendWebSocketNotification(Invoicemodel invoice, String message, int days) {
        PaymentReminderNotification notification = new PaymentReminderNotification();
        notification.setInvoiceId(invoice.getId());
        notification.setOrganizationId(invoice.getOrganizationId());
        notification.setClientId(invoice.getClientId());
        notification.setClientName(invoice.getClientName());
        notification.setCandidateName(invoice.getCandidateName());
        notification.setMessage(message);
        notification.setDaysOverdue(days);
        notification.setAmount(invoice.getCalculatedFee());
        notification.setType("PAYMENT_REMINDER");
        notification.setTimestamp(LocalDate.now().toString());
        notification.setSeverity(getSeverity(days));

        messagingTemplate.convertAndSend(
                "/topic/payment-reminders/" + invoice.getOrganizationId(),
                notification);
    }

    private void sendEmailNotification(Invoicemodel invoice, String message, int days) {
        String subject   = String.format("Payment Reminder - %d Days - %s", days, invoice.getCandidateName());
        String emailBody = createEmailBodyHtml(invoice, days);

        try {
            // ✅ Pass organizationId → routes through org's connected Gmail / Outlook
            emailService.sendHtmlEmailForOrg(
                    invoice.getOrganizationId(),
                    invoice.getSendInvoiceMailTo(),
                    subject,
                    emailBody);

            System.out.println("📧 Reminder email sent via org=" + invoice.getOrganizationId()
                    + " → " + invoice.getSendInvoiceMailTo());

        } catch (Exception e) {
            System.err.println("❌ Failed to send reminder email: " + e.getMessage());
        }
    }

    private String createEmailBodyHtml(Invoicemodel invoice, int days) {
        String severityColor = days >= 90 ? "#f44336" : days >= 60 ? "#ff9800" : "#2196F3";
        String severityLabel = days >= 90 ? "HIGH PRIORITY" : days >= 60 ? "MEDIUM PRIORITY" : "LOW PRIORITY";

        return String.format(
            "<html>" +
            "<body style='font-family:Arial,sans-serif;line-height:1.6;color:#333;'>" +
            "<div style='max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;'>" +

            "<div style='background-color:%s;color:white;padding:15px;border-radius:5px;margin-bottom:20px;'>" +
            "<h2 style='margin:0;'>Payment Reminder</h2>" +
            "<p style='margin:5px 0 0;font-size:14px;'>%s - %d Days Overdue</p>" +
            "</div>" +

            "<p style='font-size:16px;'>Dear Sir/Madam,</p>" +
            "<p>This is an automated payment reminder for the following placement:</p>" +

            "<div style='background:#f9f9f9;padding:20px;border-radius:8px;margin:20px 0;'>" +
            "<h3 style='color:#555;margin-top:0;border-bottom:2px solid #ddd;padding-bottom:10px;'>Invoice Details</h3>" +
            "<table style='width:100%%;border-collapse:collapse;'>" +
            "<tr><td style='padding:8px 0;font-weight:bold;width:180px;'>Client:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Candidate:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Designation:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Joining Date:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Days Since Joining:</td>" +
            "<td><span style='color:%s;font-weight:bold;'>%d days</span></td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Payment Terms:</td><td>%s</td></tr>" +
            "</table>" +
            "</div>" +

            "<div style='background:#e8f5e9;padding:20px;border-radius:8px;margin:20px 0;text-align:center;'>" +
            "<p style='margin:0;font-size:14px;color:#666;'>Outstanding Amount</p>" +
            "<h2 style='margin:10px 0;color:#2e7d32;font-size:32px;'>Rs. %.2f</h2>" +
            "</div>" +

            "<p style='color:#d32f2f;font-weight:bold;'>Action Required: Please process the payment at your earliest convenience.</p>" +

            "<div style='background:#fff3e0;padding:20px;border-radius:8px;margin:20px 0;'>" +
            "<h3 style='color:#555;margin-top:0;border-bottom:2px solid #ffb74d;padding-bottom:10px;'>Bank Details</h3>" +
            "<table style='width:100%%;border-collapse:collapse;'>" +
            "<tr><td style='padding:8px 0;font-weight:bold;width:180px;'>Account Number:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>IFSC Code:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>GSTIN:</td><td>%s</td></tr>" +
            "</table>" +
            "</div>" +

            "<div style='margin-top:30px;padding-top:20px;border-top:1px solid #ddd;'>" +
            "<p style='color:#666;font-size:14px;'>For any queries, please contact us.</p>" +
            "<p style='margin:0;'><strong>Best Regards,</strong><br>ATS Circle Team</p>" +
            "</div>" +
            "</div></body></html>",

            severityColor, severityLabel, days,
            invoice.getClientName(),
            invoice.getCandidateName(),
            invoice.getDesignation(),
            invoice.getJoiningDate(),
            severityColor, days,
            invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "N/A",
            invoice.getCalculatedFee(),
            invoice.getAccountNumber()  != null ? invoice.getAccountNumber()  : "N/A",
            invoice.getIfscCode()       != null ? invoice.getIfscCode()       : "N/A",
            invoice.getYourGSTIN()      != null ? invoice.getYourGSTIN()      : "N/A"
        );
    }

    private String getSeverity(int days) {
        if (days >= 90) return "HIGH";
        if (days >= 60) return "MEDIUM";
        return "LOW";
    }

    // ── Notification DTO ──────────────────────────────────────────────────────

    public static class PaymentReminderNotification {
        private String invoiceId;
        private String organizationId;
        private String clientId;
        private String clientName;
        private String candidateName;
        private String message;
        private int daysOverdue;
        private Double amount;
        private String type;
        private String timestamp;
        private String severity;

        public String getInvoiceId()                    { return invoiceId; }
        public void setInvoiceId(String v)              { this.invoiceId = v; }
        public String getOrganizationId()               { return organizationId; }
        public void setOrganizationId(String v)         { this.organizationId = v; }
        public String getClientId()                     { return clientId; }
        public void setClientId(String v)               { this.clientId = v; }
        public String getClientName()                   { return clientName; }
        public void setClientName(String v)             { this.clientName = v; }
        public String getCandidateName()                { return candidateName; }
        public void setCandidateName(String v)          { this.candidateName = v; }
        public String getMessage()                      { return message; }
        public void setMessage(String v)                { this.message = v; }
        public int getDaysOverdue()                     { return daysOverdue; }
        public void setDaysOverdue(int v)               { this.daysOverdue = v; }
        public Double getAmount()                       { return amount; }
        public void setAmount(Double v)                 { this.amount = v; }
        public String getType()                         { return type; }
        public void setType(String v)                   { this.type = v; }
        public String getTimestamp()                    { return timestamp; }
        public void setTimestamp(String v)              { this.timestamp = v; }
        public String getSeverity()                     { return severity; }
        public void setSeverity(String v)               { this.severity = v; }
    }
}