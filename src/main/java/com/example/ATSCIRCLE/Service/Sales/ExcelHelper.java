
package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Address;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;

public class ExcelHelper {

    // ✅ Create empty Excel with selected headers
    public static ByteArrayInputStream createExcelTemplate(List<String> selectedColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CompanyTemplate");

            // Header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < selectedColumns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(selectedColumns.get(i));
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ✅ Parse Excel into Company list with improved error handling
    public static List<Company> parseExcel(InputStream is) throws IOException {
        List<Company> companies = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                throw new IOException("Excel file is empty");
            }

            // First row as header
            Row headerRow = rows.next();
            List<String> selectedColumns = new ArrayList<>();
            for (Cell cell : headerRow) {
                String columnName = getCellValueAsString(cell).trim();
                if (!columnName.isEmpty()) {
                    selectedColumns.add(columnName);
                }
            }

            if (selectedColumns.isEmpty()) {
                throw new IOException("No valid column headers found in Excel");
            }

            // Apache POI formatter for correct string conversion
            DataFormatter formatter = new DataFormatter();

            int rowNum = 1; // Track row number for error reporting

            // Read data rows
            while (rows.hasNext()) {
                Row row = rows.next();
                rowNum++;
                
                // Skip empty rows
                if (isRowEmpty(row)) {
                    continue;
                }

                Company company = new Company();
                boolean hasData = false;

                for (int i = 0; i < selectedColumns.size(); i++) {
                    String col = selectedColumns.get(i);
                    Cell cell = row.getCell(i);

                    if (cell == null || getCellType(cell) == CellType.BLANK) {
                        continue;
                    }

                    // Get value as string (avoids scientific notation)
                    String value = formatter.formatCellValue(cell).trim();
                    
                    if (value.isEmpty()) {
                        continue;
                    }

                    hasData = true;

                    try {
                        setFieldValue(company, col, value, rowNum);
                    } catch (Exception e) {
                        System.err.println("⚠️ Row " + rowNum + ", Column '" + col + 
                            "': " + e.getMessage());
                    }
                }

                if (hasData) {
                    // Set default timestamps if not provided
                    if (company.getCreatedAt() == null) {
                        company.setCreatedAt(LocalDateTime.now());
                    }
                    if (company.getUpdatedAt() == null) {
                        company.setUpdatedAt(LocalDateTime.now());
                    }
                    if (company.getLastActivity() == null) {
                        company.setLastActivity(LocalDateTime.now());
                    }
                    
                    companies.add(company);
                }
            }
        }
        
        if (companies.isEmpty()) {
            throw new IOException("No valid company data found in Excel file");
        }
        
        return companies;
    }

    // ✅ Helper method to set field value with proper type conversion
    private static void setFieldValue(Company company, String fieldName, String value, int rowNum) 
            throws Exception {
        
        // Handle nested address fields
        if (fieldName.startsWith("billingAddress.") || fieldName.startsWith("shippingAddress.")) {
            setAddressField(company, fieldName, value);
            return;
        }

        try {
            Field field = Company.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            Class<?> fieldType = field.getType();
            Object valueToSet = convertValue(value, fieldType, fieldName);

            field.set(company, valueToSet);

        } catch (NoSuchFieldException e) {
            throw new Exception("Unknown field: " + fieldName);
        } catch (IllegalArgumentException e) {
            throw new Exception("Invalid value '" + value + "' for field " + fieldName);
        }
    }

    // ✅ Handle address fields
    private static void setAddressField(Company company, String fieldName, String value) 
            throws Exception {
        
        String[] parts = fieldName.split("\\.", 2);
        String addressType = parts[0]; // "billingAddress" or "shippingAddress"
        String addressField = parts[1]; // "streetLine1", "city", etc.

        Address address;
        if ("billingAddress".equals(addressType)) {
            address = company.getBillingAddress();
            if (address == null) {
                address = new Address();
                company.setBillingAddress(address);
            }
        } else {
            address = company.getShippingAddress();
            if (address == null) {
                address = new Address();
                company.setShippingAddress(address);
            }
        }

        // Set the address field
        Field field = Address.class.getDeclaredField(addressField);
        field.setAccessible(true);
        field.set(address, value);
    }

    // ✅ Convert string value to appropriate type
    private static Object convertValue(String value, Class<?> targetType, String fieldName) 
            throws Exception {
        
        if (targetType.equals(String.class)) {
            return value;
        }
        
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        
        if (targetType.equals(long.class) || targetType.equals(Long.class)) {
            return Long.parseLong(value);
        }
        
        if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            return Double.parseDouble(value);
        }
        
        if (targetType.equals(LocalDateTime.class)) {
            return LocalDateTime.parse(value);
        }
        
        if (targetType.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                // Try to find matching enum value
                Object[] enumConstants = targetType.getEnumConstants();
                for (Object constant : enumConstants) {
                    if (constant.toString().equalsIgnoreCase(value)) {
                        return constant;
                    }
                }
                throw new Exception("Invalid enum value '" + value + "' for field " + fieldName + 
                    ". Valid values: " + Arrays.toString(enumConstants));
            }
        }
        
        throw new Exception("Unsupported field type: " + targetType.getName());
    }

    // ✅ Helper to get cell value as string
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    // ✅ Helper to get cell type
    private static CellType getCellType(Cell cell) {
        if (cell == null) {
            return CellType.BLANK;
        }
        return cell.getCellType();
    }

    // ✅ Check if row is empty
    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell).trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}