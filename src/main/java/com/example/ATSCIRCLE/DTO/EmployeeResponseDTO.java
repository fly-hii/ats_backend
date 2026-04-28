package com.example.ATSCIRCLE.DTO;

import java.time.LocalDateTime;
import com.example.ATSCIRCLE.Models.UserManagement.Employee;

public class EmployeeResponseDTO {

    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private Employee.Role role;
    private String organizationId;
    private String createdBy;
    private Employee.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;   // ✅ added
    private boolean success;  // ✅ added

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    // Constructors
    public EmployeeResponseDTO() {}

    public EmployeeResponseDTO(String id, String name, String email, String phoneNumber,
                               Employee.Role role, String organizationId,
                               String createdBy, Employee.Status status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.organizationId = organizationId;
        this.createdBy = createdBy;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Employee.Role getRole() { return role; }
    public void setRole(Employee.Role role) { this.role = role; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Employee.Status getStatus() { return status; }
    public void setStatus(Employee.Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
