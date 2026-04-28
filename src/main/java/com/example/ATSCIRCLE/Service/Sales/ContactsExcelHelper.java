package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Contact;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

public class ContactsExcelHelper {

    // Generate Excel template with selected columns
    public static ByteArrayInputStream createExcelTemplate(List<String> selectedColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Contacts Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < selectedColumns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(selectedColumns.get(i));

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < selectedColumns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Parse Excel file into Contact objects (Dynamic Mapping)
    public static List<Contact> parseExcel(InputStream inputStream) throws IOException {
        List<Contact> contacts = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            Map<String, Integer> columnMap = new HashMap<>();

            // HEADER ROW
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    String columnName = getCellValueAsString(cell);
                    if (columnName != null && !columnName.trim().isEmpty()) {
                        String normalized = normalizeColumnName(columnName);
                        columnMap.put(normalized, cell.getColumnIndex());
                        columnMap.put(columnName.trim(), cell.getColumnIndex());
                    }
                }
            }

            System.out.println("📋 Detected columns: " + columnMap.keySet());

            // DATA ROWS
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (isRowEmpty(row)) continue;

                Contact contact = new Contact();

                try {
                    contact.setFirstName(getCellValue(row, columnMap, "firstName", "firstname", "first_name", "First Name"));
                    contact.setLastName(getCellValue(row, columnMap, "lastName", "lastname", "last_name", "Last Name"));
                    contact.setEmail(getCellValue(row, columnMap, "email", "Email", "email_address", "Email Address"));
                    contact.setPhone(getCellValue(row, columnMap, "phone", "phoneNumber", "phonenumber", "phone_number", "Phone Number", "Phone"));

                    contact.setCompanyName(getCellValue(row, columnMap, "companyName", "companyname", "company_name", "Company Name", "company"));
                    contact.setJobTitle(getCellValue(row, columnMap, "jobTitle", "jobtitle", "job_title", "Job Title"));

                    contact.setLinkedInUrl(getCellValue(row, columnMap, "linkedInUrl", "linkedinurl", "linkedin", "LinkedIn URL"));
                    contact.setNotes(getCellValue(row, columnMap, "notes", "Notes", "note"));

                    // BOOLEAN FIELDS
                    String doNotCallStr = getCellValue(row, columnMap, "doNotCall", "donotcall", "Do Not Call");
                    contact.setDoNotCall("true".equalsIgnoreCase(doNotCallStr) || "yes".equalsIgnoreCase(doNotCallStr));

                    String doNotEmailStr = getCellValue(row, columnMap, "doNotEmail", "donotemail", "Do Not Email");
                    contact.setDoNotEmail("true".equalsIgnoreCase(doNotEmailStr) || "yes".equalsIgnoreCase(doNotEmailStr));

                    // Timestamps
                    contact.setCreatedAt(LocalDateTime.now());
                    contact.setUpdatedAt(LocalDateTime.now());
                    contact.setLastActivity(LocalDateTime.now());

                    contacts.add(contact);

                } catch (Exception e) {
                    System.err.println("❌ Error parsing row " + row.getRowNum() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("✅ Successfully parsed " + contacts.size() + " contacts");
        return contacts;
    }

    // Dynamic matching of possible column names
    private static String getCellValue(Row row, Map<String, Integer> columnMap, String... possibleNames) {
        for (String name : possibleNames) {
            Integer colIndex = columnMap.get(name);
            if (colIndex != null) return getCellValueAsString(row.getCell(colIndex));

            String normalized = normalizeColumnName(name);
            colIndex = columnMap.get(normalized);
            if (colIndex != null) return getCellValueAsString(row.getCell(colIndex));
        }
        return null;
    }

    private static String normalizeColumnName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                    return cell.getDateCellValue().toString();
                double num = cell.getNumericCellValue();
                return (num == (long) num) ? String.valueOf((long) num) : String.valueOf(num);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); } 
                catch (Exception ex) { return String.valueOf(cell.getNumericCellValue()); }
            case BLANK: return null;
            default: return null;
        }
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            String val = getCellValueAsString(cell);
            if (val != null && !val.trim().isEmpty()) return false;
        }
        return true;
    }
}
