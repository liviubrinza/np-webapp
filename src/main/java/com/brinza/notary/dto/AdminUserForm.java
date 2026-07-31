package com.brinza.notary.dto;

import com.brinza.notary.domain.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminUserForm {

    @NotBlank(message = "Numele de utilizator este obligatoriu.")
    @Size(max = 100, message = "Numele de utilizator trebuie să aibă cel mult 100 de caractere.")
    // Usernames get logged (e.g. "author={}" on every appointment/document action) - restricting
    // the charset here rules out a crafted username carrying CR/LF for log forging.
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Numele de utilizator poate conține doar litere, cifre, punct, cratimă și underscore.")
    private String username;

    private String password;

    @NotBlank(message = "Numele complet este obligatoriu.")
    @Size(max = 100, message = "Numele complet trebuie să aibă cel mult 100 de caractere.")
    private String fullName;

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }
}
