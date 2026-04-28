package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Address;
import com.example.ATSCIRCLE.Models.Sales.Client;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums;
import com.example.ATSCIRCLE.Repository.ClientRepository;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * Migrate Company to Client with organizationId
     */
    public Client migrateCompanyToClient(
            String companyId,
            String role,
            String hiringManager,
            String pocName,
            String pocJobTitle,
            String pocEmail,
            String pocPhone,
            MultipartFile logo,
            MultipartFile primaryAgreement,
            MultipartFile workOrder,
            List<MultipartFile> supportingDocs,
            String organizationId,
            String createdBy
    ) throws IOException {
        
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new RuntimeException("Company not found with ID: " + companyId);
        }

        Company company = companyOpt.get();

        // Check if company belongs to same organization
        if (company.getOrganizationId() != null && !company.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("You don't have permission to migrate this company");
        }

        if (clientRepository.findByMigratedFromCompanyIdAndOrganizationId(companyId, organizationId).isPresent()) {
            throw new RuntimeException("Company already migrated to Client");
        }

        Client client = new Client();

        // Copy ALL fields from Company to Client
        client.setCompanyName(company.getCompanyName());
        client.setWebsiteUrl(company.getWebsiteUrl());
        client.setIndustry(company.getIndustry());
        client.setRelationshipType(company.getRelationshipType());
        client.setLeadStatus(company.getLeadStatus());
        client.setLeadSource(company.getLeadSource());
        client.setLinkedInUrl(company.getLinkedInUrl());
        client.setCreatedBy(createdBy);
        client.setCompanyStage(company.getCompanyStage());
        client.setLegalType(company.getLegalType());
        client.setEmployeeCount(company.getEmployeeCount());
        client.setAnnualRevenue(company.getAnnualRevenue());
        client.setPrimaryContact(company.getPrimaryContact());
        client.setJobTitle(company.getJobTitle());
        client.setEmail(company.getEmail());
        client.setPhone(company.getPhone());
        client.setContactOwner(company.getContactOwner());
        
        client.setBillingAddress(Address.copy(company.getBillingAddress()));
        client.setShippingAddress(Address.copy(company.getShippingAddress()));
        
        client.setSameAsBilling(company.isSameAsBilling());
        client.setTaxIdType(company.getTaxIdType());
        client.setTaxIdNumber(company.getTaxIdNumber());
        client.setPaymentTerms(company.getPaymentTerms());
        client.setCurrency(company.getCurrency());
        client.setCreatedAt(company.getCreatedAt());
        client.setLastActivity(LocalDateTime.now());
        client.setDoNotCall(company.isDoNotCall());
        client.setDoNotEmail(company.isDoNotEmail());

        // Set organizationId
        client.setOrganizationId(organizationId);

        // Set NEW Client-specific fields
        if (role == null || role.trim().isEmpty()) {
            client.setRole("CLIENT");
        } else {
            client.setRole(role);
        }
        client.setHiringManager(hiringManager);
        client.setPocName(pocName);
        client.setPocJobTitle(pocJobTitle);
        client.setPocEmail(pocEmail);
        client.setPocPhone(pocPhone);

        // Upload files to S3
        if (logo != null && !logo.isEmpty()) {
            String logoUrl = s3Service.uploadFile(logo, "client-logos");
            client.setClientCompanyLogoUrl(logoUrl);
        }

        if (primaryAgreement != null && !primaryAgreement.isEmpty()) {
            String agreementUrl = s3Service.uploadFile(primaryAgreement, "client-agreements");
            client.setPrimaryAgreementUrl(agreementUrl);
        }

        if (workOrder != null && !workOrder.isEmpty()) {
            String workOrderUrl = s3Service.uploadFile(workOrder, "client-workorders");
            client.setWorkOrderUrl(workOrderUrl);
        }

        if (supportingDocs != null && !supportingDocs.isEmpty()) {
            List<String> docUrls = new ArrayList<>();
            for (MultipartFile doc : supportingDocs) {
                if (doc != null && !doc.isEmpty()) {
                    String docUrl = s3Service.uploadFile(doc, "client-supporting-docs");
                    docUrls.add(docUrl);
                }
            }
            client.setSupportingDocumentUrls(docUrls);
        }

        client.setMigratedFromCompanyId(companyId);
        client.setMigratedAt(LocalDateTime.now());

        return clientRepository.save(client);
    }

    /**
     * Create client with file uploads and organizationId
     */
    public Client createClientWithFiles(
            String companyName,
            String websiteUrl,
            String industry,
            String relationshipType,
            String leadStatus,
            String leadSource,
            String linkedInUrl,
            String companyStage,
            String legalType,
            String employeeCount,
            String annualRevenue,
            String primaryContact,
            String jobTitle,
            String email,
            String phone,
            String contactOwner,
            String billingStreet,
            String billingCity,
            String billingState,
            String billingCountry,
            String billingZipCode,
            String shippingStreet,
            String shippingCity,
            String shippingState,
            String shippingCountry,
            String shippingZipCode,
            Boolean sameAsBilling,
            String taxIdType,
            String taxIdNumber,
            String paymentTerms,
            String currency,
            String role,
            String hiringManager,
            String pocName,
            String pocJobTitle,
            String pocEmail,
            String pocPhone,
            MultipartFile logo,
            MultipartFile primaryAgreement,
            MultipartFile workOrder,
            List<MultipartFile> supportingDocs,
            Boolean doNotCall,
            Boolean doNotEmail,
            String createdBy,
            String organizationId
    ) throws IOException {
        
        // Validation - check within organization
        if (clientRepository.existsByCompanyNameIgnoreCaseAndOrganizationId(companyName, organizationId)) {
            throw new RuntimeException("Client with name '" + companyName + "' already exists");
        }
        
        if (email != null && clientRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new RuntimeException("Client with email '" + email + "' already exists");
        }
        
        Client client = new Client();
        client.setCompanyName(companyName);
        client.setWebsiteUrl(websiteUrl);
        client.setLinkedInUrl(linkedInUrl);
        client.setPrimaryContact(primaryContact);
        client.setJobTitle(jobTitle);
        client.setEmail(email);
        client.setPhone(phone);
        client.setContactOwner(contactOwner);
        client.setTaxIdNumber(taxIdNumber);
        
        // Set organizationId
        client.setOrganizationId(organizationId);
        
        // Client-specific fields
        client.setRole(role != null ? role : "CLIENT");
        client.setHiringManager(hiringManager);
        client.setPocName(pocName);
        client.setPocJobTitle(pocJobTitle);
        client.setPocEmail(pocEmail);
        client.setPocPhone(pocPhone);
        
        // Convert enum strings
        setEnumFields(client, industry, relationshipType, leadStatus, leadSource, 
                     companyStage, legalType, employeeCount, annualRevenue, 
                     taxIdType, paymentTerms, currency);
        
        // Handle Addresses
        handleAddresses(client, billingStreet, billingCity, billingState, billingCountry, billingZipCode,
                       shippingStreet, shippingCity, shippingState, shippingCountry, shippingZipCode,
                       sameAsBilling);
        
        // Boolean flags
        if (doNotCall != null) client.setDoNotCall(doNotCall);
        if (doNotEmail != null) client.setDoNotEmail(doNotEmail);
        
        // Upload files to S3
        uploadFiles(client, logo, primaryAgreement, workOrder, supportingDocs);
        
        client.setCreatedBy(createdBy);
        client.setCreatedAt(LocalDateTime.now());
        client.setLastActivity(LocalDateTime.now());
        
        return clientRepository.save(client);
    }

    /**
     * Update client with organizationId check
     */
    public Client updateClientWithFiles(
            String id,
            String companyName,
            String websiteUrl,
            String industry,
            String relationshipType,
            String leadStatus,
            String leadSource,
            String linkedInUrl,
            String companyStage,
            String legalType,
            String employeeCount,
            String annualRevenue,
            String primaryContact,
            String jobTitle,
            String email,
            String phone,
            String contactOwner,
            String billingStreet,
            String billingCity,
            String billingState,
            String billingCountry,
            String billingZipCode,
            String shippingStreet,
            String shippingCity,
            String shippingState,
            String shippingCountry,
            String shippingZipCode,
            Boolean sameAsBilling,
            String taxIdType,
            String taxIdNumber,
            String paymentTerms,
            String currency,
            String role,
            String hiringManager,
            String pocName,
            String pocJobTitle,
            String pocEmail,
            String pocPhone,
            MultipartFile logo,
            MultipartFile primaryAgreement,
            MultipartFile workOrder,
            List<MultipartFile> supportingDocs,
            Boolean doNotCall,
            Boolean doNotEmail,
            Boolean deleteLogo,
            Boolean deletePrimaryAgreement,
            Boolean deleteWorkOrder,
            String organizationId
    ) throws IOException {
        
        Optional<Client> existingClient = clientRepository.findByIdAndOrganizationId(id, organizationId);
        
        if (!existingClient.isPresent()) {
            throw new RuntimeException("Client not found or you don't have permission");
        }
        
        Client client = existingClient.get();
        
        // Update basic fields
        if (companyName != null) client.setCompanyName(companyName);
        if (websiteUrl != null) client.setWebsiteUrl(websiteUrl);
        if (linkedInUrl != null) client.setLinkedInUrl(linkedInUrl);
        if (primaryContact != null) client.setPrimaryContact(primaryContact);
        if (jobTitle != null) client.setJobTitle(jobTitle);
        if (email != null) client.setEmail(email);
        if (phone != null) client.setPhone(phone);
        if (contactOwner != null) client.setContactOwner(contactOwner);
        if (taxIdNumber != null) client.setTaxIdNumber(taxIdNumber);
        
        // Update client-specific fields
        if (role != null) client.setRole(role);
        if (hiringManager != null) client.setHiringManager(hiringManager);
        if (pocName != null) client.setPocName(pocName);
        if (pocJobTitle != null) client.setPocJobTitle(pocJobTitle);
        if (pocEmail != null) client.setPocEmail(pocEmail);
        if (pocPhone != null) client.setPocPhone(pocPhone);
        
        // Update enums if provided
        if (industry != null || relationshipType != null || leadStatus != null || 
            leadSource != null || companyStage != null || legalType != null || 
            employeeCount != null || annualRevenue != null || taxIdType != null || 
            paymentTerms != null || currency != null) {
            setEnumFields(client, industry, relationshipType, leadStatus, leadSource,
                         companyStage, legalType, employeeCount, annualRevenue,
                         taxIdType, paymentTerms, currency);
        }
        
        // Update addresses if provided
        if (billingStreet != null || billingCity != null || billingState != null || 
            billingCountry != null || billingZipCode != null ||
            shippingStreet != null || shippingCity != null || shippingState != null || 
            shippingCountry != null || shippingZipCode != null || sameAsBilling != null) {
            handleAddresses(client, billingStreet, billingCity, billingState, billingCountry, billingZipCode,
                           shippingStreet, shippingCity, shippingState, shippingCountry, shippingZipCode,
                           sameAsBilling);
        }
        
        // Update boolean flags
        if (doNotCall != null) client.setDoNotCall(doNotCall);
        if (doNotEmail != null) client.setDoNotEmail(doNotEmail);
        
        // Handle file deletions
        if (deleteLogo != null && deleteLogo && client.getClientCompanyLogoUrl() != null) {
            s3Service.deleteFile(client.getClientCompanyLogoUrl());
            client.setClientCompanyLogoUrl(null);
        }
        
        if (deletePrimaryAgreement != null && deletePrimaryAgreement && client.getPrimaryAgreementUrl() != null) {
            s3Service.deleteFile(client.getPrimaryAgreementUrl());
            client.setPrimaryAgreementUrl(null);
        }
        
        if (deleteWorkOrder != null && deleteWorkOrder && client.getWorkOrderUrl() != null) {
            s3Service.deleteFile(client.getWorkOrderUrl());
            client.setWorkOrderUrl(null);
        }
        
        // Handle new file uploads
        if (logo != null && !logo.isEmpty()) {
            if (client.getClientCompanyLogoUrl() != null) {
                s3Service.deleteFile(client.getClientCompanyLogoUrl());
            }
            String logoUrl = s3Service.uploadFile(logo, "client-logos");
            client.setClientCompanyLogoUrl(logoUrl);
        }
        
        if (primaryAgreement != null && !primaryAgreement.isEmpty()) {
            if (client.getPrimaryAgreementUrl() != null) {
                s3Service.deleteFile(client.getPrimaryAgreementUrl());
            }
            String agreementUrl = s3Service.uploadFile(primaryAgreement, "client-agreements");
            client.setPrimaryAgreementUrl(agreementUrl);
        }
        
        if (workOrder != null && !workOrder.isEmpty()) {
            if (client.getWorkOrderUrl() != null) {
                s3Service.deleteFile(client.getWorkOrderUrl());
            }
            String workOrderUrl = s3Service.uploadFile(workOrder, "client-workorders");
            client.setWorkOrderUrl(workOrderUrl);
        }
        
        // Add new supporting documents
        if (supportingDocs != null && !supportingDocs.isEmpty()) {
            List<String> existingDocs = client.getSupportingDocumentUrls();
            if (existingDocs == null) {
                existingDocs = new ArrayList<>();
            }
            
            for (MultipartFile doc : supportingDocs) {
                if (doc != null && !doc.isEmpty()) {
                    String docUrl = s3Service.uploadFile(doc, "client-supporting-docs");
                    existingDocs.add(docUrl);
                }
            }
            client.setSupportingDocumentUrls(existingDocs);
        }
        
        client.setLastActivity(LocalDateTime.now());
        
        return clientRepository.save(client);
    }

    // ========== ORGANIZATION FILTERED METHODS ==========

    public List<Client> getAllClientsByOrganization(String organizationId) {
        return clientRepository.findByOrganizationId(organizationId);
    }

    public List<Client> getClientsByCreatedByAndOrganization(String createdBy, String organizationId) {
        return clientRepository.findByCreatedByAndOrganizationId(createdBy, organizationId);
    }

    public List<Client> getClientsAssignedToUser(String contactOwner, String organizationId) {
        return clientRepository.findByContactOwnerAndOrganizationId(contactOwner, organizationId);
    }

    public Optional<Client> getClientByIdAndOrganization(String id, String organizationId) {
        return clientRepository.findByIdAndOrganizationId(id, organizationId);
    }

    public Client updateClient(String id, Client updatedClient, String organizationId) {
        Optional<Client> existingClient = clientRepository.findByIdAndOrganizationId(id, organizationId);
        
        if (!existingClient.isPresent()) {
            throw new RuntimeException("Client not found or you don't have permission");
        }
        
        Client client = existingClient.get();
        
        if (updatedClient.getCompanyName() != null) client.setCompanyName(updatedClient.getCompanyName());
        if (updatedClient.getRole() != null) client.setRole(updatedClient.getRole());
        if (updatedClient.getHiringManager() != null) client.setHiringManager(updatedClient.getHiringManager());
        if (updatedClient.getPocName() != null) client.setPocName(updatedClient.getPocName());
        if (updatedClient.getPocEmail() != null) client.setPocEmail(updatedClient.getPocEmail());
        if (updatedClient.getPocPhone() != null) client.setPocPhone(updatedClient.getPocPhone());
        if (updatedClient.getEmail() != null) client.setEmail(updatedClient.getEmail());
        if (updatedClient.getPhone() != null) client.setPhone(updatedClient.getPhone());
        if (updatedClient.getBillingAddress() != null) client.setBillingAddress(updatedClient.getBillingAddress());
        if (updatedClient.getShippingAddress() != null) client.setShippingAddress(updatedClient.getShippingAddress());
        
        client.setLastActivity(LocalDateTime.now());
        
        return clientRepository.save(client);
    }

    public boolean deleteClient(String id, String organizationId) {
        Optional<Client> clientOpt = clientRepository.findByIdAndOrganizationId(id, organizationId);
        
        if (!clientOpt.isPresent()) {
            return false;
        }
        
        Client client = clientOpt.get();
        
        // Delete S3 files
        if (client.getClientCompanyLogoUrl() != null) {
            s3Service.deleteFile(client.getClientCompanyLogoUrl());
        }
        if (client.getPrimaryAgreementUrl() != null) {
            s3Service.deleteFile(client.getPrimaryAgreementUrl());
        }
        if (client.getWorkOrderUrl() != null) {
            s3Service.deleteFile(client.getWorkOrderUrl());
        }
        if (client.getSupportingDocumentUrls() != null) {
            for (String docUrl : client.getSupportingDocumentUrls()) {
                s3Service.deleteFile(docUrl);
            }
        }
        
        clientRepository.deleteById(id);
        return true;
    }

    public void deleteClientsByIds(List<String> ids, String organizationId) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No client IDs provided for deletion");
        }
        
        List<Client> clients = clientRepository.findByIdInAndOrganizationId(ids, organizationId);
        
        if (clients.isEmpty()) {
            throw new RuntimeException("No clients found with provided IDs in your organization");
        }
        
        for (Client client : clients) {
            if (client.getClientCompanyLogoUrl() != null) {
                s3Service.deleteFile(client.getClientCompanyLogoUrl());
            }
            if (client.getPrimaryAgreementUrl() != null) {
                s3Service.deleteFile(client.getPrimaryAgreementUrl());
            }
            if (client.getWorkOrderUrl() != null) {
                s3Service.deleteFile(client.getWorkOrderUrl());
            }
            if (client.getSupportingDocumentUrls() != null) {
                for (String docUrl : client.getSupportingDocumentUrls()) {
                    s3Service.deleteFile(docUrl);
                }
            }
        }
        
        clientRepository.deleteAll(clients);
    }

    public void updateContactOwner(List<String> ids, String contactOwner, String organizationId) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No client IDs provided for update");
        }
        if (contactOwner == null || contactOwner.trim().isEmpty()) {
            throw new RuntimeException("Contact owner cannot be empty");
        }
        
        List<Client> clients = clientRepository.findByIdInAndOrganizationId(ids, organizationId);
        if (clients.isEmpty()) {
            throw new RuntimeException("No clients found with provided IDs in your organization");
        }

        for (Client client : clients) {
            client.setContactOwner(contactOwner.trim());
            client.setLastActivity(LocalDateTime.now());
        }

        clientRepository.saveAll(clients);
    }

    public void updateClientsField(List<String> ids, String fieldName, Object fieldValue, String organizationId) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No client IDs provided for update");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new RuntimeException("Field name cannot be empty");
        }
        
        List<Client> clients = clientRepository.findByIdInAndOrganizationId(ids, organizationId);
        if (clients.isEmpty()) {
            throw new RuntimeException("No clients found with provided IDs in your organization");
        }

        for (Client client : clients) {
            try {
                Field field = Client.class.getDeclaredField(fieldName.trim());
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                Object valueToSet = fieldValue;

                if (fieldType.isEnum() && fieldValue instanceof String) {
                    valueToSet = Enum.valueOf((Class<Enum>) fieldType, (String) fieldValue);
                }

                field.set(client, valueToSet);
                client.setLastActivity(LocalDateTime.now());

            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Invalid field name: " + fieldName, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + fieldName, e);
            }
        }

        clientRepository.saveAll(clients);
    }

    public List<Client> searchClientsByName(String name, String organizationId) {
        return clientRepository.findByCompanyNameContainingAndOrganizationId(name, organizationId);
    }

    public Optional<Client> findByEmail(String email, String organizationId) {
        return clientRepository.findByEmailAndOrganizationId(email, organizationId);
    }

    public List<Client> getClientsByRole(String role, String organizationId) {
        return clientRepository.findByRoleAndOrganizationId(role, organizationId);
    }

    public long getTotalClientCount(String organizationId) {
        return clientRepository.countByOrganizationId(organizationId);
    }

    public boolean isCompanyAlreadyMigrated(String companyId, String organizationId) {
        return clientRepository.findByMigratedFromCompanyIdAndOrganizationId(companyId, organizationId).isPresent();
    }

    public void importExcel(MultipartFile file, String createdBy, String organizationId) throws IOException {
        List<Client> clients = ClientExcelHelper.parseExcel(file.getInputStream());
        
        for (Client client : clients) {
            if (client.getCompanyName() != null && 
                clientRepository.existsByCompanyNameIgnoreCaseAndOrganizationId(client.getCompanyName(), organizationId)) {
                throw new RuntimeException("Client with name '" + client.getCompanyName() + "' already exists");
            }
            if (client.getEmail() != null && 
                clientRepository.existsByEmailAndOrganizationId(client.getEmail(), organizationId)) {
                throw new RuntimeException("Client with email '" + client.getEmail() + "' already exists");
            }
            
            client.setCreatedBy(createdBy);
            client.setOrganizationId(organizationId);
            client.setCreatedAt(LocalDateTime.now());
            client.setLastActivity(LocalDateTime.now());
        }
        
        clientRepository.saveAll(clients);
    }

    public Client uploadClientLogo(String clientId, MultipartFile logo, String organizationId) throws IOException {
        Optional<Client> clientOpt = clientRepository.findByIdAndOrganizationId(clientId, organizationId);
        
        if (!clientOpt.isPresent()) {
            throw new RuntimeException("Client not found or you don't have permission");
        }
        
        Client client = clientOpt.get();
        
        if (client.getClientCompanyLogoUrl() != null) {
            s3Service.deleteFile(client.getClientCompanyLogoUrl());
        }
        
        String logoUrl = s3Service.uploadFile(logo, "client-logos");
        client.setClientCompanyLogoUrl(logoUrl);
        client.setLastActivity(LocalDateTime.now());
        
        return clientRepository.save(client);
    }

    public Client uploadSupportingDocuments(String clientId, List<MultipartFile> documents, String organizationId) throws IOException {
        Optional<Client> clientOpt = clientRepository.findByIdAndOrganizationId(clientId, organizationId);
        
        if (!clientOpt.isPresent()) {
            throw new RuntimeException("Client not found or you don't have permission");
        }
        
        Client client = clientOpt.get();
        List<String> docUrls = client.getSupportingDocumentUrls();
        
        if (docUrls == null) {
            docUrls = new ArrayList<>();
        }
        
        for (MultipartFile doc : documents) {
            if (doc != null && !doc.isEmpty()) {
                String docUrl = s3Service.uploadFile(doc, "client-supporting-docs");
                docUrls.add(docUrl);
            }
        }
        
        client.setSupportingDocumentUrls(docUrls);
        client.setLastActivity(LocalDateTime.now());
        
        return clientRepository.save(client);
    }

    // ========== HELPER METHODS ==========

    public List<String> getAllColumns() {
        Field[] fields = Client.class.getDeclaredFields();
        List<String> columns = new ArrayList<>();
        for (Field field : fields) {
            columns.add(field.getName());
        }
        return columns;
    }

    public ByteArrayInputStream generateExcelTemplate(List<String> selectedColumns) throws IOException {
        return ExcelHelper.createExcelTemplate(selectedColumns);
    }

    private void setEnumFields(Client client, String industry, String relationshipType, 
                              String leadStatus, String leadSource, String companyStage, 
                              String legalType, String employeeCount, String annualRevenue,
                              String taxIdType, String paymentTerms, String currency) {
        
        if (industry != null && !industry.trim().isEmpty()) {
            try {
                client.setIndustry(SalesEnums.Industry.valueOf(industry.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (relationshipType != null && !relationshipType.trim().isEmpty()) {
            try {
                client.setRelationshipType(SalesEnums.RelationshipType.valueOf(relationshipType.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (leadStatus != null && !leadStatus.trim().isEmpty()) {
            try {
                client.setLeadStatus(SalesEnums.LeadStatus.valueOf(leadStatus.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (leadSource != null && !leadSource.trim().isEmpty()) {
            try {
                client.setLeadSource(SalesEnums.LeadSource.valueOf(leadSource.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (companyStage != null && !companyStage.trim().isEmpty()) {
            try {
                client.setCompanyStage(SalesEnums.CompanyStage.valueOf(companyStage.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (legalType != null && !legalType.trim().isEmpty()) {
            try {
                client.setLegalType(SalesEnums.LegalType.valueOf(legalType.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (employeeCount != null && !employeeCount.trim().isEmpty()) {
            try {
                client.setEmployeeCount(SalesEnums.EmployeeCount.valueOf(employeeCount.toUpperCase().replace(" ", "_").replace("-", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (annualRevenue != null && !annualRevenue.trim().isEmpty()) {
            try {
                client.setAnnualRevenue(SalesEnums.AnnualRevenue.valueOf(annualRevenue.toUpperCase().replace(" ", "_").replace("-", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (taxIdType != null && !taxIdType.trim().isEmpty()) {
            try {
                client.setTaxIdType(SalesEnums.TaxIdType.valueOf(taxIdType.toUpperCase()));
            } catch (IllegalArgumentException e) { }
        }
        
        if (paymentTerms != null && !paymentTerms.trim().isEmpty()) {
            try {
                client.setPaymentTerms(SalesEnums.PaymentTerms.valueOf(paymentTerms.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) { }
        }
        
        if (currency != null && !currency.trim().isEmpty()) {
            try {
                client.setCurrency(SalesEnums.CurrencyType.valueOf(currency.toUpperCase()));
            } catch (IllegalArgumentException e) { }
        }
    }

    private void handleAddresses(Client client, String billingStreet, String billingCity, 
                                String billingState, String billingCountry, String billingZipCode,
                                String shippingStreet, String shippingCity, String shippingState, 
                                String shippingCountry, String shippingZipCode, Boolean sameAsBilling) {
        
        if (billingStreet != null || billingCity != null || billingState != null || 
            billingCountry != null || billingZipCode != null) {
            Address billing = client.getBillingAddress();
            if (billing == null) billing = new Address();
            if (billingStreet != null) billing.setStreetLine1(billingStreet);
            if (billingCity != null) billing.setCity(billingCity);
            if (billingState != null) billing.setState(billingState);
            if (billingCountry != null) billing.setCountry(billingCountry);
            if (billingZipCode != null) billing.setZipcode(billingZipCode);
            client.setBillingAddress(billing);
        }
        
        if (sameAsBilling != null && sameAsBilling) {
            client.setShippingAddress(Address.copy(client.getBillingAddress()));
            client.setSameAsBilling(true);
        } else if (shippingStreet != null || shippingCity != null || shippingState != null || 
                   shippingCountry != null || shippingZipCode != null) {
            Address shipping = client.getShippingAddress();
            if (shipping == null) shipping = new Address();
            if (shippingStreet != null) shipping.setStreetLine1(shippingStreet);
            if (shippingCity != null) shipping.setCity(shippingCity);
            if (shippingState != null) shipping.setState(shippingState);
            if (shippingCountry != null) shipping.setCountry(shippingCountry);
            if (shippingZipCode != null) shipping.setZipcode(shippingZipCode);
            client.setShippingAddress(shipping);
            client.setSameAsBilling(false);
        }
    }

    private void uploadFiles(Client client, MultipartFile logo, MultipartFile primaryAgreement,
                           MultipartFile workOrder, List<MultipartFile> supportingDocs) throws IOException {
        
        if (logo != null && !logo.isEmpty()) {
            String logoUrl = s3Service.uploadFile(logo, "client-logos");
            client.setClientCompanyLogoUrl(logoUrl);
        }
        
        if (primaryAgreement != null && !primaryAgreement.isEmpty()) {
            String agreementUrl = s3Service.uploadFile(primaryAgreement, "client-agreements");
            client.setPrimaryAgreementUrl(agreementUrl);
        }
        
        if (workOrder != null && !workOrder.isEmpty()) {
            String workOrderUrl = s3Service.uploadFile(workOrder, "client-workorders");
            client.setWorkOrderUrl(workOrderUrl);
        }
        
        if (supportingDocs != null && !supportingDocs.isEmpty()) {
            List<String> docUrls = new ArrayList<>();
            for (MultipartFile doc : supportingDocs) {
                if (doc != null && !doc.isEmpty()) {
                    String docUrl = s3Service.uploadFile(doc, "client-supporting-docs");
                    docUrls.add(docUrl);
                }
            }
            client.setSupportingDocumentUrls(docUrls);
        }
    }
}