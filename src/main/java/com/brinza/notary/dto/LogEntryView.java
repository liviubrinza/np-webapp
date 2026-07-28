package com.brinza.notary.dto;

public record LogEntryView(
        String timestamp,
        String correlationId,
        String className,
        String level,
        String message) {
}
