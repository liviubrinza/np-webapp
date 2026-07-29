package com.brinza.notary.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestTest {

    @Test
    void nullRequestedAtPassesThrough() {
        assertThat(requestWith(null).isRequestedAtOnHalfHour()).isTrue();
    }

    @Test
    void onTheHourWithinWorkdayIsValid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 10, 0, 0)).isRequestedAtOnHalfHour()).isTrue();
    }

    @Test
    void onTheHalfHourWithinWorkdayIsValid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 10, 30, 0)).isRequestedAtOnHalfHour()).isTrue();
    }

    @Test
    void offGridMinuteIsInvalid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 10, 15, 0)).isRequestedAtOnHalfHour()).isFalse();
    }

    @Test
    void nonZeroSecondIsInvalid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 10, 0, 1)).isRequestedAtOnHalfHour()).isFalse();
    }

    @Test
    void beforeOpeningHourIsInvalid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 8, 30, 0)).isRequestedAtOnHalfHour()).isFalse();
    }

    @Test
    void exactlyOpeningHourIsValid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 9, 0, 0)).isRequestedAtOnHalfHour()).isTrue();
    }

    @Test
    void exactlyClosingHourIsValid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 17, 0, 0)).isRequestedAtOnHalfHour()).isTrue();
    }

    @Test
    void halfHourPastClosingIsInvalid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 17, 30, 0)).isRequestedAtOnHalfHour()).isFalse();
    }

    @Test
    void afterClosingHourIsInvalid() {
        assertThat(requestWith(LocalDateTime.of(2026, 8, 1, 18, 0, 0)).isRequestedAtOnHalfHour()).isFalse();
    }

    private static BookingRequest requestWith(LocalDateTime requestedAt) {
        BookingRequest request = new BookingRequest();
        request.setRequestedAt(requestedAt);
        return request;
    }
}
