package com.brinza.notary.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {

    @NotBlank(message = "Parola curentă este obligatorie.")
    private String currentPassword;

    @NotBlank(message = "Parola nouă este obligatorie.")
    @Size(min = 4, message = "Parola nouă trebuie să aibă cel puțin 4 caractere.")
    private String newPassword;

    @NotBlank(message = "Vă rugăm confirmați parola nouă.")
    private String confirmNewPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    @AssertTrue(message = "Parola nouă și confirmarea nu coincid.")
    public boolean isConfirmationMatching() {
        return newPassword == null || newPassword.equals(confirmNewPassword);
    }
}
