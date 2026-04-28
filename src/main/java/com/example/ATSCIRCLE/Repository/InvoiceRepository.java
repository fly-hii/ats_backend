package com.example.ATSCIRCLE.Repository;
import com.example.ATSCIRCLE.Models.Invoice.Invoicemodel;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoicemodel, String> {
    
    // Find by organization
    List<Invoicemodel> findByOrganizationId(String organizationId);
    
    // Find by client
    List<Invoicemodel> findByClientId(String clientId);
    List<Invoicemodel> findByOrganizationIdAndClientId(String organizationId, String clientId);
    
    // Find by candidate
    List<Invoicemodel> findByCandidateId(String candidateId);
    Optional<Invoicemodel> findByOrganizationIdAndCandidateId(String organizationId, String candidateId);
    
    // Find by PO Number
    Optional<Invoicemodel> findByPoNumber(String poNumber);
    Optional<Invoicemodel> findByOrganizationIdAndPoNumber(String organizationId, String poNumber);
    
    // Find by date range
    List<Invoicemodel> findByJoiningDateBetween(LocalDate startDate, LocalDate endDate);
    List<Invoicemodel> findByOrganizationIdAndJoiningDateBetween(String organizationId, LocalDate startDate, LocalDate endDate);
    
    // Find by placement type
    List<Invoicemodel> findByPlacementType(String placementType);
    List<Invoicemodel> findByOrganizationIdAndPlacementType(String organizationId, String placementType);
    
    // Find by fee model
    List<Invoicemodel> findByFeeModel(String feeModel);
    
    // Find by created date
    List<Invoicemodel> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate);
    
    // Count queries
    long countByOrganizationId(String organizationId);
    long countByClientId(String clientId);
    long countByCandidateId(String candidateId);
}