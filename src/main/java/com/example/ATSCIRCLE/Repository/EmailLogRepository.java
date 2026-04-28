package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Sales.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {

    // Find all email logs for an organization
    List<EmailLog> findByOrganizationId(String organizationId);

    // Find email logs sent by a specific user
    List<EmailLog> findByOrganizationIdAndSenderId(String organizationId, String senderId);

    // Find email logs by status
    List<EmailLog> findByOrganizationIdAndStatus(String organizationId, EmailLog.EmailStatus status);

    // Find email logs within a date range
    List<EmailLog> findByOrganizationIdAndSentAtBetween(String organizationId, LocalDateTime startDate, LocalDateTime endDate);

    // Find logs for a specific sender with status
    List<EmailLog> findByOrganizationIdAndSenderIdAndStatus(String organizationId, String senderId, EmailLog.EmailStatus status);
}
