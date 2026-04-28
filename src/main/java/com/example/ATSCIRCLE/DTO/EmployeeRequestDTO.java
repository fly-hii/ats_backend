package com.example.ATSCIRCLE.DTO;

import com.example.ATSCIRCLE.Models.UserManagement.Employee;

public class EmployeeRequestDTO {

    private String name;
    private String email;
    private String phoneNumber;
    private Employee.Role role;
    private String organizationId;

    // Constructors
    public EmployeeRequestDTO() {}

    public EmployeeRequestDTO(String name, String email, String phoneNumber,
                              Employee.Role role, String organizationId) {

        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.organizationId = organizationId;
    }

    // Getters & Setters
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
}
