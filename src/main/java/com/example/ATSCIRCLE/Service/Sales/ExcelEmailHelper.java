package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.DTO.EmailRecipient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class ExcelEmailHelper {

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Parse Excel file and extract email recipients.
     *
     * Expected columns (case-insensitive):
     *   email            -> required
     *   fullname / full_name           -> optional
     *   firstname / first_name         -> optional (combined with lastname)
     *   lastname / last_name           -> optional
     *   companyname / company_name / company -> optional
     */
    public static List<EmailRecipient> parseExcelForEmails(InputStream inputStream) throws IOException {
        List<EmailRecipient> recipients = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IOException("No sheet found in Excel file");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel file is empty or has no header row");
            }

            Map<String, Integer> columnIndices = getColumnIndices(headerRow);

            if (!columnIndices.containsKey("email")) {
                throw new IOException("Excel file must contain an 'email' column");
            }

            // Parse data rows (skip header row 0)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                try {
                    EmailRecipient recipient = parseRow(row, columnIndices);
                    if (recipient != null && isValidEmail(recipient.getEmail())) {
                        recipients.add(recipient);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing row " + rowIndex + ": " + e.getMessage());
                }
            }

            if (recipients.isEmpty()) {
                throw new IOException("No valid email recipients found in Excel file");
            }

            return recipients;

        } catch (IOException e) {
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Map header column names to their index positions
     */
    private static Map<String, Integer> getColumnIndices(Row headerRow) {
        Map<String, Integer> columnIndices = new HashMap<>();

        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell == null) continue;

            String columnName = cell.getStringCellValue().trim().toLowerCase();

            switch (columnName) {
                case "email":
                    columnIndices.put("email", cellIndex);
                    break;
                case "fullname":
                case "full_name":
                    columnIndices.put("fullname", cellIndex);
                    break;
                case "firstname":
                case "first_name":
                    columnIndices.put("firstname", cellIndex);
                    break;
                case "lastname":
                case "last_name":
                    columnIndices.put("lastname", cellIndex);
                    break;
                case "companyname":
                case "company_name":
                case "company":
                    columnIndices.put("company", cellIndex);
                    break;
                default:
                    break;
            }
        }

        return columnIndices;
    }

    /**
     * Parse a single data row into an EmailRecipient
     */
    private static EmailRecipient parseRow(Row row, Map<String, Integer> columnIndices) {
        String email = getCellValue(row, columnIndices.get("email"));
        if (email == null || email.isEmpty()) {
            return null;
        }

        // Resolve full name: prefer fullname column, else combine first + last
        String fullName;
        if (columnIndices.containsKey("fullname")) {
            fullName = getCellValue(row, columnIndices.get("fullname"));
        } else {
            String firstName = columnIndices.containsKey("firstname")
                    ? getCellValue(row, columnIndices.get("firstname")) : "";
            String lastName = columnIndices.containsKey("lastname")
                    ? getCellValue(row, columnIndices.get("lastname")) : "";

            if (firstName == null) firstName = "";
            if (lastName == null) lastName = "";

            fullName = (firstName + " " + lastName).trim();
        }

        String companyName = columnIndices.containsKey("company")
                ? getCellValue(row, columnIndices.get("company"))
                : null;

        return new EmailRecipient(email.trim(), fullName, companyName);
    }

    /**
     * Read a cell value as a String regardless of cell type
     */
    private static String getCellValue(Row row, Integer cellIndex) {
        if (cellIndex == null || cellIndex < 0) return null;

        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Avoid ".0" for plain integers
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    /**
     * Returns true if every cell in the row is null or empty
     */
    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            String val = getCellValue(row, cellIndex);
            if (val != null && !val.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Basic email format validation
     */
    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}