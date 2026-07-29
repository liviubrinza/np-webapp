package com.brinza.notary.config.properties;

import com.brinza.notary.domain.AppointmentStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppointmentSeedProperties(List<AppointmentDefinition> demoAppointments) {

    public record AppointmentDefinition(String clientName, String email, String phone, String bookedService,
                                         LocalDateTime requestedAt, LocalDateTime bookedDate, int duration,
                                         AppointmentStatus status, String notes) {
    }
}
