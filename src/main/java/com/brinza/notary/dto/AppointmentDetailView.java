package com.brinza.notary.dto;

import com.brinza.notary.domain.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AppointmentDetailView(
        Long id,
        String clientName,
        String email,
        String phone,
        String serviceName,
        LocalDateTime requestedAt,
        LocalDateTime endedAt,
        AppointmentStatus status,
        String notes,
        List<InternalNoteView> internalNotes,
        LocalDateTime createdAt) {
}
