package com.brinza.notary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminUserForm {

    @NotBlank(message = "Numele de utilizator este obligatoriu.")
    @Size(max = 100, message = "Numele de utilizator trebuie să aibă cel mult 100 de caractere.")
    private String username;

    private String password;

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
}
