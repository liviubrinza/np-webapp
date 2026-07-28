package com.brinza.notary.dto;

public record AdminActivityEntryView(
        String timestamp,
        String correlationId,
        String username,
        String action) {
}
