package com.brinza.notary.dto;

import com.brinza.notary.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentListItemView(
        Long id,
        String clientName,
        String serviceName,
        LocalDateTime requestedAt,
        AppointmentStatus status,
        LocalDateTime createdAt) {
}
