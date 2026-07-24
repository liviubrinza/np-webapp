package com.brinza.notary.dto;

import java.time.LocalDateTime;

public record InternalNoteView(String authorUsername, String note, LocalDateTime createdAt) {
}
