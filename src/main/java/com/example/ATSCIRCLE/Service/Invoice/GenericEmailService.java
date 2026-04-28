package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import com.example.ATSCIRCLE.Models.Invoice.InvoiceItem;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Address;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GenericEmailService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvoicePdfService pdfService;

    // =========================================================================
    // sendInvoiceEmail
    // supplier.getId() = organizationId → EmailService routes via their
    // connected Gmail / Outlook, or falls back to SMTP if none configured.
    // =========================================================================

    public void sendInvoiceEmail(Invoice invoice, Users supplier, Company customer) {
        try {
            String subject   = "Invoice " + invoice.getInvoiceNumber()
                    + " from " + supplier.getOrganizationName();
            String emailBody = buildInvoiceEmailBody(invoice, supplier, customer);
            String toEmail   = getEmailAddress(invoice, customer);

            byte[] pdfBytes      = pdfService.generateInvoicePdf(invoice, supplier, customer);
            String pdfFilename   = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
            MultipartFile pdfAttachment = new MockMultipartFile(
                    pdfFilename, pdfFilename, "application/pdf", pdfBytes);

            // ✅ Pass supplier.getId() → routes through supplier's connected email account
            emailService.sendHtmlEmailWithAttachmentsForOrg(
                    supplier.getId(), toEmail, subject, emailBody,
                    new MultipartFile[]{pdfAttachment});

            System.out.println("✅ Invoice email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send invoice email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // sendReminderEmail
    // =========================================================================

    public void sendReminderEmail(Invoice invoice, Users supplier, Company customer,
                                   String reminderType) {
        try {
            String subject;
            if ("due".equals(reminderType)) {
                subject = "Payment Due - Invoice " + invoice.getInvoiceNumber();
            } else if ("overdue".equals(reminderType)) {
                subject = "Payment Overdue - Invoice " + invoice.getInvoiceNumber();
            } else {
                subject = "Payment Reminder - Invoice " + invoice.getInvoiceNumber();
            }

            String emailBody = buildReminderEmailBody(invoice, supplier, customer, reminderType);
            String toEmail   = getEmailAddress(invoice, customer);

            byte[] pdfBytes      = pdfService.generateInvoicePdf(invoice, supplier, customer);
            String pdfFilename   = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
            MultipartFile pdfAttachment = new MockMultipartFile(
                    pdfFilename, pdfFilename, "application/pdf", pdfBytes);

            // ✅ Pass supplier.getId() → routes through supplier's connected email account
            emailService.sendHtmlEmailWithAttachmentsForOrg(
                    supplier.getId(), toEmail, subject, emailBody,
                    new MultipartFile[]{pdfAttachment});

            System.out.println("✅ Reminder email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send reminder email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getEmailAddress(Invoice invoice, Company customer) {
        if (invoice.getSendToEmail() != null && !invoice.getSendToEmail().trim().isEmpty()) {
            return invoice.getSendToEmail();
        }
        if (customer != null && customer.getEmail() != null
                && !customer.getEmail().trim().isEmpty()) {
            return customer.getEmail();
        }
        return "customer@example.com";
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getStreetLine1() != null) sb.append(address.getStreetLine1());
        if (address.getStreetLine2() != null) sb.append("<br>").append(address.getStreetLine2());
        if (address.getCity() != null)        sb.append("<br>").append(address.getCity());
        if (address.getState() != null)       sb.append(", ").append(address.getState());
        if (address.getZipcode() != null)     sb.append(" - ").append(address.getZipcode());
        if (address.getCountry() != null)     sb.append("<br>").append(address.getCountry());
        return sb.toString();
    }

    // =========================================================================
    // EMAIL BODY BUILDERS (unchanged from original)
    // =========================================================================

    private String buildInvoiceEmailBody(Invoice invoice, Users supplier, Company customer) {

        // Line items table
        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<table style='width:100%;border-collapse:collapse;margin:20px 0;font-size:13px;'>");
        itemsTable.append("<tr style='background:#f3f4f6;'>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:left;'>Image</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:left;'>Description</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:left;'>HSN/SAC</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>Qty</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>Rate</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>Amount</th>");
        itemsTable.append("<th style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>GST %%</th>");
        itemsTable.append("</tr>");

        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                itemsTable.append("<tr>");

                itemsTable.append("<td style='border:1px solid #e5e7eb;padding:10px;text-align:center;'>");
                if (item.getItemImageUrl() != null && !item.getItemImageUrl().isEmpty()) {
                    itemsTable.append(String.format(
                        "<a href='%s' target='_blank' style='text-decoration:none;display:block;'>" +
                        "<img src='%s' style='max-width:60px;max-height:60px;border-radius:4px;display:block;margin:0 auto 8px;border:1px solid #e5e7eb;' alt='%s'>" +
                        "<span style='color:#2563eb;font-size:11px;font-weight:500;text-decoration:underline;'>View Image</span>" +
                        "</a>",
                        item.getItemImageUrl(), item.getItemImageUrl(),
                        item.getItemName() != null ? item.getItemName() : "Item"));
                } else {
                    itemsTable.append("<span style='color:#9ca3af;'>-</span>");
                }
                itemsTable.append("</td>");

                itemsTable.append("<td style='border:1px solid #e5e7eb;padding:10px;'>");
                itemsTable.append(String.format("<strong>%s</strong>",
                        item.getItemName() != null ? item.getItemName() : "-"));
                if (item.getItemDescription() != null && !item.getItemDescription().isEmpty()) {
                    itemsTable.append(String.format(
                        "<br><span style='color:#6b7280;font-size:11px;'>%s</span>",
                        item.getItemDescription()));
                }
                itemsTable.append("</td>");

                itemsTable.append(String.format(
                    "<td style='border:1px solid #e5e7eb;padding:10px;'>%s</td>",
                    item.getHsnSac() != null ? item.getHsnSac() : ""));
                itemsTable.append(String.format(
                    "<td style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>%d</td>",
                    item.getQuantity() != null ? item.getQuantity() : 0));
                itemsTable.append(String.format(
                    "<td style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>Rs. %.2f</td>",
                    item.getRate() != null ? item.getRate() : 0.0));
                itemsTable.append(String.format(
                    "<td style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>Rs. %.2f</td>",
                    item.getAmount() != null ? item.getAmount() : 0.0));
                itemsTable.append(String.format(
                    "<td style='border:1px solid #e5e7eb;padding:10px;text-align:right;'>%.1f%%</td>",
                    item.getGstPercent() != null ? item.getGstPercent() : 0.0));
                itemsTable.append("</tr>");
            }
        }
        itemsTable.append("</table>");

        // Payment link section
        String paymentLinkSection = "";
        if (invoice.getPaymentLink() != null && !invoice.getPaymentLink().isEmpty()) {
            paymentLinkSection = String.format(
                "<div style='background-color:#e3f2fd;padding:20px;border-radius:8px;margin:20px 0;text-align:center;'>" +
                "<h3 style='color:#1976d2;margin-top:0;'>Quick Payment</h3>" +
                "<p style='margin:10px 0;'>Click the button below to pay online:</p>" +
                "<a href='%s' style='display:inline-block;background-color:#1976d2;color:white;padding:12px 30px;text-decoration:none;border-radius:6px;font-weight:bold;margin:10px 0;'>Pay Now</a>" +
                "</div>",
                invoice.getPaymentLink());
        }

        // UPI section
        String upiSection = "";
        if (invoice.getUpiId() != null && !invoice.getUpiId().isEmpty()) {
            upiSection = "<div style='background-color:#f3e5f5;padding:20px;border-radius:8px;margin:20px 0;'>" +
                "<h3 style='color:#7b1fa2;margin-top:0;border-bottom:2px solid #ba68c8;padding-bottom:10px;'>UPI Payment Details</h3>" +
                String.format("<p style='margin:10px 0;'><strong>UPI ID:</strong> %s</p>", invoice.getUpiId());
            if (invoice.getUpiNote() != null && !invoice.getUpiNote().isEmpty()) {
                upiSection += String.format(
                    "<p style='margin:10px 0;font-size:12px;color:#666;'>%s</p>", invoice.getUpiNote());
            }
            if (invoice.getUpiQrCodeUrl() != null && !invoice.getUpiQrCodeUrl().isEmpty()) {
                upiSection += String.format(
                    "<p style='margin:15px 0;'><strong>Scan to Pay:</strong></p>" +
                    "<img src='%s' style='max-width:180px;border:1px solid #e5e7eb;border-radius:8px;' alt='UPI QR Code'>",
                    invoice.getUpiQrCodeUrl());
            }
            upiSection += "</div>";
        }

        // Additional details section
        String additionalDetailsSection = "";
        if (invoice.getEwayBillNumber() != null || invoice.getArnNumber() != null
                || invoice.getFssaiNumber() != null || invoice.getPackingSlipNumber() != null) {
            additionalDetailsSection =
                "<div style='background-color:#fff9e6;padding:20px;border-radius:8px;margin:20px 0;border-left:4px solid #fbbf24;'>" +
                "<h3 style='color:#92400e;margin-top:0;'>Additional Details</h3>" +
                "<table style='width:100%;font-size:13px;'>";
            if (invoice.getEwayBillNumber() != null)
                additionalDetailsSection += String.format(
                    "<tr><td style='padding:5px 0;color:#666;width:150px;'><strong>E-Way Bill:</strong></td><td>%s</td></tr>",
                    invoice.getEwayBillNumber());
            if (invoice.getArnNumber() != null)
                additionalDetailsSection += String.format(
                    "<tr><td style='padding:5px 0;color:#666;'><strong>ARN Number:</strong></td><td>%s</td></tr>",
                    invoice.getArnNumber());
            if (invoice.getFssaiNumber() != null)
                additionalDetailsSection += String.format(
                    "<tr><td style='padding:5px 0;color:#666;'><strong>FSSAI License:</strong></td><td>%s</td></tr>",
                    invoice.getFssaiNumber());
            if (invoice.getPackingSlipNumber() != null)
                additionalDetailsSection += String.format(
                    "<tr><td style='padding:5px 0;color:#666;'><strong>Packing Slip:</strong></td><td>%s</td></tr>",
                    invoice.getPackingSlipNumber());
            additionalDetailsSection += "</table></div>";
        }

        // Notes section
        String notesSection = "";
        if (invoice.getAdditionalNotes() != null && !invoice.getAdditionalNotes().isEmpty()) {
            notesSection = String.format(
                "<div style='background-color:#fafafa;padding:20px;border-radius:8px;margin:20px 0;border-left:4px solid #0f766e;'>" +
                "<h3 style='color:#555;margin-top:0;'>Additional Notes</h3>" +
                "<p style='margin:0;white-space:pre-wrap;'>%s</p>" +
                "</div>",
                invoice.getAdditionalNotes().replace("\n", "<br>"));
        }

        // Supplier address
        String supplierAddress = "";
        if (invoice.getStreetLine1() != null) {
            supplierAddress = invoice.getStreetLine1();
            if (invoice.getStreetLine2() != null) supplierAddress += "<br>" + invoice.getStreetLine2();
            if (invoice.getCity() != null)        supplierAddress += "<br>" + invoice.getCity();
            if (invoice.getState() != null)       supplierAddress += ", " + invoice.getState();
            if (invoice.getZipcode() != null)     supplierAddress += " - " + invoice.getZipcode();
            if (invoice.getCountry() != null)     supplierAddress += "<br>" + invoice.getCountry();
        }

        // Signature section
        String signatureSection = "";
        if (invoice.getAuthorisedSignatoryName() != null || invoice.getDesignation() != null) {
            signatureSection = "<div style='text-align:right;margin-top:30px;'>" +
                String.format("<p style='margin:2px 0;'>For <strong>%s</strong></p>",
                        invoice.getLegalBusinessName()) +
                "<p style='margin:30px 0 2px;'>___________________________</p>" +
                "<p style='margin:0;'>Authorised Signatory</p>";
            if (invoice.getAuthorisedSignatoryName() != null) {
                signatureSection += String.format("<p style='margin:5px 0;'><strong>%s</strong>",
                        invoice.getAuthorisedSignatoryName());
                if (invoice.getDesignation() != null)
                    signatureSection += String.format(", %s", invoice.getDesignation());
                signatureSection += "</p>";
            }
            signatureSection += "</div>";
        }

        return String.format(
            "<html><head><meta charset='UTF-8'></head>" +
            "<body style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Arial,sans-serif;line-height:1.6;color:#1f2937;background-color:#f9fafb;margin:0;padding:20px;'>" +
            "<div style='max-width:900px;margin:0 auto;background:#ffffff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1);overflow:hidden;'>" +

            "<div style='background:linear-gradient(135deg,#0f766e 0%%,#115e63 100%%);color:white;padding:30px;text-align:center;'>" +
            "<h1 style='margin:0;font-size:28px;font-weight:700;'>TAX INVOICE</h1>" +
            "<p style='margin:8px 0 0;font-size:15px;opacity:0.95;'>GST Compliant Invoice</p>" +
            "<p style='margin:8px 0 0;font-size:13px;opacity:0.9;'>Invoice PDF attached to this email</p>" +
            "</div>" +

            "<div style='padding:30px;'>" +
            "<p style='font-size:16px;margin-bottom:8px;'>Dear <strong>%s</strong>,</p>" +
            "<p style='color:#6b7280;margin-bottom:24px;'>Please find the details of your invoice below. A PDF copy is attached for your records.</p>" +

            "<div style='background:#f9fafb;padding:20px;border-radius:8px;border:1px solid #e5e7eb;margin-bottom:24px;'>" +
            "<table style='width:100%%;font-size:14px;'>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Invoice No:</td><td style='padding:4px 0;text-align:right;font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Date:</td><td style='padding:4px 0;text-align:right;font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>PO/Ref:</td><td style='padding:4px 0;text-align:right;font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Amount Due:</td><td style='padding:4px 0;text-align:right;font-weight:700;font-size:18px;color:#0f766e;'>Rs. %.2f</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Payment Terms:</td><td style='padding:4px 0;text-align:right;'>%s</td></tr>" +
            "</table>" +
            "</div>" +

            "<div style='background:#f9fafb;padding:20px;border-radius:8px;border:1px solid #e5e7eb;margin-bottom:24px;'>" +
            "<table style='width:100%%;font-size:13px;'>" +
            "<tr>" +
            "<td style='width:50%%;vertical-align:top;padding-right:20px;'>" +
            "<h4 style='margin:0 0 8px;color:#0f766e;'>Supplier (From)</h4>" +
            "<p style='margin:0;font-weight:600;'>%s</p>" +
            "<p style='margin:4px 0 0;color:#6b7280;'>%s</p>" +
            "%s" +
            "</td>" +
            "<td style='width:50%%;vertical-align:top;'>" +
            "<h4 style='margin:0 0 8px;color:#0f766e;'>Recipient (Bill To)</h4>" +
            "<p style='margin:0;font-weight:600;'>%s</p>" +
            "<p style='margin:4px 0 0;color:#6b7280;'>%s</p>" +
            "%s" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "</div>" +

            "<div style='margin-bottom:24px;'>" +
            "<h3 style='margin:0 0 12px;border-bottom:2px solid #e5e7eb;padding-bottom:8px;'>Invoice Items</h3>" +
            "%s" +
            "</div>" +

            "<div style='background:#f9fafb;padding:20px;border-radius:8px;border:1px solid #e5e7eb;margin-bottom:24px;text-align:right;font-size:14px;'>" +
            "<table style='width:100%%;'>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Subtotal:</td><td style='text-align:right;font-weight:600;'>Rs. %.2f</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>GST:</td><td style='text-align:right;font-weight:600;'>Rs. %.2f</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>TDS (-):</td><td style='text-align:right;font-weight:600;color:#dc2626;'>Rs. %.2f</td></tr>" +
            "<tr><td style='padding:4px 0;color:#6b7280;'>Advance (-):</td><td style='text-align:right;font-weight:600;color:#dc2626;'>Rs. %.2f</td></tr>" +
            "<tr style='border-top:2px solid #0f766e;'><td style='padding:8px 0 0;font-weight:700;font-size:16px;color:#0f766e;'>Net Payable:</td>" +
            "<td style='text-align:right;font-weight:700;font-size:18px;color:#0f766e;padding-top:8px;'>Rs. %.2f</td></tr>" +
            "</table>" +
            "</div>" +

            "%s%s" +  // additionalDetailsSection + paymentLinkSection

            "<div style='background:#fff3e0;padding:20px;border-radius:8px;border:1px solid #ffb74d;margin-bottom:24px;'>" +
            "<h3 style='color:#e65100;margin:0 0 16px;'>Bank Transfer Details</h3>" +
            "<table style='width:100%%;font-size:14px;'>" +
            "<tr><td style='padding:6px 0;color:#666;width:180px;'>Account Name:</td><td style='font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:6px 0;color:#666;'>Bank:</td><td style='font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:6px 0;color:#666;'>Account Number:</td><td style='font-weight:600;'>%s</td></tr>" +
            "<tr><td style='padding:6px 0;color:#666;'>IFSC Code:</td><td style='font-weight:600;'>%s</td></tr>" +
            "</table>" +
            "</div>" +

            "%s%s%s" +  // upiSection + notesSection + signatureSection

            "<div style='margin-top:30px;padding-top:20px;border-top:2px solid #e5e7eb;text-align:center;'>" +
            "<p style='color:#6b7280;font-size:13px;margin:0 0 8px;'>If you have any questions, please contact us.</p>" +
            "<p style='margin:0;font-size:14px;'><strong>%s</strong></p>" +
            "<p style='margin:4px 0 0;font-size:13px;color:#6b7280;'>%s</p>" +
            "</div>" +
            "</div></div></body></html>",

            // greeting
            customer.getCompanyName(),
            // invoice details
            invoice.getInvoiceNumber(),
            invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : "N/A",
            invoice.getCustomerPO() != null && !invoice.getCustomerPO().isEmpty() ? invoice.getCustomerPO() : "N/A",
            invoice.getNetPayable() != null ? invoice.getNetPayable() : 0.0,
            invoice.getPaymentTerms() != null && !invoice.getPaymentTerms().isEmpty() ? invoice.getPaymentTerms() : "N/A",
            // supplier
            invoice.getLegalBusinessName() != null ? invoice.getLegalBusinessName() : supplier.getOrganizationName(),
            supplierAddress,
            invoice.getGstNumber() != null && !invoice.getGstNumber().isEmpty()
                ? String.format("<p style='margin:4px 0 0;font-size:13px;'><strong>GSTIN:</strong> %s</p>", invoice.getGstNumber()) : "",
            // customer
            customer.getCompanyName(),
            customer.getBillingAddress() != null ? formatAddress(customer.getBillingAddress()) : "N/A",
            invoice.getCustomerGSTIN() != null && !invoice.getCustomerGSTIN().isEmpty()
                ? String.format("<p style='margin:4px 0 0;font-size:13px;'><strong>GSTIN:</strong> %s</p>", invoice.getCustomerGSTIN()) : "",
            // items
            itemsTable.toString(),
            // totals
            invoice.getSubTotal() != null ? invoice.getSubTotal() : 0.0,
            invoice.getGstAmount() != null ? invoice.getGstAmount() : 0.0,
            invoice.getTdsAmount() != null ? invoice.getTdsAmount() : 0.0,
            invoice.getAdvanceReceived() != null ? invoice.getAdvanceReceived() : 0.0,
            invoice.getNetPayable() != null ? invoice.getNetPayable() : 0.0,
            // sections
            additionalDetailsSection, paymentLinkSection,
            // bank details
            invoice.getAccountName() != null ? invoice.getAccountName() : "N/A",
            invoice.getBankName() != null ? invoice.getBankName() : "N/A",
            invoice.getAccountNo() != null ? invoice.getAccountNo() : "N/A",
            invoice.getIfscCode() != null ? invoice.getIfscCode() : "N/A",
            // final sections
            upiSection, notesSection, signatureSection,
            // footer
            supplier.getOrganizationName(),
            supplier.getEmail() != null ? supplier.getEmail() : ""
        );
    }

    private String buildReminderEmailBody(Invoice invoice, Users supplier, Company customer,
                                           String reminderType) {
        String headerColor  = "overdue".equals(reminderType) ? "#f44336" : "#ff9800";
        String severityLabel = "overdue".equals(reminderType) ? "PAYMENT OVERDUE" : "PAYMENT DUE";
        String icon         = "overdue".equals(reminderType) ? "Warning" : "Payment";

        return String.format(
            "<html><body style='font-family:Arial,sans-serif;line-height:1.6;color:#333;'>" +
            "<div style='max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;'>" +

            "<div style='background-color:%s;color:white;padding:20px;border-radius:8px;margin-bottom:20px;'>" +
            "<h2 style='margin:0;'>%s - %s</h2>" +
            "<p style='margin:5px 0 0;font-size:14px;'>Invoice %s</p>" +
            "<p style='margin:5px 0 0;font-size:13px;'>Invoice PDF attached</p>" +
            "</div>" +

            "<p style='font-size:16px;'>Dear %s,</p>" +
            "<p>%s</p>" +

            "<div style='background:#f9f9f9;padding:20px;border-radius:8px;margin:20px 0;'>" +
            "<h3 style='color:#555;margin-top:0;'>Invoice Details</h3>" +
            "<table style='width:100%%;border-collapse:collapse;'>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Invoice Number:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Invoice Date:</td><td>%s</td></tr>" +
            "<tr><td style='padding:8px 0;font-weight:bold;'>Payment Terms:</td><td>%s</td></tr>" +
            "</table>" +
            "</div>" +

            "<div style='background:#ffebee;padding:20px;border-radius:8px;margin:20px 0;text-align:center;'>" +
            "<p style='margin:0;font-size:14px;color:#666;'>Outstanding Amount</p>" +
            "<h2 style='margin:10px 0;color:%s;font-size:32px;'>Rs. %.2f</h2>" +
            "</div>" +

            "<p style='color:#d32f2f;font-weight:bold;'>Action Required: Please process the payment at your earliest convenience.</p>" +

            "<div style='margin-top:30px;padding-top:20px;border-top:1px solid #ddd;'>" +
            "<p style='color:#666;font-size:14px;'>Thank you for your prompt attention.</p>" +
            "<p style='margin:0;'><strong>Best Regards,</strong><br>%s<br>%s</p>" +
            "</div>" +
            "</div></body></html>",

            headerColor, icon, severityLabel, invoice.getInvoiceNumber(),
            customer.getCompanyName(),
            "overdue".equals(reminderType)
                ? "This is a reminder that the following invoice is now <strong>overdue</strong>:"
                : "This is a friendly reminder that the following invoice is due for payment:",
            invoice.getInvoiceNumber(),
            invoice.getInvoiceDate(),
            invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "N/A",
            headerColor,
            invoice.getNetPayable(),
            supplier.getOrganizationName(),
            supplier.getEmail()
        );
    }
}