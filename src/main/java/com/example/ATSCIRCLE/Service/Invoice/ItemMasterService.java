package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.Invoice.ItemMaster;
import com.example.ATSCIRCLE.Repository.ItemMasterRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItemMasterService {

    @Autowired
    private ItemMasterRepository itemMasterRepository;

    /**
     * Create a single item with optional image and description
     */
    public ItemMaster createItem(String organizationId, String itemName, String itemDescription,
                                  String hsnSac, String itemImageUrl) {
        // Check if item already exists
        Optional<ItemMaster> existing = itemMasterRepository
            .findByOrganizationIdAndItemNameAndHsnSac(organizationId, itemName, hsnSac);
        
        if (existing.isPresent()) {
            // Update last used date and optionally update image/description
            ItemMaster item = existing.get();
            item.setLastUsedAt(LocalDateTime.now());
            
            // Update description if provided
            if (itemDescription != null && !itemDescription.isEmpty()) {
                item.setItemDescription(itemDescription);
            }
            
            // Update image URL if provided
            if (itemImageUrl != null && !itemImageUrl.isEmpty()) {
                item.setItemImageUrl(itemImageUrl);
            }
            
            return itemMasterRepository.save(item);
        }
        
        // Create new item
        ItemMaster item = new ItemMaster();
        item.setOrganizationId(organizationId);
        item.setItemName(itemName);
        item.setItemDescription(itemDescription);
        item.setHsnSac(hsnSac);
        item.setItemImageUrl(itemImageUrl);
        item.setCreatedAt(LocalDateTime.now());
        item.setLastUsedAt(LocalDateTime.now());
        
        return itemMasterRepository.save(item);
    }

    /**
     * Get all items for an organization
     */
    public List<ItemMaster> getAllItems(String organizationId) {
        return itemMasterRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get item by ID
     */
    public Optional<ItemMaster> getItemById(String id) {
        return itemMasterRepository.findById(id);
    }

    /**
     * Search items by name
     */
    public List<ItemMaster> searchByName(String organizationId, String searchTerm) {
        return itemMasterRepository
            .findByOrganizationIdAndItemNameContainingIgnoreCase(organizationId, searchTerm);
    }

    /**
     * Search items by HSN/SAC
     */
    public List<ItemMaster> searchByHsn(String organizationId, String hsnSac) {
        return itemMasterRepository
            .findByOrganizationIdAndHsnSac(organizationId, hsnSac);
    }

    /**
 * Update an item with partial update support
 * Only updates fields that are provided (not null)
 */
public ItemMaster updateItemPartial(String id, String itemName, String itemDescription,
                                     String hsnSac, String itemImageUrl, Boolean removeImage) {
    ItemMaster item = itemMasterRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
    
    // Only update fields that are provided (not null)
    if (itemName != null && !itemName.trim().isEmpty()) {
        item.setItemName(itemName);
    }
    
    if (itemDescription != null) {
        // Allow empty string to clear description
        item.setItemDescription(itemDescription.isEmpty() ? null : itemDescription);
    }
    
    if (hsnSac != null && !hsnSac.trim().isEmpty()) {
        item.setHsnSac(hsnSac);
    }
    
    // Handle image updates
    if (removeImage != null && removeImage) {
        // Explicitly remove the image
        item.setItemImageUrl(null);
    } else if (itemImageUrl != null && !itemImageUrl.isEmpty()) {
        // Update with new image URL
        item.setItemImageUrl(itemImageUrl);
    }
    // If neither removeImage nor new imageUrl provided, keep existing image
    
    item.setLastUsedAt(LocalDateTime.now());
    
    return itemMasterRepository.save(item);
}

    /**
     * Delete an item
     */
    public void deleteItem(String id) {
        itemMasterRepository.deleteById(id);
    }

    /**
     * Bulk upload from Excel
     */
    public List<ItemMaster> bulkUploadFromExcel(String organizationId, MultipartFile file) 
            throws IOException {
        List<ItemMaster> uploadedItems = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell itemNameCell = row.getCell(0);
                Cell hsnCell = row.getCell(1);
                Cell descriptionCell = row.getCell(2); // Optional description column
                
                if (itemNameCell == null || hsnCell == null) continue;
                
                String itemName = getCellValueAsString(itemNameCell);
                String hsnSac = getCellValueAsString(hsnCell);
                String description = descriptionCell != null ? getCellValueAsString(descriptionCell) : null;
                
                if (itemName.trim().isEmpty() || hsnSac.trim().isEmpty()) continue;
                
                // Create or update item (without image for bulk upload)
                ItemMaster item = createItem(organizationId, itemName.trim(), description, 
                                            hsnSac.trim(), null);
                uploadedItems.add(item);
            }
        }
        
        return uploadedItems;
    }

    /**
     * Export items to Excel
     */
    public byte[] exportToExcel(String organizationId) throws IOException {
        List<ItemMaster> items = itemMasterRepository.findByOrganizationId(organizationId);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Items");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            String[] headers = {"Item Name", "HSN/SAC", "Description", "Image URL", "Created At", "Last Used At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowNum = 1;
            for (ItemMaster item : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getItemName());
                row.createCell(1).setCellValue(item.getHsnSac());
                row.createCell(2).setCellValue(item.getItemDescription() != null ? item.getItemDescription() : "");
                row.createCell(3).setCellValue(item.getItemImageUrl() != null ? item.getItemImageUrl() : "");
                row.createCell(4).setCellValue(item.getCreatedAt().toString());
                row.createCell(5).setCellValue(item.getLastUsedAt().toString());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Generate Excel template for bulk upload
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Items Template");
            
            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Create header cells
            String[] headers = {"Item Name", "HSN/SAC", "Description (Optional)","Image URL"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create example rows
            CellStyle exampleStyle = workbook.createCellStyle();
            Font exampleFont = workbook.createFont();
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleFont.setItalic(true);
            exampleStyle.setFont(exampleFont);
            exampleStyle.setBorderBottom(BorderStyle.THIN);
            exampleStyle.setBorderLeft(BorderStyle.THIN);
            exampleStyle.setBorderRight(BorderStyle.THIN);
            
            // Sample data rows
            String[][] sampleData = {
                {"Recruitment Services", "9989", "Professional staffing and recruitment solutions"},
                {"Executive Search", "998912", "C-level and senior management search services"},
                {"HR Consulting", "998913", "Strategic HR advisory and consulting"},
                {"Temporary Staffing", "998914", "Short-term and contract staffing solutions"},
                {"Payroll Management", "998915", "End-to-end payroll processing services"}
            };
            
            for (int i = 0; i < sampleData.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < sampleData[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(sampleData[i][j]);
                    cell.setCellStyle(exampleStyle);
                }
            }
            
            // Add instructions sheet
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            
            CellStyle instructionHeaderStyle = workbook.createCellStyle();
            Font instructionHeaderFont = workbook.createFont();
            instructionHeaderFont.setBold(true);
            instructionHeaderFont.setFontHeightInPoints((short) 14);
            instructionHeaderStyle.setFont(instructionHeaderFont);
            
            CellStyle instructionTextStyle = workbook.createCellStyle();
            instructionTextStyle.setWrapText(true);
            
            String[] instructions = {
                "How to Use This Template:",
                "",
                "1. Go to 'Items Template' sheet",
                "2. Replace the sample data with your actual items",
                "3. Column A: Enter the Item Name (required)",
                "4. Column B: Enter the HSN/SAC Code (required)",
                "5. Column C: Enter Item Description (optional)",
                "6. Do not delete the header row (Row 1)",
                "7. You can add as many rows as needed",
                "8. Save the file as .xlsx format",
                "9. Upload the file using the bulk upload API",
                "",
                "Note: Item images must be uploaded separately through the UI or API.",
                "",
                "Common HSN/SAC Codes for Reference:",
                "9989 - Manpower recruitment services",
                "998911 - Executive search services",
                "998912 - Temporary staffing services",
                "998913 - Payroll management services",
                "998914 - HR consulting services",
                "998915 - Background verification services"
            };
            
            for (int i = 0; i < instructions.length; i++) {
                Row row = instructionsSheet.createRow(i);
                Cell cell = row.createCell(0);
                cell.setCellValue(instructions[i]);
                if (i == 0) {
                    cell.setCellStyle(instructionHeaderStyle);
                } else {
                    cell.setCellStyle(instructionTextStyle);
                }
            }
            
            // Set column widths
            sheet.setColumnWidth(0, 8000);  // Item Name
            sheet.setColumnWidth(1, 4000);  // HSN/SAC
            sheet.setColumnWidth(2, 12000); // Description
            
            instructionsSheet.setColumnWidth(0, 15000);
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Process items from invoice and update ItemMaster
     * This fetches item details including images from ItemMaster
     */
    public void processInvoiceItems(String organizationId, List<String> itemNames, 
                                   List<String> hsnCodes) {
        for (int i = 0; i < itemNames.size(); i++) {
            if (i < hsnCodes.size()) {
                createItem(organizationId, itemNames.get(i), null, hsnCodes.get(i), null);
            }
        }
    }
    
    /**
     * Get item details by name and HSN (to retrieve image URL for invoice)
     */
    public Optional<ItemMaster> getItemByNameAndHsn(String organizationId, String itemName, String hsnSac) {
        return itemMasterRepository.findByOrganizationIdAndItemNameAndHsnSac(
            organizationId, itemName, hsnSac);
    }
}