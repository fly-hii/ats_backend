package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Address;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Contact;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.CompanyStage;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.Industry;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LeadSource;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.LeadStatus;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums.RelationshipType;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import com.example.ATSCIRCLE.Repository.ContactRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.ATSCIRCLE.Service.Sales.ExcelHelper;

import static com.example.ATSCIRCLE.Models.Sales.SalesEnums.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.lang.reflect.Field;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    // ========== ENHANCED UPDATE METHOD WITH ORGANIZATION CONTEXT ==========
    
    public Company updateCompany(String id, Company updatedCompany, String organizationId, String userType) {
    Optional<Company> existingCompany = companyRepository.findById(id);
    
    if (!existingCompany.isPresent()) {
        throw new RuntimeException("Company not found with id: " + id);
    }
    
    Company company = existingCompany.get();
    
    if (!company.getOrganizationId().equals(organizationId)) {
        throw new RuntimeException("Access denied: Company belongs to different organization");
    }
    
    if (updatedCompany.getCompanyName() != null && 
        !updatedCompany.getCompanyName().equals(company.getCompanyName())) {
        if (companyRepository.existsByCompanyNameIgnoreCaseAndOrganizationId(
                updatedCompany.getCompanyName(), organizationId)) {
            throw new RuntimeException("Company with name '" + updatedCompany.getCompanyName() + "' already exists in your organization");
        }
    }
    
    if (updatedCompany.getEmail() != null && 
        !updatedCompany.getEmail().equals(company.getEmail())) {
        if (companyRepository.existsByEmailAndOrganizationId(updatedCompany.getEmail(), organizationId)) {
            throw new RuntimeException("Company with email '" + updatedCompany.getEmail() + "' already exists in your organization");
        }
    }
    
    // Basic fields
    if (updatedCompany.getCompanyName() != null) company.setCompanyName(updatedCompany.getCompanyName());
    if (updatedCompany.getWebsiteUrl() != null) company.setWebsiteUrl(updatedCompany.getWebsiteUrl());
    if (updatedCompany.getIndustry() != null) company.setIndustry(updatedCompany.getIndustry());
    if (updatedCompany.getRelationshipType() != null) company.setRelationshipType(updatedCompany.getRelationshipType());
    if (updatedCompany.getLeadStatus() != null) company.setLeadStatus(updatedCompany.getLeadStatus());
    if (updatedCompany.getLeadSource() != null) company.setLeadSource(updatedCompany.getLeadSource());
    if (updatedCompany.getLinkedInUrl() != null) company.setLinkedInUrl(updatedCompany.getLinkedInUrl());
    if (updatedCompany.getCompanyStage() != null) company.setCompanyStage(updatedCompany.getCompanyStage());
    if (updatedCompany.getLegalType() != null) company.setLegalType(updatedCompany.getLegalType());
    if (updatedCompany.getEmployeeCount() != null) company.setEmployeeCount(updatedCompany.getEmployeeCount());
    if (updatedCompany.getAnnualRevenue() != null) company.setAnnualRevenue(updatedCompany.getAnnualRevenue());
    if (updatedCompany.getPrimaryContact() != null) company.setPrimaryContact(updatedCompany.getPrimaryContact());
    if (updatedCompany.getJobTitle() != null) company.setJobTitle(updatedCompany.getJobTitle());
    if (updatedCompany.getEmail() != null) company.setEmail(updatedCompany.getEmail());
    if (updatedCompany.getPhone() != null) company.setPhone(updatedCompany.getPhone());
    if (updatedCompany.getContactOwner() != null) company.setContactOwner(updatedCompany.getContactOwner());
    if (updatedCompany.getNotes() != null) company.setNotes(updatedCompany.getNotes());
    if (updatedCompany.getTaxIdType() != null) company.setTaxIdType(updatedCompany.getTaxIdType());
    if (updatedCompany.getTaxIdNumber() != null) company.setTaxIdNumber(updatedCompany.getTaxIdNumber());
    if (updatedCompany.getPaymentTerms() != null) company.setPaymentTerms(updatedCompany.getPaymentTerms());
    if (updatedCompany.getCurrency() != null) company.setCurrency(updatedCompany.getCurrency());
    if (updatedCompany.getAssignedTo() != null) company.setAssignedTo(updatedCompany.getAssignedTo());

    // Address handling
    if (updatedCompany.getBillingAddress() != null) {
        Address billing = company.getBillingAddress();
        if (billing == null) billing = new Address();
        Address updatedBilling = updatedCompany.getBillingAddress();
        if (updatedBilling.getStreetLine1() != null) billing.setStreetLine1(updatedBilling.getStreetLine1());
        if (updatedBilling.getStreetLine2() != null) billing.setStreetLine2(updatedBilling.getStreetLine2());
        if (updatedBilling.getCity() != null) billing.setCity(updatedBilling.getCity());
        if (updatedBilling.getState() != null) billing.setState(updatedBilling.getState());
        if (updatedBilling.getCountry() != null) billing.setCountry(updatedBilling.getCountry());
        if (updatedBilling.getZipcode() != null) billing.setZipcode(updatedBilling.getZipcode());
        company.setBillingAddress(billing);
    }

    if (updatedCompany.isSameAsBilling()) {
        company.setShippingAddress(Address.copy(company.getBillingAddress()));
        company.setSameAsBilling(true);
    } else if (updatedCompany.getShippingAddress() != null) {
        Address shipping = company.getShippingAddress();
        if (shipping == null) shipping = new Address();
        Address updatedShipping = updatedCompany.getShippingAddress();
        if (updatedShipping.getStreetLine1() != null) shipping.setStreetLine1(updatedShipping.getStreetLine1());
        if (updatedShipping.getStreetLine2() != null) shipping.setStreetLine2(updatedShipping.getStreetLine2());
        if (updatedShipping.getCity() != null) shipping.setCity(updatedShipping.getCity());
        if (updatedShipping.getState() != null) shipping.setState(updatedShipping.getState());
        if (updatedShipping.getCountry() != null) shipping.setCountry(updatedShipping.getCountry());
        if (updatedShipping.getZipcode() != null) shipping.setZipcode(updatedShipping.getZipcode());
        company.setShippingAddress(shipping);
        company.setSameAsBilling(false);
    }

    company.setDoNotCall(updatedCompany.isDoNotCall());
    company.setDoNotEmail(updatedCompany.isDoNotEmail());

    // ---- Contact IDs relationship sync ----
    if (updatedCompany.getContactIds() != null) {
        List<String> oldIds = company.getContactIds() != null ? company.getContactIds() : new ArrayList<>();
        List<String> newIds = updatedCompany.getContactIds();

        // Contacts removed from this company
        List<String> removed = new ArrayList<>(oldIds);
        removed.removeAll(newIds);
        if (!removed.isEmpty()) {
            List<Contact> removedContacts = contactRepository.findAllById(removed);
            for (Contact c : removedContacts) {
                c.setCompanyId(null);
                c.setCompanyName(null);
                c.setUpdatedAt(LocalDateTime.now());
            }
            contactRepository.saveAll(removedContacts);
        }

        // Contacts newly added to this company
        List<String> added = new ArrayList<>(newIds);
        added.removeAll(oldIds);
        if (!added.isEmpty()) {
            List<Contact> addedContacts = contactRepository.findAllById(added);
            for (Contact c : addedContacts) {
                if (c.getOrganizationId().equals(organizationId)) {
                    c.setCompanyId(company.getId());
                    c.setCompanyName(company.getCompanyName() != null
                            ? company.getCompanyName()
                            : updatedCompany.getCompanyName());
                    c.setUpdatedAt(LocalDateTime.now());
                }
            }
            contactRepository.saveAll(addedContacts);
        }

        company.setContactIds(newIds);
    }

    company.setLastActivity(LocalDateTime.now());

    return companyRepository.save(company);
}
    // ========== CREATE METHOD WITH ORGANIZATION CONTEXT ==========
    
    public Company createCompany(Company company, String organizationId, String createdBy) {
        // Set organization context
        company.setOrganizationId(organizationId);
        company.setCreatedBy(createdBy);
        
        if (companyRepository.existsByCompanyNameIgnoreCaseAndOrganizationId(
                company.getCompanyName(), organizationId)) {
            throw new RuntimeException("Company with name '" + company.getCompanyName() + "' already exists in your organization");
        }
        
        if (company.getEmail() != null && companyRepository.existsByEmailAndOrganizationId(
                company.getEmail(), organizationId)) {
            throw new RuntimeException("Company with email '" + company.getEmail() + "' already exists in your organization");
        }
        
        // ✅ Handle sameAsBilling during creation
        if (company.isSameAsBilling() && company.getBillingAddress() != null) {
            company.setShippingAddress(Address.copy(company.getBillingAddress()));
        }
        
        company.setCreatedAt(LocalDateTime.now());
        company.setLastActivity(LocalDateTime.now());
        
       Company saved = companyRepository.save(company);

if (company.getContactIds() != null && !company.getContactIds().isEmpty()) {
    List<Contact> contacts = contactRepository.findAllById(company.getContactIds());
    for (Contact c : contacts) {
        if (c.getOrganizationId().equals(organizationId)) {
            c.setCompanyId(saved.getId());
            c.setCompanyName(saved.getCompanyName());
        }
    }
    contactRepository.saveAll(contacts);
}

return saved;
    }

    // ========== ORGANIZATION-SCOPED METHODS ==========

    public List<Company> getAllCompanies(String organizationId) {
        return companyRepository.findByOrganizationId(organizationId);
    }

    public Page<Company> getAllCompanies(String organizationId, Pageable pageable) {
        return companyRepository.findByOrganizationId(organizationId, pageable);
    }

    public Optional<Company> getCompanyById(String id, String organizationId) {
        Optional<Company> company = companyRepository.findById(id);
        if (company.isPresent() && !company.get().getOrganizationId().equals(organizationId)) {
            return Optional.empty(); // Hide companies from other organizations
        }
        return company;
    }

    // public boolean deleteCompany(String id, String organizationId, String userType) {
    //     // Only ADMIN can delete
    //     if (!"ADMIN".equals(userType)) {
    //         throw new RuntimeException("Access denied: Only administrators can delete companies");
    //     }
        
    //     Optional<Company> company = companyRepository.findById(id);
    //     if (company.isPresent()) {
    //         if (!company.get().getOrganizationId().equals(organizationId)) {
    //             throw new RuntimeException("Access denied: Company belongs to different organization");
    //         }
    //         companyRepository.deleteById(id);
    //         return true;
    //     }
    //     return false;
    // }

    public Optional<Company> findByName(String companyName, String organizationId) {
        return companyRepository.findByCompanyNameIgnoreCaseAndOrganizationId(companyName, organizationId);
    }

    public List<Company> searchCompaniesByName(String searchTerm, String organizationId) {
        return companyRepository.findByCompanyNameContainingAndOrganizationId(searchTerm, organizationId);
    }

    public List<Company> getCompaniesByIndustry(Industry industry, String organizationId) {
        return companyRepository.findByIndustryAndOrganizationId(industry, organizationId);
    }

    public Page<Company> getCompaniesByIndustry(Industry industry, String organizationId, Pageable pageable) {
        return companyRepository.findByIndustryAndOrganizationId(industry, organizationId, pageable);
    }

    public List<Company> getCompaniesByRelationshipType(RelationshipType relationshipType, String organizationId) {
        return companyRepository.findByRelationshipTypeAndOrganizationId(relationshipType, organizationId);
    }

    public List<Company> getCompaniesByLeadStatus(LeadStatus leadStatus, String organizationId) {
        return companyRepository.findByLeadStatusAndOrganizationId(leadStatus, organizationId);
    }

    public List<Company> getCompaniesByLeadSource(LeadSource leadSource, String organizationId) {
        return companyRepository.findByLeadSourceAndOrganizationId(leadSource, organizationId);
    }

    public List<Company> getCompaniesByStage(CompanyStage companyStage, String organizationId) {
        return companyRepository.findByCompanyStageAndOrganizationId(companyStage, organizationId);
    }

     public List<Company> getCompaniesByCreatorOrAssignee(String userId, String organizationId) {
        return companyRepository.findByCreatedByOrAssignedTo(organizationId, userId);
    }

    public List<Company> getCompaniesByDateRange(LocalDateTime startDate, LocalDateTime endDate, String organizationId) {
        return companyRepository.findByCreatedAtBetweenAndOrganizationId(startDate, endDate, organizationId);
    }

    public Optional<Company> findByEmail(String email, String organizationId) {
        return companyRepository.findByEmailAndOrganizationId(email, organizationId);
    }

    public Optional<Company> findByPhone(String phone, String organizationId) {
        return companyRepository.findByPhoneAndOrganizationId(phone, organizationId);
    }

    public List<Company> searchCompanies(String searchTerm, Industry industry, LeadStatus leadStatus, String organizationId) {
        return companyRepository.findByNameOrEmailAndIndustryAndLeadStatusAndOrganizationId(
            searchTerm, industry, leadStatus, organizationId);
    }

    public List<Company> getCompaniesByCity(String city, String organizationId) {
        return companyRepository.findByCityAndOrganizationId(city, organizationId);
    }

    public List<Company> getCompaniesByState(String state, String organizationId) {
        return companyRepository.findByStateAndOrganizationId(state, organizationId);
    }

    public List<Company> getCompaniesByCountry(String country, String organizationId) {
        return companyRepository.findByCountryAndOrganizationId(country, organizationId);
    }

    public List<Company> getContactableCompanies(String organizationId) {
        return companyRepository.findContactableCompaniesByOrganizationId(organizationId);
    }

    public List<Company> getCompaniesWithRecentActivity(int days, String organizationId) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return companyRepository.findByLastActivityAfterAndOrganizationId(cutoffDate, organizationId);
    }

    public long getCompanyCountByIndustry(Industry industry, String organizationId) {
        return companyRepository.countByIndustryAndOrganizationId(industry, organizationId);
    }

    public long getCompanyCountByLeadStatus(LeadStatus leadStatus, String organizationId) {
        return companyRepository.countByLeadStatusAndOrganizationId(leadStatus, organizationId);
    }

    public long getTotalCompanyCount(String organizationId) {
        return companyRepository.countByOrganizationId(organizationId);
    }

    public boolean existsByName(String companyName, String organizationId) {
        return companyRepository.existsByCompanyNameIgnoreCaseAndOrganizationId(companyName, organizationId);
    }

    public boolean existsByEmail(String email, String organizationId) {
        return companyRepository.existsByEmailAndOrganizationId(email, organizationId);
    }

    public void updateLastActivity(String companyId, String organizationId) {
        Optional<Company> company = companyRepository.findById(companyId);
        if (company.isPresent()) {
            Company comp = company.get();
            if (!comp.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Company belongs to different organization");
            }
            comp.setLastActivity(LocalDateTime.now());
            companyRepository.save(comp);
        }
    }

    public List<String> getAllColumns() {
        Field[] fields = Company.class.getDeclaredFields();
        List<String> columns = new ArrayList<>();
        for (Field field : fields) {
            columns.add(field.getName());
        }
        return columns;
    }

    public ByteArrayInputStream generateExcelTemplate(List<String> selectedColumns) throws IOException {
        return ExcelHelper.createExcelTemplate(selectedColumns);
    }

    public void importExcel(MultipartFile file, String organizationId, String createdBy) throws IOException {
        List<Company> companies = ExcelHelper.parseExcel(file.getInputStream());
        
        for (Company company : companies) {
            // Set organization context
            company.setOrganizationId(organizationId);
            company.setCreatedBy(createdBy);
            
            if (company.getCompanyName() != null && existsByName(company.getCompanyName(), organizationId)) {
                throw new RuntimeException("Company with name '" + company.getCompanyName() + "' already exists in your organization");
            }
            if (company.getEmail() != null && existsByEmail(company.getEmail(), organizationId)) {
                throw new RuntimeException("Company with email '" + company.getEmail() + "' already exists in your organization");
            }
        }
        
        companyRepository.saveAll(companies);
    }

    // public void deleteCompaniesByIds(List<String> ids, String organizationId, String userType) {
    //     // Only ADMIN can delete
    //     if (!"ADMIN".equals(userType)) {
    //         throw new RuntimeException("Access denied: Only administrators can delete companies");
    //     }
        
    //     if (ids == null || ids.isEmpty()) {
    //         throw new RuntimeException("No company IDs provided for deletion");
    //     }
        
    //     List<Company> companies = companyRepository.findAllById(ids);
    //     for (Company company : companies) {
    //         if (!company.getOrganizationId().equals(organizationId)) {
    //             throw new RuntimeException("Access denied: Cannot delete companies from other organizations");
    //         }
    //     }
        
    //     companyRepository.deleteAllById(ids);
    // }


public boolean deleteCompany(String id, String organizationId, String userType) {
   
    
    Optional<Company> company = companyRepository.findById(id);
    if (company.isPresent()) {
        if (!company.get().getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Company belongs to different organization");
        }
        companyRepository.deleteById(id);
        return true;
    }
    return false;
}



public void deleteCompaniesByIds(List<String> ids, String organizationId, String userType) {
   
    
    if (ids == null || ids.isEmpty()) {
        throw new RuntimeException("No company IDs provided for deletion");
    }
    
    List<Company> companies = companyRepository.findAllById(ids);
    for (Company company : companies) {
        if (!company.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Cannot delete companies from other organizations");
        }
    }
    
    companyRepository.deleteAllById(ids);
}
    public void updateContactOwner(List<String> ids, String contactOwner, String organizationId) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No company IDs provided for update");
        }
        if (contactOwner == null || contactOwner.trim().isEmpty()) {
            throw new RuntimeException("Contact owner cannot be empty");
        }
        
        List<Company> companies = companyRepository.findAllById(ids);
        if (companies.isEmpty()) {
            throw new RuntimeException("No companies found with provided IDs");
        }

        for (Company company : companies) {
            if (!company.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Cannot update companies from other organizations");
            }
            company.setContactOwner(contactOwner.trim());
            company.setLastActivity(LocalDateTime.now());
        }

        companyRepository.saveAll(companies);
    }
 public void updateAssignedTo(List<String> ids, String assignedTo, String organizationId) {

    if (ids == null || ids.isEmpty()) {
        throw new RuntimeException("No company IDs provided for update");
    }
    if (assignedTo == null || assignedTo.trim().isEmpty()) {
        throw new RuntimeException("AssignedTo cannot be empty");
    }

    // Fetch all companies
    List<Company> companies = companyRepository.findAllById(ids);

    if (companies.isEmpty()) {
        throw new RuntimeException("No companies found with provided IDs");
    }

    // Update each company
    for (Company company : companies) {

        // Org validation
        if (!company.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Cannot update companies from other organizations");
        }

        company.setAssignedTo(assignedTo.trim());
        company.setLastActivity(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
    }

    // Save all in one go
    companyRepository.saveAll(companies);
}


    public void updateCompaniesField(List<String> ids, String fieldName, Object fieldValue, String organizationId) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No company IDs provided for update");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new RuntimeException("Field name cannot be empty");
        }
        
        List<Company> companies = companyRepository.findAllById(ids);
        if (companies.isEmpty()) {
            throw new RuntimeException("No companies found with provided IDs");
        }

        for (Company company : companies) {
            if (!company.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Cannot update companies from other organizations");
            }
            
            try {
                Field field = Company.class.getDeclaredField(fieldName.trim());
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                Object valueToSet = fieldValue;

                if (fieldType.isEnum() && fieldValue instanceof String) {
                    valueToSet = Enum.valueOf((Class<Enum>) fieldType, (String) fieldValue);
                }

                field.set(company, valueToSet);
                company.setLastActivity(LocalDateTime.now());

            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Invalid field name: " + fieldName + ". Field does not exist in Company model.", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + fieldName, e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid value for field " + fieldName + ": " + fieldValue, e);
            }
        }

        companyRepository.saveAll(companies);
    }

     public Optional<Company> updateNotes(String companyId, String notes, String organizationId) {

        Optional<Company> existing = companyRepository.findById(companyId);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Company company = existing.get();

        // Security Check (Important in SaaS)
        if (!company.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied");
        }

        company.setNotes(notes);
        company.setUpdatedAt(LocalDateTime.now());
        company.setLastActivity(LocalDateTime.now());

        return Optional.of(companyRepository.save(company));
    }

    // ✅ Get Notes Only
    
    public Optional<String> getNotes(String companyId, String organizationId) {

        Optional<Company> company = companyRepository.findById(companyId);

        if (company.isEmpty()) return Optional.empty();

        if (!company.get().getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied");
        }

        return Optional.ofNullable(company.get().getNotes());
    }



// ---- Get all contacts linked to a company ----
public List<Contact> getLinkedContacts(String companyId, String organizationId) {
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new RuntimeException("Company not found"));

    if (!company.getOrganizationId().equals(organizationId)) {
        throw new RuntimeException("Access denied: Company belongs to different organization");
    }

    if (company.getContactIds() == null || company.getContactIds().isEmpty()) {
        return new ArrayList<>();
    }

    return contactRepository.findAllById(company.getContactIds())
        .stream()
        .filter(c -> c.getOrganizationId().equals(organizationId))
        .collect(java.util.stream.Collectors.toList());
}

// ---- Link a contact to a company (sync both sides) ----
public void linkContactToCompany(String companyId, String contactId, String organizationId) {
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new RuntimeException("Company not found"));
    if (!company.getOrganizationId().equals(organizationId))
        throw new RuntimeException("Access denied");

    Contact contact = contactRepository.findById(contactId)
        .orElseThrow(() -> new RuntimeException("Contact not found"));
    if (!contact.getOrganizationId().equals(organizationId))
        throw new RuntimeException("Access denied");

    company.addContactId(contactId);
    contact.setCompanyId(companyId);
    contact.setCompanyName(company.getCompanyName());
    contact.setUpdatedAt(LocalDateTime.now());

    companyRepository.save(company);
    contactRepository.save(contact);
}

// ---- Unlink a contact from a company (sync both sides) ----
public void unlinkContactFromCompany(String companyId, String contactId, String organizationId) {
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new RuntimeException("Company not found"));
    if (!company.getOrganizationId().equals(organizationId))
        throw new RuntimeException("Access denied");

    Contact contact = contactRepository.findById(contactId)
        .orElseThrow(() -> new RuntimeException("Contact not found"));

    company.removeContactId(contactId);
    contact.setCompanyId(null);
    contact.setCompanyName(null);
    contact.setUpdatedAt(LocalDateTime.now());

    companyRepository.save(company);
    contactRepository.save(contact);
}
}