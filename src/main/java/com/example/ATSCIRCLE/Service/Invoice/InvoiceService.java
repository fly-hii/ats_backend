package com.example.ATSCIRCLE.Service.Invoice;

import com.example.ATSCIRCLE.Models.ATS.CandidateApplication;
import com.example.ATSCIRCLE.Models.Invoice.Invoicemodel;
import com.example.ATSCIRCLE.Models.Sales.Client;
import com.example.ATSCIRCLE.Repository.Candidateapplicationrepository;
import com.example.ATSCIRCLE.Repository.ClientRepository;
import com.example.ATSCIRCLE.Repository.InvoiceRepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.Models.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository placementOrderRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private Candidateapplicationrepository candidateApplicationRepository;

    @Autowired
    private UserRepository usersRepository;

    /**
     * Create new placement order - Frontend nundi complete data vasthundi
     * After saving, update related tables if data is missing
     */
    @Transactional
    public Invoicemodel createPlacementOrder(Invoicemodel placementOrder, String organizationId) {
        placementOrder.setOrganizationId(organizationId);
        placementOrder.prePersist();
        
        // First save the placement order with all data from frontend
        Invoicemodel savedOrder = placementOrderRepository.save(placementOrder);
        
        // Now update related tables if they have missing data
        updateClientIfNeeded(savedOrder);
        updateCandidateIfNeeded(savedOrder);
        updateUsersIfNeeded(savedOrder, organizationId);
        
        return savedOrder;
    }

    /**
     * Update Client table - only if client has missing data
     */
    private void updateClientIfNeeded(Invoicemodel placementOrder) {
        if (placementOrder.getClientId() == null) return;
        
        Optional<Client> clientOpt = clientRepository.findById(placementOrder.getClientId());
        if (!clientOpt.isPresent()) return;
        
        Client client = clientOpt.get();
        boolean needsUpdate = false;
        
        // Update only if client doesn't have the data
        if (client.getEmail() == null && placementOrder.getClientEmail() != null) {
            client.setEmail(placementOrder.getClientEmail());
            needsUpdate = true;
        }
        
        if (client.getTaxIdNumber() == null && placementOrder.getClientGSTIN() != null) {
            client.setTaxIdNumber(placementOrder.getClientGSTIN());
            needsUpdate = true;
        }
        
        if (client.taxStatus() == null && placementOrder.getGstStatus() != null) {
            client.settaxStatus(placementOrder.getGstStatus());
            needsUpdate = true;
        }
        
        if (client.getPaymentTerms() == null && placementOrder.getPaymentTerms() != null) {
            try {
                client.setPaymentTerms(com.example.ATSCIRCLE.Models.Sales.SalesEnums.PaymentTerms.valueOf(placementOrder.getPaymentTerms()));
                needsUpdate = true;
            } catch (Exception e) {
                // Invalid payment terms format
            }
        }
        
        if (client.getBillingAddress() == null && placementOrder.getBillingAddress() != null) {
            client.setBillingAddress(convertToClientAddress(placementOrder.getBillingAddress()));
            needsUpdate = true;
        }
        
        if (client.getShippingAddress() == null && placementOrder.getShippingAddress() != null) {
            client.setShippingAddress(convertToClientAddress(placementOrder.getShippingAddress()));
            needsUpdate = true;
        }
        
        if (client.getBillingAddress() != null && !client.isSameAsBilling() && placementOrder.getSameAsBilling() != null) {
            client.setSameAsBilling(placementOrder.getSameAsBilling());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            clientRepository.save(client);
        }
    }

    /**
     * Update CandidateApplication - only if candidate has missing data
     */
    private void updateCandidateIfNeeded(Invoicemodel placementOrder) {
        if (placementOrder.getCandidateId() == null) return;
        
        Optional<CandidateApplication> candidateOpt = candidateApplicationRepository.findById(placementOrder.getCandidateId());
        if (!candidateOpt.isPresent()) return;
        
        CandidateApplication candidate = candidateOpt.get();
        boolean needsUpdate = false;
        
        // Update only if candidate doesn't have the data
        if (candidate.getgivenCTC() == null && placementOrder.getCtc() != null) {
            candidate.setgivenCTC(String.valueOf(placementOrder.getCtc()));
            needsUpdate = true;
        }
        
        if (candidate.getactualjoiningdate() == null && placementOrder.getJoiningDate() != null) {
            candidate.setactualjoiningdate(placementOrder.getJoiningDate().toString());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            candidateApplicationRepository.save(candidate);
        }
    }

    /**
     * Update Users table - only if user has missing data
     */
    private void updateUsersIfNeeded(Invoicemodel placementOrder, String organizationId) {
        Optional<Users> userOpt = usersRepository.findById(organizationId);
        if (!userOpt.isPresent()) return;
        
        Users user = userOpt.get();
        boolean needsUpdate = false;
        
        // Update only if user doesn't have the data
        if (user.getGSTnumber() == null && placementOrder.getYourGSTIN() != null) {
            user.setGSTnumber(placementOrder.getYourGSTIN());
            needsUpdate = true;
        }
        
        if (user.getPannumber() == null && placementOrder.getYourPan() != null) {
            user.setPannumber(placementOrder.getYourPan());
            needsUpdate = true;
        }
        
        if (user.getAccountno() == null && placementOrder.getAccountNumber() != null) {
            user.setAccountno(placementOrder.getAccountNumber());
            needsUpdate = true;
        }
        
        if (user.getIfsccode() == null && placementOrder.getIfscCode() != null) {
            user.setIfsccode(placementOrder.getIfscCode());
            needsUpdate = true;
        }
        
        // Update address fields if missing
        if (placementOrder.getFromAddress() != null) {
            Invoicemodel.Address fromAddr = placementOrder.getFromAddress();
            
            if (user.getStreetLine1() == null && fromAddr.getStreetLine1() != null) {
                user.setStreetLine1(fromAddr.getStreetLine1());
                needsUpdate = true;
            }
            if (user.getStreetLine2() == null && fromAddr.getStreetLine2() != null) {
                user.setStreetLine2(fromAddr.getStreetLine2());
                needsUpdate = true;
            }
            if (user.getCity() == null && fromAddr.getCity() != null) {
                user.setCity(fromAddr.getCity());
                needsUpdate = true;
            }
            if (user.getState() == null && fromAddr.getState() != null) {
                user.setState(fromAddr.getState());
                needsUpdate = true;
            }
            if (user.getCountry() == null && fromAddr.getCountry() != null) {
                user.setCountry(fromAddr.getCountry());
                needsUpdate = true;
            }
            if (user.getZipcode() == null && fromAddr.getZipcode() != null) {
                user.setZipcode(fromAddr.getZipcode());
                needsUpdate = true;
            }
        }
        
        if (needsUpdate) {
            usersRepository.save(user);
        }
    }

    /**
     * Get all placement orders for an organization
     */
    public List<Invoicemodel> getAllPlacementOrders(String organizationId) {
        return placementOrderRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get placement order by ID
     */
    public Optional<Invoicemodel> getPlacementOrderById(String id) {
        return placementOrderRepository.findById(id);
    }

    /**
     * Get placement orders by client
     */
    public List<Invoicemodel> getPlacementOrdersByClient(String organizationId, String clientId) {
        return placementOrderRepository.findByOrganizationIdAndClientId(organizationId, clientId);
    }

    /**
     * Get placement order by candidate
     */
    public Optional<Invoicemodel> getPlacementOrderByCandidate(String organizationId, String candidateId) {
        return placementOrderRepository.findByOrganizationIdAndCandidateId(organizationId, candidateId);
    }

    /**
     * Update placement order - Frontend nundi complete updated data vasthundi
     */
    @Transactional
    public Invoicemodel updatePlacementOrder(String id, Invoicemodel updatedOrder) {
        Optional<Invoicemodel> existingOpt = placementOrderRepository.findById(id);
        
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("Placement Order not found with id: " + id);
        }
        
        Invoicemodel existing = existingOpt.get();
        
        // Update all fields from frontend
        if (updatedOrder.getClientId() != null) existing.setClientId(updatedOrder.getClientId());
        if (updatedOrder.getClientName() != null) existing.setClientName(updatedOrder.getClientName());
        if (updatedOrder.getClientEmail() != null) existing.setClientEmail(updatedOrder.getClientEmail());
        if (updatedOrder.getPoNumber() != null) existing.setPoNumber(updatedOrder.getPoNumber());
        if (updatedOrder.getClientGSTIN() != null) existing.setClientGSTIN(updatedOrder.getClientGSTIN());
        if (updatedOrder.getGstStatus() != null) existing.setGstStatus(updatedOrder.getGstStatus());
        if (updatedOrder.getPaymentTerms() != null) existing.setPaymentTerms(updatedOrder.getPaymentTerms());
        if (updatedOrder.getBillingAddress() != null) existing.setBillingAddress(updatedOrder.getBillingAddress());
        if (updatedOrder.getSameAsBilling() != null) existing.setSameAsBilling(updatedOrder.getSameAsBilling());
        if (updatedOrder.getShippingAddress() != null) existing.setShippingAddress(updatedOrder.getShippingAddress());
        
        if (updatedOrder.getCandidateId() != null) existing.setCandidateId(updatedOrder.getCandidateId());
        if (updatedOrder.getCandidateName() != null) existing.setCandidateName(updatedOrder.getCandidateName());
        if (updatedOrder.getDesignation() != null) existing.setDesignation(updatedOrder.getDesignation());
        if (updatedOrder.getPlacementType() != null) existing.setPlacementType(updatedOrder.getPlacementType());
        if (updatedOrder.getCtc() != null) existing.setCtc(updatedOrder.getCtc());
        if (updatedOrder.getJoiningDate() != null) existing.setJoiningDate(updatedOrder.getJoiningDate());
        
        if (updatedOrder.getFeeModel() != null) existing.setFeeModel(updatedOrder.getFeeModel());
        if (updatedOrder.getFeePercentage() != null) existing.setFeePercentage(updatedOrder.getFeePercentage());
        if (updatedOrder.getFixedAmount() != null) existing.setFixedAmount(updatedOrder.getFixedAmount());
        if (updatedOrder.getCalculatedFee() != null) existing.setCalculatedFee(updatedOrder.getCalculatedFee());
        if (updatedOrder.getGuaranteeDays() != null) existing.setGuaranteeDays(updatedOrder.getGuaranteeDays());
        if (updatedOrder.getGuaranteeEndDate() != null) existing.setGuaranteeEndDate(updatedOrder.getGuaranteeEndDate());
        if (updatedOrder.getIfCandidateLeaves() != null) existing.setIfCandidateLeaves(updatedOrder.getIfCandidateLeaves());
        if (updatedOrder.getExitPenalty() != null) existing.setExitPenalty(updatedOrder.getExitPenalty());
        
        if (updatedOrder.getYourGSTIN() != null) existing.setYourGSTIN(updatedOrder.getYourGSTIN());
        if (updatedOrder.getYourPan() != null) existing.setYourPan(updatedOrder.getYourPan());
        if (updatedOrder.getAccountNumber() != null) existing.setAccountNumber(updatedOrder.getAccountNumber());
        if (updatedOrder.getIfscCode() != null) existing.setIfscCode(updatedOrder.getIfscCode());
        if (updatedOrder.getFromAddress() != null) existing.setFromAddress(updatedOrder.getFromAddress());
        
        if (updatedOrder.getTDSApplicable() != null) existing.setTDSApplicable(updatedOrder.getTDSApplicable());
        if (updatedOrder.getDocuments() != null) existing.setDocuments(updatedOrder.getDocuments());
        if (updatedOrder.getAutomaticPaymentReminders() != null) existing.setAutomaticPaymentReminders(updatedOrder.getAutomaticPaymentReminders());
        if (updatedOrder.getSendInvoiceMailTo() != null) existing.setSendInvoiceMailTo(updatedOrder.getSendInvoiceMailTo());
        
        // ✅ CRITICAL: Copy updatedBy field from controller-set value
        if (updatedOrder.getUpdatedBy() != null) {
            existing.setUpdatedBy(updatedOrder.getUpdatedBy());
            System.out.println("✅ Service layer: Setting updatedBy to: " + updatedOrder.getUpdatedBy());
        }
        existing.preUpdate();
        
        // Save placement order first
        Invoicemodel savedOrder = placementOrderRepository.save(existing);
        
        // Then update related tables if needed
        updateClientIfNeeded(savedOrder);
        updateCandidateIfNeeded(savedOrder);
        updateUsersIfNeeded(savedOrder, savedOrder.getOrganizationId());
        
        return savedOrder;
    }

    /**
     * Delete placement order
     */
    public void deletePlacementOrder(String id) {
        placementOrderRepository.deleteById(id);
    }

    /**
     * Get placement orders by date range
     */
    public List<Invoicemodel> getPlacementOrdersByDateRange(String organizationId, LocalDate startDate, LocalDate endDate) {
        return placementOrderRepository.findByOrganizationIdAndJoiningDateBetween(organizationId, startDate, endDate);
    }

    /**
     * Get placement orders by placement type
     */
    public List<Invoicemodel> getPlacementOrdersByType(String organizationId, String placementType) {
        return placementOrderRepository.findByOrganizationIdAndPlacementType(organizationId, placementType);
    }

    /**
     * Helper method to convert Invoice Address to Client Address
     * Handles zipcode vs zipCode field name difference
     */
    private com.example.ATSCIRCLE.Models.Sales.Address convertToClientAddress(Invoicemodel.Address invoiceAddress) {
        if (invoiceAddress == null) return null;
        
        com.example.ATSCIRCLE.Models.Sales.Address clientAddress = new com.example.ATSCIRCLE.Models.Sales.Address();
        clientAddress.setStreetLine1(invoiceAddress.getStreetLine1());
        clientAddress.setStreetLine2(invoiceAddress.getStreetLine2());
        clientAddress.setCity(invoiceAddress.getCity());
        clientAddress.setState(invoiceAddress.getState());
        clientAddress.setCountry(invoiceAddress.getCountry());
        // Convert zipcode to zipCode (note the capital C)
        clientAddress.setZipcode(invoiceAddress.getZipcode());
        return clientAddress;
    }

    public Optional<Invoicemodel> getInvoiceForEmail(String id) {
    return placementOrderRepository.findById(id);
}

   
}