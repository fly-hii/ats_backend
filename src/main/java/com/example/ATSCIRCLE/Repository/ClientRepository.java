package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Sales.Client;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends MongoRepository<Client, String> {

    // -------------------- ORGANIZATION FILTERED QUERIES --------------------
    
    // Find all by organizationId
    List<Client> findByOrganizationId(String organizationId);
    
    // Find by ID and organizationId
    Optional<Client> findByIdAndOrganizationId(String id, String organizationId);
    
    // Find by company name within organization
    Optional<Client> findByCompanyNameIgnoreCaseAndOrganizationId(String companyName, String organizationId);
    
    // Find by email within organization
    Optional<Client> findByEmailAndOrganizationId(String email, String organizationId);
    
    // Check if exists by email within organization
    boolean existsByEmailAndOrganizationId(String email, String organizationId);
    
    // Check if exists by company name within organization
    boolean existsByCompanyNameIgnoreCaseAndOrganizationId(String companyName, String organizationId);
    
    // -------------------- MIGRATION QUERIES --------------------
    
    // Find by migrated company ID within organization
    Optional<Client> findByMigratedFromCompanyIdAndOrganizationId(String companyId, String organizationId);
    
    // -------------------- USER TRACKING QUERIES (Organization Filtered) --------------------
    
    // Find clients by creator within organization
    List<Client> findByCreatedByAndOrganizationId(String createdBy, String organizationId);
    
    // Find by contact owner within organization (assigned clients)
    List<Client> findByContactOwnerAndOrganizationId(String contactOwner, String organizationId);
    
    // -------------------- CLIENT-SPECIFIC QUERIES (Organization Filtered) --------------------
    
    // Find by role within organization
    List<Client> findByRoleAndOrganizationId(String role, String organizationId);
    
    // Find by hiring manager within organization
    List<Client> findByHiringManagerAndOrganizationId(String hiringManager, String organizationId);
    
    // Find by POC email within organization
    Optional<Client> findByPocEmailAndOrganizationId(String pocEmail, String organizationId);
    
    // -------------------- FILTER QUERIES (Organization Filtered) --------------------
    
    // Find by industry within organization
    List<Client> findByIndustryAndOrganizationId(Industry industry, String organizationId);
    
    // Find by lead status within organization
    List<Client> findByLeadStatusAndOrganizationId(LeadStatus leadStatus, String organizationId);
    
    // -------------------- SEARCH QUERIES (Organization Filtered) --------------------
    
    // Search by name containing within organization
    @Query("{ 'companyName' : { $regex: ?0, $options: 'i' }, 'organizationId' : ?1 }")
    List<Client> findByCompanyNameContainingAndOrganizationId(String name, String organizationId);
    
    // -------------------- BULK OPERATIONS (Organization Filtered) --------------------
    
    // Find all by IDs within organization
    @Query("{ '_id' : { $in: ?0 }, 'organizationId' : ?1 }")
    List<Client> findByIdInAndOrganizationId(List<String> ids, String organizationId);
    
    // -------------------- COUNT QUERIES (Organization Filtered) --------------------
    
    // Count by organizationId
    long countByOrganizationId(String organizationId);
    
    // Count by industry within organization
    long countByIndustryAndOrganizationId(Industry industry, String organizationId);
    
    // Count by role within organization
    long countByRoleAndOrganizationId(String role, String organizationId);
    
    // Count by created by within organization
    long countByCreatedByAndOrganizationId(String createdBy, String organizationId);
    
    // -------------------- ACTIVITY QUERIES (Organization Filtered) --------------------
    
    // Find clients with recent activity within organization
    @Query("{ 'lastActivity' : { $gte: ?0 }, 'organizationId' : ?1 }")
    List<Client> findByLastActivityAfterAndOrganizationId(LocalDateTime date, String organizationId);
    
    // -------------------- LEGACY QUERIES (Without Organization - For Admin/Internal Use Only) --------------------
    
    // Find by company name (global - admin use)
    Optional<Client> findByCompanyNameIgnoreCase(String companyName);
    
    // Find by email (global - admin use)
    Optional<Client> findByEmail(String email);
    
    // Check if exists by email (global)
    boolean existsByEmail(String email);
    
    // Check if exists by company name (global)
    boolean existsByCompanyNameIgnoreCase(String companyName);
    
    // Find by migrated company ID (global)
    Optional<Client> findByMigratedFromCompanyId(String companyId);
    
    // Find by role (global)
    List<Client> findByRole(String role);
    
    // Find by hiring manager (global)
    List<Client> findByHiringManager(String hiringManager);
    
    // Find by POC email (global)
    Optional<Client> findByPocEmail(String pocEmail);
    
    // Find clients by creator (global)
    List<Client> findByCreatedBy(String createdBy);
    
    // Find by contact owner (global)
    List<Client> findByContactOwner(String contactOwner);
    
    // Find clients with recent activity (global)
    @Query("{ 'lastActivity' : { $gte: ?0 } }")
    List<Client> findByLastActivityAfter(LocalDateTime date);
    
    // Find by industry (global)
    List<Client> findByIndustry(Industry industry);
    
    // Find by lead status (global)
    List<Client> findByLeadStatus(LeadStatus leadStatus);
    
    // Search by name containing (global)
    @Query("{ 'companyName' : { $regex: ?0, $options: 'i' } }")
    List<Client> findByCompanyNameContaining(String name);
    
    // Count by industry (global)
    long countByIndustry(Industry industry);
    
    // Count by role (global)
    long countByRole(String role);
    
    // Count by created by (global)
    long countByCreatedBy(String createdBy);
    
    // Find all by IDs (global)
    List<Client> findAllById(Iterable<String> ids);
}