package com.example.ATSCIRCLE.DTO;

public class UpdatePasswordDTO {

    private String oldPassword;
    private String newPassword;

    // Constructors
    public UpdatePasswordDTO() {}

    public UpdatePasswordDTO(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    // Getters & Setters
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
