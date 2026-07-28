package com.brinza.notary.dto;

import java.time.LocalDate;

/** One row of the Requests statistics tab: appointment counts by status, for a given creation month. */
public record AppointmentMonthlyStatsView(
        LocalDate month,
        long pending,
        long confirmed,
        long cancelled,
        long completed,
        long total) {
}
