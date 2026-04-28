package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GenericInvoicerepository extends MongoRepository<Invoice, String> {
    
    /**
     * Find all invoices by organization ID
     */
    List<Invoice> findByOrganizationId(String organizationId);
    
    /**
     * Find invoice by invoice number and organization
     */
    Optional<Invoice> findByOrganizationIdAndInvoiceNumber(
        String organizationId, String invoiceNumber);
    
    /**
     * Find invoices by customer name
     */
    List<Invoice> findByOrganizationIdAndCustomerNameContainingIgnoreCase(
        String organizationId, String customerName);
    
    /**
     * Find invoices by customer GSTIN
     */
    List<Invoice> findByOrganizationIdAndCustomerGSTIN(
        String organizationId, String customerGSTIN);
    
    /**
     * Find invoices by date range
     */
    List<Invoice> findByOrganizationIdAndInvoiceDateBetween(
        String organizationId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if invoice number exists for organization
     */
    boolean existsByOrganizationIdAndInvoiceNumber(
        String organizationId, String invoiceNumber);
    
    /**
     * Find invoices with reminders on specific date
     */
    List<Invoice> findByRemindersContaining(LocalDate reminderDate);
    
    /**
     * Count invoices by organization
     */
    long countByOrganizationId(String organizationId);
    
    /**
     * Delete all invoices for an organization
     */
    void deleteByOrganizationId(String organizationId);

    

    
}