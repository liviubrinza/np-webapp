package com.brinza.notary.dto;

import java.time.LocalDateTime;

public record AppointmentConfirmationView(String clientName, String serviceName, LocalDateTime requestedAt) {
}
