package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.Invoicemodel;
import com.example.ATSCIRCLE.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class InvoiceEmailService {

    @Autowired
    private EmailService emailService;

    /**
     * Send invoice email to client with file attachments.
     * Routes through the org's connected Gmail / Outlook account.
     * Falls back to SMTP if no provider is configured.
     */
    public void sendInvoiceEmail(Invoicemodel invoice, MultipartFile[] files) {
        String toEmail = invoice.getSendInvoiceMailTo() != null
                ? invoice.getSendInvoiceMailTo()
                : invoice.getClientEmail();

        if (toEmail == null || toEmail.isEmpty()) {
            throw new RuntimeException("No email address found to send invoice");
        }

        String subject     = "Invoice - Placement Fee for " + invoice.getCandidateName();
        String htmlContent = generateInvoiceHtml(invoice);

        // ✅ Pass organizationId → routes through org's connected Gmail / Outlook
        emailService.sendHtmlEmailWithAttachmentsForOrg(
                invoice.getOrganizationId(), toEmail, subject, htmlContent, files);

        int fileCount = (files != null && files.length > 0) ? files.length : 0;
        System.out.println("📧 Invoice email sent via org=" + invoice.getOrganizationId()
                + " to=" + toEmail
                + (fileCount > 0 ? " with " + fileCount + " attachments" : ""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    private String generateInvoiceHtml(Invoicemodel invoice) {
        double feeAmount = calculateFeeAmount(invoice);
        double gstAmount = 0;
        double tdsAmount = 0;

        if ("registered".equals(invoice.getGstStatus())) {
            gstAmount = (feeAmount * 18) / 100;
        }
        if (Boolean.TRUE.equals(invoice.getTDSApplicable())) {
            tdsAmount = (feeAmount * 10) / 100;
        }

        double netAmount = feeAmount + gstAmount - tdsAmount;

        LocalDate today   = LocalDate.now();
        LocalDate dueDate = calculateDueDate(today, invoice.getPaymentTerms());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        String invoiceNumber    = "INV-" + today.getYear() + "-" + String.format("%04d", new Random().nextInt(9999));
        String billingModelText = getBillingModelText(invoice);

        String css = "<style>" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;padding:0;background:#f5f5f5;}" +
            ".wrap{max-width:800px;margin:20px auto;background:#fff;box-shadow:0 2px 10px rgba(0,0,0,.1);}" +
            ".top-bar{height:6px;background:linear-gradient(90deg,#218084,#1a6470);}" +
            ".header{padding:48px 48px 32px;background:#f8fafb;border-bottom:1px solid #e5e7eb;}" +
            ".header-flex{display:flex;justify-content:space-between;align-items:flex-start;}" +
            ".inv-title{font-size:28px;font-weight:800;color:#218084;margin:0 0 4px;}" +
            ".inv-sub{font-size:13px;color:#6b7280;margin:0;}" +
            ".inv-meta{text-align:right;}" +
            ".inv-meta table{border-collapse:collapse;font-size:12px;}" +
            ".inv-meta td{padding:6px 12px;}" +
            ".lbl{color:#6b7280;font-weight:600;}" +
            ".val{color:#1f2937;font-weight:600;}" +
            ".inv-no{color:#218084!important;font-weight:800!important;font-size:14px!important;}" +
            ".due{color:#ef4444!important;font-weight:700!important;}" +
            ".addrs{display:grid;grid-template-columns:1fr 1fr;gap:48px;padding:40px 48px;border-bottom:1px solid #e5e7eb;}" +
            ".addr-ttl{font-size:11px;font-weight:800;text-transform:uppercase;color:#218084;letter-spacing:1.2px;margin-bottom:16px;}" +
            ".co-name{font-size:15px;font-weight:700;color:#1f2937;margin-bottom:8px;}" +
            ".addr-txt{font-size:13px;color:#6b7280;line-height:1.8;margin-bottom:12px;}" +
            ".addr-det{border-top:1px solid #e5e7eb;padding-top:12px;font-size:12px;}" +
            ".addr-det div{margin-bottom:6px;}" +
            ".al{color:#6b7280;} .av{color:#1f2937;font-weight:600;}" +
            ".place{padding:40px 48px;border-bottom:1px solid #e5e7eb;}" +
            ".sec-ttl{font-size:11px;font-weight:800;text-transform:uppercase;color:#218084;letter-spacing:1.2px;margin-bottom:20px;}" +
            ".info-tbl{width:100%;border-collapse:collapse;}" +
            ".info-tbl tr{border:1px solid #e5e7eb;}" +
            ".info-tbl tr:first-child{background:#f8fafb;}" +
            ".info-tbl td{padding:14px 16px;font-size:12px;}" +
            ".info-tbl .lbl{font-weight:700;color:#6b7280;width:30%;}" +
            ".info-tbl .val{font-size:13px;color:#1f2937;font-weight:600;}" +
            ".info-tbl .hl{color:#218084;font-weight:700;}" +
            ".charges{padding:40px 48px;}" +
            ".li-tbl{width:100%;border-collapse:collapse;margin-bottom:32px;}" +
            ".li-tbl tr{border-bottom:1px solid #e5e7eb;}" +
            ".li-tbl tr:first-child{background:#f8fafb;border-top:1px solid #e5e7eb;}" +
            ".li-tbl td{padding:12px 0;font-size:12px;}" +
            ".li-tbl .hd{font-weight:700;color:#6b7280;}" +
            ".li-tbl .ham{text-align:right;font-weight:700;color:#6b7280;}" +
            ".li-tbl .id{font-size:13px;color:#1f2937;}" +
            ".li-tbl .ia{text-align:right;font-size:14px;color:#218084;font-weight:700;}" +
            ".tax-box{background:linear-gradient(135deg,#f0fdf4,#e8f5e9);border:1px solid #86efac;border-radius:10px;padding:28px;margin-bottom:24px;}" +
            ".tax-tbl{width:100%;border-collapse:collapse;font-size:13px;}" +
            ".tax-tbl tr{border-bottom:1px solid rgba(34,197,94,.2);}" +
            ".tax-tbl td{padding:12px 0;color:#1f2937;}" +
            ".tax-tbl .r{text-align:right;font-weight:600;}" +
            ".tax-tbl .tot{border-bottom:2px solid #22c55e;}" +
            ".tax-tbl .tds{color:#ef4444;font-weight:700;}" +
            ".tax-tbl .fin td{padding:16px 0;font-size:15px;color:#22c55e;font-weight:800;}" +
            ".terms{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:24px;}" +
            ".tb{border-radius:8px;padding:20px;}" +
            ".tb.pay{background:#e3f2fd;border-left:4px solid #2196f3;}" +
            ".tb.guar{background:#f0fdf4;border-left:4px solid #22c55e;}" +
            ".tt{font-size:11px;font-weight:800;text-transform:uppercase;letter-spacing:.8px;margin-bottom:8px;}" +
            ".tb.pay .tt{color:#1565c0;} .tb.guar .tt{color:#15803d;}" +
            ".tv{font-size:14px;color:#1f2937;font-weight:700;margin-bottom:6px;}" +
            ".td2{font-size:11px;color:#6b7280;}" +
            ".td2 strong{font-weight:600;}" +
            ".note{background:#fffbeb;border-left:4px solid #f59e0b;border-radius:8px;padding:16px;font-size:12px;color:#6b7280;}" +
            ".note strong{color:#b45309;}" +
            ".footer{background:linear-gradient(135deg,#f8fafb,#f3f4f6);padding:32px 48px;border-top:1px solid #e5e7eb;text-align:center;}" +
            ".footer p{margin:0 0 8px;}" +
            ".ft{font-size:12px;color:#1f2937;font-weight:600;}" +
            ".fs{font-size:11px;color:#9ca3af;}" +
            "</style>";

        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            + "<title>Invoice - " + safeStr(invoice.getCandidateName()) + "</title>"
            + css
            + "</head><body><div class='wrap'>"
            + "<div class='top-bar'></div>"

            // Header
            + "<div class='header'><div class='header-flex'>"
            + "<div><div class='inv-title'>INVOICE</div>"
            + "<div class='inv-sub'>PlacementPay - Professional Recruitment Services</div></div>"
            + "<div class='inv-meta'><table>"
            + "<tr><td class='lbl'>Invoice No.</td><td class='val inv-no'>" + invoiceNumber + "</td></tr>"
            + "<tr><td class='lbl'>Issue Date</td><td class='val'>" + today.format(formatter) + "</td></tr>"
            + "<tr><td class='lbl'>Due Date</td><td class='val due'>" + dueDate.format(formatter) + "</td></tr>"
            + "</table></div></div></div>"

            // Addresses
            + "<div class='addrs'>"
            + "<div><div class='addr-ttl'>From</div>"
            + "<div class='co-name'>Your Company</div>"
            + "<div class='addr-txt'>"
            + getAddressField(invoice.getFromAddress(), "streetLine1", "") + "<br>"
            + getAddressField(invoice.getFromAddress(), "city", "") + ", "
            + getAddressField(invoice.getFromAddress(), "state", "") + " "
            + getAddressField(invoice.getFromAddress(), "zipcode", "") + "<br>"
            + getAddressField(invoice.getFromAddress(), "country", "India")
            + "</div>"
            + "<div class='addr-det'>"
            + "<div><span class='al'>GSTIN:</span> <span class='av'>" + safeStr(invoice.getYourGSTIN(), "N/A") + "</span></div>"
            + "<div><span class='al'>Bank A/c:</span> <span class='av'>" + safeStr(invoice.getAccountNumber(), "N/A") + "</span></div>"
            + "</div></div>"
            + "<div><div class='addr-ttl'>Bill To</div>"
            + "<div class='co-name'>" + safeStr(invoice.getClientName(), "Client") + "</div>"
            + "<div class='addr-txt'>"
            + getAddressField(invoice.getShippingAddress(), "streetLine1", "") + "<br>"
            + getAddressField(invoice.getShippingAddress(), "city", "") + ", "
            + getAddressField(invoice.getShippingAddress(), "state", "") + " "
            + getAddressField(invoice.getShippingAddress(), "zipcode", "") + "<br>"
            + getAddressField(invoice.getShippingAddress(), "country", "India")
            + "</div>"
            + "<div class='addr-det'>"
            + "<div><span class='al'>Email:</span> <span class='av'>" + safeStr(invoice.getClientEmail(), "N/A") + "</span></div>"
            + "<div><span class='al'>GSTIN:</span> <span class='av'>" + safeStr(invoice.getClientGSTIN(), "N/A") + "</span></div>"
            + "</div></div></div>"

            // Placement info
            + "<div class='place'><div class='sec-ttl'>Placement Information</div>"
            + "<table class='info-tbl'>"
            + "<tr><td class='lbl'>Candidate Name</td><td class='val'>" + safeStr(invoice.getCandidateName(), "N/A") + "</td>"
            + "<td class='lbl'>Designation</td><td class='val'>" + safeStr(invoice.getDesignation(), "N/A") + "</td></tr>"
            + "<tr><td class='lbl'>Placement Type</td><td class='val'>" + safeStr(invoice.getPlacementType(), "N/A") + "</td>"
            + "<td class='lbl'>Annual CTC</td><td class='val hl'>Rs. " + formatAmount(invoice.getCtc()) + "</td></tr>"
            + "<tr><td class='lbl'>Joining Date</td><td class='val'>"
            + (invoice.getJoiningDate() != null ? invoice.getJoiningDate().format(formatter) : "N/A") + "</td>"
            + "<td class='lbl'>Guarantee Period</td><td class='val'>"
            + (invoice.getGuaranteeDays() != null ? invoice.getGuaranteeDays() : 90) + " days</td></tr>"
            + "</table></div>"

            // Charges
            + "<div class='charges'><div class='sec-ttl'>Charges and Taxes</div>"
            + "<table class='li-tbl'>"
            + "<tr><td class='hd'>Description</td><td class='ham'>Amount</td></tr>"
            + "<tr><td class='id'>" + billingModelText + "</td><td class='ia'>Rs. " + formatAmount(feeAmount) + "</td></tr>"
            + "</table>"
            + "<div class='tax-box'><table class='tax-tbl'>"
            + "<tr><td>Subtotal</td><td class='r'>Rs. " + formatAmount(feeAmount) + "</td></tr>"
            + "<tr><td>Add: GST @ 18% (SAC 9989)</td><td class='r'>Rs. " + formatAmount(gstAmount) + "</td></tr>"
            + "<tr class='tot'><td><strong>Less: TDS @ 10%</strong></td><td class='r tds'>Rs. " + formatAmount(tdsAmount) + "</td></tr>"
            + "<tr class='fin'><td>TOTAL PAYABLE</td><td class='r'>Rs. " + formatAmount(netAmount) + "</td></tr>"
            + "</table></div>"
            + "<div class='terms'>"
            + "<div class='tb pay'><div class='tt'>Payment Terms</div>"
            + "<div class='tv'>" + safeStr(invoice.getPaymentTerms(), "Net 30") + "</div>"
            + "<div class='td2'>Due: <strong>" + dueDate.format(formatter) + "</strong></div></div>"
            + "<div class='tb guar'><div class='tt'>Guarantee</div>"
            + "<div class='tv'>" + (invoice.getGuaranteeDays() != null ? invoice.getGuaranteeDays() : 90) + " days from joining date</div>"
            + "<div class='td2'>Ends: <strong>"
            + (invoice.getGuaranteeEndDate() != null ? invoice.getGuaranteeEndDate().format(formatter) : "N/A")
            + "</strong></div></div></div>"
            + "<div class='note'><strong>Important:</strong> This invoice is generated by PlacementPay and constitutes "
            + "a valid tax document for GST compliance in India (SAC 9989 - Professional Services).</div>"
            + "</div>"

            // Footer
            + "<div class='footer'>"
            + "<p class='ft'>PlacementPay - Fast, Compliant Recruitment Invoicing</p>"
            + "<p class='fs'>Computer-generated invoice - No signature required</p>"
            + "</div>"
            + "</div></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private double calculateFeeAmount(Invoicemodel invoice) {
        double ctc      = invoice.getCtc() != null ? invoice.getCtc() : 0;
        String feeModel = invoice.getFeeModel();

        if (feeModel == null)
            return invoice.getCalculatedFee() != null ? invoice.getCalculatedFee() : 0;

        switch (feeModel.toLowerCase()) {
            case "placement":
                double pct = invoice.getFeePercentage() != null ? invoice.getFeePercentage() : 15;
                return (ctc * pct) / 100;
            case "fixed":
                return invoice.getFixedAmount() != null ? invoice.getFixedAmount() : 0;
            case "hybrid":
                double fixed = invoice.getFixedAmount()   != null ? invoice.getFixedAmount()   : 0;
                double hp    = invoice.getFeePercentage() != null ? invoice.getFeePercentage() : 10;
                return fixed + (ctc * hp) / 100;
            default:
                return invoice.getCalculatedFee() != null ? invoice.getCalculatedFee() : 0;
        }
    }

    private String getBillingModelText(Invoicemodel invoice) {
        String feeModel = invoice.getFeeModel();
        if (feeModel == null) return "Placement Fee";

        switch (feeModel.toLowerCase()) {
            case "placement":
                double p = invoice.getFeePercentage() != null ? invoice.getFeePercentage() : 15;
                return String.format("Placement Fee (%.0f%% of CTC)", p);
            case "fixed":    return "Fixed Amount";
            case "hybrid":
                double hp = invoice.getFeePercentage() != null ? invoice.getFeePercentage() : 10;
                return String.format("Hybrid (Fixed + %.0f%%)", hp);
            case "retainer": return "Monthly Retainer";
            case "hourly":   return "Hourly Billing";
            default:         return "Placement Fee";
        }
    }

    private LocalDate calculateDueDate(LocalDate issueDate, String paymentTerms) {
        if (paymentTerms == null) return issueDate.plusDays(30);
        if (paymentTerms.contains("7"))  return issueDate.plusDays(7);
        if (paymentTerms.contains("45")) return issueDate.plusDays(45);
        if (paymentTerms.contains("60")) return issueDate.plusDays(60);
        if (paymentTerms.toLowerCase().contains("immediate")) return issueDate;
        return issueDate.plusDays(30);
    }

    private String formatAmount(Double amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    private String safeStr(String value) {
        return value != null ? value : "";
    }

    private String safeStr(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    private String getAddressField(Invoicemodel.Address address, String field, String defaultValue) {
        if (address == null) return defaultValue;
        try {
            switch (field.toLowerCase()) {
                case "streetline1": return address.getStreetLine1() != null ? address.getStreetLine1() : defaultValue;
                case "streetline2": return address.getStreetLine2() != null ? address.getStreetLine2() : defaultValue;
                case "city":        return address.getCity()        != null ? address.getCity()        : defaultValue;
                case "state":       return address.getState()       != null ? address.getState()       : defaultValue;
                case "zipcode":     return address.getZipcode()     != null ? address.getZipcode()     : defaultValue;
                case "country":     return address.getCountry()     != null ? address.getCountry()     : defaultValue;
                default:            return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }
}