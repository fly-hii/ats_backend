package com.example.ATSCIRCLE.Models.UserManagement;

import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String phoneNumber;

    private String password;

    private Role role;

    private String organizationId;

    private String createdBy;

    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ---------- ENUMS ----------
    public enum Role {
        ATS, CRM, INVOICE, USER_MANAGEMENT
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    // ---------- CONSTRUCTORS ----------

    public Employee() {}

    public Employee(String id, String name, String email, String phoneNumber, String password,
                    Role role, String organizationId, String createdBy,
                    Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
       	this.organizationId = organizationId;
        this.createdBy = createdBy;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---------- GETTERS & SETTERS ----------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
