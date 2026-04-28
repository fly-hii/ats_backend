package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Address;
import com.example.ATSCIRCLE.Models.Sales.Client;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientExcelHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Create empty Excel template with selected headers
     */
    public static ByteArrayInputStream createExcelTemplate(List<String> selectedColumns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ClientTemplate");

            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

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

    /**
     * Parse Excel into Client list (Headers taken automatically from first row)
     */
    public static List<Client> parseExcel(InputStream is) throws IOException {
        List<Client> clients = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) return clients;

            // First row as header
            Row headerRow = rows.next();
            List<String> selectedColumns = new ArrayList<>();
            for (Cell cell : headerRow) {
                selectedColumns.add(cell.getStringCellValue().trim());
            }

            // Apache POI formatter for correct string conversion
            DataFormatter formatter = new DataFormatter();

            // Read data rows
            while (rows.hasNext()) {
                Row row = rows.next();
                
                // Skip empty rows
                if (isRowEmpty(row)) continue;
                
                Client client = new Client();

                for (int i = 0; i < selectedColumns.size(); i++) {
                    String col = selectedColumns.get(i);
                    Cell cell = row.getCell(i);

                    if (cell == null) continue;

                    // Get as string (avoids scientific notation)
                    String value = formatter.formatCellValue(cell).trim();
                    
                    if (value.isEmpty()) continue;

                    try {
                        setFieldValue(client, col, value);
                    } catch (NoSuchFieldException e) {
                        System.out.println("⚠️ Unknown field: " + col);
                    } catch (Exception e) {
                        System.out.println("⚠️ Error setting field: " + col + " -> " + e.getMessage());
                    }
                }
                
                clients.add(client);
            }
        }
        return clients;
    }

    /**
     * Set field value on Client object based on field name and string value
     */
    private static void setFieldValue(Client client, String fieldName, String value) 
            throws NoSuchFieldException, IllegalAccessException {
        
        // Handle nested Address fields
        if (fieldName.startsWith("billingAddress.")) {
            handleAddressField(client.getBillingAddress(), client::setBillingAddress, 
                             fieldName.substring("billingAddress.".length()), value);
            return;
        }
        
        if (fieldName.startsWith("shippingAddress.")) {
            handleAddressField(client.getShippingAddress(), client::setShippingAddress,
                             fieldName.substring("shippingAddress.".length()), value);
            return;
        }

        Field field = Client.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        Object convertedValue = convertValue(field.getType(), value);
        field.set(client, convertedValue);
    }

    /**
     * Handle nested Address field setting
     */
    private static void handleAddressField(Address address, 
                                          java.util.function.Consumer<Address> setter,
                                          String fieldName, 
                                          String value) 
            throws NoSuchFieldException, IllegalAccessException {
        
        if (address == null) {
            address = new Address();
            setter.accept(address);
        }

        Field addressField = Address.class.getDeclaredField(fieldName);
        addressField.setAccessible(true);
        
        Object convertedValue = convertValue(addressField.getType(), value);
        addressField.set(address, convertedValue);
    }

    /**
     * Convert string value to appropriate type
     */
    private static Object convertValue(Class<?> fieldType, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        try {
            if (fieldType.equals(String.class)) {
                return value;
            } else if (fieldType.isEnum()) {
                return Enum.valueOf((Class<Enum>) fieldType, value.toUpperCase().replace(" ", "_"));
            } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
                return Boolean.parseBoolean(value) || value.equalsIgnoreCase("yes") || value.equals("1");
            } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
            } else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
                return Long.parseLong(value.replaceAll("[^0-9-]", ""));
            } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
            } else if (fieldType.equals(LocalDateTime.class)) {
                return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } else if (fieldType.equals(List.class)) {
                // Handle List<String> fields (like supportingDocumentUrls)
                return Arrays.asList(value.split(","));
            } else {
                // Default: return as string
                return value;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Conversion error for value '" + value + "' to type " + fieldType.getSimpleName());
            return null;
        }
    }

    /**
     * Check if a row is empty
     */
    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Export clients to Excel
     */
    public static ByteArrayInputStream exportClientsToExcel(List<Client> clients, List<String> columns) 
            throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            DataFormatter formatter = new DataFormatter();
            int rowNum = 1;

            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);
                
                for (int i = 0; i < columns.size(); i++) {
                    String columnName = columns.get(i);
                    Cell cell = row.createCell(i);
                    
                    try {
                        Object value = getFieldValue(client, columnName);
                        setCellValue(cell, value);
                    } catch (Exception e) {
                        cell.setCellValue("");
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * Get field value from Client object
     */
    private static Object getFieldValue(Client client, String fieldName) 
            throws NoSuchFieldException, IllegalAccessException {
        
        // Handle nested Address fields
        if (fieldName.startsWith("billingAddress.")) {
            if (client.getBillingAddress() == null) return null;
            Field field = Address.class.getDeclaredField(fieldName.substring("billingAddress.".length()));
            field.setAccessible(true);
            return field.get(client.getBillingAddress());
        }
        
        if (fieldName.startsWith("shippingAddress.")) {
            if (client.getShippingAddress() == null) return null;
            Field field = Address.class.getDeclaredField(fieldName.substring("shippingAddress.".length()));
            field.setAccessible(true);
            return field.get(client.getShippingAddress());
        }

        Field field = Client.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(client);
    }

    /**
     * Set cell value based on object type
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATE_TIME_FORMATTER));
        } else if (value instanceof List) {
            cell.setCellValue(String.join(", ", (List<String>) value));
        } else if (value instanceof Enum) {
            cell.setCellValue(((Enum<?>) value).name());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}