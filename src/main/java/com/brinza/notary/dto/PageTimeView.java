package com.brinza.notary.dto;

import com.brinza.notary.service.PublicPage;

import java.time.Duration;

/** Time a client spent on a single public page, one row of {@link ClientTrafficView#pages()}. */
public record PageTimeView(PublicPage page, Duration time) {
}
