
package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Sales.Company;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CompanyRepository extends MongoRepository<Company, String> {

    // ========== ORGANIZATION-BASED QUERIES ==========
    
    // Find all companies in an organization
    List<Company> findByOrganizationId(String organizationId);
    
    Page<Company> findByOrganizationId(String organizationId, Pageable pageable);
    
    // Find by company name within organization
    Optional<Company> findByCompanyNameIgnoreCaseAndOrganizationId(String companyName, String organizationId);
    
    // Find companies by industry within organization
    List<Company> findByIndustryAndOrganizationId(Industry industry, String organizationId);
    
    Page<Company> findByIndustryAndOrganizationId(Industry industry, String organizationId, Pageable pageable);
    
    // Find companies by relationship type within organization
    List<Company> findByRelationshipTypeAndOrganizationId(RelationshipType relationshipType, String organizationId);
    
    // Find companies by lead status within organization
    List<Company> findByLeadStatusAndOrganizationId(LeadStatus leadStatus, String organizationId);
    
    // Find companies by lead source within organization
    List<Company> findByLeadSourceAndOrganizationId(LeadSource leadSource, String organizationId);
    
    // Find companies by company stage within organization
    List<Company> findByCompanyStageAndOrganizationId(CompanyStage companyStage, String organizationId);
    
   
    
    // Find companies by assigned to within organization
    List<Company> findByAssignedToAndOrganizationId(String assignedTo, String organizationId);
    
    // Find companies created within date range in organization
    List<Company> findByCreatedAtBetweenAndOrganizationId(LocalDateTime startDate, LocalDateTime endDate, String organizationId);
    
    // Find companies by email within organization
    Optional<Company> findByEmailAndOrganizationId(String email, String organizationId);
    
    // Find companies by phone within organization
    Optional<Company> findByPhoneAndOrganizationId(String phone, String organizationId);
    
    // Search companies by name containing text within organization
    @Query("{ 'companyName' : { $regex: ?0, $options: 'i' }, 'organizationId': ?1 }")
    List<Company> findByCompanyNameContainingAndOrganizationId(String name, String organizationId);
    
    // Find companies by multiple criteria within organization
    @Query("{ $and: [ " +
           "{ $or: [ { 'companyName' : { $regex: ?0, $options: 'i' } }, { 'email' : { $regex: ?0, $options: 'i' } } ] }, " +
           "{ 'industry' : ?1 }, " +
           "{ 'leadStatus' : ?2 }, " +
           "{ 'organizationId' : ?3 } ] }")
    List<Company> findByNameOrEmailAndIndustryAndLeadStatusAndOrganizationId(
        String searchTerm, 
        Industry industry, 
        LeadStatus leadStatus,
        String organizationId);
    
    // Find companies by city within organization
    @Query("{ $and: [ " +
           "{ $or: [ { 'billingAddress.city' : ?0 }, { 'shippingAddress.city' : ?0 } ] }, " +
           "{ 'organizationId' : ?1 } ] }")
    List<Company> findByCityAndOrganizationId(String city, String organizationId);
    
    // Find companies by state within organization
    @Query("{ $and: [ " +
           "{ $or: [ { 'billingAddress.state' : ?0 }, { 'shippingAddress.state' : ?0 } ] }, " +
           "{ 'organizationId' : ?1 } ] }")
    List<Company> findByStateAndOrganizationId(String state, String organizationId);
    
    // Find companies by country within organization
    @Query("{ $and: [ " +
           "{ $or: [ { 'billingAddress.country' : ?0 }, { 'shippingAddress.country' : ?0 } ] }, " +
           "{ 'organizationId' : ?1 } ] }")
    List<Company> findByCountryAndOrganizationId(String country, String organizationId);
    
    // Find contactable companies within organization
    @Query("{ 'doNotCall' : false, 'doNotEmail' : false, 'organizationId' : ?0 }")
    List<Company> findContactableCompaniesByOrganizationId(String organizationId);
    
    // Find companies with recent activity within organization
    @Query("{ 'lastActivity' : { $gte: ?0 }, 'organizationId' : ?1 }")
    List<Company> findByLastActivityAfterAndOrganizationId(LocalDateTime date, String organizationId);
    
    // Find shared companies in organization
    @Query("{ 'isShared' : true, 'organizationId' : ?0 }")
    List<Company> findSharedCompaniesByOrganizationId(String organizationId);
    
    // ========== EXISTENCE CHECKS WITHIN ORGANIZATION ==========
    
    // Check if company exists by name within organization
    boolean existsByCompanyNameIgnoreCaseAndOrganizationId(String companyName, String organizationId);
    
    // Check if company exists by email within organization
    boolean existsByEmailAndOrganizationId(String email, String organizationId);
    
    // ========== COUNT METHODS WITHIN ORGANIZATION ==========
    
    // Count companies by industry within organization
    long countByIndustryAndOrganizationId(Industry industry, String organizationId);
    
    // Count companies by lead status within organization
    long countByLeadStatusAndOrganizationId(LeadStatus leadStatus, String organizationId);
    
    // Count total companies in organization
    long countByOrganizationId(String organizationId);
    
    // Count companies by created by within organization
    long countByCreatedByAndOrganizationId(String createdBy, String organizationId);
    
    // Count companies by assigned to within organization
    long countByAssignedToAndOrganizationId(String assignedTo, String organizationId);

    List<Company> findByCreatedByOrAssignedTo(String organizationId, String userId);

    List<Company> findByOrganizationIdAndCompanyNameContainingIgnoreCase(String organizationId, String name);

    Optional<Company> findByOrganizationIdAndTaxIdNumber(String organizationId, String gstin);



}