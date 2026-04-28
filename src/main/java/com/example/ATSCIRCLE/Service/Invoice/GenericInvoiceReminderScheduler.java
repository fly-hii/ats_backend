package com.example.ATSCIRCLE.Service.Invoice;
import com.example.ATSCIRCLE.Models.Sales.Company;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Models.Invoice.Invoice;
import com.example.ATSCIRCLE.Repository.CompanyRepository;
import com.example.ATSCIRCLE.Repository.GenericInvoicerepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled task to send invoice reminders
 * Runs daily at 9:00 AM
 * Includes WebSocket notifications similar to PaymentReminderScheduler
 */
@Component
public class GenericInvoiceReminderScheduler {

    @Autowired
    private GenericInvoicerepository invoiceRepository;
    
    @Autowired
    private UserRepository usersRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private GenericEmailService emailService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Check and send reminders daily at 9:00 AM
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyReminders() {
        System.out.println("Starting daily invoice reminder check at: " + LocalDate.now());
        
        LocalDate today = LocalDate.now();
        
        // Find all invoices with reminders scheduled for today
        List<Invoice> invoicesWithReminders = invoiceRepository.findByRemindersContaining(today);
        
        System.out.println("Found " + invoicesWithReminders.size() + " invoices with reminders for today");
        
        for (Invoice invoice : invoicesWithReminders) {
            try {
                processInvoiceReminder(invoice, today);
            } catch (Exception e) {
                System.err.println("Failed to process reminder for invoice: " + 
                    invoice.getInvoiceNumber() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Completed daily invoice reminder check");
    }

    /**
     * Process reminder for a single invoice
     */
    private void processInvoiceReminder(Invoice invoice, LocalDate today) {
        // Get organization/user details
        Optional<Users> userOpt = usersRepository.findById(invoice.getOrganizationId());
        if (!userOpt.isPresent()) {
            System.err.println("User not found for organization: " + invoice.getOrganizationId());
            return;
        }
        Users user = userOpt.get();
        
        // Get customer/company details
        Optional<Company> companyOpt = findCompanyByGstinOrName(
            invoice.getOrganizationId(), 
            invoice.getCustomerGSTIN(), 
            invoice.getCustomerName()
        );
        
        if (!companyOpt.isPresent()) {
            System.err.println("Company not found for customer: " + invoice.getCustomerName());
            return;
        }
        Company company = companyOpt.get();
        
        // Determine reminder type
        String reminderType = determineReminderType(invoice, today);
        
        // 1. Send WebSocket notification
        sendWebSocketNotification(invoice, user, company, reminderType);
        
        // 2. Send reminder email
        emailService.sendReminderEmail(invoice, user, company, reminderType);
        
        System.out.println("Sent " + reminderType + " reminder for invoice: " + 
            invoice.getInvoiceNumber());
    }

    /**
     * Send WebSocket notification to frontend (matching PaymentReminderScheduler pattern)
     */
    private void sendWebSocketNotification(Invoice invoice, Users supplier, Company customer, 
                                          String reminderType) {
        InvoiceReminderNotification notification = new InvoiceReminderNotification();
        notification.setInvoiceId(invoice.getId());
        notification.setOrganizationId(invoice.getOrganizationId());
        notification.setInvoiceNumber(invoice.getInvoiceNumber());
        notification.setCustomerName(invoice.getCustomerName());
        notification.setMessage(createNotificationMessage(invoice, reminderType));
        notification.setAmount(invoice.getNetPayable());
        notification.setType("INVOICE_REMINDER");
        notification.setReminderType(reminderType);
        notification.setTimestamp(LocalDate.now().toString());
        notification.setSeverity(getSeverity(reminderType));
        
        // Send to organization-specific topic
        messagingTemplate.convertAndSend(
            "/topic/invoice-reminders/" + invoice.getOrganizationId(), 
            notification
        );
    }

    /**
     * Create notification message
     */
    private String createNotificationMessage(Invoice invoice, String reminderType) {
        if ("overdue".equals(reminderType)) {
            return String.format(
                "Payment Overdue: Invoice %s for %s (₹%.2f) is now overdue. Immediate action required.",
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getNetPayable()
            );
        } else if ("due".equals(reminderType)) {
            return String.format(
                "Payment Due: Invoice %s for %s (₹%.2f) is due for payment today.",
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getNetPayable()
            );
        } else {
            return String.format(
                "Invoice Created: Invoice %s for %s (₹%.2f) has been generated and sent.",
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getNetPayable()
            );
        }
    }

    /**
     * Get severity level based on reminder type
     */
    private String getSeverity(String reminderType) {
        if ("overdue".equals(reminderType)) return "HIGH";
        if ("due".equals(reminderType)) return "MEDIUM";
        return "LOW";
    }

    /**
     * Find company by GSTIN or name
     */
    private Optional<Company> findCompanyByGstinOrName(String organizationId, 
                                                        String gstin, String name) {
        // Try to find by GSTIN first
        if (gstin != null && !gstin.isEmpty()) {
            Optional<Company> company = companyRepository
                .findByOrganizationIdAndTaxIdNumber(organizationId, gstin);
            if (company.isPresent()) {
                return company;
            }
        }
        
        // Try to find by name
        List<Company> companies = companyRepository
            .findByOrganizationIdAndCompanyNameContainingIgnoreCase(organizationId, name);
        
        if (!companies.isEmpty()) {
            return Optional.of(companies.get(0));
        }
        
        return Optional.empty();
    }

    /**
     * Determine the type of reminder based on invoice date and today's date
     */
    private String determineReminderType(Invoice invoice, LocalDate today) {
        LocalDate invoiceDate = invoice.getInvoiceDate();
        LocalDate dueDate = calculateDueDate(invoiceDate, invoice.getPaymentTerms());
        
        if (today.equals(invoiceDate)) {
            return "invoice";
        } else if (today.equals(dueDate)) {
            return "due";
        } else if (today.isAfter(dueDate)) {
            return "overdue";
        } else {
            return "reminder";
        }
    }

    /**
     * Calculate due date from payment terms
     */
    private LocalDate calculateDueDate(LocalDate invoiceDate, String paymentTerms) {
        if (paymentTerms == null) return invoiceDate.plusDays(30);
        
        String terms = paymentTerms.toLowerCase();
        
        if (terms.contains("immediate")) return invoiceDate;
        if (terms.contains("7")) return invoiceDate.plusDays(7);
        if (terms.contains("30")) return invoiceDate.plusDays(30);
        if (terms.contains("45")) return invoiceDate.plusDays(45);
        if (terms.contains("60")) return invoiceDate.plusDays(60);
        
        return invoiceDate.plusDays(30); // default
    }

    /**
     * Manual trigger for testing (can be called via API)
     */
    public void triggerManualReminders() {
        System.out.println("Manual reminder trigger initiated");
        sendDailyReminders();
    }

    /**
     * Notification DTO class (matching PaymentReminderScheduler pattern)
     */
    public static class InvoiceReminderNotification {
        private String invoiceId;
        private String organizationId;
        private String invoiceNumber;
        private String customerName;
        private String message;
        private Double amount;
        private String type;
        private String reminderType;
        private String timestamp;
        private String severity;

        // Getters and Setters
        public String getInvoiceId() { return invoiceId; }
        public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getReminderType() { return reminderType; }
        public void setReminderType(String reminderType) { this.reminderType = reminderType; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
}