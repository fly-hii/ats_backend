package com.example.ATSCIRCLE.Models.ATS;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "job_postings")
public class Jobs {

    @Id
    private String id;

    private String organizationId; // Organization context
    private String clientId; // Client ID for multi-tenant support

    private String jobCode; // Auto-generated if left blank
    private String client;
    private String jobTitle;
    private String department;
    private String jobStatus;
    private String jobPriority;
    private List<String> jobLocation; // Multi-select
    private String workLocation;
    private String workModel;
    private String workingHours;
    private String jobType;
    private String numberOfOpenings;

    private List<String> primarySkills; // List structure
    private List<String> secondarySkills; // List structure

    private Integer experienceMin;
    private Integer experienceMax;

    private String jobDescription; // Rich text or attachment reference
    private String hiringManagerName;
    private String hiringManagerEmail;

    private String compensationCurrency;
    private String compensationPaytype;
    private Double compensationMin;
    private Double compensationMax;

    private String education;
    private String preferredInstitution;
    private String noticePeriod;

    private String interviewMode;
    private List<String> interviewRounds; // Can have multiple rounds

    private String assignedTo;
    private String vendorCommercials;
    private String workAuthorization;
    private String visaSponsorship;
    private String backgroundCheck;
    private String securityClearance;
    private String licenseCertifications;
    private String idealCandidateProfile;

    private List<ScreeningQuestion> screeningQuestions; // List structure for flexible questions

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public Jobs() {
        this.primarySkills = new ArrayList<>();
        this.secondarySkills = new ArrayList<>();
        this.jobLocation = new ArrayList<>();
        this.interviewRounds = new ArrayList<>();
        this.screeningQuestions = new ArrayList<>();
    }

    public Jobs(String organizationId, String clientId) {
        this();
        this.organizationId = organizationId;
        this.clientId = clientId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobPriority() {
        return jobPriority;
    }

    public void setJobPriority(String jobPriority) {
        this.jobPriority = jobPriority;
    }

    public List<String> getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(List<String> jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public String getWorkModel() {
        return workModel;
    }

    public void setWorkModel(String workModel) {
        this.workModel = workModel;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getNumberOfOpenings() {
        return numberOfOpenings;
    }

    public void setNumberOfOpenings(String numberOfOpenings) {
        this.numberOfOpenings = numberOfOpenings;
    }

    public List<String> getPrimarySkills() {
        return primarySkills;
    }

    public void setPrimarySkills(List<String> primarySkills) {
        this.primarySkills = primarySkills;
    }

    public List<String> getSecondarySkills() {
        return secondarySkills;
    }

    public void setSecondarySkills(List<String> secondarySkills) {
        this.secondarySkills = secondarySkills;
    }

    public Integer getExperienceMin() {
        return experienceMin;
    }

    public void setExperienceMin(Integer experienceMin) {
        this.experienceMin = experienceMin;
    }

    public Integer getExperienceMax() {
        return experienceMax;
    }

    public void setExperienceMax(Integer experienceMax) {
        this.experienceMax = experienceMax;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getHiringManagerName() {
        return hiringManagerName;
    }

    public void setHiringManagerName(String hiringManagerName) {
        this.hiringManagerName = hiringManagerName;
    }

    public String getHiringManagerEmail() {
        return hiringManagerEmail;
    }

    public void setHiringManagerEmail(String hiringManagerEmail) {
        this.hiringManagerEmail = hiringManagerEmail;
    }

    public String getCompensationCurrency() {
        return compensationCurrency;
    }

    public void setCompensationCurrency(String compensationCurrency) {
        this.compensationCurrency = compensationCurrency;
    }

    public String getCompensationPaytype() {
        return compensationPaytype;
    }

    public void setCompensationPaytype(String compensationPaytype) {
        this.compensationPaytype = compensationPaytype;
    }

    public Double getCompensationMin() {
        return compensationMin;
    }

    public void setCompensationMin(Double compensationMin) {
        this.compensationMin = compensationMin;
    }

    public Double getCompensationMax() {
        return compensationMax;
    }

    public void setCompensationMax(Double compensationMax) {
        this.compensationMax = compensationMax;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getPreferredInstitution() {
        return preferredInstitution;
    }

    public void setPreferredInstitution(String preferredInstitution) {
        this.preferredInstitution = preferredInstitution;
    }

    public String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(String interviewMode) {
        this.interviewMode = interviewMode;
    }

    public List<String> getInterviewRounds() {
        return interviewRounds;
    }

    public void setInterviewRounds(List<String> interviewRounds) {
        this.interviewRounds = interviewRounds;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getVendorCommercials() {
        return vendorCommercials;
    }

    public void setVendorCommercials(String vendorCommercials) {
        this.vendorCommercials = vendorCommercials;
    }

    public String getWorkAuthorization() {
        return workAuthorization;
    }

    public void setWorkAuthorization(String workAuthorization) {
        this.workAuthorization = workAuthorization;
    }

    public String getVisaSponsorship() {
        return visaSponsorship;
    }

    public void setVisaSponsorship(String visaSponsorship) {
        this.visaSponsorship = visaSponsorship;
    }

    public String getBackgroundCheck() {
        return backgroundCheck;
    }

    public void setBackgroundCheck(String backgroundCheck) {
        this.backgroundCheck = backgroundCheck;
    }

    public String getSecurityClearance() {
        return securityClearance;
    }

    public void setSecurityClearance(String securityClearance) {
        this.securityClearance = securityClearance;
    }

    public String getLicenseCertifications() {
        return licenseCertifications;
    }

    public void setLicenseCertifications(String licenseCertifications) {
        this.licenseCertifications = licenseCertifications;
    }

    public String getIdealCandidateProfile() {
        return idealCandidateProfile;
    }

    public void setIdealCandidateProfile(String idealCandidateProfile) {
        this.idealCandidateProfile = idealCandidateProfile;
    }

    public List<ScreeningQuestion> getScreeningQuestions() {
        return screeningQuestions;
    }

    public void setScreeningQuestions(List<ScreeningQuestion> screeningQuestions) {
        this.screeningQuestions = screeningQuestions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Helper methods
    public void generateJobCodeIfEmpty() {
        if (this.jobCode == null || this.jobCode.trim().isEmpty()) {
            this.jobCode = generateUniqueJobCode();
        }
    }

    private String generateUniqueJobCode() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String uniqueId = String.format("%05d", (int) (Math.random() * 100000));
        return "JOB-" + year + "-" + uniqueId;
    }

    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        generateJobCodeIfEmpty();
        
        if (this.primarySkills == null) {
            this.primarySkills = new ArrayList<>();
        }
        if (this.secondarySkills == null) {
            this.secondarySkills = new ArrayList<>();
        }
        if (this.jobLocation == null) {
            this.jobLocation = new ArrayList<>();
        }
        if (this.interviewRounds == null) {
            this.interviewRounds = new ArrayList<>();
        }
        if (this.screeningQuestions == null) {
            this.screeningQuestions = new ArrayList<>();
        }
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Nested class for Screening Questions
    public static class ScreeningQuestion {
        private String questionId;
        private String questionText;
        private QuestionType questionType;
        private List<String> options;
        private boolean required;

        public enum QuestionType {
            SHORT_ANSWER,
            MULTIPLE_CHOICE
        }

        // Constructors
        public ScreeningQuestion() {
            this.options = new ArrayList<>();
        }

        public ScreeningQuestion(String questionId, String questionText, QuestionType questionType, 
                                List<String> options, boolean required) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.questionType = questionType;
            this.options = options != null ? options : new ArrayList<>();
            this.required = required;
        }

        // Getters and Setters
        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public QuestionType getQuestionType() {
            return questionType;
        }

        public void setQuestionType(QuestionType questionType) {
            this.questionType = questionType;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}

