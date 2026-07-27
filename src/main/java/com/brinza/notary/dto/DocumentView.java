package com.brinza.notary.dto;

import java.time.LocalDateTime;

public record DocumentView(Long id, String originalFilename, LocalDateTime uploadedAt) {
}
