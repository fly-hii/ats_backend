package com.example.ATSCIRCLE.Repository;
import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface Candidateapplicationrepository extends MongoRepository<CandidateApplication, String> {
    
    // Find by organization ID
    List<CandidateApplication> findByOrganizationId(String organizationId);
    
    // Find by job ID (checks if jobId exists in jobIds list)
    List<CandidateApplication> findByJobIdsContaining(String jobId);
    
    // Find by client ID (checks if clientId exists in clientIds list)
    List<CandidateApplication> findByClientIdsContaining(String clientId);
    
    // Find by organization and job
    List<CandidateApplication> findByOrganizationIdAndJobIdsContaining(String organizationId, String jobId);
    
    // Find by organization and client
    List<CandidateApplication> findByOrganizationIdAndClientIdsContaining(String organizationId, String clientId);
    
    // Find by client and job (both in lists)
    List<CandidateApplication> findByClientIdsContainingAndJobIdsContaining(String clientId, String jobId);
    
    // Find by application ID
    Optional<CandidateApplication> findByApplicationId(String applicationId);
    
    // Find by email (to prevent duplicates)
    Optional<CandidateApplication> findByPrimaryEmail(String primaryEmail);
    
    // Find by status
    List<CandidateApplication> findByStatus(String status);
    
    // Find by organization and status
    List<CandidateApplication> findByOrganizationIdAndStatus(String organizationId, String status);
    
    // Find by job and status
    List<CandidateApplication> findByJobIdsContainingAndStatus(String jobId, String status);
    
    // Count applications by organization
    Long countByOrganizationId(String organizationId);
    
    // Count applications by job (in jobIds list)
    Long countByJobIdsContaining(String jobId);
    
    // Count applications by status
    Long countByStatus(String status);

    List<CandidateApplication> findByOrganizationIdAndClientIdsContainingAndJobIdsContaining(String organizationId,
            String clientId, String jobId);
}

