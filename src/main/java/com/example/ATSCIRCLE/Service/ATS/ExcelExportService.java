
package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportApplicationsToExcel(List<CandidateApplication> applications, 
                                           List<String> selectedFields) throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Candidate Applications");
            
            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            createHeaderRow(headerRow, selectedFields, headerStyle);
            
            // Create data rows
            int rowNum = 1;
            for (CandidateApplication application : applications) {
                Row row = sheet.createRow(rowNum++);
                createDataRow(row, application, selectedFields, dateStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < selectedFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createHeaderRow(Row row, List<String> fields, CellStyle style) {
        int cellNum = 0;
        for (String fieldName : fields) {
            Cell cell = row.createCell(cellNum++);
            cell.setCellValue(formatFieldName(fieldName));
            cell.setCellStyle(style);
        }
    }

    private void createDataRow(Row row, CandidateApplication application, 
                              List<String> fields, CellStyle dateStyle) {
        int cellNum = 0;
        for (String fieldName : fields) {
            Cell cell = row.createCell(cellNum++);
            Object value = getFieldValue(application, fieldName);
            setCellValue(cell, value, dateStyle);
        }
    }

    private Object getFieldValue(CandidateApplication application, String fieldName) {
        try {
            Field field = CandidateApplication.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(application);
        } catch (Exception e) {
            return null;
        }
    }

    private void setCellValue(Cell cell, Object value, CellStyle dateStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDate) {
            cell.setCellValue(((LocalDate) value).format(DATE_FORMATTER));
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATETIME_FORMATTER));
            cell.setCellStyle(dateStyle);
        } else if (value instanceof List) {
            cell.setCellValue(listToString((List<?>) value));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private String listToString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof CandidateApplication.TechnicalSkill) {
                CandidateApplication.TechnicalSkill skill = (CandidateApplication.TechnicalSkill) item;
                sb.append(skill.getSkillName()).append(" (").append(skill.getRating()).append(")");
            } else if (item instanceof CandidateApplication.SoftSkill) {
                CandidateApplication.SoftSkill skill = (CandidateApplication.SoftSkill) item;
                sb.append(skill.getSkillName()).append(" (").append(skill.getRating()).append(")");
            } else if (item instanceof CandidateApplication.Language) {
                CandidateApplication.Language lang = (CandidateApplication.Language) item;
                sb.append(lang.getLanguageName());
                List<String> abilities = new java.util.ArrayList<>();
                if (Boolean.TRUE.equals(lang.getCanRead())) abilities.add("Read");
                if (Boolean.TRUE.equals(lang.getCanWrite())) abilities.add("Write");
                if (Boolean.TRUE.equals(lang.getCanSpeak())) abilities.add("Speak");
                if (!abilities.isEmpty()) {
                    sb.append(" (").append(String.join(", ", abilities)).append(")");
                }
            } else {
                sb.append(item.toString());
            }
            
            if (i < list.size() - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    private String formatFieldName(String fieldName) {
        // Convert camelCase to Title Case
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(fieldName.charAt(0)));
        
        for (int i = 1; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        
        return result.toString();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper()
                .createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }
}