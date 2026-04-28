package com.example.ATSCIRCLE.Service.Invoice;
import com.example.ATSCIRCLE.DTO.InvoiceDTO;
import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import com.example.ATSCIRCLE.Models.Invoice.InvoiceItem;
import com.example.ATSCIRCLE.Models.Sales.Address;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Sales.SalesEnums;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import com.example.ATSCIRCLE.Repository.GenericInvoicerepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GenericInvoiceservice {

    @Autowired
    private GenericInvoicerepository invoiceRepository;
    
    @Autowired
    private UserRepository usersRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private ItemMasterService itemMasterService;
    
    @Autowired
    private GenericEmailService emailService;

   
    @Transactional
    public Invoice createInvoice(InvoiceDTO dto, String organizationId) {
        // Validate organization
        Users user = usersRepository.findById(organizationId)
            .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        // 1. Update User table
        updateUserInfo(user, dto);
        
        // 2. Create or update Company
        Company company = createOrUpdateCompany(dto, organizationId);
        
        // 3. Process items
        processItems(dto, organizationId);
        
        // 4. Create Invoice
        Invoice invoice = buildInvoice(dto, organizationId);
        
        // 5. Generate invoice number
        invoice.generateInvoiceNumber();
        
        // 6. Check if generated invoice number already exists (rare collision case)
        int attempts = 0;
        while (invoiceRepository.existsByOrganizationIdAndInvoiceNumber(
                organizationId, invoice.getInvoiceNumber()) && attempts < 5) {
            invoice.generateInvoiceNumber();
            attempts++;
        }
        
        if (attempts >= 5) {
            throw new RuntimeException("Failed to generate unique invoice number after 5 attempts");
        }
        
        // 7. Set reminders - FIXED: Now setting reminders before saving
        List<LocalDate> reminders = calculateReminders(invoice, dto);
        invoice.setReminders(reminders);
        
        // 8. Save invoice
        invoice = invoiceRepository.save(invoice);
        
        // 9. Send invoice email if sendToEmail is provided
        if (invoice.getSendToEmail() != null && !invoice.getSendToEmail().isEmpty()) {
            try {
                emailService.sendInvoiceEmail(invoice, user, company);
                System.out.println("Invoice email sent to: " + invoice.getSendToEmail());
            } catch (Exception e) {
                System.err.println("Failed to send invoice email: " + e.getMessage());
                // Don't fail the invoice creation if email fails
            }
        }
        
        return invoice;
    }

    private void updateUserInfo(Users user, InvoiceDTO dto) {
        boolean needsUpdate = false;
        
        if (isEmpty(user.getGSTnumber()) && !isEmpty(dto.getGstNumber())) {
            user.setGSTnumber(dto.getGstNumber());
            needsUpdate = true;
        }
        
        if (isEmpty(user.getAccountno()) && !isEmpty(dto.getAccountNo())) {
            user.setAccountno(dto.getAccountNo());
            user.setIfsccode(dto.getIfscCode());
            user.setaccountname(dto.getAccountName());
            user.setbankname(dto.getBankName());
            needsUpdate = true;
        }
        
        if (isEmpty(user.getStreetLine1()) && !isEmpty(dto.getStreetLine1())) {
            user.setStreetLine1(dto.getStreetLine1());
            user.setStreetLine2(dto.getStreetLine2());
            user.setCity(dto.getCity());
            user.setState(dto.getState());
            user.setCountry(dto.getCountry());
            user.setZipcode(dto.getZipcode());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            usersRepository.save(user);
        }
    }

    private Company createOrUpdateCompany(InvoiceDTO dto, String organizationId) {
        Optional<Company> existingCompany = Optional.empty();
        
        if (!isEmpty(dto.getCustomerGSTIN())) {
            existingCompany = companyRepository.findByOrganizationIdAndTaxIdNumber(
                organizationId, dto.getCustomerGSTIN());
        }
        
        if (!existingCompany.isPresent()) {
            List<Company> companies = companyRepository
                .findByOrganizationIdAndCompanyNameContainingIgnoreCase(
                    organizationId, dto.getCustomerName());
            if (!companies.isEmpty()) {
                existingCompany = Optional.of(companies.get(0));
            }
        }
        
        Company company;
        if (existingCompany.isPresent()) {
            company = existingCompany.get();
            updateCompanyFromInvoice(company, dto);
        } else {
            company = createNewCompanyFromInvoice(dto, organizationId);
        }
        
        return companyRepository.save(company);
    }

    private void updateCompanyFromInvoice(Company company, InvoiceDTO dto) {
        if (isEmpty(company.getTaxIdNumber()) && !isEmpty(dto.getCustomerGSTIN())) {
            company.setTaxIdNumber(dto.getCustomerGSTIN());
        }
        
        if (company.getBillingAddress() == null && dto.getCustomerAddress() != null) {
            Address address = parseAddress(dto.getCustomerAddress(), dto.getCustomerState());
            company.setBillingAddress(address);
        }
        
        // Update company email if sendToEmail is provided
        if (!isEmpty(dto.getSendToEmail()) && isEmpty(company.getEmail())) {
            company.setEmail(dto.getSendToEmail());
        }
        
        company.setLastActivity(LocalDateTime.now());
    }

    private Company createNewCompanyFromInvoice(InvoiceDTO dto, String organizationId) {
        Company company = new Company();
        company.setOrganizationId(organizationId);
        company.setCompanyName(dto.getCustomerName());
        company.setTaxIdNumber(dto.getCustomerGSTIN());
        
        if (dto.getCustomerAddress() != null) {
            Address address = parseAddress(dto.getCustomerAddress(), dto.getCustomerState());
            company.setBillingAddress(address);
        }
        
        // Set email from sendToEmail field
        if (!isEmpty(dto.getSendToEmail())) {
            company.setEmail(dto.getSendToEmail());
        }
        
        company.setRelationshipType(SalesEnums.RelationshipType.CLIENT);
        company.setLeadStatus(SalesEnums.LeadStatus.NEW);
        company.setCreatedBy(organizationId);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company.setLastActivity(LocalDateTime.now());
        
        return company;
    }

    private Address parseAddress(String addressString, String state) {
        Address address = new Address();
        String[] parts = addressString.split("[,\n]");
        
        if (parts.length > 0) address.setStreetLine1(parts[0].trim());
        if (parts.length > 1) address.setStreetLine2(parts[1].trim());
        if (parts.length > 2) address.setCity(parts[2].trim());
        
        address.setState(state);
        address.setCountry("India");
        
        return address;
    }

    private void processItems(InvoiceDTO dto, String organizationId) {
        List<String> itemNames = dto.getItems().stream()
            .map(InvoiceDTO.InvoiceItemDTO::getItemName)
            .collect(Collectors.toList());
        
        List<String> hsnCodes = dto.getItems().stream()
            .map(InvoiceDTO.InvoiceItemDTO::getHsnOrSac)
            .collect(Collectors.toList());
        
        itemMasterService.processInvoiceItems(organizationId, itemNames, hsnCodes);
    }

    private Invoice buildInvoice(InvoiceDTO dto, String organizationId) {
        Invoice invoice = new Invoice();
        
        // Set organizationId from JWT token parameter
        invoice.setOrganizationId(organizationId);
        
        // Invoice number will be generated separately
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setCustomerPO(dto.getCustomerPO());
        
        invoice.setLegalBusinessName(dto.getLegalBusinessName());
        invoice.setGstNumber(dto.getGstNumber());
        invoice.setSupplierGstStatus(dto.getSupplierGstStatus());
        invoice.setDefaultGstRate(dto.getDefaultGstRate());
        
        invoice.setStreetLine1(dto.getStreetLine1());
        invoice.setStreetLine2(dto.getStreetLine2());
        invoice.setCity(dto.getCity());
        invoice.setState(dto.getState());
        invoice.setCountry(dto.getCountry());
        invoice.setZipcode(dto.getZipcode());
        
        invoice.setAccountNo(dto.getAccountNo());
        invoice.setIfscCode(dto.getIfscCode());
        invoice.setAccountName(dto.getAccountName());
        invoice.setBankName(dto.getBankName());
        
        invoice.setCustomerName(dto.getCustomerName());
        invoice.setCustomerGSTIN(dto.getCustomerGSTIN());
        invoice.setSendToEmail(dto.getSendToEmail());
        
        List<InvoiceItem> items = dto.getItems().stream()
            .map(this::convertToInvoiceItem)
            .collect(Collectors.toList());
        invoice.setItems(items);
        
        invoice.setPaymentTerms(dto.getPaymentTerms());
        invoice.setIsTdsApplicable(dto.getIsTdsApplicable());
        invoice.setAdvanceReceived(dto.getAdvanceReceived());
        invoice.setPreferredPaymentMethod(dto.getPreferredPaymentMethod());
        invoice.setPaymentLink(dto.getPaymentLink());
        invoice.setUpiId(dto.getUpiId());
        invoice.setUpiNote(dto.getUpiNote());
        invoice.setUpiQrCodeUrl(dto.getUpiQrCodeUrl());
        
        invoice.setAdditionalNotes(dto.getAdditionalNotes());
        invoice.setAuthorisedSignatoryName(dto.getAuthorisedSignatoryName());
        invoice.setDesignation(dto.getDesignation());
        
        calculateTotals(invoice, dto);
        
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
        
        return invoice;
    }

    private InvoiceItem convertToInvoiceItem(InvoiceDTO.InvoiceItemDTO dto) {
        InvoiceItem item = new InvoiceItem();
        item.setItemName(dto.getItemName());
        item.setItemDescription(dto.getItemDescription());  
        item.setItemImageUrl(dto.getItemImageUrl()); 
        item.setHsnSac(dto.getHsnOrSac());
        item.setQuantity(dto.getQuantity());
        item.setRate(dto.getRate());
        item.setAmount(dto.getQuantity() * dto.getRate());
        item.setGstPercent(dto.getGstPercent());
        return item;
    }

    private void calculateTotals(Invoice invoice, InvoiceDTO dto) {
        double subtotal = invoice.getItems().stream()
            .mapToDouble(InvoiceItem::getAmount)
            .sum();
        invoice.setSubTotal(subtotal);
        
        double gstAmount = 0.0;
        if ("registered".equals(dto.getSupplierGstStatus())) {
            gstAmount = subtotal * (dto.getDefaultGstRate() / 100.0);
        }
        invoice.setGstAmount(gstAmount);
        
        double tdsAmount = 0.0;
        if (Boolean.TRUE.equals(dto.getIsTdsApplicable())) {
            double tdsRate = dto.getCustomTdsRate() != null ? dto.getCustomTdsRate() : 10.0;
            tdsAmount = subtotal * (tdsRate / 100.0);
        }
        invoice.setTdsAmount(tdsAmount);
        
        double advanceReceived = dto.getAdvanceReceived() != null ? dto.getAdvanceReceived() : 0.0;
        double netPayable = Math.max(0, subtotal + gstAmount - tdsAmount - advanceReceived);
        invoice.setNetPayable(netPayable);
    }

    /**
     * Calculate reminders based on DTO settings
     * FIXED: Now properly returns the calculated reminders list
     */
    private List<LocalDate> calculateReminders(Invoice invoice, InvoiceDTO dto) {
        List<LocalDate> reminders = new ArrayList<>();
        
        // Check if reminders list is provided directly in DTO
        if (dto.getReminders() != null && !dto.getReminders().isEmpty()) {
            reminders.addAll(dto.getReminders());
        }
        
        // Add reminders based on boolean flags
        if (Boolean.TRUE.equals(dto.getSendOnInvoiceDate())) {
            LocalDate invoiceDate = invoice.getInvoiceDate();
            if (!reminders.contains(invoiceDate)) {
                reminders.add(invoiceDate);
            }
        }
        
        if (Boolean.TRUE.equals(dto.getSendOnDueDate())) {
            LocalDate dueDate = calculateDueDate(invoice.getInvoiceDate(), dto.getPaymentTerms());
            if (!reminders.contains(dueDate)) {
                reminders.add(dueDate);
            }
        }
        
        if (Boolean.TRUE.equals(dto.getSendOnOverdue())) {
            LocalDate dueDate = calculateDueDate(invoice.getInvoiceDate(), dto.getPaymentTerms());
            LocalDate overdueDate = dueDate.plusDays(15);
            if (!reminders.contains(overdueDate)) {
                reminders.add(overdueDate);
            }
        }
        
        return reminders;
    }

    private LocalDate calculateDueDate(LocalDate invoiceDate, String paymentTerms) {
        if (paymentTerms == null) return invoiceDate.plusDays(30);
        
        if (paymentTerms.contains("7")) return invoiceDate.plusDays(7);
        if (paymentTerms.contains("30")) return invoiceDate.plusDays(30);
        if (paymentTerms.contains("45")) return invoiceDate.plusDays(45);
        if (paymentTerms.contains("60")) return invoiceDate.plusDays(60);
        
        return invoiceDate.plusDays(30);
    }

    public List<Invoice> getAllInvoices(String organizationId) {
        return invoiceRepository.findByOrganizationId(organizationId);
    }

    public Optional<Invoice> getInvoiceById(String id) {
        return invoiceRepository.findById(id);
    }

    @Transactional
    public Invoice updateInvoice(String id, InvoiceDTO dto) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        invoice.setCustomerPO(dto.getCustomerPO());
        invoice.setPaymentTerms(dto.getPaymentTerms());
        invoice.setAdditionalNotes(dto.getAdditionalNotes());
        
        // Update sendToEmail if provided
        if (dto.getSendToEmail() != null) {
            invoice.setSendToEmail(dto.getSendToEmail());
        }

        if (dto.getItems() != null) {
            List<InvoiceItem> items = dto.getItems().stream()
                .map(this::convertToInvoiceItem)
                .collect(Collectors.toList());
            invoice.setItems(items);
            
            // Recalculate totals
            calculateTotals(invoice, dto);
        }
        
        // FIXED: Update reminders if provided
        List<LocalDate> reminders = calculateReminders(invoice, dto);
        invoice.setReminders(reminders);
        
        invoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(String id) {
        invoiceRepository.deleteById(id);
    }

    public List<Invoice> searchByCustomer(String organizationId, String customerName) {
        return invoiceRepository
            .findByOrganizationIdAndCustomerNameContainingIgnoreCase(organizationId, customerName);
    }

    public List<Invoice> getInvoicesByDateRange(String organizationId, 
                                                 LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByOrganizationIdAndInvoiceDateBetween(
            organizationId, startDate, endDate);
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}