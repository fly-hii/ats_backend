package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Task;
import com.example.ATSCIRCLE.Repository.EmployeeRepository;
import com.example.ATSCIRCLE.Repository.TaskRepository;
import com.example.ATSCIRCLE.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailService mailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ✅ Create Task
    public Task createTask(Task task, String organizationId, String createdBy) {
        task.setOrganizationId(organizationId);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        Task saved = taskRepository.save(task);
        scheduleReminder(saved);
        return saved;
    }

    // ✅ Update Task (type instead of role - SAME AS CompanyService)
   public Optional<Task> updateTask(String id, Task updatedTask, String organizationId,
                                 String currentUser, String type) {

    Optional<Task> existingTask = taskRepository.findById(id);

    if (existingTask.isEmpty()) {
        return Optional.empty();
    }

    Task task = existingTask.get();

    // ✅ Verify organization access
    if (!task.getOrganizationId().equals(organizationId)) {
        throw new RuntimeException("Access denied: Task belongs to different organization");
    }

    // ✅ Only update fields that are provided (NOT NULL)

    if (updatedTask.getSubject() != null)
        task.setSubject(updatedTask.getSubject());

    if (updatedTask.getType() != null)
        task.setType(updatedTask.getType());

    if (updatedTask.getDueDate() != null)
        task.setDueDate(updatedTask.getDueDate());

    if (updatedTask.getReminder() != null)
        task.setReminder(updatedTask.getReminder());

    if (updatedTask.getPriority() != null)
        task.setPriority(updatedTask.getPriority());

    if (updatedTask.getAssignedTo() != null)
        task.setAssignedTo(updatedTask.getAssignedTo());

    if (updatedTask.getStatus() != null)
        task.setStatus(updatedTask.getStatus());

    if (updatedTask.getDescription() != null)
        task.setDescription(updatedTask.getDescription());

    if (updatedTask.getNotes() != null)
        task.setNotes(updatedTask.getNotes());

    if (updatedTask.getLinkedContact() != null)
        task.setLinkedContact(updatedTask.getLinkedContact());

    if (updatedTask.getLinkedAccount() != null)
        task.setLinkedAccount(updatedTask.getLinkedAccount());

    // Boolean needs special handling (because false is valid value)
    if (updatedTask.isShared() != task.isShared())
        task.setShared(updatedTask.isShared());

    task.setUpdatedAt(LocalDateTime.now());

    Task saved = taskRepository.save(task);

    // reschedule reminder only if reminder changed
    if (updatedTask.getReminder() != null || updatedTask.getDueDate() != null) {
        scheduleReminder(saved);
    }

    return Optional.of(saved);
}
    // ✅ Delete Task (type instead of role)
    public void deleteTask(String id, String organizationId, String currentUser, String type) {
        Optional<Task> task = taskRepository.findById(id);
        
        if (task.isEmpty()) {
            throw new RuntimeException("Task not found");
        }
        
        // Verify organization access
        if (!task.get().getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Access denied: Task belongs to different organization");
        }
        
        taskRepository.deleteById(id);
    }

    // 🔥 Bulk Delete Tasks
    public int bulkDeleteTasks(List<String> taskIds, String organizationId, 
                               String currentUser, String type) {
        if (taskIds == null || taskIds.isEmpty()) {
            return 0;
        }
        
        List<Task> tasks = taskRepository.findAllById(taskIds);
        
        // Verify all tasks belong to the organization
        for (Task task : tasks) {
            if (!task.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Some tasks belong to different organization");
            }
        }
        
        Query query = new Query(Criteria.where("_id").in(taskIds)
                .and("organizationId").is(organizationId));
        long deletedCount = mongoTemplate.remove(query, Task.class).getDeletedCount();
        
        return (int) deletedCount;
    }

    // ✅ Bulk update any field
    public void bulkUpdateField(List<String> ids, String fieldName, Object fieldValue, 
                                String organizationId, String currentUser, String type) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No task IDs provided for update");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new RuntimeException("Field name cannot be empty");
        }

        List<Task> tasks = taskRepository.findAllById(ids);
        if (tasks.isEmpty()) {
            throw new RuntimeException("No tasks found with provided IDs");
        }

        // Verify organization access
        for (Task task : tasks) {
            if (!task.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Some tasks belong to different organization");
            }
        }

        for (Task task : tasks) {
            try {
                Field field = Task.class.getDeclaredField(fieldName.trim());
                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                // Handle enums properly
                if (fieldType.isEnum() && fieldValue instanceof String) {
                    @SuppressWarnings("unchecked")
                    Object enumValue = Enum.valueOf((Class<Enum>) fieldType, ((String) fieldValue).toUpperCase());
                    field.set(task, enumValue);
                } else {
                    field.set(task, fieldValue);
                }
                
                task.setUpdatedAt(LocalDateTime.now());

            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Invalid field name: " + fieldName, e);
            } catch (Exception e) {
                throw new RuntimeException("Error updating field " + fieldName + ": " + e.getMessage(), e);
            }
        }

        taskRepository.saveAll(tasks);
    }

    // ✅ Bulk assign tasks
    public void bulkAssign(List<String> ids, String newAssignee, 
                          String organizationId, String currentUser, String type) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("No task IDs provided for assignment");
        }
        if (newAssignee == null || newAssignee.trim().isEmpty()) {
            throw new RuntimeException("New assignee cannot be empty");
        }

        List<Task> tasks = taskRepository.findAllById(ids);
        if (tasks.isEmpty()) {
            throw new RuntimeException("No tasks found with provided IDs");
        }

        // Verify organization access
        for (Task task : tasks) {
            if (!task.getOrganizationId().equals(organizationId)) {
                throw new RuntimeException("Access denied: Some tasks belong to different organization");
            }
        }

        for (Task task : tasks) {
            task.setAssignedTo(newAssignee.trim());
            task.setUpdatedAt(LocalDateTime.now());
        }

        taskRepository.saveAll(tasks);
    }

    // ✅ Get all tasks for organization
    public List<Task> getAllTasks(String organizationId) {
        return taskRepository.findByOrganizationId(organizationId);
    }

    // ✅ Get my tasks
    public List<Task> getMyTasks(String userId, String organizationId) {
        return taskRepository.findByOrganizationIdAndAssignedToOrCreatedBy(
                organizationId, userId, userId);
    }

    // ✅ Today tasks for current user
    public List<Task> getTodayTasks(String userId, String organizationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        return taskRepository.findByOrganizationIdAndDueDateBetweenAndAssignedToOrCreatedBy(
                organizationId, start, end, userId, userId);
    }

    // ✅ Upcoming tasks for current user
    public List<Task> getUpcomingTasks(String userId, String organizationId) {
        return taskRepository.findByOrganizationIdAndDueDateAfterAndAssignedToOrCreatedBy(
                organizationId, LocalDateTime.now(), userId, userId);
    }

    // ✅ Overdue tasks for current user
    public List<Task> getOverdueTasks(String userId, String organizationId) {
        return taskRepository.findByOrganizationIdAndDueDateBeforeAndAssignedToOrCreatedBy(
                organizationId, LocalDateTime.now(), userId, userId);
    }

    // ✅ Today tasks (all)
    public List<Task> getTodayTasksAll(String organizationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        return taskRepository.findByOrganizationIdAndDueDateBetween(organizationId, start, end);
    }

    // ✅ Upcoming tasks (all)
    public List<Task> getUpcomingTasksAll(String organizationId) {
        return taskRepository.findByOrganizationIdAndDueDateAfter(organizationId, LocalDateTime.now());
    }

    // ✅ Overdue tasks (all)
    public List<Task> getOverdueTasksAll(String organizationId) {
        return taskRepository.findByOrganizationIdAndDueDateBefore(organizationId, LocalDateTime.now());
    }

   



private void scheduleReminder(Task task) {
    if (task.getReminder() == null || task.getReminder() == Task.Reminder.NO_REMINDER) return;
    if (task.getAssignedTo() == null) return;

    // ✅ Look up actual email instead of passing employeeId
    String recipientEmail = employeeRepository.findById(task.getAssignedTo())
            .map(emp -> emp.getEmail())
            .orElse(null);

    if (recipientEmail == null) {
        logger.warn("Cannot send reminder: employee not found for id {}", task.getAssignedTo());
        return; // Don't crash, just skip
    }

    try {
        mailService.sendTaskReminder(
                recipientEmail,  // ✅ actual email now
                "Reminder set for: " + task.getSubject(),
                "Task '" + task.getSubject() + "' is due at " + task.getDueDate()
        );

        notificationService.sendNotification(
                task.getAssignedTo(),  // employeeId is fine for socket
                "✅ Reminder set for task: " + task.getSubject()
        );
    } catch (Exception e) {
        // ✅ Don't let reminder failure crash task creation
        logger.error("Failed to send reminder for task {}: {}", task.getId(), e.getMessage());
    }
}
    // ✅ Get tasks by linked contact (ordered by due date)
public List<Task> getTasksByLinkedContact(String linkedContactId, String organizationId) {
    return taskRepository.findByOrganizationIdAndLinkedContactOrderByDueDateAsc(
            organizationId, linkedContactId);
}

// ✅ Get tasks by linked account/company (ordered by due date)
public List<Task> getTasksByLinkedAccount(String linkedAccountId, String organizationId) {
    return taskRepository.findByOrganizationIdAndLinkedAccountOrderByDueDateAsc(
            organizationId, linkedAccountId);
}
}