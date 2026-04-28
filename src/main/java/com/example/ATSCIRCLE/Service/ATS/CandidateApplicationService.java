package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;
import com.opencsv.CSVReader;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Sheet;

import com.opencsv.exceptions.CsvException;


@Service
public class CandidateApplicationService {

    @Autowired
    private Candidateapplicationrepository applicationRepository;

    // Get all available field names from CandidateApplication model
    public List<String> getAllAvailableFields() {
        List<String> fieldNames = new ArrayList<>();
        Field[] fields = CandidateApplication.class.getDeclaredFields();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            if (!fieldName.equals("id") && !fieldName.equals("createdAt") && 
                !fieldName.equals("updatedAt") && !fieldName.equals("createdBy") && 
                !fieldName.equals("updatedBy")) {
                fieldNames.add(fieldName);
            }
        }
        
        return fieldNames;
    }

    // Get application by email
    public Optional<CandidateApplication> getApplicationByEmail(String email) {
        return applicationRepository.findByPrimaryEmail(email);
    }

   // Create new application from Map
    @Transactional
    public CandidateApplication createApplicationFromMap(
            Map<String, Object> requestBody, 
            String organizationId, 
            String jobId, 
            String clientId, 
            String userId) {
        
        CandidateApplication application = new CandidateApplication();
        
        // Set organization and IDs
        application.setOrganizationId(organizationId);
        application.setJobIds(new ArrayList<>(Arrays.asList(jobId)));
        application.setClientIds(new ArrayList<>(Arrays.asList(clientId)));
        
        // Set default status
        application.setStatus("Applied");
        
        // Map all fields from request body
        mapFieldsToApplication(application, requestBody);

        
        // Set audit fields
        application.setCreatedBy(userId);
        application.setUpdatedBy(userId);
        application.prePersist();
        
        return applicationRepository.save(application);
    }

    // Append job and client to existing application
    @Transactional
    public CandidateApplication appendJobToApplication(
            String applicationId, 
            String jobId, 
            String clientId, 
            String userId) {
        
        Optional<CandidateApplication> existing = applicationRepository.findById(applicationId);
        if (existing.isPresent()) {
            CandidateApplication application = existing.get();
            
            if (application.getJobIds() == null) {
                application.setJobIds(new ArrayList<>());
            }
            if (application.getClientIds() == null) {
                application.setClientIds(new ArrayList<>());
            }
            
            // Add new job and client
            application.getJobIds().add(jobId);
            application.getClientIds().add(clientId);
            
            application.setUpdatedBy(userId);
            application.preUpdate();
            
            return applicationRepository.save(application);
        }
        throw new RuntimeException("Application not found with id: " + applicationId);
    }

    // Helper method to map fields from request body to application
    private void mapFieldsToApplication(CandidateApplication application, Map<String, Object> requestBody) {
    // ═══════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("firstName")) 
        application.setFirstName((String) requestBody.get("firstName"));
    if (requestBody.containsKey("middleName")) 
        application.setMiddleName((String) requestBody.get("middleName"));
    if (requestBody.containsKey("lastName")) 
        application.setLastName((String) requestBody.get("lastName"));
    if (requestBody.containsKey("fullName")) 
        application.setFullName((String) requestBody.get("fullName"));
    if (requestBody.containsKey("dateOfBirth")) {
        Object dobObj = requestBody.get("dateOfBirth");
        if (dobObj instanceof String) {
            application.setDateOfBirth(LocalDate.parse((String) dobObj));
        }
    }
    if (requestBody.containsKey("age")) {
        Object ageObj = requestBody.get("age");
        if (ageObj instanceof Integer) {
            application.setAge((Integer) ageObj);
        } else if (ageObj instanceof String) {
            try {
                application.setAge(Integer.parseInt((String) ageObj));
            } catch (NumberFormatException e) {
            }
        }
    }
    if (requestBody.containsKey("gender")) 
        application.setGender((String) requestBody.get("gender"));
    if (requestBody.containsKey("nationality")) 
        application.setNationality((String) requestBody.get("nationality"));
    if (requestBody.containsKey("maritalStatus")) 
        application.setMaritalStatus((String) requestBody.get("maritalStatus"));
    
    // ═══════════════════════════════════════════════════════════════
    // CONTACT INFORMATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("primaryEmail")) 
        application.setPrimaryEmail((String) requestBody.get("primaryEmail"));
    if (requestBody.containsKey("primaryPhone")) 
        application.setPrimaryPhone((String) requestBody.get("primaryPhone"));
    if (requestBody.containsKey("whatsappNumber")) 
        application.setWhatsappNumber((String) requestBody.get("whatsappNumber"));
    if (requestBody.containsKey("linkedInProfile")) 
        application.setLinkedInProfile((String) requestBody.get("linkedInProfile"));
    if (requestBody.containsKey("githubProfile")) 
        application.setGithubProfile((String) requestBody.get("githubProfile"));
    if (requestBody.containsKey("portfolioWebsite")) 
        application.setPortfolioWebsite((String) requestBody.get("portfolioWebsite"));
    if (requestBody.containsKey("videoIntroductionURL")) 
        application.setVideoIntroductionURL((String) requestBody.get("videoIntroductionURL"));
    if (requestBody.containsKey("sourceOfApplication")) 
        application.setSourceOfApplication((String) requestBody.get("sourceOfApplication"));
    
    // ═══════════════════════════════════════════════════════════════
    // ADDRESS INFORMATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("addressLine")) 
        application.setAddressLine((String) requestBody.get("addressLine"));
    if (requestBody.containsKey("city")) 
        application.setCity((String) requestBody.get("city"));
    if (requestBody.containsKey("state")) 
        application.setState((String) requestBody.get("state"));
    if (requestBody.containsKey("province")) 
        application.setProvince((String) requestBody.get("province"));
    if (requestBody.containsKey("region")) 
        application.setRegion((String) requestBody.get("region")); 
    if (requestBody.containsKey("postalCode")) 
        application.setPostalCode((String) requestBody.get("postalCode"));
    if (requestBody.containsKey("country")) 
        application.setCountry((String) requestBody.get("country"));
    
    // ═══════════════════════════════════════════════════════════════
    // LOCATION PREFERENCES
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("currentLocation")) 
        application.setCurrentLocation((String) requestBody.get("currentLocation"));
    if (requestBody.containsKey("willingToRelocate")) {
        Object relocateObj = requestBody.get("willingToRelocate");
        if (relocateObj instanceof Boolean) {
            application.setWillingToRelocate((Boolean) relocateObj);
        } else if (relocateObj instanceof String) {
            String relocateStr = (String) relocateObj;
            if ("Yes".equalsIgnoreCase(relocateStr)) {
                application.setWillingToRelocate(true);
            } else if ("No".equalsIgnoreCase(relocateStr)) {
                application.setWillingToRelocate(false);
            } else if ("Maybe".equalsIgnoreCase(relocateStr)) {
                application.setWillingToRelocate(null);
            }
        }
    }
    if (requestBody.containsKey("preferredLocations")) {
        application.setPreferredLocations((List<String>) requestBody.get("preferredLocations"));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // EXPERIENCE & EMPLOYMENT
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("totalYearsOfExperience")) 
        application.setTotalYearsOfExperience((String) requestBody.get("totalYearsOfExperience"));
    if (requestBody.containsKey("totalExperienceInTargetRole")) 
        application.setTotalExperienceInTargetRole((String) requestBody.get("totalExperienceInTargetRole"));
    if (requestBody.containsKey("teamManagementExperienceYears")) 
        application.setTeamManagementExperienceYears((String) requestBody.get("teamManagementExperienceYears"));
    if (requestBody.containsKey("currentJobTitle")) 
        application.setCurrentJobTitle((String) requestBody.get("currentJobTitle"));
    if (requestBody.containsKey("currentCompany")) 
        application.setCurrentCompany((String) requestBody.get("currentCompany"));
    if (requestBody.containsKey("currentJobStartDate")) {
        Object dateObj = requestBody.get("currentJobStartDate");
        if (dateObj instanceof String) {
            application.setCurrentJobStartDate(LocalDate.parse((String) dateObj));
        }
    }
    if (requestBody.containsKey("previousJobTitle")) 
        application.setPreviousJobTitle((String) requestBody.get("previousJobTitle"));
    if (requestBody.containsKey("previousExperience")) 
        application.setPreviousExperience((String) requestBody.get("previousExperience"));
    if (requestBody.containsKey("previousJobStartDate")) {
        Object dateObj = requestBody.get("previousJobStartDate");
        if (dateObj instanceof String) {
            application.setPreviousJobStartDate(LocalDate.parse((String) dateObj));
        }
    }
    if (requestBody.containsKey("reasonForLeaving")) 
        application.setReasonForLeaving((String) requestBody.get("reasonForLeaving"));
    if (requestBody.containsKey("teamSizeManaged")) 
        application.setTeamSizeManaged((String) requestBody.get("teamSizeManaged"));
    if (requestBody.containsKey("careerGapsExplanation")) 
        application.setCareerGapsExplanation((String) requestBody.get("careerGapsExplanation"));
    
    // ═══════════════════════════════════════════════════════════════
    // COMPENSATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("currentCTC")) 
        application.setCurrentCTC((String) requestBody.get("currentCTC"));
    if (requestBody.containsKey("expectedCTC")) 
        application.setExpectedCTC((String) requestBody.get("expectedCTC"));
    if (requestBody.containsKey("givenCTC")) 
        application.setgivenCTC((String) requestBody.get("givenCTC"));
    if (requestBody.containsKey("actualjoiningdate")) 
        application.setactualjoiningdate((String) requestBody.get("actualjoiningdate"));
    if (requestBody.containsKey("salaryCurrency")) 
        application.setSalaryCurrency((String) requestBody.get("salaryCurrency"));
    if (requestBody.containsKey("salaryFrequency")) 
        application.setSalaryFrequency((String) requestBody.get("salaryFrequency"));
    if (requestBody.containsKey("noticePeriod")) 
        application.setNoticePeriod((String) requestBody.get("noticePeriod"));
    
    // ═══════════════════════════════════════════════════════════════
    // WORK AUTHORIZATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("workAuthorizationStatus")) 
        application.setWorkAuthorizationStatus((String) requestBody.get("workAuthorizationStatus"));
    
    if (requestBody.containsKey("workAuthorizationCountries")) 
        application.setWorkAuthorizationCountries((String) requestBody.get("workAuthorizationCountries"));
    
    if (requestBody.containsKey("visaType")) 
        application.setVisaType((String) requestBody.get("visaType"));
 
    if (requestBody.containsKey("visaExpiryDate")) {
        Object dateObj = requestBody.get("visaExpiryDate");
        if (dateObj instanceof String) {
            application.setVisaExpiryDate(LocalDate.parse((String) dateObj));
        }
    }
    if (requestBody.containsKey("visaExtensionPossible")) 
        application.setVisaExtensionPossible((String) requestBody.get("visaExtensionPossible"));
    
    // ═══════════════════════════════════════════════════════════════
    // EDUCATION
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("highestEducationLevel")) 
        application.setHighestEducationLevel((String) requestBody.get("highestEducationLevel"));
    if (requestBody.containsKey("fieldOfStudy")) 
        application.setFieldOfStudy((String) requestBody.get("fieldOfStudy"));
    if (requestBody.containsKey("universityName")) 
        application.setUniversityName((String) requestBody.get("universityName"));
    if (requestBody.containsKey("gpa")) 
        application.setGpa((String) requestBody.get("gpa"));
    
   
    if (requestBody.containsKey("additionalDegrees")) 
        application.setAdditionalDegrees((String) requestBody.get("additionalDegrees"));
    
    // ═══════════════════════════════════════════════════════════════
    // REFERENCES
    // ═══════════════════════════════════════════════════════════════
   
    if (requestBody.containsKey("reference1Name")) 
        application.setReference1Name((String) requestBody.get("reference1Name"));
    if (requestBody.containsKey("reference1Title")) 
        application.setReference1Title((String) requestBody.get("reference1Title"));
    if (requestBody.containsKey("reference1Company")) 
        application.setReference1Company((String) requestBody.get("reference1Company"));
    if (requestBody.containsKey("reference1Email")) 
        application.setReference1Email((String) requestBody.get("reference1Email"));
    if (requestBody.containsKey("reference1Phone")) 
        application.setReference1Phone((String) requestBody.get("reference1Phone"));
    if (requestBody.containsKey("reference1Relationship")) 
        application.setReference1Relationship((String) requestBody.get("reference1Relationship"));
    
    // ═══════════════════════════════════════════════════════════════
    // CONSENTS (BOOLEAN FIELDS)
    // ═══════════════════════════════════════════════════════════════

    if (requestBody.containsKey("backgroundCheckConsent")) {
        application.setBackgroundCheckConsent(parseBoolean(requestBody.get("backgroundCheckConsent")));
    }
    if (requestBody.containsKey("drugTestConsent")) {
        application.setDrugTestConsent(parseBoolean(requestBody.get("drugTestConsent")));
    }
    if (requestBody.containsKey("rightToWorkConfirm")) {
        application.setRightToWorkConfirm(parseBoolean(requestBody.get("rightToWorkConfirm")));
    }
    if (requestBody.containsKey("dataProcessingConsent")) {
        application.setDataProcessingConsent(parseBoolean(requestBody.get("dataProcessingConsent")));
    }
    if (requestBody.containsKey("communicationsConsent")) {
        application.setCommunicationsConsent(parseBoolean(requestBody.get("communicationsConsent")));
    }
    if (requestBody.containsKey("termsAndConditionsConsent")) {
        application.setTermsAndConditionsConsent(parseBoolean(requestBody.get("termsAndConditionsConsent")));
    }
    if (requestBody.containsKey("creditCheckConsent")) {
        application.setCreditCheckConsent(parseBoolean(requestBody.get("creditCheckConsent")));
    }
    if (requestBody.containsKey("employmentVerificationConsent")) {
        application.setEmploymentVerificationConsent(parseBoolean(requestBody.get("employmentVerificationConsent")));
    }
    if (requestBody.containsKey("educationVerificationConsent")) {
        application.setEducationVerificationConsent(parseBoolean(requestBody.get("educationVerificationConsent")));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DOCUMENT URLs
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("resumeUrl")) 
        application.setResumeUrl((String) requestBody.get("resumeUrl"));
    if (requestBody.containsKey("coverLetterUrl")) 
        application.setCoverLetterUrl((String) requestBody.get("coverLetterUrl"));
    if (requestBody.containsKey("profilePhotoUrl")) 
        application.setProfilePhotoUrl((String) requestBody.get("profilePhotoUrl"));
    if (requestBody.containsKey("professionalLicenses")) 
        application.setProfessionalLicenses((String) requestBody.get("professionalLicenses"));
    if (requestBody.containsKey("certificationsUpload")) 
        application.setCertificationsUpload((String) requestBody.get("certificationsUpload"));
    if (requestBody.containsKey("supportingDocuments")) 
        application.setSupportingDocuments((String) requestBody.get("supportingDocuments"));
    if (requestBody.containsKey("areaProfessionalCertifications")) 
        application.setAreaProfessionalCertifications((String) requestBody.get("areaProfessionalCertifications"));
    
    // ═══════════════════════════════════════════════════════════════
    // COMPLEX TYPES (Lists and Maps)
    // ═══════════════════════════════════════════════════════════════
    if (requestBody.containsKey("technicalSkills")) {
        application.setTechnicalSkills((List<CandidateApplication.TechnicalSkill>) requestBody.get("technicalSkills"));
    }
    if (requestBody.containsKey("softSkills")) {
        application.setSoftSkills((List<CandidateApplication.SoftSkill>) requestBody.get("softSkills"));
    }
    if (requestBody.containsKey("languages")) {
        application.setLanguages((List<CandidateApplication.Language>) requestBody.get("languages"));
    }
    if (requestBody.containsKey("keyAchievements")) {
        application.setKeyAchievements((List<String>) requestBody.get("keyAchievements"));
    }
    
    // ✅ FIX: Awards Recognition mapping
    if (requestBody.containsKey("awardsRecognition")) {
        application.setAwardsRecognition((List<String>) requestBody.get("awardsRecognition"));
    }
        if (requestBody.containsKey("screeningQuestionsByJob")) {
    Object screeningQuestionsObj = requestBody.get("screeningQuestionsByJob");
    
    if (screeningQuestionsObj instanceof Map) {
        Map<String, Object> screeningQuestionsMap = (Map<String, Object>) screeningQuestionsObj;
        Map<String, List<CandidateApplication.QuestionAnswer>> convertedMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : screeningQuestionsMap.entrySet()) {
            String jobId = entry.getKey();
            Object questionsListObj = entry.getValue();
            
            if (questionsListObj instanceof List) {
                List<Object> questionsList = (List<Object>) questionsListObj;
                List<CandidateApplication.QuestionAnswer> qaList = new ArrayList<>();
                
                for (Object qaObj : questionsList) {
                    if (qaObj instanceof Map) {
                        Map<String, Object> qaMap = (Map<String, Object>) qaObj;
                        CandidateApplication.QuestionAnswer qa = new CandidateApplication.QuestionAnswer();
                        qa.setQuestion((String) qaMap.get("question"));
                        qa.setAnswer((String) qaMap.get("answer"));
                        qaList.add(qa);
                    }
                }
                
                convertedMap.put(jobId, qaList);
            }
        }
        
        application.setScreeningQuestionsByJob(convertedMap);
    }
}
    }
private Boolean parseBoolean(Object value) {
    if (value == null) {
        return null;
    }
    if (value instanceof Boolean) {
        return (Boolean) value;
    }
    if (value instanceof String) {
        String strValue = (String) value;
        if ("Yes".equalsIgnoreCase(strValue) || "true".equalsIgnoreCase(strValue)) {
            return true;
        } else if ("No".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
            return false;
        } else if ("Maybe".equalsIgnoreCase(strValue)) {
            return null;
        }
    }
    return null;
}
   

// ✅ COMPLETE FIXED VERSION of updateApplicationPartial method
// Replace the existing method in CandidateApplicationService.java with this version

@Transactional
public CandidateApplication updateApplicationPartial(String id, Map<String, Object> updates, String userId) {
    Optional<CandidateApplication> existing = applicationRepository.findById(id);
    if (existing.isPresent()) {
        CandidateApplication application = existing.get();
        
        // ============= PERSONAL INFORMATION =============
        if (updates.containsKey("firstName")) 
            application.setFirstName((String) updates.get("firstName"));
        if (updates.containsKey("middleName")) 
            application.setMiddleName((String) updates.get("middleName"));
        if (updates.containsKey("lastName")) 
            application.setLastName((String) updates.get("lastName"));
        if (updates.containsKey("fullName")) 
            application.setFullName((String) updates.get("fullName"));
        if (updates.containsKey("gender")) 
            application.setGender((String) updates.get("gender"));
        if (updates.containsKey("nationality")) 
            application.setNationality((String) updates.get("nationality"));
        if (updates.containsKey("maritalStatus")) 
            application.setMaritalStatus((String) updates.get("maritalStatus"));
        
        // Age field (Integer)
        if (updates.containsKey("age")) {
            Object ageObj = updates.get("age");
            if (ageObj instanceof Integer) {
                application.setAge((Integer) ageObj);
            } else if (ageObj instanceof String) {
                try {
                    application.setAge(Integer.parseInt((String) ageObj));
                } catch (NumberFormatException e) {
                    // Skip invalid age
                }
            }
        }
        
        // ============= CONTACT INFORMATION =============
        if (updates.containsKey("primaryEmail")) 
            application.setPrimaryEmail((String) updates.get("primaryEmail"));
        if (updates.containsKey("primaryPhone")) 
            application.setPrimaryPhone((String) updates.get("primaryPhone"));
        if (updates.containsKey("whatsappNumber")) 
            application.setWhatsappNumber((String) updates.get("whatsappNumber"));
        if (updates.containsKey("linkedInProfile")) 
            application.setLinkedInProfile((String) updates.get("linkedInProfile"));
        if (updates.containsKey("githubProfile")) 
            application.setGithubProfile((String) updates.get("githubProfile"));
        if (updates.containsKey("portfolioWebsite")) 
            application.setPortfolioWebsite((String) updates.get("portfolioWebsite"));
        if (updates.containsKey("videoIntroductionURL")) 
            application.setVideoIntroductionURL((String) updates.get("videoIntroductionURL"));
        if (updates.containsKey("sourceOfApplication")) 
            application.setSourceOfApplication((String) updates.get("sourceOfApplication"));
        
        // ============= ADDRESS INFORMATION =============
        if (updates.containsKey("addressLine")) 
            application.setAddressLine((String) updates.get("addressLine"));
        if (updates.containsKey("city")) 
            application.setCity((String) updates.get("city"));
        if (updates.containsKey("state")) 
            application.setState((String) updates.get("state"));
        if (updates.containsKey("province")) 
            application.setProvince((String) updates.get("province"));
        if (updates.containsKey("region")) 
            application.setRegion((String) updates.get("region"));
        if (updates.containsKey("postalCode")) 
            application.setPostalCode((String) updates.get("postalCode"));
        if (updates.containsKey("country")) 
            application.setCountry((String) updates.get("country"));
        
        // ============= LOCATION PREFERENCES =============
        if (updates.containsKey("currentLocation")) 
            application.setCurrentLocation((String) updates.get("currentLocation"));
        if (updates.containsKey("willingToRelocate")) 
            application.setWillingToRelocate((Boolean) updates.get("willingToRelocate"));
        if (updates.containsKey("preferredLocations")) 
            application.setPreferredLocations((List<String>) updates.get("preferredLocations"));
        
        // ============= EXPERIENCE & EMPLOYMENT =============
        if (updates.containsKey("totalYearsOfExperience")) 
            application.setTotalYearsOfExperience((String) updates.get("totalYearsOfExperience"));
        if (updates.containsKey("totalExperienceInTargetRole")) 
            application.setTotalExperienceInTargetRole((String) updates.get("totalExperienceInTargetRole"));
        if (updates.containsKey("teamManagementExperienceYears")) 
            application.setTeamManagementExperienceYears((String) updates.get("teamManagementExperienceYears"));
        if (updates.containsKey("currentJobTitle")) 
            application.setCurrentJobTitle((String) updates.get("currentJobTitle"));
        if (updates.containsKey("currentCompany")) 
            application.setCurrentCompany((String) updates.get("currentCompany"));
        if (updates.containsKey("previousJobTitle")) 
            application.setPreviousJobTitle((String) updates.get("previousJobTitle"));
        if (updates.containsKey("previousExperience")) 
            application.setPreviousExperience((String) updates.get("previousExperience"));
        if (updates.containsKey("reasonForLeaving")) 
            application.setReasonForLeaving((String) updates.get("reasonForLeaving"));
        if (updates.containsKey("careerGapsExplanation")) 
            application.setCareerGapsExplanation((String) updates.get("careerGapsExplanation"));
        if (updates.containsKey("teamSizeManaged")) 
            application.setTeamSizeManaged((String) updates.get("teamSizeManaged"));
        
        // ============= COMPENSATION =============
        if (updates.containsKey("currentCTC")) 
            application.setCurrentCTC((String) updates.get("currentCTC"));
        if (updates.containsKey("expectedCTC")) 
            application.setExpectedCTC((String) updates.get("expectedCTC"));
        if (updates.containsKey("givenCTC")) 
            application.setgivenCTC((String) updates.get("givenCTC"));
        if (updates.containsKey("actualjoiningdate")) 
            application.setactualjoiningdate((String) updates.get("actualjoiningdate"));
        if (updates.containsKey("salaryCurrency")) 
            application.setSalaryCurrency((String) updates.get("salaryCurrency"));
        if (updates.containsKey("salaryFrequency")) 
            application.setSalaryFrequency((String) updates.get("salaryFrequency"));
        if (updates.containsKey("noticePeriod")) 
            application.setNoticePeriod((String) updates.get("noticePeriod"));
        
        // ============= WORK AUTHORIZATION =============
        if (updates.containsKey("workAuthorizationStatus")) 
            application.setWorkAuthorizationStatus((String) updates.get("workAuthorizationStatus"));
        if (updates.containsKey("workAuthorizationCountries")) 
            application.setWorkAuthorizationCountries((String) updates.get("workAuthorizationCountries"));
        if (updates.containsKey("visaType")) 
            application.setVisaType((String) updates.get("visaType"));
        if (updates.containsKey("visaExtensionPossible")) 
            application.setVisaExtensionPossible((String) updates.get("visaExtensionPossible"));
        
        // ============= EDUCATION =============
        if (updates.containsKey("highestEducationLevel")) 
            application.setHighestEducationLevel((String) updates.get("highestEducationLevel"));
        if (updates.containsKey("fieldOfStudy")) 
            application.setFieldOfStudy((String) updates.get("fieldOfStudy"));
        if (updates.containsKey("universityName")) 
            application.setUniversityName((String) updates.get("universityName"));
        if (updates.containsKey("gpa")) 
            application.setGpa((String) updates.get("gpa"));
        if (updates.containsKey("additionalDegrees")) 
            application.setAdditionalDegrees((String) updates.get("additionalDegrees"));
        
        // ============= REFERENCES =============
        if (updates.containsKey("reference1Name")) 
            application.setReference1Name((String) updates.get("reference1Name"));
        if (updates.containsKey("reference1Title")) 
            application.setReference1Title((String) updates.get("reference1Title"));
        if (updates.containsKey("reference1Company")) 
            application.setReference1Company((String) updates.get("reference1Company"));
        if (updates.containsKey("reference1Email")) 
            application.setReference1Email((String) updates.get("reference1Email"));
        if (updates.containsKey("reference1Phone")) 
            application.setReference1Phone((String) updates.get("reference1Phone"));
        if (updates.containsKey("reference1Relationship")) 
            application.setReference1Relationship((String) updates.get("reference1Relationship"));
        
        // ============= DOCUMENTS =============
        if (updates.containsKey("professionalLicenses")) 
            application.setProfessionalLicenses((String) updates.get("professionalLicenses"));
        if (updates.containsKey("certificationsUpload")) 
            application.setCertificationsUpload((String) updates.get("certificationsUpload"));
        if (updates.containsKey("supportingDocuments")) 
            application.setSupportingDocuments((String) updates.get("supportingDocuments"));
        if (updates.containsKey("areaProfessionalCertifications")) 
            application.setAreaProfessionalCertifications((String) updates.get("areaProfessionalCertifications"));
        if (updates.containsKey("resumeUrl")) 
            application.setResumeUrl((String) updates.get("resumeUrl"));
        if (updates.containsKey("coverLetterUrl")) 
            application.setCoverLetterUrl((String) updates.get("coverLetterUrl"));
        if (updates.containsKey("profilePhotoUrl")) 
            application.setProfilePhotoUrl((String) updates.get("profilePhotoUrl"));
        
        // ============= STATUS =============
        if (updates.containsKey("status")) 
            application.setStatus((String) updates.get("status"));
        
        // ============= DATE FIELDS =============
        if (updates.containsKey("dateOfBirth")) {
            Object dobObj = updates.get("dateOfBirth");
            if (dobObj instanceof String) {
                String dobStr = (String) dobObj;
                if (dobStr != null && !dobStr.isEmpty()) {
                    application.setDateOfBirth(java.time.LocalDate.parse(dobStr));
                }
            }
        }
        if (updates.containsKey("currentJobStartDate")) {
            Object dateObj = updates.get("currentJobStartDate");
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                if (dateStr != null && !dateStr.isEmpty()) {
                    application.setCurrentJobStartDate(java.time.LocalDate.parse(dateStr));
                }
            }
        }
        if (updates.containsKey("previousJobStartDate")) {
            Object dateObj = updates.get("previousJobStartDate");
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                if (dateStr != null && !dateStr.isEmpty()) {
                    application.setPreviousJobStartDate(java.time.LocalDate.parse(dateStr));
                }
            }
        }
        if (updates.containsKey("visaExpiryDate")) {
            Object dateObj = updates.get("visaExpiryDate");
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                if (dateStr != null && !dateStr.isEmpty()) {
                    application.setVisaExpiryDate(java.time.LocalDate.parse(dateStr));
                }
            }
        }
        
        // ============= BOOLEAN FIELDS (CONSENTS) =============
        if (updates.containsKey("backgroundCheckConsent")) 
            application.setBackgroundCheckConsent((Boolean) updates.get("backgroundCheckConsent"));
        if (updates.containsKey("drugTestConsent")) 
            application.setDrugTestConsent((Boolean) updates.get("drugTestConsent"));
        if (updates.containsKey("rightToWorkConfirm")) 
            application.setRightToWorkConfirm((Boolean) updates.get("rightToWorkConfirm"));
        if (updates.containsKey("dataProcessingConsent")) 
            application.setDataProcessingConsent((Boolean) updates.get("dataProcessingConsent"));
        if (updates.containsKey("communicationsConsent")) 
            application.setCommunicationsConsent((Boolean) updates.get("communicationsConsent"));
        if (updates.containsKey("termsAndConditionsConsent")) 
            application.setTermsAndConditionsConsent((Boolean) updates.get("termsAndConditionsConsent"));
        if (updates.containsKey("creditCheckConsent")) 
            application.setCreditCheckConsent((Boolean) updates.get("creditCheckConsent"));
        if (updates.containsKey("employmentVerificationConsent")) 
            application.setEmploymentVerificationConsent((Boolean) updates.get("employmentVerificationConsent"));
        if (updates.containsKey("educationVerificationConsent")) 
            application.setEducationVerificationConsent((Boolean) updates.get("educationVerificationConsent"));
        
        // ============= COMPLEX LIST FIELDS =============
        if (updates.containsKey("technicalSkills")) 
            application.setTechnicalSkills((List<CandidateApplication.TechnicalSkill>) updates.get("technicalSkills"));
        if (updates.containsKey("softSkills")) 
            application.setSoftSkills((List<CandidateApplication.SoftSkill>) updates.get("softSkills"));
        if (updates.containsKey("languages")) 
            application.setLanguages((List<CandidateApplication.Language>) updates.get("languages"));
        if (updates.containsKey("keyAchievements")) 
            application.setKeyAchievements((List<String>) updates.get("keyAchievements"));
        if (updates.containsKey("awardsRecognition")) 
            application.setAwardsRecognition((List<String>) updates.get("awardsRecognition"));
        
        // ============= SCREENING QUESTIONS BY JOB =============
        if (updates.containsKey("screeningQuestionsByJob")) {
            Object screeningQuestionsObj = updates.get("screeningQuestionsByJob");
            
            if (screeningQuestionsObj instanceof Map) {
                Map<String, Object> screeningQuestionsMap = (Map<String, Object>) screeningQuestionsObj;
                Map<String, List<CandidateApplication.QuestionAnswer>> convertedMap = new HashMap<>();
                
                for (Map.Entry<String, Object> entry : screeningQuestionsMap.entrySet()) {
                    String jobId = entry.getKey();
                    Object questionsListObj = entry.getValue();
                    
                    if (questionsListObj instanceof List) {
                        List<Object> questionsList = (List<Object>) questionsListObj;
                        List<CandidateApplication.QuestionAnswer> qaList = new ArrayList<>();
                        
                        for (Object qaObj : questionsList) {
                            if (qaObj instanceof Map) {
                                Map<String, Object> qaMap = (Map<String, Object>) qaObj;
                                CandidateApplication.QuestionAnswer qa = new CandidateApplication.QuestionAnswer();
                                qa.setQuestion((String) qaMap.get("question"));
                                qa.setAnswer((String) qaMap.get("answer"));
                                qaList.add(qa);
                            }
                        }
                        
                        convertedMap.put(jobId, qaList);
                    }
                }
                
                application.setScreeningQuestionsByJob(convertedMap);
            }
        }
        
        // ============= UPDATE METADATA =============
        application.setUpdatedBy(userId);
        application.preUpdate();
        
        return applicationRepository.save(application);
    }
    throw new RuntimeException("Application not found with id: " + id);
}
    // Get application by ID
    public Optional<CandidateApplication> getApplicationById(String id) {
        return applicationRepository.findById(id);
    }

    // Get application by application ID
    public Optional<CandidateApplication> getApplicationByApplicationId(String applicationId) {
        return applicationRepository.findByApplicationId(applicationId);
    }

    // Get all applications
    public List<CandidateApplication> getAllApplications() {
        return applicationRepository.findAll();
    }

    // Get applications by organization ID
    public List<CandidateApplication> getApplicationsByOrganizationId(String organizationId) {
        return applicationRepository.findByOrganizationId(organizationId);
    }

    // Get applications by job ID
    public List<CandidateApplication> getApplicationsByJobId(String jobId) {
        return applicationRepository.findByJobIdsContaining(jobId);
    }

    // Get applications by client ID
    public List<CandidateApplication> getApplicationsByClientId(String clientId) {
        return applicationRepository.findByClientIdsContaining(clientId);
    }

    // Get applications by organization and job
    public List<CandidateApplication> getApplicationsByOrganizationAndJob(String organizationId, String jobId) {
        return applicationRepository.findByOrganizationIdAndJobIdsContaining(organizationId, jobId);
    }

    // Get applications by organization and client
    public List<CandidateApplication> getApplicationsByOrganizationAndClient(String organizationId, String clientId) {
        return applicationRepository.findByOrganizationIdAndClientIdsContaining(organizationId, clientId);
    }

    // Get applications by organization, client and job
    public List<CandidateApplication> getApplicationsByOrganizationClientAndJob(
            String organizationId, String clientId, String jobId) {
        return applicationRepository.findByOrganizationIdAndClientIdsContainingAndJobIdsContaining(
            organizationId, clientId, jobId);
    }

    // Get applications by status
    public List<CandidateApplication> getApplicationsByStatus(String status) {
        return applicationRepository.findByStatus(status);
    }

    // Get applications by organization and status
    public List<CandidateApplication> getApplicationsByOrganizationAndStatus(String organizationId, String status) {
        return applicationRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    // Get applications by organization, job and status
    public List<CandidateApplication> getApplicationsByOrganizationJobAndStatus(
            String organizationId, String jobId, String status) {
        List<CandidateApplication> apps = applicationRepository.findByOrganizationIdAndStatus(organizationId, status);
        return apps.stream()
                .filter(app -> app.getJobIds() != null && app.getJobIds().contains(jobId))
                .collect(Collectors.toList());
    }

    // Get applications with only filled (non-null) fields for organization
    public List<Map<String, Object>> getApplicationsWithFilledFieldsByOrganization(String organizationId) {
        List<CandidateApplication> applications = applicationRepository.findByOrganizationId(organizationId);
        return applications.stream()
                .map(this::extractNonNullFields)
                .collect(Collectors.toList());
    }

    // Get single application with only filled fields
    public Map<String, Object> getApplicationWithFilledFields(String id) {
        Optional<CandidateApplication> application = applicationRepository.findById(id);
        return application.map(this::extractNonNullFields).orElse(null);
    }

    // Helper method to extract non-null fields
    private Map<String, Object> extractNonNullFields(CandidateApplication application) {
        Map<String, Object> filledFields = new LinkedHashMap<>();
        Field[] fields = CandidateApplication.class.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(application);
                if (value != null) {
                    if (value instanceof List) {
                        if (!((List<?>) value).isEmpty()) {
                            filledFields.put(field.getName(), value);
                        }
                    } else {
                        filledFields.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        return filledFields;
    }

    // Delete application
    @Transactional
    public void deleteApplication(String id) {
        applicationRepository.deleteById(id);
    }

    // Count applications
    public Long countApplicationsByOrganization(String organizationId) {
        return applicationRepository.countByOrganizationId(organizationId);
    }

    public Long countApplicationsByJob(String jobId) {
        return applicationRepository.countByJobIdsContaining(jobId);
    }

    public Long countApplicationsByStatus(String status) {
        return applicationRepository.countByStatus(status);
    }

    // Update application status
   @Transactional
public CandidateApplication updateApplicationStatus(
        String id,
        String status,
        Map<String, Object> payload,   // <── replaces the old single-String parameter
        String userId) {

    Optional<CandidateApplication> existing = applicationRepository.findById(id);
    if (!existing.isPresent()) {
        throw new RuntimeException("Application not found with id: " + id);
    }

    CandidateApplication application = existing.get();

    // ── Normalise for case-insensitive comparison ──────────────────────────
    String normalised = status == null ? "" : status.trim().toLowerCase();

    switch (normalised) {

        // ── SHORTLISTED: status only, nothing else required ────────────────
        case "shortlisted":
            application.setStatus(status);
            // Clear any previously stored interview/offer data so the record
            // stays clean when cycling back through stages.
            application.setTimeslots(new ArrayList<>());
            application.setReason(payload != null ? (String) payload.get("reason") : null);
            break;

        // ── INTERVIEW SCHEDULED: timeslots required ────────────────────────
        case "interview scheduled":
            if (payload == null || !payload.containsKey("timeslots")) {
                throw new IllegalArgumentException(
                        "timeslots are required when status is 'Interview Scheduled'");
            }

            Object tsObj = payload.get("timeslots");
            if (!(tsObj instanceof List) || ((List<?>) tsObj).isEmpty()) {
                throw new IllegalArgumentException(
                        "timeslots must be a non-empty list of date/time strings");
            }

            @SuppressWarnings("unchecked")
            List<String> timeslots = (List<String>) tsObj;

            application.setStatus(status);
            application.setTimeslots(timeslots);
            application.setReason(payload.containsKey("reason") ? (String) payload.get("reason") : null);
            break;

        // ── SELECTED: givenCTC required ────────────────────────────────────
        case "selected":
            if (payload == null || !payload.containsKey("givenCTC")) {
                throw new IllegalArgumentException(
                        "givenCTC is required when status is 'Selected'");
            }

            String givenCTC = (String) payload.get("givenCTC");
            if (givenCTC == null || givenCTC.trim().isEmpty()) {
                throw new IllegalArgumentException("givenCTC must not be empty");
            }

            application.setStatus(status);
            application.setgivenCTC(givenCTC);
            application.setReason(payload.containsKey("reason") ? (String) payload.get("reason") : null);

            // Optionally capture actual joining date if provided alongside selection
            if (payload.containsKey("actualjoiningdate")) {
                application.setactualjoiningdate((String) payload.get("actualjoiningdate"));
            }
            break;

        // ── ALL OTHER STATUSES (Applied, Rejected, On-Hold, etc.) ──────────
        default:
            application.setStatus(status);
            if (payload != null && payload.containsKey("reason")) {
                application.setReason((String) payload.get("reason"));
            }
            break;
    }

    application.setUpdatedBy(userId);
    application.preUpdate();
    return applicationRepository.save(application);
}
    // Check if email already exists
    public boolean isEmailExists(String email) {
        return applicationRepository.findByPrimaryEmail(email).isPresent();
    }

    // Add job and client to existing application
    @Transactional
    public CandidateApplication addJobToApplication(String applicationId, String jobId, String clientId, String userId) {
        Optional<CandidateApplication> existing = applicationRepository.findById(applicationId);
        if (existing.isPresent()) {
            CandidateApplication application = existing.get();
            
            if (application.getJobIds() == null) {
                application.setJobIds(new ArrayList<>());
            }
            if (application.getClientIds() == null) {
                application.setClientIds(new ArrayList<>());
            }
            
            if (!application.getJobIds().contains(jobId)) {
                application.getJobIds().add(jobId);
                application.getClientIds().add(clientId);
                application.setUpdatedBy(userId);
                application.preUpdate();
                return applicationRepository.save(application);
            }
            
            throw new RuntimeException("Job already exists in this application");
        }
        throw new RuntimeException("Application not found with id: " + applicationId);
    }

    // Remove job and corresponding client from application
    @Transactional
    public CandidateApplication removeJobFromApplication(String applicationId, String jobId, String userId) {
        Optional<CandidateApplication> existing = applicationRepository.findById(applicationId);
        if (existing.isPresent()) {
            CandidateApplication application = existing.get();
            
            if (application.getJobIds() != null && application.getJobIds().contains(jobId)) {
                int index = application.getJobIds().indexOf(jobId);
                application.getJobIds().remove(index);
                
                if (application.getClientIds() != null && index < application.getClientIds().size()) {
                    application.getClientIds().remove(index);
                }
                
                application.setUpdatedBy(userId);
                application.preUpdate();
                return applicationRepository.save(application);
            }
            
            throw new RuntimeException("Job not found in this application");
        }
        throw new RuntimeException("Application not found with id: " + applicationId);
    }

    // Add multiple jobs at once
    @Transactional
    public CandidateApplication addMultipleJobsToApplication(
            String applicationId, List<String> jobIds, List<String> clientIds, String userId) {
        if (jobIds.size() != clientIds.size()) {
            throw new RuntimeException("JobIds and ClientIds lists must have same size");
        }
        
        Optional<CandidateApplication> existing = applicationRepository.findById(applicationId);
        if (existing.isPresent()) {
            CandidateApplication application = existing.get();
            
            if (application.getJobIds() == null) {
                application.setJobIds(new ArrayList<>());
            }
            if (application.getClientIds() == null) {
                application.setClientIds(new ArrayList<>());
            }
            
            for (int i = 0; i < jobIds.size(); i++) {
                String jobId = jobIds.get(i);
                String clientId = clientIds.get(i);
                
                if (!application.getJobIds().contains(jobId)) {
                    application.getJobIds().add(jobId);
                    application.getClientIds().add(clientId);
                }
            }
            
            application.setUpdatedBy(userId);
            application.preUpdate();
            return applicationRepository.save(application);
        }
        throw new RuntimeException("Application not found with id: " + applicationId);
    }

   // Bulk import from file (CSV or Excel)
@Transactional
public Map<String, Object> bulkImportApplicationsFromFile(
        MultipartFile file,
        String organizationId,
        String jobId,
        String clientId,
        String userId) throws IOException {
    
    String filename = file.getOriginalFilename();
    List<Map<String, Object>> applications;
    
    if (filename.endsWith(".csv")) {
        applications = parseCSV(file);
    } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
        applications = parseExcel(file);
    } else {
        throw new IllegalArgumentException("Unsupported file format");
    }
    
    // Add jobId and clientId to each application
    for (Map<String, Object> app : applications) {
        app.put("jobId", jobId);
        app.put("clientId", clientId);
    }
    
    // Process bulk import with validation
    return bulkImportApplications(applications, organizationId, userId);
}

// Parse CSV file
private List<Map<String, Object>> parseCSV(MultipartFile file) throws IOException {
    List<Map<String, Object>> applications = new ArrayList<>();
    
    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
        List<String[]> rows = reader.readAll();
        
        if (rows.isEmpty()) {
            return applications;
        }
        
        // First row is header
        String[] headers = rows.get(0);
        
        // Process data rows
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            Map<String, Object> appData = new LinkedHashMap<>();
            
            for (int j = 0; j < headers.length && j < row.length; j++) {
                String header = headers[j].trim();
                String value = row[j].trim();
                
                if (!value.isEmpty()) {
                    appData.put(header, value);
                }
            }
            
            if (!appData.isEmpty()) {
                applications.add(appData);
            }
        }
        
    } catch (CsvException e) {
        throw new IOException("Error parsing CSV file: " + e.getMessage());
    }
    
    return applications;
}

// Parse Excel file
private List<Map<String, Object>> parseExcel(MultipartFile file) throws IOException {
    List<Map<String, Object>> applications = new ArrayList<>();
    
    Workbook workbook = null;
    try {
        // Try XLSX first, then XLS
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (Exception e) {
            workbook = new HSSFWorkbook(file.getInputStream());
        }
        
        Sheet sheet = workbook.getSheetAt(0);
        
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return applications;
        }
        
        // First row is header
        Row headerRow = sheet.getRow(0);
        List<String> headers = new ArrayList<>();
        
        for (Cell cell : headerRow) {
            headers.add(getCellValueAsString(cell).trim());
        }
        
        // Process data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Map<String, Object> appData = new LinkedHashMap<>();
            
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                String header = headers.get(j);
                String value = getCellValueAsString(cell).trim();
                
                if (!value.isEmpty()) {
                    appData.put(header, value);
                }
            }
            
            if (!appData.isEmpty()) {
                applications.add(appData);
            }
        }
        
    } finally {
        if (workbook != null) {
            workbook.close();
        }
    }
    
    return applications;
}

// Helper method to get cell value as string
private String getCellValueAsString(Cell cell) {
    if (cell == null) {
        return "";
    }
    
    switch (cell.getCellType()) {
        case STRING:
            return cell.getStringCellValue();
        case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            }
            return String.valueOf((long) cell.getNumericCellValue());
        case BOOLEAN:
            return String.valueOf(cell.getBooleanCellValue());
        case FORMULA:
            return cell.getCellFormula();
        default:
            return "";
    }
}

// Bulk import applications with validation
@Transactional
public Map<String, Object> bulkImportApplications(
        List<Map<String, Object>> applications, 
        String organizationId, 
        String userId) {
    
    List<Map<String, Object>> successfulImports = new ArrayList<>();
    List<Map<String, Object>> failedImports = new ArrayList<>();
    List<Map<String, Object>> existingApplications = new ArrayList<>();
    
    int totalProcessed = 0;
    int successCount = 0;
    int failedCount = 0;
    int existingCount = 0;
    
    for (Map<String, Object> appData : applications) {
        totalProcessed++;
        
        try {
            // Extract required fields
            String email = (String) appData.get("primaryEmail");
            String jobId = (String) appData.get("jobId");
            String clientId = (String) appData.get("clientId");
            
            // Validate required fields
            if (email == null || email.trim().isEmpty()) {
                failedImports.add(Map.of(
                    "data", appData,
                    "reason", "primaryEmail is required",
                    "rowNumber", totalProcessed
                ));
                failedCount++;
                continue;
            }
            
            if (jobId == null || jobId.trim().isEmpty()) {
                failedImports.add(Map.of(
                    "data", appData,
                    "reason", "jobId is required",
                    "rowNumber", totalProcessed
                ));
                failedCount++;
                continue;
            }
            
            if (clientId == null || clientId.trim().isEmpty()) {
                failedImports.add(Map.of(
                    "data", appData,
                    "reason", "clientId is required",
                    "rowNumber", totalProcessed
                ));
                failedCount++;
                continue;
            }
            
            // Check if application with this email already exists
            Optional<CandidateApplication> existingApp = getApplicationByEmail(email);
            
            if (existingApp.isPresent()) {
                CandidateApplication existing = existingApp.get();
                
                // Check if same client already exists (regardless of job)
                if (existing.getClientIds() != null && existing.getClientIds().contains(clientId)) {
                    // Check if same job also exists with this client
                    boolean sameJobExists = false;
                    boolean sameClientDifferentJob = false;
                    
                    for (int i = 0; i < existing.getClientIds().size(); i++) {
                        if (existing.getClientIds().get(i).equals(clientId)) {
                            if (existing.getJobIds().get(i).equals(jobId)) {
                                sameJobExists = true;
                                break;
                            } else {
                                sameClientDifferentJob = true;
                                break;
                            }
                        }
                    }
                    
                    if (sameJobExists) {
                        existingApplications.add(Map.of(
                            "data", appData,
                            "reason", "Candidate has already applied for this job with this client",
                            "existingApplicationId", existing.getId(),
                            "email", email,
                            "rowNumber", totalProcessed
                        ));
                        existingCount++;
                        continue;
                    }
                    
                    if (sameClientDifferentJob) {
                        existingApplications.add(Map.of(
                            "data", appData,
                            "reason", "Candidate has already applied to this client. Multiple job applications to the same client are not allowed",
                            "existingApplicationId", existing.getId(),
                            "email", email,
                            "rowNumber", totalProcessed
                        ));
                        existingCount++;
                        continue;
                    }
                }
                
                // Append new job and client to existing application (only if different client)
                try {
                     CandidateApplication updated = appendJobToApplication(
       existing.getId(), jobId, clientId, userId);
                    
                    successfulImports.add(Map.of(
                        "applicationId", updated.getId(),
                        "email", email,
                        "jobId", jobId,
                        "clientId", clientId,
                        "status", "Job added to existing application",
                        "rowNumber", totalProcessed
                    ));
                    successCount++;
                } catch (Exception e) {
                    failedImports.add(Map.of(
                        "data", appData,
                        "reason", "Error appending job: " + e.getMessage(),
                        "rowNumber", totalProcessed
                    ));
                    failedCount++;
                }
                
            } else {
                // Create new application if email doesn't exist
                try {
                    CandidateApplication newApplication = createApplicationFromMap(
                        appData, organizationId, jobId, clientId, userId);
                    
                    successfulImports.add(Map.of(
                        "applicationId", newApplication.getId(),
                        "email", email,
                        "jobId", jobId,
                        "clientId", clientId,
                        "status", "New application created",
                        "rowNumber", totalProcessed
                    ));
                    successCount++;
                } catch (Exception e) {
                    failedImports.add(Map.of(
                        "data", appData,
                        "reason", "Error creating application: " + e.getMessage(),
                        "rowNumber", totalProcessed
                    ));
                    failedCount++;
                }
            }
            
        } catch (Exception e) {
            failedImports.add(Map.of(
                "data", appData,
                "reason", "Unexpected error: " + e.getMessage(),
                "rowNumber", totalProcessed
            ));
            failedCount++;
        }
    }
    
    // Build response
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("summary", Map.of(
        "totalProcessed", totalProcessed,
        "successCount", successCount,
        "failedCount", failedCount,
        "existingCount", existingCount
    ));
    response.put("successfulImports", successfulImports);
    response.put("failedImports", failedImports);
    response.put("existingApplications", existingApplications);
    
    return response;
}

}