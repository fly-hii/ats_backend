package com.example.ATSCIRCLE.DTO;

public class EmailRecipient {
    private String email;
    private String fullName;
    private String companyName;

    // ================================
    // CONSTRUCTORS
    // ================================

    public EmailRecipient() {
    }

    public EmailRecipient(String email, String fullName, String companyName) {
        this.email = email;
        this.fullName = fullName;
        this.companyName = companyName;
    }

    // ================================
    // GETTERS AND SETTERS
    // ================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "EmailRecipient{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
