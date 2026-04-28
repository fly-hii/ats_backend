package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Sales.Contact;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends MongoRepository<Contact, String> {

    // ========== ORGANIZATION-SCOPED QUERIES ==========
    
    // Find all contacts in organization
    List<Contact> findByOrganizationId(String organizationId);

    // Find by email
    Optional<Contact> findByEmail(String email);

    // Check if email exists globally (for system-wide uniqueness)
    boolean existsByEmail(String email);

    // Find by organization and email
    Optional<Contact> findByOrganizationIdAndEmail(String organizationId, String email);

    // Check if email exists in organization
    boolean existsByOrganizationIdAndEmail(String organizationId, String email);

    // Find by created by
    List<Contact> findByOrganizationIdAndCreatedBy(String organizationId, String createdBy);

    // Find by assigned to
    List<Contact> findByOrganizationIdAndAssignedTo(String organizationId, String assignedTo);

    // Find shared contacts in organization
    List<Contact> findByOrganizationIdAndIsShared(String organizationId, boolean isShared);

    

    // ========== SEARCH QUERIES ==========
    
    // Search by name (first name or last name)
    @Query("{ 'organizationId': ?0, $or: [ { 'firstName': { $regex: ?1, $options: 'i' } }, { 'lastName': { $regex: ?1, $options: 'i' } } ] }")
    List<Contact> findByOrganizationIdAndNameContaining(String organizationId, String name);

    // Find contacts created by OR assigned to a specific employee
    @Query("{ 'organizationId': ?0, $or: [ { 'createdBy': ?1 }, { 'assignedTo': ?1 } ] }")
    List<Contact> findByOrganizationIdAndCreatedByOrAssignedTo(String organizationId, String email);

    // ========== ACCESS CONTROL QUERY ==========
    
    /**
     * Find contacts that an employee can view:
     * - Created by them
     * - Assigned to them
     * - Shared contacts
     */
    @Query("{ 'organizationId': ?0, $or: [ { 'createdBy': ?1 }, { 'assignedTo': ?1 }, { 'isShared': true } ] }")
    List<Contact> findAccessibleContactsForEmployee(String organizationId, String email);

    // ========== ADDITIONAL QUERIES ==========
    
    // Find by company ID
    @Query("{ 'organizationId': ?0, 'companyId': ?1 }")
    List<Contact> findByOrganizationIdAndCompanyId(String organizationId, String companyId);
    
    // Find by company name
    @Query("{ 'organizationId': ?0, 'companyName': { $regex: ?1, $options: 'i' } }")
    List<Contact> findByOrganizationIdAndCompanyName(String organizationId, String companyName);
    

    
    // Find contactable contacts (not opted out)
    @Query("{ 'organizationId': ?0, 'doNotCall': false, 'doNotEmail': false }")
    List<Contact> findContactableContactsByOrganizationId(String organizationId);
    
    // ========== COUNT QUERIES ==========
    

    
    // Count by created by
    long countByOrganizationIdAndCreatedBy(String organizationId, String createdBy);
    
    // Count by assigned to
    long countByOrganizationIdAndAssignedTo(String organizationId, String assignedTo);
    
    // Count total contacts in organization
    long countByOrganizationId(String organizationId);

    List<Contact> findByOrganizationIdAndAssignedToOrOrganizationIdAndCreatedBy(
            String org1, String assignedTo,
            String org2, String createdBy
    );
}