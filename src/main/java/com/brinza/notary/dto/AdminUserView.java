package com.brinza.notary.dto;

import com.brinza.notary.domain.AdminRole;

import java.time.LocalDateTime;

public record AdminUserView(Long id, String username, AdminRole role, LocalDateTime createdAt, LocalDateTime lastLogin) {
}
