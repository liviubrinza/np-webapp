package com.brinza.notary.dto;

import java.time.Duration;
import java.util.List;

/**
 * One row of the Traffic statistics page: a client (identified by IP), their time on site, how
 * many separate sessions they've established ({@code sessionCount} - a session ends after 10
 * minutes of inactivity, see {@code TrafficStatsService.SESSION_TIMEOUT}), and their approximate
 * (city-level) {@code location}, which is {@code null} until the background lookup resolves it
 * (or forever, if it fails - see {@code GeoLocationService}).
 */
public record ClientTrafficView(String ip, Duration totalTime, int sessionCount, String location, List<PageTimeView> pages) {
}
