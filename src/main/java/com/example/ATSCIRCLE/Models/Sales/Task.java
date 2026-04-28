package com.example.ATSCIRCLE.Models.Sales;
import org.springframework.data.annotation.Id;
 import org.springframework.data.mongodb.core.mapping.Document; 
 import org.springframework.data.mongodb.core.mapping.Field; 

import java.time.LocalDateTime; 

@Document(collection = "tasks") 
public class Task { 

@Id 
private String id; 
 
private String subject; 
 
@Field("type") 
private TaskType type;   // Email, To-Do, Call 
 
@Field("due_date") 
private LocalDateTime dueDate;  // Calendar + Time 
 
@Field("reminder") 
private Reminder reminder;  // No Reminder, 30m before, etc. 
 
@Field("priority") 
private Priority priority; // None, Low, Medium, High 
 
// 🔥 ACCESS CONTROL FIELDS 
@Field("organization_id") 
private String organizationId;  // Which organization owns this task 
 
@Field("created_by") 
private String createdBy;       // Who created this task (userId/employeeId) 
 
@Field("assigned_to") 
private String assignedTo;      // Who is responsible for this task (can be null) 
 
@Field("is_shared") 
private boolean isShared = false;  // If true, all org members can view (read-only) 
 
@Field("status") 
private Status status = Status.PENDING; // Default status 

@Field("description")
private String description;

@Field("notes")
private String notes;
 
// LINKING 
@Field("linked_contact") 
private String linkedContact;  // Can be a contactId 
 
@Field("linked_account") 
private String linkedAccount;  // Can be an accountId (companyId) 
 
@Field("created_at") 
private LocalDateTime createdAt = LocalDateTime.now(); 
 
@Field("updated_at") 
private LocalDateTime updatedAt = LocalDateTime.now(); 
 
// --- ENUMS --- 
 
public enum TaskType { 
    EMAIL, 
    TODO, 
    CALL 
} 
 
public enum Reminder { 
    NO_REMINDER, 
    THIRTY_MINUTES_BEFORE, 
    ONE_HOUR_BEFORE, 
    ONE_DAY_BEFORE 
} 
 
public enum Priority { 
    NONE, 
    LOW, 
    MEDIUM, 
    HIGH 
} 
 
public enum Status { 
    PENDING, 
    IN_PROGRESS, 
    COMPLETED, 
    CANCELLED 
} 
 
// --- Constructors --- 
public Task() {} 
 
public Task(String subject, TaskType type, LocalDateTime dueDate, Reminder reminder,
            Priority priority, String organizationId, String createdBy, String assignedTo,
            boolean isShared, Status status, String linkedContact, String linkedAccount,
            String description, String notes) { 
    this.subject = subject; 
    this.type = type; 
    this.dueDate = dueDate; 
    this.reminder = reminder; 
    this.priority = priority; 
    this.organizationId = organizationId; 
    this.createdBy = createdBy; 
    this.assignedTo = assignedTo; 
    this.isShared = isShared; 
    this.status = status; 
    this.description = description;
this.notes = notes;
    this.linkedContact = linkedContact; 
    this.linkedAccount = linkedAccount; 
    this.createdAt = LocalDateTime.now(); 
    this.updatedAt = LocalDateTime.now(); 
} 
 
// --- Getters & Setters --- 
public String getId() { return id; } 
 
public String getSubject() { return subject; } 
public void setSubject(String subject) {  
    this.subject = subject; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public TaskType getType() { return type; } 
public void setType(TaskType type) {  
    this.type = type; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public LocalDateTime getDueDate() { return dueDate; } 
public void setDueDate(LocalDateTime dueDate) {  
    this.dueDate = dueDate; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public Reminder getReminder() { return reminder; } 
public void setReminder(Reminder reminder) {  
    this.reminder = reminder; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public Priority getPriority() { return priority; } 
public void setPriority(Priority priority) {  
    this.priority = priority; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getOrganizationId() { return organizationId; } 
public void setOrganizationId(String organizationId) { this.organizationId = organizationId; } 
 
public String getCreatedBy() { return createdBy; } 
public void setCreatedBy(String createdBy) { this.createdBy = createdBy; } 
 
public String getAssignedTo() { return assignedTo; } 
public void setAssignedTo(String assignedTo) {  
    this.assignedTo = assignedTo; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public boolean isShared() { return isShared; } 
public void setShared(boolean shared) {  
    isShared = shared; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public Status getStatus() { return status; } 
public void setStatus(Status status) {  
    this.status = status; 
    this.updatedAt = LocalDateTime.now(); 
} 

public String getDescription() { return description; }
public void setDescription(String description) {
    this.description = description;
    this.updatedAt = LocalDateTime.now();
}

public String getNotes() { return notes; }
public void setNotes(String notes) {
    this.notes = notes;
    this.updatedAt = LocalDateTime.now();
}
 
public String getLinkedContact() { return linkedContact; } 
public void setLinkedContact(String linkedContact) {  
    this.linkedContact = linkedContact; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public String getLinkedAccount() { return linkedAccount; } 
public void setLinkedAccount(String linkedAccount) {  
    this.linkedAccount = linkedAccount; 
    this.updatedAt = LocalDateTime.now(); 
} 
 
public LocalDateTime getCreatedAt() { return createdAt; } 
public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } 
 
public LocalDateTime getUpdatedAt() { return updatedAt; } 
public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; } 
  

} 