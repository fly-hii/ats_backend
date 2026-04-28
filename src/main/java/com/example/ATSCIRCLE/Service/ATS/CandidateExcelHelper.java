package com.example.ATSCIRCLE.Service.ATS;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CandidateExcelHelper {
    
    // Field display name mapping (kept as-is, even though headers now use key : key)
    private static final Map<String, String> FIELD_DISPLAY_NAMES = new HashMap<>();

    static {
        // Personal Information
        FIELD_DISPLAY_NAMES.put("firstName", "firstName");
        FIELD_DISPLAY_NAMES.put("middleName", "middleName");
        FIELD_DISPLAY_NAMES.put("lastName", "lastName");
        FIELD_DISPLAY_NAMES.put("fullName", "fullName");
        FIELD_DISPLAY_NAMES.put("dateOfBirth", "dateOfBirth");
        FIELD_DISPLAY_NAMES.put("age", "age");
        FIELD_DISPLAY_NAMES.put("gender", "gender");
        FIELD_DISPLAY_NAMES.put("nationality", "nationality");
        FIELD_DISPLAY_NAMES.put("maritalStatus", "maritalStatus");

        // Contact Information
        FIELD_DISPLAY_NAMES.put("primaryEmail", "primaryEmail");
        FIELD_DISPLAY_NAMES.put("primaryPhone", "primaryPhone");
        FIELD_DISPLAY_NAMES.put("whatsappNumber", "whatsappNumber");
        FIELD_DISPLAY_NAMES.put("linkedInProfile", "linkedInProfile");
        FIELD_DISPLAY_NAMES.put("videoIntroductionURL", "videoIntroductionURL");
        FIELD_DISPLAY_NAMES.put("githubProfile", "githubProfile");
        FIELD_DISPLAY_NAMES.put("portfolioWebsite", "portfolioWebsite");
        FIELD_DISPLAY_NAMES.put("sourceOfApplication", "sourceOfApplication");

        // Address
        FIELD_DISPLAY_NAMES.put("addressLine", "addressLine");
        FIELD_DISPLAY_NAMES.put("city", "city");
        FIELD_DISPLAY_NAMES.put("state", "state");
        FIELD_DISPLAY_NAMES.put("province", "province");
        FIELD_DISPLAY_NAMES.put("region", "region");
        FIELD_DISPLAY_NAMES.put("postalCode", "postalCode");
        FIELD_DISPLAY_NAMES.put("country", "country");

        // Skills & Experience
        FIELD_DISPLAY_NAMES.put("technicalSkills", "technicalSkills (JSON format)");
        FIELD_DISPLAY_NAMES.put("softSkills", "softSkills (JSON format)");
        FIELD_DISPLAY_NAMES.put("keyAchievements", "keyAchievements (comma separated)");
        FIELD_DISPLAY_NAMES.put("awardsRecognition", "awardsRecognition (comma separated)");
        FIELD_DISPLAY_NAMES.put("teamManagementExperienceYears", "teamManagementExperienceYears");
        FIELD_DISPLAY_NAMES.put("languages", "languages (JSON format)");
        FIELD_DISPLAY_NAMES.put("totalYearsOfExperience", "totalYearsOfExperience");
        FIELD_DISPLAY_NAMES.put("totalExperienceInTargetRole", "totalExperienceInTargetRole");

        // Employment
        FIELD_DISPLAY_NAMES.put("currentJobTitle", "currentJobTitle");
        FIELD_DISPLAY_NAMES.put("currentCompany", "currentCompany");
        FIELD_DISPLAY_NAMES.put("currentJobStartDate", "currentJobStartDate (YYYY-MM-DD)");
        FIELD_DISPLAY_NAMES.put("previousJobTitle", "previousJobTitle");
        FIELD_DISPLAY_NAMES.put("previousExperience", "previousExperience");
        FIELD_DISPLAY_NAMES.put("previousJobStartDate", "previousJobStartDate (YYYY-MM-DD)");
        FIELD_DISPLAY_NAMES.put("reasonForLeaving", "reasonForLeaving");

        // Compensation
        FIELD_DISPLAY_NAMES.put("currentCTC", "currentCTC");
        FIELD_DISPLAY_NAMES.put("expectedCTC", "expectedCTC");
        FIELD_DISPLAY_NAMES.put("salaryCurrency", "Salary Currency");
        FIELD_DISPLAY_NAMES.put("salaryFrequency", "Salary Frequency");
        FIELD_DISPLAY_NAMES.put("noticePeriod", "Notice Period");
        FIELD_DISPLAY_NAMES.put("careerGapsExplanation", "Career Gaps Explanation");
        FIELD_DISPLAY_NAMES.put("teamSizeManaged", "Team Size Managed");

        // Work Authorization
        FIELD_DISPLAY_NAMES.put("workAuthorizationStatus", "Work Authorization Status");
        FIELD_DISPLAY_NAMES.put("workAuthorizationCountries", "Work Authorization Countries");
        FIELD_DISPLAY_NAMES.put("visaType", "Visa Type");
        FIELD_DISPLAY_NAMES.put("visaExpiryDate", "Visa Expiry Date (YYYY-MM-DD)");
        FIELD_DISPLAY_NAMES.put("visaExtensionPossible", "Visa Extension Possible");

        // Certifications & Documents
        FIELD_DISPLAY_NAMES.put("professionalLicenses", "Professional Licenses");
        FIELD_DISPLAY_NAMES.put("certificationsUpload", "Certifications Upload URL");
        FIELD_DISPLAY_NAMES.put("supportingDocuments", "Supporting Documents URL");

        // References
        FIELD_DISPLAY_NAMES.put("reference1Name", "Reference 1 Name");
        FIELD_DISPLAY_NAMES.put("reference1Title", "Reference 1 Title");
        FIELD_DISPLAY_NAMES.put("reference1Company", "Reference 1 Company");
        FIELD_DISPLAY_NAMES.put("reference1Email", "Reference 1 Email");
        FIELD_DISPLAY_NAMES.put("reference1Phone", "Reference 1 Phone");
        FIELD_DISPLAY_NAMES.put("reference1Relationship", "Reference 1 Relationship");

        // Education
        FIELD_DISPLAY_NAMES.put("highestEducationLevel", "Highest Education Level");
        FIELD_DISPLAY_NAMES.put("fieldOfStudy", "Field of Study");
        FIELD_DISPLAY_NAMES.put("universityName", "University Name");
        FIELD_DISPLAY_NAMES.put("gpa", "GPA/Percentage");
        FIELD_DISPLAY_NAMES.put("additionalDegrees", "Additional Degrees");
        FIELD_DISPLAY_NAMES.put("areaProfessionalCertifications", "Professional Certifications");

        // Consents
        FIELD_DISPLAY_NAMES.put("backgroundCheckConsent", "Background Check Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("drugTestConsent", "Drug Test Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("rightToWorkConfirm", "Right to Work Confirmation (true/false)");
        FIELD_DISPLAY_NAMES.put("dataProcessingConsent", "Data Processing Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("communicationsConsent", "Communications Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("termsAndConditionsConsent", "Terms & Conditions Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("creditCheckConsent", "Credit Check Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("employmentVerificationConsent", "Employment Verification Consent (true/false)");
        FIELD_DISPLAY_NAMES.put("educationVerificationConsent", "Education Verification Consent (true/false)");
    }

    public static ByteArrayInputStream createExcelTemplate(List<String> selectedFields) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Candidate Application Template");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            int colNum = 0;

            for (String fieldName : selectedFields) {
    Cell cell = headerRow.createCell(colNum++);

    // header = only key (camelCase)
    cell.setCellValue(fieldName);

    cell.setCellStyle(headerStyle);
}


            // Set fixed width
            for (int i = 0; i < selectedFields.size(); i++) {
                sheet.setColumnWidth(i, 7000);
            }

            // Instructions Sheet
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            Row instrRow = instructionsSheet.createRow(0);
            instrRow.createCell(0).setCellValue("Candidate Application Template Instructions");

            instructionsSheet.createRow(2).createCell(0).setCellValue("1. Fill details in the main sheet.");
            instructionsSheet.createRow(3).createCell(0).setCellValue("2. Date format: YYYY-MM-DD");
            instructionsSheet.createRow(4).createCell(0).setCellValue("3. Boolean fields: true / false");
            instructionsSheet.createRow(5).createCell(0).setCellValue("4. Lists: comma separated");
            instructionsSheet.createRow(6).createCell(0).setCellValue("5. JSON fields: provide proper JSON");

            instructionsSheet.setColumnWidth(0, 20000);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
