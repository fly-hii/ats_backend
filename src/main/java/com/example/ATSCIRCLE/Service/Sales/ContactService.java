package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.Contact;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import com.example.ATSCIRCLE.Repository.ContactRepository;

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
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

      @Autowired
    private CompanyRepository companyRepository;  

    // ========== CREATE CONTACT WITH ORGANIZATION CONTEXT ==========
    
    public Contact createContact(Contact contact, String organizationId, String createdBy, String userType) {
    contact.setOrganizationId(organizationId);
    contact.setCreatedBy(createdBy);

    validateContact(contact);

    if (contactRepository.existsByOrganizationIdAndEmail(organizationId, contact.getEmail())) {
        throw new RuntimeException("Contact with email '" + contact.getEmail() + "' already exists in your organization");
    }

    // ---- Company relationship sync on create ----
    if (contact.getCompanyId() != null && !contact.getCompanyId().isBlank()) {

        Optional<Company> companyOpt = companyRepository.findById(contact.getCompanyId());

        if (companyOpt.isEmpty()) {
            throw new RuntimeException("Company not found with id: " + contact.getCompanyId());
        }

        Company company = companyOpt.get();

        if (!company.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Company belongs to a different organization");
        }

        // Set company name on contact automatically
        contact.setCompanyName(company.getCompanyName());
    }

    contact.setCreatedAt(LocalDateTime.now());
    contact.setLastActivity(LocalDateTime.now());

    // Save contact first to get the generated id
    Contact saved = contactRepository.save(contact);

    // Now add this contact's id into the company's contactIds list
    if (saved.getCompanyId() != null && !saved.getCompanyId().isBlank()) {
        companyRepository.findById(saved.getCompanyId()).ifPresent(company -> {
            company.addContactId(saved.getId());
            company.setUpdatedAt(LocalDateTime.now());
            companyRepository.save(company);
        });
    }

    return saved;
}
    // ========== GET ALL CONTACTS (ORGANIZATION-SCOPED) ==========
    
    public List<Contact> getAllContacts(String organizationId, String userType, String userEmail) {
    // Irrespective of type, always return all organization contacts
    return contactRepository.findByOrganizationId(organizationId);
}


    // ========== GET CONTACT BY ID ==========
    
   public Optional<Contact> getContactById(String id, String organizationId, String userType, String userEmail) {

    Optional<Contact> contact = contactRepository.findById(id);

    if (contact.isEmpty()) {
        return Optional.empty(); 
    }

    Contact cont = contact.get();
    if (!cont.getOrganizationId().equals(organizationId)) {
        return Optional.empty(); // Different organization → deny access
    }
    return Optional.of(cont);
}


    // ========== GET CONTACT BY EMAIL ==========
    
    public Optional<Contact> getContactByEmail(String email, String organizationId, String userType, String userEmail) {
        Optional<Contact> contact = contactRepository.findByOrganizationIdAndEmail(organizationId, email);
        
        if (contact.isPresent()) {
            Contact cont = contact.get();
            
            // Check user access
            if ("ADMIN".equals(userType)) {
                return contact;
            } else {
                // Employee can access if: created by them, assigned to them, or shared
                if (cont.getCreatedBy().equals(userEmail) || 
                    userEmail.equals(cont.getAssignedTo()) || 
                    cont.isShared()) {
                    return contact;
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    // ========== UPDATE CONTACT ==========
    
   public Contact updateContact(String id, Contact updatedContact, String organizationId, String userType, String userEmail) {

    Optional<Contact> existingContactOpt = getContactById(id, organizationId, userType, userEmail);

    if (existingContactOpt.isEmpty()) {
        throw new RuntimeException("Contact not found or access denied");
    }

    Contact contact = existingContactOpt.get();

    // Basic fields
    if (updatedContact.getFirstName() != null) contact.setFirstName(updatedContact.getFirstName());
    if (updatedContact.getLastName() != null) contact.setLastName(updatedContact.getLastName());
    if (updatedContact.getPhone() != null) contact.setPhone(updatedContact.getPhone());

    // Email duplicate check
    if (updatedContact.getEmail() != null) {
        if (!updatedContact.getEmail().equals(contact.getEmail()) &&
                contactRepository.existsByOrganizationIdAndEmail(organizationId, updatedContact.getEmail())) {
            throw new RuntimeException("Email already exists in your organization");
        }
        contact.setEmail(updatedContact.getEmail());
    }

    if (updatedContact.getJobTitle() != null) contact.setJobTitle(updatedContact.getJobTitle());
    if (updatedContact.getLinkedInUrl() != null) contact.setLinkedInUrl(updatedContact.getLinkedInUrl());
    if (updatedContact.getDepartment() != null) contact.setDepartment(updatedContact.getDepartment());
    if (updatedContact.getLeadStatus() != null) contact.setLeadStatus(updatedContact.getLeadStatus());
    if (updatedContact.getLeadSource() != null) contact.setLeadSource(updatedContact.getLeadSource());
    if (updatedContact.getNotes() != null) contact.setNotes(updatedContact.getNotes());
    if (updatedContact.getAssignedTo() != null) contact.setAssignedTo(updatedContact.getAssignedTo());
    contact.setShared(updatedContact.isShared());
    contact.setDoNotCall(updatedContact.isDoNotCall());
    contact.setDoNotEmail(updatedContact.isDoNotEmail());

    // ---- Company relationship sync ----
    String incomingCompanyId = updatedContact.getCompanyId();

    if (incomingCompanyId != null) {
        String currentCompanyId = contact.getCompanyId();
        boolean companyChanged = !incomingCompanyId.equals(currentCompanyId);

        if (companyChanged) {
            // Remove from old company's contactIds
            if (currentCompanyId != null && !currentCompanyId.isBlank()) {
                companyRepository.findById(currentCompanyId).ifPresent(oldCompany -> {
                    if (oldCompany.getOrganizationId().equals(organizationId)) {
                        oldCompany.removeContactId(contact.getId());
                        oldCompany.setUpdatedAt(LocalDateTime.now());
                        companyRepository.save(oldCompany);
                    }
                });
            }

            // Add to new company's contactIds
            Optional<Company> newCompanyOpt = companyRepository.findById(incomingCompanyId);
            if (newCompanyOpt.isEmpty()) {
                throw new RuntimeException("Company not found with id: " + incomingCompanyId);
            }
            Company newCompany = newCompanyOpt.get();
            if (!newCompany.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Company belongs to a different organization");
            }

            newCompany.addContactId(contact.getId());
            newCompany.setUpdatedAt(LocalDateTime.now());
            companyRepository.save(newCompany);

            contact.setCompanyId(incomingCompanyId);
            contact.setCompanyName(newCompany.getCompanyName());
        }

    } else if (updatedContact.getCompanyName() != null) {
        // companyId not sent but companyName was — just update the display name
        contact.setCompanyName(updatedContact.getCompanyName());
    }

    contact.setLastActivity(LocalDateTime.now());

    return contactRepository.save(contact);
}

    // ========== DELETE CONTACT (ADMIN ONLY) ==========
    
    public boolean deleteContact(String id, String organizationId, String userType) {
    
        
        Optional<Contact> contact = contactRepository.findById(id);
        if (contact.isPresent()) {
            if (!contact.get().getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Contact belongs to different organization");
            }
            contactRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // ========== SEARCH CONTACTS BY NAME ==========
    
    public List<Contact> searchContactsByName(String name, String organizationId, String userType, String userEmail) {
        List<Contact> contacts = contactRepository.findByOrganizationIdAndNameContaining(organizationId, name);
        
        if ("ADMIN".equals(userType)) {
            return contacts;
        } else {
            // Filter to only contacts the employee can access
            return contacts.stream()
                    .filter(contact -> contact.getCreatedBy().equals(userEmail) || 
                                     userEmail.equals(contact.getAssignedTo()) || 
                                     contact.isShared())
                    .toList();
        }
    }

    

    // ========== GET CONTACTS CREATED BY USER ==========
    
    public List<Contact> getContactsByCreatedBy(String createdBy, String organizationId) {
        return contactRepository.findByOrganizationIdAndCreatedBy(organizationId, createdBy);
    }

    // ========== GET CONTACTS ASSIGNED TO USER ==========
    
    public List<Contact> getContactsByAssignedTo(String assignedTo, String organizationId) {
        return contactRepository.findByOrganizationIdAndAssignedTo(organizationId, assignedTo);
    }

    // ========== GET SHARED CONTACTS ==========
    
    public List<Contact> getSharedContacts(String organizationId) {
        return contactRepository.findByOrganizationIdAndIsShared(organizationId, true);
    }

    // ========== BULK OPERATIONS ==========

    public List<String> getAllColumns() {
        Field[] fields = Contact.class.getDeclaredFields();
        List<String> columns = new ArrayList<>();
        for (Field field : fields) {
            columns.add(field.getName());
        }
        return columns;
    }

    public ByteArrayInputStream generateExcelTemplate(List<String> selectedColumns) throws IOException {
        return ContactsExcelHelper.createExcelTemplate(selectedColumns);
    }

    public void importExcel(MultipartFile file, String organizationId, String createdBy) throws IOException {
        List<Contact> contacts = ContactsExcelHelper.parseExcel(file.getInputStream());
        
        // Validate contacts before saving
        for (Contact contact : contacts) {
            // Set organization context
            contact.setOrganizationId(organizationId);
            contact.setCreatedBy(createdBy);
            
            if (contact.getEmail() != null && 
                contactRepository.existsByOrganizationIdAndEmail(organizationId, contact.getEmail())) {
                throw new RuntimeException("Contact with email '" + contact.getEmail() + "' already exists in your organization");
            }
            
            contact.setCreatedAt(LocalDateTime.now());
            contact.setLastActivity(LocalDateTime.now());
        }
        
        contactRepository.saveAll(contacts);
    }

    public void deleteContactsByIds(List<String> ids, String organizationId, String userType) {
    // FIX: userType is null, so check against null and also accept ROLE_ADMIN
    if (userType == null || (!userType.equals("ADMIN") && !userType.equals("ROLE_ADMIN"))) {
        throw new RuntimeException("Access denied: Only administrators can delete contacts");
    }
    
    if (ids == null || ids.isEmpty()) {
        throw new RuntimeException("No contact IDs provided for deletion");
    }
    
    List<Contact> contacts = contactRepository.findAllById(ids);
    for (Contact contact : contacts) {
        if (!contact.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Cannot delete contacts from other organizations");
        }
    }
    
    contactRepository.deleteAllById(ids);
}

    public void updateAssignedTo(List<String> ids, String assignedTo, String organizationId) {
        // Only ADMIN can bulk update assignment
        
        
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No contact IDs provided for update");
        }
        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            throw new RuntimeException("Assigned to cannot be empty");
        }
        
        List<Contact> contacts = contactRepository.findAllById(ids);
        if (contacts.isEmpty()) {
            throw new RuntimeException("No contacts found with provided IDs");
        }

        for (Contact contact : contacts) {
            if (!contact.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Cannot update contacts from other organizations");
            }
            contact.setAssignedTo(assignedTo.trim());
            contact.setLastActivity(LocalDateTime.now());
        }

        contactRepository.saveAll(contacts);
    }

    public void updateContactsField(List<String> ids, String fieldName, Object fieldValue, String organizationId, String userType, String userEmail) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No contact IDs provided for update");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new RuntimeException("Field name cannot be empty");
        }
        
        List<Contact> contacts = contactRepository.findAllById(ids);
        if (contacts.isEmpty()) {
            throw new RuntimeException("No contacts found with provided IDs");
        }

        for (Contact contact : contacts) {
            if (!contact.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Cannot update contacts from other organizations");
            }
            
          
            
            try {
                Field field = Contact.class.getDeclaredField(fieldName.trim());
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                Object valueToSet = fieldValue;

                if (fieldType.isEnum() && fieldValue instanceof String) {
                    valueToSet = Enum.valueOf((Class<Enum>) fieldType, (String) fieldValue);
                }

                field.set(contact, valueToSet);
                contact.setLastActivity(LocalDateTime.now());

            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Invalid field name: " + fieldName + ". Field does not exist in Contact model.", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + fieldName, e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid value for field " + fieldName + ": " + fieldValue, e);
            }
        }

        contactRepository.saveAll(contacts);
    }

    // ========== VALIDATION ==========
    
    private void validateContact(Contact contact) {
        if (contact.getFirstName() == null || contact.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (contact.getEmail() == null || contact.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (!isValidEmail(contact.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (contact.getPhone() != null && !contact.getPhone().isEmpty() && 
            !isValidPhoneNumber(contact.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^[+]?[0-9\\s\\-\\(\\)]{10,15}$");
    }

    // ========== CHECK EXISTENCE ==========
    
    public boolean existsByEmail(String email, String organizationId) {
        return contactRepository.existsByOrganizationIdAndEmail(organizationId, email);
    }

      public List<Contact> getUserContacts(String organizationId, String userId) {

        return contactRepository.findByOrganizationIdAndAssignedToOrOrganizationIdAndCreatedBy(
                organizationId, userId,
                organizationId, userId
        );
    }

    public Optional<Contact> updateNotes(String contactId, String notes, String organizationId) {

        Optional<Contact> existing = contactRepository.findById(contactId);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Contact contact = existing.get();

        // Security Check (Important in SaaS)
        if (!contact.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied");
        }

        contact.setNotes(notes);
        contact.setUpdatedAt(LocalDateTime.now());
        contact.setLastActivity(LocalDateTime.now());

        return Optional.of(contactRepository.save(contact));
    }

    // ✅ Get Notes Only
    
    public Optional<String> getNotes(String contactId, String organizationId) {

        Optional<Contact> contact = contactRepository.findById(contactId);

        if (contact.isEmpty()) return Optional.empty();

        if (!contact.get().getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied");
        }

        return Optional.ofNullable(contact.get().getNotes());
    }

  

// ---- Get the company a contact belongs to ----
public Optional<Company> getAssociatedCompany(String contactId, String organizationId) {
    Contact contact = contactRepository.findById(contactId)
        .orElseThrow(() -> new RuntimeException("Contact not found"));

    if (!contact.getOrganizationId().equals(organizationId))
        throw new RuntimeException("Access denied");

    if (contact.getCompanyId() == null || contact.getCompanyId().isBlank())
        return Optional.empty();

    return companyRepository.findById(contact.getCompanyId())
        .filter(c -> c.getOrganizationId().equals(organizationId));
}

}