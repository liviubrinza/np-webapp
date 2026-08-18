package com.brinza.notary.service;

import com.brinza.notary.dto.ClientTrafficView;
import com.brinza.notary.dto.PageTimeView;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory, per-IP breakdown of how long visitors spend on each public page. Deliberately not
 * persisted - this is runtime-only traffic insight for the admin Statistics &gt; Traffic tab, not
 * a durable record, so it resets on every app restart and is never written to the database.
 *
 * <p>Time on a page is inferred from the gap between consecutive requests from the same IP,
 * since HTTP gives no signal on its own for when a visitor leaves a page. A gap of less than
 * {@link #SESSION_TIMEOUT} is counted as time spent on the page that preceded it; a longer gap
 * means the visitor went idle, so that pause is discarded rather than being attributed to the
 * page as reading time, and the next request starts a fresh session. The very last page in an
 * still-ongoing session has no known duration yet - it's only added to the total once another
 * request arrives (or is discarded if that request never comes within the timeout).
 *
 * <p>Sessions for the same IP are independent: totals keep accumulating across sessions for as
 * long as the app runs, only the idle gap between sessions is excluded.
 *
 * <p>The first time an IP is ever seen, its approximate location is resolved in the background
 * via {@link GeoLocationService} and attached to that IP's entry once available - this never
 * blocks page-view recording, and a lookup that fails or never returns just leaves the location
 * unset.
 */
@Service
public class TrafficStatsService {

    static final Duration SESSION_TIMEOUT = Duration.ofMinutes(10);

    private final Map<String, ClientSession> sessionsByIp = new ConcurrentHashMap<>();
    private final GeoLocationService geoLocationService;

    public TrafficStatsService(GeoLocationService geoLocationService) {
        this.geoLocationService = geoLocationService;
    }

    public void recordPageView(String ip, PublicPage page) {
        recordPageView(ip, page, Instant.now());
    }

    void recordPageView(String ip, PublicPage page, Instant now) {
        AtomicBoolean isNewClient = new AtomicBoolean(false);
        ClientSession session = sessionsByIp.computeIfAbsent(ip, key -> {
            isNewClient.set(true);
            return new ClientSession();
        });
        session.recordPageView(page, now);
        if (isNewClient.get()) {
            geoLocationService.resolveAsync(ip, session::setLocation);
        }
    }

    public List<ClientTrafficView> snapshot() {
        return sessionsByIp.entrySet().stream()
                .map(entry -> entry.getValue().toView(entry.getKey()))
                .sorted(Comparator.comparing(ClientTrafficView::totalTime).reversed())
                .toList();
    }

    private static final class ClientSession {

        private final Map<PublicPage, Duration> timeByPage = new EnumMap<>(PublicPage.class);
        private PublicPage pendingPage;
        private Instant pendingSince;
        private int sessionCount;
        private String location;

        synchronized void recordPageView(PublicPage page, Instant now) {
            boolean isNewSession = true;
            if (pendingPage != null) {
                Duration gap = Duration.between(pendingSince, now);
                if (!gap.isNegative() && gap.compareTo(SESSION_TIMEOUT) < 0) {
                    timeByPage.merge(pendingPage, gap, Duration::plus);
                    isNewSession = false;
                }
            }
            if (isNewSession) {
                sessionCount++;
            }
            pendingPage = page;
            pendingSince = now;
        }

        synchronized void setLocation(String location) {
            this.location = location;
        }

        synchronized ClientTrafficView toView(String ip) {
            // timeByPage is an EnumMap, so this iterates in PublicPage's declared (natural)
            // order - every client's page list is sorted the same way, not by that client's
            // own time-per-page, so the rows line up across the whole snapshot.
            List<PageTimeView> pages = timeByPage.entrySet().stream()
                    .map(entry -> new PageTimeView(entry.getKey(), entry.getValue()))
                    .toList();
            Duration total = pages.stream().map(PageTimeView::time).reduce(Duration.ZERO, Duration::plus);
            return new ClientTrafficView(ip, total, sessionCount, location, pages);
        }
    }
}
