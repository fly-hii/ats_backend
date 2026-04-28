package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import com.example.ATSCIRCLE.Models.Invoice.InvoiceItem;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Users;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

    /**
     * Generate PDF invoice with item images
     */
    public byte[] generateInvoicePdf(Invoice invoice, Users supplier, Company customer) throws Exception {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            
            // Header with gradient background
            addHeader(document, invoice);
            
            // Invoice details and parties
            addInvoiceDetailsSection(document, invoice, supplier, customer);
            
            // Line items with images
            addLineItemsTable(document, invoice);
            
            // Tax summary
            addTaxSummary(document, invoice);
            
            // Additional details (E-Way Bill, FSSAI, etc.)
            addAdditionalDetails(document, invoice);
            
            // Bank details and UPI
            addPaymentDetails(document, invoice);
            
            // Notes and signature
            addNotesAndSignature(document, invoice);
            
            // Footer
            addFooter(document);
            
            document.close();
            return out.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            throw new Exception("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, Invoice invoice) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(new BaseColor(15, 118, 110)); // Teal color
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(15);
        
        Paragraph title = new Paragraph("TAX INVOICE", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(title);
        
        Paragraph subtitle = new Paragraph("GST Compliant Invoice • India", 
            new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(5);
        headerCell.addElement(subtitle);
        
        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(new Paragraph(" ")); // Spacing
    }

    private void addInvoiceDetailsSection(Document document, Invoice invoice, 
                                         Users supplier, Company customer) throws DocumentException {
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setSpacingBefore(10);
        mainTable.setSpacingAfter(10);
        
        // Invoice details box
        PdfPCell invoiceDetailsCell = createDetailBox("Invoice Details",
            "Invoice No: " + invoice.getInvoiceNumber() + "\n" +
            "Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) + "\n" +
            "PO/Ref: " + (invoice.getCustomerPO() != null ? invoice.getCustomerPO() : "N/A"));
        mainTable.addCell(invoiceDetailsCell);
        
        // Amount due box
        PdfPCell amountCell = createHighlightBox("Amount Due",
            String.format("₹ %.2f", invoice.getNetPayable()),
            "Payment Terms: " + (invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "N/A"));
        mainTable.addCell(amountCell);
        
        document.add(mainTable);
        
        // Parties section
        PdfPTable partiesTable = new PdfPTable(2);
        partiesTable.setWidthPercentage(100);
        partiesTable.setSpacingBefore(10);
        partiesTable.setSpacingAfter(10);
        
        // Supplier details
        String supplierInfo = (invoice.getLegalBusinessName() != null ? invoice.getLegalBusinessName() : supplier.getOrganizationName()) + "\n";
        if (invoice.getStreetLine1() != null) {
            supplierInfo += invoice.getStreetLine1() + "\n";
            if (invoice.getCity() != null) supplierInfo += invoice.getCity() + ", ";
            if (invoice.getState() != null) supplierInfo += invoice.getState() + "\n";
        }
        if (invoice.getGstNumber() != null) supplierInfo += "GSTIN: " + invoice.getGstNumber();
        
        PdfPCell supplierCell = createDetailBox("Supplier (From)", supplierInfo);
        partiesTable.addCell(supplierCell);
        
        // Customer details
        String customerInfo = customer.getCompanyName() + "\n";
        if (customer.getBillingAddress() != null) {
            customerInfo += formatAddress(customer.getBillingAddress());
        }
        if (invoice.getCustomerGSTIN() != null) customerInfo += "\nGSTIN: " + invoice.getCustomerGSTIN();
        
        PdfPCell customerCell = createDetailBox("Recipient (Bill To)", customerInfo);
        partiesTable.addCell(customerCell);
        
        document.add(partiesTable);
    }

   private void addLineItemsTable(Document document, Invoice invoice) throws DocumentException, IOException {
    Paragraph itemsHeader = new Paragraph("Invoice Items", HEADER_FONT);
    itemsHeader.setSpacingBefore(10);
    itemsHeader.setSpacingAfter(5);
    document.add(itemsHeader);
    
    PdfPTable table = new PdfPTable(7);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{2, 3, 2, 1.5f, 1.5f, 2, 1.5f});
    
    // Header row
    String[] headers = {"Image", "Description", "HSN/SAC", "Qty", "Rate", "Amount", "GST %"};
    for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
        cell.setBackgroundColor(new BaseColor(243, 244, 246));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    
    // Data rows with images
    for (InvoiceItem item : invoice.getItems()) {
        // Image cell with improved error handling
        PdfPCell imageCell = new PdfPCell();
        imageCell.setPadding(5);
        imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        if (item.getItemImageUrl() != null && !item.getItemImageUrl().trim().isEmpty()) {
            try {
                String imageUrl = item.getItemImageUrl().trim();
                System.out.println("📸 Attempting to load image from: " + imageUrl);
                
                // Validate URL format
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    throw new IllegalArgumentException("Invalid URL protocol");
                }
                
                // Load image with timeout handling
                Image img = Image.getInstance(new URL(imageUrl));
                
                // Scale image to fit cell while maintaining aspect ratio
                img.scaleToFit(50, 50);
                img.setAlignment(Element.ALIGN_CENTER);
                
                imageCell.addElement(img);
                System.out.println("✅ Image loaded successfully");
                
            } catch (java.net.MalformedURLException e) {
                System.err.println("❌ Malformed URL for item image: " + item.getItemImageUrl());
                addPlaceholderImage(imageCell, "Invalid URL");
            } catch (java.io.IOException e) {
                System.err.println("❌ Failed to fetch image from URL: " + item.getItemImageUrl());
                System.err.println("   Error: " + e.getMessage());
                addPlaceholderImage(imageCell, "Not Found");
            } catch (IllegalArgumentException e) {
                System.err.println("❌ Invalid URL protocol: " + item.getItemImageUrl());
                addPlaceholderImage(imageCell, "Invalid");
            } catch (Exception e) {
                System.err.println("❌ Unexpected error loading image: " + e.getMessage());
                e.printStackTrace();
                addPlaceholderImage(imageCell, "Error");
            }
        } else {
            addPlaceholderImage(imageCell, "No Image");
        }
        table.addCell(imageCell);
        
        // Description cell
        String description = item.getItemName() != null ? item.getItemName() : "—";
        if (item.getItemDescription() != null && !item.getItemDescription().isEmpty()) {
            description += "\n" + item.getItemDescription();
        }
        PdfPCell descCell = new PdfPCell(new Phrase(description, NORMAL_FONT));
        descCell.setPadding(8);
        table.addCell(descCell);
        
        // Other cells
        table.addCell(createCell(item.getHsnSac() != null ? item.getHsnSac() : "—", Element.ALIGN_LEFT));
        table.addCell(createCell(item.getQuantity() != null ? String.valueOf(item.getQuantity()) : "0", Element.ALIGN_RIGHT));
        table.addCell(createCell(String.format("₹ %.2f", item.getRate() != null ? item.getRate() : 0.0), Element.ALIGN_RIGHT));
        table.addCell(createCell(String.format("₹ %.2f", item.getAmount() != null ? item.getAmount() : 0.0), Element.ALIGN_RIGHT));
        table.addCell(createCell(String.format("%.1f%%", item.getGstPercent() != null ? item.getGstPercent() : 0.0), Element.ALIGN_RIGHT));
    }
    
    document.add(table);
}

/**
 * Alternative method: Download and cache image if direct URL loading fails
 */
private Image loadImageFromUrl(String imageUrl) throws Exception {
    try {
        // Try direct loading first
        return Image.getInstance(new URL(imageUrl));
    } catch (Exception e) {
        // Fallback: Try with custom connection settings
        System.out.println("⚠️ Direct load failed, trying with custom connection...");
        
        java.net.URL url = new URL(imageUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        
        // Set headers to mimic browser request
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        
        // Read image bytes
        java.io.InputStream inputStream = connection.getInputStream();
        byte[] imageBytes = inputStream.readAllBytes();
        inputStream.close();
        
        return Image.getInstance(imageBytes);
    }
}

/**
 * Add a placeholder when image cannot be loaded
 */
private void addPlaceholderImage(PdfPCell cell, String message) {
    try {
        // Create a small colored rectangle as placeholder
        PdfPTable placeholderTable = new PdfPTable(1);
        placeholderTable.setWidthPercentage(100);
        
        PdfPCell placeholderCell = new PdfPCell();
        placeholderCell.setBackgroundColor(new BaseColor(243, 244, 246));
        placeholderCell.setBorder(Rectangle.BOX);
        placeholderCell.setBorderColor(new BaseColor(209, 213, 219));
        placeholderCell.setFixedHeight(50);
        placeholderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        placeholderCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph placeholder = new Paragraph("🖼️\n" + message, SMALL_FONT);
        placeholder.setAlignment(Element.ALIGN_CENTER);
        placeholderCell.addElement(placeholder);
        
        placeholderTable.addCell(placeholderCell);
        cell.addElement(placeholderTable);
        
    } catch (Exception e) {
        // Fallback to simple text if even placeholder fails
        cell.addElement(new Phrase(message, SMALL_FONT));
    }
}
    private void addTaxSummary(Document document, Invoice invoice) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(10);
        
        summaryTable.addCell(createSummaryCell("Subtotal:", false));
        summaryTable.addCell(createSummaryCell(String.format("₹ %.2f", invoice.getSubTotal()), true));
        
        summaryTable.addCell(createSummaryCell("GST:", false));
        summaryTable.addCell(createSummaryCell(String.format("₹ %.2f", invoice.getGstAmount()), true));
        
        if (invoice.getTdsAmount() != null && invoice.getTdsAmount() > 0) {
            summaryTable.addCell(createSummaryCell("TDS (-):", false));
            summaryTable.addCell(createSummaryCell(String.format("₹ %.2f", invoice.getTdsAmount()), true));
        }
        
        if (invoice.getAdvanceReceived() != null && invoice.getAdvanceReceived() > 0) {
            summaryTable.addCell(createSummaryCell("Advance (-):", false));
            summaryTable.addCell(createSummaryCell(String.format("₹ %.2f", invoice.getAdvanceReceived()), true));
        }
        
        PdfPCell totalLabelCell = createSummaryCell("Net Payable:", false);
        totalLabelCell.setBorderWidthTop(2);
        totalLabelCell.setPaddingTop(8);
        summaryTable.addCell(totalLabelCell);
        
        PdfPCell totalValueCell = createSummaryCell(String.format("₹ %.2f", invoice.getNetPayable()), true);
        totalValueCell.setBorderWidthTop(2);
        totalValueCell.setPaddingTop(8);
        summaryTable.addCell(totalValueCell);
        
        document.add(summaryTable);
    }

    private void addAdditionalDetails(Document document, Invoice invoice) throws DocumentException {
        if (invoice.getEwayBillNumber() != null || invoice.getArnNumber() != null || 
            invoice.getFssaiNumber() != null || invoice.getPackingSlipNumber() != null) {
            
            Paragraph header = new Paragraph("Additional Details", HEADER_FONT);
            header.setSpacingBefore(10);
            header.setSpacingAfter(5);
            document.add(header);
            
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            
            if (invoice.getEwayBillNumber() != null) {
                detailsTable.addCell(createDetailCell("E-Way Bill Number:", invoice.getEwayBillNumber()));
            }
            if (invoice.getArnNumber() != null) {
                detailsTable.addCell(createDetailCell("ARN Number:", invoice.getArnNumber()));
            }
            if (invoice.getFssaiNumber() != null) {
                detailsTable.addCell(createDetailCell("FSSAI License:", invoice.getFssaiNumber()));
            }
            if (invoice.getPackingSlipNumber() != null) {
                detailsTable.addCell(createDetailCell("Packing Slip:", invoice.getPackingSlipNumber()));
            }
            
            document.add(detailsTable);
        }
    }

    private void addPaymentDetails(Document document, Invoice invoice) throws DocumentException {
        // Bank details
        Paragraph bankHeader = new Paragraph("Bank Transfer Details", HEADER_FONT);
        bankHeader.setSpacingBefore(10);
        bankHeader.setSpacingAfter(5);
        document.add(bankHeader);
        
        PdfPTable bankTable = new PdfPTable(2);
        bankTable.setWidthPercentage(100);
        
        bankTable.addCell(createDetailCell("Account Name:", invoice.getAccountName()));
        bankTable.addCell(createDetailCell("Bank:", invoice.getBankName()));
        bankTable.addCell(createDetailCell("Account Number:", invoice.getAccountNo()));
        bankTable.addCell(createDetailCell("IFSC Code:", invoice.getIfscCode()));
        
        document.add(bankTable);
        
        // UPI details
        if (invoice.getUpiId() != null) {
            Paragraph upiHeader = new Paragraph("UPI Payment Details", HEADER_FONT);
            upiHeader.setSpacingBefore(10);
            upiHeader.setSpacingAfter(5);
            document.add(upiHeader);
            
            Paragraph upiInfo = new Paragraph("UPI ID: " + invoice.getUpiId(), NORMAL_FONT);
            document.add(upiInfo);
            
            if (invoice.getUpiNote() != null) {
                Paragraph upiNote = new Paragraph(invoice.getUpiNote(), SMALL_FONT);
                upiNote.setSpacingBefore(5);
                document.add(upiNote);
            }
        }
    }

    private void addNotesAndSignature(Document document, Invoice invoice) throws DocumentException {
        if (invoice.getAdditionalNotes() != null) {
            Paragraph notesHeader = new Paragraph("Additional Notes", HEADER_FONT);
            notesHeader.setSpacingBefore(10);
            notesHeader.setSpacingAfter(5);
            document.add(notesHeader);
            
            Paragraph notes = new Paragraph(invoice.getAdditionalNotes(), NORMAL_FONT);
            document.add(notes);
        }
        
        // Signature
        if (invoice.getAuthorisedSignatoryName() != null) {
            Paragraph signature = new Paragraph();
            signature.setSpacingBefore(20);
            signature.setAlignment(Element.ALIGN_RIGHT);
            signature.add(new Chunk("For " + invoice.getLegalBusinessName() + "\n\n\n", NORMAL_FONT));
            signature.add(new Chunk("_________________________\n", NORMAL_FONT));
            signature.add(new Chunk("Authorised Signatory\n", NORMAL_FONT));
            signature.add(new Chunk(invoice.getAuthorisedSignatoryName(), BOLD_FONT));
            if (invoice.getDesignation() != null) {
                signature.add(new Chunk(", " + invoice.getDesignation(), NORMAL_FONT));
            }
            document.add(signature);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
            "This is a computer-generated invoice and does not require a physical signature.\n" +
            "Generated by InvoiceHub • GST Compliant Invoicing System",
            SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }

    // Helper methods
    private PdfPCell createDetailBox(String title, String content) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(249, 250, 251));
        cell.setPadding(15);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(229, 231, 235));
        
        Paragraph titlePara = new Paragraph(title, HEADER_FONT);
        titlePara.setSpacingAfter(8);
        cell.addElement(titlePara);
        
        Paragraph contentPara = new Paragraph(content, NORMAL_FONT);
        cell.addElement(contentPara);
        
        return cell;
    }

    private PdfPCell createHighlightBox(String title, String amount, String subtitle) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(224, 242, 241));
        cell.setPadding(15);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(15, 118, 110));
        
        Paragraph titlePara = new Paragraph(title, NORMAL_FONT);
        cell.addElement(titlePara);
        
        Paragraph amountPara = new Paragraph(amount, 
            new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, new BaseColor(15, 118, 110)));
        amountPara.setSpacingBefore(5);
        cell.addElement(amountPara);
        
        Paragraph subtitlePara = new Paragraph(subtitle, SMALL_FONT);
        subtitlePara.setSpacingBefore(5);
        cell.addElement(subtitlePara);
        
        return cell;
    }

    private PdfPCell createCell(String content, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, NORMAL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell createSummaryCell(String text, boolean isValue) {
        PdfPCell cell = new PdfPCell(new Phrase(text, isValue ? BOLD_FONT : NORMAL_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setHorizontalAlignment(isValue ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell createDetailCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(229, 231, 235));
        
        Paragraph para = new Paragraph();
        para.add(new Chunk(label + " ", BOLD_FONT));
        para.add(new Chunk(value != null ? value : "N/A", NORMAL_FONT));
        cell.addElement(para);
        
        return cell;
    }

    private String formatAddress(com.example.ATSCIRCLE.Models.Sales.Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getStreetLine1() != null) sb.append(address.getStreetLine1()).append("\n");
        if (address.getStreetLine2() != null) sb.append(address.getStreetLine2()).append("\n");
        if (address.getCity() != null) sb.append(address.getCity());
        if (address.getState() != null) sb.append(", ").append(address.getState());
        if (address.getZipcode() != null) sb.append(" - ").append(address.getZipcode());
        return sb.toString();
    }
}