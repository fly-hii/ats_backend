package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Sales.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    // ✅ Find tasks by organization
    List<Task> findByOrganizationId(String organizationId);

    // ✅ Find tasks assigned to specific user in organization
    List<Task> findByAssignedToAndOrganizationId(String assignedTo, String organizationId);

    // ✅ Find tasks by assignedTo and organization with date range
    List<Task> findByAssignedToAndOrganizationIdAndDueDateBetween(
            String assignedTo, String organizationId, LocalDateTime start, LocalDateTime end);

    // ✅ Find overdue tasks for user in organization
    List<Task> findByAssignedToAndOrganizationIdAndDueDateBefore(
            String assignedTo, String organizationId, LocalDateTime now);

    // ✅ Find upcoming tasks for user in organization
    List<Task> findByAssignedToAndOrganizationIdAndDueDateAfter(
            String assignedTo, String organizationId, LocalDateTime now);

    // ✅ Find today's tasks for all users in organization (ADMIN)
    List<Task> findByOrganizationIdAndDueDateBetween(
            String organizationId, LocalDateTime start, LocalDateTime end);

    // ✅ Find upcoming tasks for all users in organization (ADMIN)
    List<Task> findByOrganizationIdAndDueDateAfter(
            String organizationId, LocalDateTime now);

    // ✅ Find overdue tasks for all users in organization (ADMIN)
    List<Task> findByOrganizationIdAndDueDateBefore(
            String organizationId, LocalDateTime now);

    // ✅ Find shared tasks in organization
    List<Task> findByOrganizationIdAndIsShared(String organizationId, boolean isShared);

    // ✅ Find tasks by status in organization
    List<Task> findByOrganizationIdAndStatus(String organizationId, Task.Status status);

    List<Task> findByOrganizationIdAndAssignedToOrCreatedBy(String organizationId, String userId, String userId2);

    List<Task> findByOrganizationIdAndDueDateBetweenAndAssignedToOrCreatedBy(String organizationId, LocalDateTime start,
            LocalDateTime end, String userId, String userId2);

    List<Task> findByOrganizationIdAndDueDateAfterAndAssignedToOrCreatedBy(String organizationId, LocalDateTime now,
            String userId, String userId2);

    List<Task> findByOrganizationIdAndDueDateBeforeAndAssignedToOrCreatedBy(String organizationId, LocalDateTime now,
            String userId, String userId2);

   // Add these to TaskRepository interface

List<Task> findByOrganizationIdAndLinkedContactOrderByDueDateAsc(String organizationId, String linkedContact);

List<Task> findByOrganizationIdAndLinkedAccountOrderByDueDateAsc(String organizationId, String linkedAccount);
}