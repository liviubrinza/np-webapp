package com.brinza.notary.dto;

import com.brinza.notary.domain.AdminRole;

import java.time.LocalDateTime;

public record AdminUserView(Long id, String username, String fullName, AdminRole role, LocalDateTime createdAt,
                             LocalDateTime lastLogin, boolean locked, LocalDateTime lockUntil) {
}
