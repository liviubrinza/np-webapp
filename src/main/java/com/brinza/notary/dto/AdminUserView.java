package com.brinza.notary.dto;

import java.time.LocalDateTime;

public record AdminUserView(Long id, String username, LocalDateTime createdAt, LocalDateTime lastLogin) {
}
