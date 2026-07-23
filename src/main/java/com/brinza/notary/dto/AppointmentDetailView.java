package com.brinza.notary.dto;

import com.brinza.notary.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentDetailView(
        Long id,
        String clientName,
        String email,
        String phone,
        String serviceName,
        LocalDateTime requestedAt,
        AppointmentStatus status,
        String notes,
        String internalNotes,
        LocalDateTime createdAt) {
}
