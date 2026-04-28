package com.example.ATSCIRCLE.Service.ATS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.ATSCIRCLE.Models.ATS.Jobs;
import com.example.ATSCIRCLE.Repository.JobRepository;

@Service
public class JobService {

    @Autowired
    private JobRepository jobPostingRepository;

    public Jobs createJobPosting(Jobs jobPosting, String organizationId, String userId) {
        jobPosting.setOrganizationId(organizationId);
        jobPosting.setCreatedBy(userId);
        jobPosting.prePersist();
        
        // Ensure unique job code within organization
        while (jobPostingRepository.existsByJobCodeAndOrganizationId(
                jobPosting.getJobCode(), organizationId)) {
            jobPosting.generateJobCodeIfEmpty();
        }
        
        return jobPostingRepository.save(jobPosting);
    }

    public Jobs updateJobPosting(String id, Jobs jobPosting, 
                                      String organizationId, String userId) {
        Optional<Jobs> existing = jobPostingRepository
            .findByIdAndOrganizationId(id, organizationId);
        
        if (existing.isPresent()) {
            Jobs existingJob = existing.get();
            
            // Update only non-null fields
            if (jobPosting.getJobCode() != null) {
                existingJob.setJobCode(jobPosting.getJobCode());
            }
            if (jobPosting.getClient() != null) {
                existingJob.setClient(jobPosting.getClient());
            }
             if (jobPosting.getClientId() != null) {
                existingJob.setClientId(jobPosting.getClientId());
            }
            if (jobPosting.getJobTitle() != null) {
                existingJob.setJobTitle(jobPosting.getJobTitle());
            }
            if (jobPosting.getDepartment() != null) {
                existingJob.setDepartment(jobPosting.getDepartment());
            }
            if (jobPosting.getJobStatus() != null) {
                existingJob.setJobStatus(jobPosting.getJobStatus());
            }
            if (jobPosting.getJobPriority() != null) {
                existingJob.setJobPriority(jobPosting.getJobPriority());
            }
            if (jobPosting.getJobLocation() != null && !jobPosting.getJobLocation().isEmpty()) {
                existingJob.setJobLocation(jobPosting.getJobLocation());
            }
            if (jobPosting.getWorkLocation() != null) {
                existingJob.setWorkLocation(jobPosting.getWorkLocation());
            }
            if (jobPosting.getWorkModel() != null) {
                existingJob.setWorkModel(jobPosting.getWorkModel());
            }
            if (jobPosting.getWorkingHours() != null) {
                existingJob.setWorkingHours(jobPosting.getWorkingHours());
            }
            if (jobPosting.getJobType() != null) {
                existingJob.setJobType(jobPosting.getJobType());
            }
            if (jobPosting.getNumberOfOpenings() != null) {
                existingJob.setNumberOfOpenings(jobPosting.getNumberOfOpenings());
            }
            if (jobPosting.getPrimarySkills() != null && !jobPosting.getPrimarySkills().isEmpty()) {
                existingJob.setPrimarySkills(jobPosting.getPrimarySkills());
            }
            if (jobPosting.getSecondarySkills() != null && !jobPosting.getSecondarySkills().isEmpty()) {
                existingJob.setSecondarySkills(jobPosting.getSecondarySkills());
            }
            if (jobPosting.getExperienceMin() != null) {
                existingJob.setExperienceMin(jobPosting.getExperienceMin());
            }
            if (jobPosting.getExperienceMax() != null) {
                existingJob.setExperienceMax(jobPosting.getExperienceMax());
            }
            if (jobPosting.getJobDescription() != null) {
                existingJob.setJobDescription(jobPosting.getJobDescription());
            }
            if (jobPosting.getHiringManagerName() != null) {
                existingJob.setHiringManagerName(jobPosting.getHiringManagerName());
            }
            if (jobPosting.getHiringManagerEmail() != null) {
                existingJob.setHiringManagerEmail(jobPosting.getHiringManagerEmail());
            }
            if (jobPosting.getCompensationCurrency() != null) {
                existingJob.setCompensationCurrency(jobPosting.getCompensationCurrency());
            }
            if (jobPosting.getCompensationPaytype() != null) {
                existingJob.setCompensationPaytype(jobPosting.getCompensationPaytype());
            }
            if (jobPosting.getCompensationMin() != null) {
                existingJob.setCompensationMin(jobPosting.getCompensationMin());
            }
            if (jobPosting.getCompensationMax() != null) {
                existingJob.setCompensationMax(jobPosting.getCompensationMax());
            }
            if (jobPosting.getEducation() != null) {
                existingJob.setEducation(jobPosting.getEducation());
            }
            if (jobPosting.getPreferredInstitution() != null) {
                existingJob.setPreferredInstitution(jobPosting.getPreferredInstitution());
            }
            if (jobPosting.getNoticePeriod() != null) {
                existingJob.setNoticePeriod(jobPosting.getNoticePeriod());
            }
            if (jobPosting.getInterviewMode() != null) {
                existingJob.setInterviewMode(jobPosting.getInterviewMode());
            }
            if (jobPosting.getInterviewRounds() != null && !jobPosting.getInterviewRounds().isEmpty()) {
                existingJob.setInterviewRounds(jobPosting.getInterviewRounds());
            }
            if (jobPosting.getAssignedTo() != null) {
                existingJob.setAssignedTo(jobPosting.getAssignedTo());
            }
            if (jobPosting.getVendorCommercials() != null) {
                existingJob.setVendorCommercials(jobPosting.getVendorCommercials());
            }
            if (jobPosting.getWorkAuthorization() != null) {
                existingJob.setWorkAuthorization(jobPosting.getWorkAuthorization());
            }
            if (jobPosting.getVisaSponsorship() != null) {
                existingJob.setVisaSponsorship(jobPosting.getVisaSponsorship());
            }
            if (jobPosting.getBackgroundCheck() != null) {
                existingJob.setBackgroundCheck(jobPosting.getBackgroundCheck());
            }
            if (jobPosting.getSecurityClearance() != null) {
                existingJob.setSecurityClearance(jobPosting.getSecurityClearance());
            }
            if (jobPosting.getLicenseCertifications() != null) {
                existingJob.setLicenseCertifications(jobPosting.getLicenseCertifications());
            }
            if (jobPosting.getIdealCandidateProfile() != null) {
                existingJob.setIdealCandidateProfile(jobPosting.getIdealCandidateProfile());
            }
            if (jobPosting.getScreeningQuestions() != null && !jobPosting.getScreeningQuestions().isEmpty()) {
                existingJob.setScreeningQuestions(jobPosting.getScreeningQuestions());
            }
            
            // Set updatedBy and update timestamp
            existingJob.setUpdatedBy(userId);
            existingJob.preUpdate();
            
            return jobPostingRepository.save(existingJob);
        }
        
        throw new RuntimeException("Job posting not found with id: " + id);
    }

    public Optional<Jobs> getJobPostingById(String id, String organizationId) {
        return jobPostingRepository.findByIdAndOrganizationId(id, organizationId);
    }

    public Optional<Jobs> getJobPostingByJobCode(String jobCode, String organizationId) {
        return jobPostingRepository.findByJobCodeAndOrganizationId(jobCode, organizationId);
    }

    public List<Jobs> getAllJobPostings(String organizationId) {
        return jobPostingRepository.findByOrganizationId(organizationId);
    }

    public List<Jobs> getJobPostingsByStatus(String status, String organizationId) {
        return jobPostingRepository.findByJobStatusAndOrganizationId(status, organizationId);
    }

    public List<Jobs> getJobPostingsByClient(String clientId, String organizationId) {
        return jobPostingRepository.findByClientIdAndOrganizationId(clientId, organizationId);
    }

    public boolean deleteJobPosting(String id, String organizationId) {
        Optional<Jobs> jobPosting = jobPostingRepository
            .findByIdAndOrganizationId(id, organizationId);
        
        if (jobPosting.isPresent()) {
            jobPostingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Jobs> searchByPrimarySkills(List<String> skills, String organizationId) {
        return jobPostingRepository.findByPrimarySkillsInAndOrganizationId(skills, organizationId);
    }

    public List<Jobs> searchBySecondarySkills(List<String> skills, String organizationId) {
        return jobPostingRepository.findBySecondarySkillsInAndOrganizationId(skills, organizationId);
    }

   public List<Jobs> getJobPostingsByCreatorOrAssignee(String userId, String organizationId) {
    return jobPostingRepository.findByCreatedByOrAssignedToAndOrganizationId(userId, organizationId);
}



public void updateAssignedTo(List<String> ids, String assignedTo, String organizationId) {
  
    if (ids == null || ids.isEmpty()) {
        throw new RuntimeException("No job IDs provided for update");
    }
    if (assignedTo == null || assignedTo.trim().isEmpty()) {
        throw new RuntimeException("Assigned to cannot be empty");
    }
    
    List<Jobs> jobs = jobPostingRepository.findAllById(ids);
    if (jobs.isEmpty()) {
        throw new RuntimeException("No jobs found with provided IDs");
    }

    for (Jobs job : jobs) {
        if (!job.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Cannot update jobs from other organizations");
        }
        job.setAssignedTo(assignedTo.trim());
        job.setUpdatedAt(LocalDateTime.now());
    }

    jobPostingRepository.saveAll(jobs);
}
}