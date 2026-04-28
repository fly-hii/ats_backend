package com.example.ATSCIRCLE.Models.ATS;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Document(collection = "candidate_applications")
public class CandidateApplication {

    @Id
    private String id;
    
    // Core IDs for linking
    private String organizationId;
    private List<String> jobIds;
    private String applicationId;
    private List<String> clientIds;
    
    // Application Status
    private String status;

    // Status-related fields
    private String reason;                      // General reason (e.g., for rejection or any status note)
    private List<String> timeslots;             // Interview time slots (used when status = "Interview Scheduled")
    private String clientTimeZone;              // Client's timezone (e.g., "Asia/Kolkata")
    private String userTimeZone;                // User's timezone (selected by user)
    private List<String> convertedSlots;        // Slots converted to user's timezone
    private String selectedSlot;                // Slot selected by user (in user's timezone)
    private String meetLink;                    // Google Meet link (created when interview is scheduled)
    
    // Personal Information
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String nationality;
    private String maritalStatus;
    
    // Contact Information
    private String primaryEmail;
    private String primaryPhone;
    private String whatsappNumber;
    private String linkedInProfile;
    private String videoIntroductionURL;
    private String githubProfile;
    private String portfolioWebsite;
    private String sourceOfApplication;
    
    // Document URLs (S3 stored)
    private String resumeUrl;
    private String coverLetterUrl;
    private String profilePhotoUrl;
    
    // Address Information
    private String addressLine;
    private String city;
    private String state;
    private String province;
    private String region;
    private String postalCode;
    private String country;
    
    // Location Preferences
    private String currentLocation;
    private Boolean willingToRelocate;
    private List<String> preferredLocations;
    
    // Skills & Achievements
    private List<TechnicalSkill> technicalSkills;
    private List<SoftSkill> softSkills;
    private List<String> keyAchievements;
    private List<String> awardsRecognition;
    
    // Screening Questions Map (JobId -> List of Q&A)
    private Map<String, List<QuestionAnswer>> screeningQuestionsByJob;
    
    // Experience
    private String teamManagementExperienceYears;
    private List<Language> languages;
    private String totalYearsOfExperience;
    private String totalExperienceInTargetRole;
    
    // Current Employment
    private String currentJobTitle;
    private String currentCompany;
    private LocalDate currentJobStartDate;
    private String previousJobTitle;
    private String previousExperience;
    private LocalDate previousJobStartDate;
    private String reasonForLeaving;
    
    // Compensation
    private String currentCTC;
    private String expectedCTC;
    private String salaryCurrency;
    private String salaryFrequency;
    private String noticePeriod;
    private String careerGapsExplanation;
    private String teamSizeManaged;
    private String givenCTC;
    private String actualjoiningdate;

    // Work Authorization
    private String workAuthorizationStatus;
    private String workAuthorizationCountries;
    private String visaType;
    private LocalDate visaExpiryDate;
    private String visaExtensionPossible;
    
    // Certifications & Documents
    private String professionalLicenses;
    private String certificationsUpload;
    private String supportingDocuments;
    
    // References
    private String reference1Name;
    private String reference1Title;
    private String reference1Company;
    private String reference1Email;
    private String reference1Phone;
    private String reference1Relationship;
    
    // Education
    private String highestEducationLevel;
    private String fieldOfStudy;
    private String universityName;
    private String gpa;
    private String additionalDegrees;
    private String areaProfessionalCertifications;
    
    // Consents
    private Boolean backgroundCheckConsent;
    private Boolean drugTestConsent;
    private Boolean rightToWorkConfirm;
    private Boolean dataProcessingConsent;
    private Boolean communicationsConsent;
    private Boolean termsAndConditionsConsent;
    private Boolean creditCheckConsent;
    private Boolean employmentVerificationConsent;
    private Boolean educationVerificationConsent;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public CandidateApplication() {
        this.jobIds = new ArrayList<>();
        this.clientIds = new ArrayList<>();
        this.technicalSkills = new ArrayList<>();
        this.softSkills = new ArrayList<>();
        this.keyAchievements = new ArrayList<>();
        this.awardsRecognition = new ArrayList<>();
        this.languages = new ArrayList<>();
        this.preferredLocations = new ArrayList<>();
        this.screeningQuestionsByJob = new HashMap<>();
        this.timeslots = new ArrayList<>();
        this.convertedSlots = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────
    // NESTED CLASSES
    // ─────────────────────────────────────────────────────────────

    public static class TechnicalSkill {
        private String skillName;
        private Integer rating;
        
        public TechnicalSkill() {}
        
        public TechnicalSkill(String skillName, Integer rating) {
            this.skillName = skillName;
            this.rating = rating;
        }
        
        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }
    
    public static class SoftSkill {
        private String skillName;
        private Integer rating;
        
        public SoftSkill() {}
        
        public SoftSkill(String skillName, Integer rating) {
            this.skillName = skillName;
            this.rating = rating;
        }
        
        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }
    
    public static class Language {
        private String languageName;
        private Boolean canRead;
        private Boolean canWrite;
        private Boolean canSpeak;
        
        public Language() {}
        
        public Language(String languageName, Boolean canRead, Boolean canWrite, Boolean canSpeak) {
            this.languageName = languageName;
            this.canRead = canRead;
            this.canWrite = canWrite;
            this.canSpeak = canSpeak;
        }
        
        public String getLanguageName() { return languageName; }
        public void setLanguageName(String languageName) { this.languageName = languageName; }
        public Boolean getCanRead() { return canRead; }
        public void setCanRead(Boolean canRead) { this.canRead = canRead; }
        public Boolean getCanWrite() { return canWrite; }
        public void setCanWrite(Boolean canWrite) { this.canWrite = canWrite; }
        public Boolean getCanSpeak() { return canSpeak; }
        public void setCanSpeak(Boolean canSpeak) { this.canSpeak = canSpeak; }
    }
    
    public static class QuestionAnswer {
        private String question;
        private String answer;
        
        public QuestionAnswer() {}
        
        public QuestionAnswer(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────

    public void calculateAge() {
        if (this.dateOfBirth != null) {
            this.age = LocalDate.now().getYear() - this.dateOfBirth.getYear();
        }
    }
    
    public void generateApplicationIdIfEmpty() {
        if (this.applicationId == null || this.applicationId.trim().isEmpty()) {
            this.applicationId = generateUniqueApplicationId();
        }
    }
    
    private String generateUniqueApplicationId() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String uniqueId = String.format("%06d", (int) (Math.random() * 1000000));
        return "APP-" + year + "-" + uniqueId;
    }
    
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        generateApplicationIdIfEmpty();
        calculateAge();
        
        if (this.jobIds == null) this.jobIds = new ArrayList<>();
        if (this.clientIds == null) this.clientIds = new ArrayList<>();
        if (this.technicalSkills == null) this.technicalSkills = new ArrayList<>();
        if (this.softSkills == null) this.softSkills = new ArrayList<>();
        if (this.keyAchievements == null) this.keyAchievements = new ArrayList<>();
        if (this.awardsRecognition == null) this.awardsRecognition = new ArrayList<>();
        if (this.languages == null) this.languages = new ArrayList<>();
        if (this.preferredLocations == null) this.preferredLocations = new ArrayList<>();
        if (this.screeningQuestionsByJob == null) this.screeningQuestionsByJob = new HashMap<>();
        if (this.timeslots == null) this.timeslots = new ArrayList<>();
    }
    
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateAge();
    }

    // ─────────────────────────────────────────────────────────────
    // GETTERS AND SETTERS
    // ─────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public List<String> getJobIds() { return jobIds; }
    public void setJobIds(List<String> jobIds) { this.jobIds = jobIds; }
    
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    
    public List<String> getClientIds() { return clientIds; }
    public void setClientIds(List<String> clientIds) { this.clientIds = clientIds; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ── Status-related fields ──
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<String> getTimeslots() { return timeslots; }
    public void setTimeslots(List<String> timeslots) { this.timeslots = timeslots; }

    // ── Compensation ──
    public String getgivenCTC() { return givenCTC; }
    public void setgivenCTC(String givenCTC) { this.givenCTC = givenCTC; }

    public String getactualjoiningdate() { return actualjoiningdate; }
    public void setactualjoiningdate(String actualjoiningdate) { this.actualjoiningdate = actualjoiningdate; }
    
    // ── Document URLs ──
    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
    
    public String getCoverLetterUrl() { return coverLetterUrl; }
    public void setCoverLetterUrl(String coverLetterUrl) { this.coverLetterUrl = coverLetterUrl; }
    
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    
    // ── Location Preferences ──
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    
    public Boolean getWillingToRelocate() { return willingToRelocate; }
    public void setWillingToRelocate(Boolean willingToRelocate) { this.willingToRelocate = willingToRelocate; }
    
    public List<String> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<String> preferredLocations) { this.preferredLocations = preferredLocations; }
    
    // ── Screening Questions ──
    public Map<String, List<QuestionAnswer>> getScreeningQuestionsByJob() { return screeningQuestionsByJob; }
    public void setScreeningQuestionsByJob(Map<String, List<QuestionAnswer>> screeningQuestionsByJob) { 
        this.screeningQuestionsByJob = screeningQuestionsByJob; 
    }
    
    // ── Personal Information ──
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { 
        this.dateOfBirth = dateOfBirth;
        calculateAge();
    }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    
    // ── Contact Information ──
    public String getPrimaryEmail() { return primaryEmail; }
    public void setPrimaryEmail(String primaryEmail) { this.primaryEmail = primaryEmail; }
    
    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }
    
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    
    public String getLinkedInProfile() { return linkedInProfile; }
    public void setLinkedInProfile(String linkedInProfile) { this.linkedInProfile = linkedInProfile; }
    
    public String getVideoIntroductionURL() { return videoIntroductionURL; }
    public void setVideoIntroductionURL(String videoIntroductionURL) { this.videoIntroductionURL = videoIntroductionURL; }
    
    public String getGithubProfile() { return githubProfile; }
    public void setGithubProfile(String githubProfile) { this.githubProfile = githubProfile; }
    
    public String getPortfolioWebsite() { return portfolioWebsite; }
    public void setPortfolioWebsite(String portfolioWebsite) { this.portfolioWebsite = portfolioWebsite; }
    
    public String getSourceOfApplication() { return sourceOfApplication; }
    public void setSourceOfApplication(String sourceOfApplication) { this.sourceOfApplication = sourceOfApplication; }
    
    // ── Address Information ──
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    // ── Skills & Achievements ──
    public List<TechnicalSkill> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(List<TechnicalSkill> technicalSkills) { this.technicalSkills = technicalSkills; }
    
    public List<SoftSkill> getSoftSkills() { return softSkills; }
    public void setSoftSkills(List<SoftSkill> softSkills) { this.softSkills = softSkills; }
    
    public List<String> getKeyAchievements() { return keyAchievements; }
    public void setKeyAchievements(List<String> keyAchievements) { this.keyAchievements = keyAchievements; }
    
    public List<String> getAwardsRecognition() { return awardsRecognition; }
    public void setAwardsRecognition(List<String> awardsRecognition) { this.awardsRecognition = awardsRecognition; }
    
    // ── Experience ──
    public String getTeamManagementExperienceYears() { return teamManagementExperienceYears; }
    public void setTeamManagementExperienceYears(String teamManagementExperienceYears) { this.teamManagementExperienceYears = teamManagementExperienceYears; }
    
    public List<Language> getLanguages() { return languages; }
    public void setLanguages(List<Language> languages) { this.languages = languages; }
    
    public String getTotalYearsOfExperience() { return totalYearsOfExperience; }
    public void setTotalYearsOfExperience(String totalYearsOfExperience) { this.totalYearsOfExperience = totalYearsOfExperience; }
    
    public String getTotalExperienceInTargetRole() { return totalExperienceInTargetRole; }
    public void setTotalExperienceInTargetRole(String totalExperienceInTargetRole) { this.totalExperienceInTargetRole = totalExperienceInTargetRole; }
    
    // ── Current Employment ──
    public String getCurrentJobTitle() { return currentJobTitle; }
    public void setCurrentJobTitle(String currentJobTitle) { this.currentJobTitle = currentJobTitle; }
    
    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }
    
    public LocalDate getCurrentJobStartDate() { return currentJobStartDate; }
    public void setCurrentJobStartDate(LocalDate currentJobStartDate) { this.currentJobStartDate = currentJobStartDate; }
    
    public String getPreviousJobTitle() { return previousJobTitle; }
    public void setPreviousJobTitle(String previousJobTitle) { this.previousJobTitle = previousJobTitle; }
    
    public String getPreviousExperience() { return previousExperience; }
    public void setPreviousExperience(String previousExperience) { this.previousExperience = previousExperience; }
    
    public LocalDate getPreviousJobStartDate() { return previousJobStartDate; }
    public void setPreviousJobStartDate(LocalDate previousJobStartDate) { this.previousJobStartDate = previousJobStartDate; }
    
    public String getReasonForLeaving() { return reasonForLeaving; }
    public void setReasonForLeaving(String reasonForLeaving) { this.reasonForLeaving = reasonForLeaving; }
    
    // ── Compensation ──
    public String getCurrentCTC() { return currentCTC; }
    public void setCurrentCTC(String currentCTC) { this.currentCTC = currentCTC; }
    
    public String getExpectedCTC() { return expectedCTC; }
    public void setExpectedCTC(String expectedCTC) { this.expectedCTC = expectedCTC; }
    
    public String getSalaryCurrency() { return salaryCurrency; }
    public void setSalaryCurrency(String salaryCurrency) { this.salaryCurrency = salaryCurrency; }
    
    public String getSalaryFrequency() { return salaryFrequency; }
    public void setSalaryFrequency(String salaryFrequency) { this.salaryFrequency = salaryFrequency; }
    
    public String getNoticePeriod() { return noticePeriod; }
    public void setNoticePeriod(String noticePeriod) { this.noticePeriod = noticePeriod; }
    
    public String getCareerGapsExplanation() { return careerGapsExplanation; }
    public void setCareerGapsExplanation(String careerGapsExplanation) { this.careerGapsExplanation = careerGapsExplanation; }
    
    public String getTeamSizeManaged() { return teamSizeManaged; }
    public void setTeamSizeManaged(String teamSizeManaged) { this.teamSizeManaged = teamSizeManaged; }
    
    // ── Work Authorization ──
    public String getWorkAuthorizationStatus() { return workAuthorizationStatus; }
    public void setWorkAuthorizationStatus(String workAuthorizationStatus) { this.workAuthorizationStatus = workAuthorizationStatus; }
    
    public String getWorkAuthorizationCountries() { return workAuthorizationCountries; }
    public void setWorkAuthorizationCountries(String workAuthorizationCountries) { this.workAuthorizationCountries = workAuthorizationCountries; }
    
    public String getVisaType() { return visaType; }
    public void setVisaType(String visaType) { this.visaType = visaType; }
    
    public LocalDate getVisaExpiryDate() { return visaExpiryDate; }
    public void setVisaExpiryDate(LocalDate visaExpiryDate) { this.visaExpiryDate = visaExpiryDate; }
    
    public String getVisaExtensionPossible() { return visaExtensionPossible; }
    public void setVisaExtensionPossible(String visaExtensionPossible) { this.visaExtensionPossible = visaExtensionPossible; }
    
    // ── Certifications & Documents ──
    public String getProfessionalLicenses() { return professionalLicenses; }
    public void setProfessionalLicenses(String professionalLicenses) { this.professionalLicenses = professionalLicenses; }
    
    public String getCertificationsUpload() { return certificationsUpload; }
    public void setCertificationsUpload(String certificationsUpload) { this.certificationsUpload = certificationsUpload; }
    
    public String getSupportingDocuments() { return supportingDocuments; }
    public void setSupportingDocuments(String supportingDocuments) { this.supportingDocuments = supportingDocuments; }
    
    // ── References ──
    public String getReference1Name() { return reference1Name; }
    public void setReference1Name(String reference1Name) { this.reference1Name = reference1Name; }
    
    public String getReference1Title() { return reference1Title; }
    public void setReference1Title(String reference1Title) { this.reference1Title = reference1Title; }
    
    public String getReference1Company() { return reference1Company; }
    public void setReference1Company(String reference1Company) { this.reference1Company = reference1Company; }
    
    public String getReference1Email() { return reference1Email; }
    public void setReference1Email(String reference1Email) { this.reference1Email = reference1Email; }
    
    public String getReference1Phone() { return reference1Phone; }
    public void setReference1Phone(String reference1Phone) { this.reference1Phone = reference1Phone; }
    
    public String getReference1Relationship() { return reference1Relationship; }
    public void setReference1Relationship(String reference1Relationship) { this.reference1Relationship = reference1Relationship; }
    
    // ── Education ──
    public String getHighestEducationLevel() { return highestEducationLevel; }
    public void setHighestEducationLevel(String highestEducationLevel) { this.highestEducationLevel = highestEducationLevel; }
    
    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }
    
    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    
    public String getGpa() { return gpa; }
    public void setGpa(String gpa) { this.gpa = gpa; }
    
    public String getAdditionalDegrees() { return additionalDegrees; }
    public void setAdditionalDegrees(String additionalDegrees) { this.additionalDegrees = additionalDegrees; }
    
    public String getAreaProfessionalCertifications() { return areaProfessionalCertifications; }
    public void setAreaProfessionalCertifications(String areaProfessionalCertifications) { this.areaProfessionalCertifications = areaProfessionalCertifications; }
    
    // ── Consents ──
    public Boolean getBackgroundCheckConsent() { return backgroundCheckConsent; }
    public void setBackgroundCheckConsent(Boolean backgroundCheckConsent) { this.backgroundCheckConsent = backgroundCheckConsent; }
    
    public Boolean getDrugTestConsent() { return drugTestConsent; }
    public void setDrugTestConsent(Boolean drugTestConsent) { this.drugTestConsent = drugTestConsent; }
    
    public Boolean getRightToWorkConfirm() { return rightToWorkConfirm; }
    public void setRightToWorkConfirm(Boolean rightToWorkConfirm) { this.rightToWorkConfirm = rightToWorkConfirm; }
    
    public Boolean getDataProcessingConsent() { return dataProcessingConsent; }
    public void setDataProcessingConsent(Boolean dataProcessingConsent) { this.dataProcessingConsent = dataProcessingConsent; }
    
    public Boolean getCommunicationsConsent() { return communicationsConsent; }
    public void setCommunicationsConsent(Boolean communicationsConsent) { this.communicationsConsent = communicationsConsent; }
    
    public Boolean getTermsAndConditionsConsent() { return termsAndConditionsConsent; }
    public void setTermsAndConditionsConsent(Boolean termsAndConditionsConsent) { this.termsAndConditionsConsent = termsAndConditionsConsent; }
    
    public Boolean getCreditCheckConsent() { return creditCheckConsent; }
    public void setCreditCheckConsent(Boolean creditCheckConsent) { this.creditCheckConsent = creditCheckConsent; }
    
    public Boolean getEmploymentVerificationConsent() { return employmentVerificationConsent; }
    public void setEmploymentVerificationConsent(Boolean employmentVerificationConsent) { this.employmentVerificationConsent = employmentVerificationConsent; }
    
    public Boolean getEducationVerificationConsent() { return educationVerificationConsent; }
    public void setEducationVerificationConsent(Boolean educationVerificationConsent) { this.educationVerificationConsent = educationVerificationConsent; }
    
    // ── Metadata ──
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    // ── Timezone-related fields ──
    public String getClientTimeZone() { return clientTimeZone; }
    public void setClientTimeZone(String clientTimeZone) { this.clientTimeZone = clientTimeZone; }
    
    public String getUserTimeZone() { return userTimeZone; }
    public void setUserTimeZone(String userTimeZone) { this.userTimeZone = userTimeZone; }
    
    public List<String> getConvertedSlots() { return convertedSlots; }
    public void setConvertedSlots(List<String> convertedSlots) { this.convertedSlots = convertedSlots; }
    
    public String getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(String selectedSlot) { this.selectedSlot = selectedSlot; }
    
    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }
}