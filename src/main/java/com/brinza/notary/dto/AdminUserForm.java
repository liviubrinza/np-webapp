package com.brinza.notary.dto;

import com.brinza.notary.domain.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUserForm {

    @NotBlank(message = "Numele de utilizator este obligatoriu.")
    @Size(max = 100, message = "Numele de utilizator trebuie să aibă cel mult 100 de caractere.")
    private String username;

    private String password;

    @NotNull(message = "Rolul este obligatoriu.")
    private AdminRole role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }
}
