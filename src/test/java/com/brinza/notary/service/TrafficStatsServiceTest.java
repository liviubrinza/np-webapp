package com.brinza.notary.service;

import com.brinza.notary.dto.ClientTrafficView;
import com.brinza.notary.dto.PageTimeView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TrafficStatsServiceTest {

    private final GeoLocationService geoLocationService = mock(GeoLocationService.class);
    private final TrafficStatsService service = new TrafficStatsService(geoLocationService);
    private final Instant t0 = Instant.parse("2026-08-18T10:00:00Z");

    @Test
    void withNoRequestsSnapshotIsEmpty() {
        assertThat(service.snapshot()).isEmpty();
    }

    @Test
    void singlePageViewHasNoCountedTimeYetSinceItsStillPending() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);

        List<ClientTrafficView> snapshot = service.snapshot();
        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.getFirst().ip()).isEqualTo("1.2.3.4");
        assertThat(snapshot.getFirst().totalTime()).isEqualTo(Duration.ZERO);
        assertThat(snapshot.getFirst().sessionCount()).isEqualTo(1);
        assertThat(snapshot.getFirst().pages()).isEmpty();
    }

    @Test
    void gapBelowTimeoutIsAttributedToThePreviousPage() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(3)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.totalTime()).isEqualTo(Duration.ofMinutes(3));
        assertThat(view.sessionCount()).isEqualTo(1);
        assertThat(view.pages()).containsExactly(new PageTimeView(PublicPage.HOME, Duration.ofMinutes(3)));
    }

    @Test
    void repeatedVisitsToSamePageAccumulateDuration() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0.plus(Duration.ofMinutes(2)));
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(5)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.totalTime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(view.sessionCount()).isEqualTo(1);
        assertThat(view.pages()).containsExactly(new PageTimeView(PublicPage.HOME, Duration.ofMinutes(5)));
    }

    @Test
    void gapAtOrAboveTenMinuteTimeoutIsDiscardedAndStartsANewSession() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        // exactly at the timeout boundary - the visitor is considered to have gone idle
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(10)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.totalTime()).isEqualTo(Duration.ZERO);
        assertThat(view.sessionCount()).isEqualTo(2);
        assertThat(view.pages()).isEmpty();
    }

    @Test
    void longIdlePauseIsNotCountedButLaterActivityInTheNewSessionStillAccumulates() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofHours(2)));
        service.recordPageView("1.2.3.4", PublicPage.CONTACT, t0.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(4)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.totalTime()).isEqualTo(Duration.ofMinutes(4));
        assertThat(view.sessionCount()).isEqualTo(2);
        assertThat(view.pages()).containsExactly(new PageTimeView(PublicPage.SERVICES, Duration.ofMinutes(4)));
    }

    @Test
    void statisticsForSameIpAccumulateAcrossSeparateSessions() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(2)));
        // long idle pause ends the first session
        Instant secondSessionStart = t0.plus(Duration.ofHours(1));
        service.recordPageView("1.2.3.4", PublicPage.CONTACT, secondSessionStart);
        service.recordPageView("1.2.3.4", PublicPage.BOOK, secondSessionStart.plus(Duration.ofMinutes(3)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.totalTime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(view.sessionCount()).isEqualTo(2);
        assertThat(view.pages()).containsExactly(
                new PageTimeView(PublicPage.HOME, Duration.ofMinutes(2)),
                new PageTimeView(PublicPage.CONTACT, Duration.ofMinutes(3)));
    }

    @Test
    void sessionCountIncrementsOnceMoreForEachAdditionalIdleGap() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(2)));
        Instant secondSession = t0.plus(Duration.ofHours(1));
        service.recordPageView("1.2.3.4", PublicPage.HOME, secondSession);
        Instant thirdSession = secondSession.plus(Duration.ofHours(1));
        service.recordPageView("1.2.3.4", PublicPage.HOME, thirdSession);

        assertThat(service.snapshot().getFirst().sessionCount()).isEqualTo(3);
    }

    @Test
    void sessionCountsAreTrackedIndependentlyPerIp() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofHours(1)));
        service.recordPageView("5.6.7.8", PublicPage.HOME, t0);

        List<ClientTrafficView> snapshot = service.snapshot();
        assertThat(pagesOfIp(snapshot, "1.2.3.4").sessionCount()).isEqualTo(2);
        assertThat(pagesOfIp(snapshot, "5.6.7.8").sessionCount()).isEqualTo(1);
    }

    private static ClientTrafficView pagesOfIp(List<ClientTrafficView> snapshot, String ip) {
        return snapshot.stream().filter(view -> view.ip().equals(ip)).findFirst().orElseThrow();
    }

    @Test
    void pageOrderWithinAClientsListFollowsPublicPageDeclarationOrderRegardlessOfVisitOrder() {
        // visited in the order Services, Contact, Home, Book
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0);
        service.recordPageView("1.2.3.4", PublicPage.CONTACT, t0.plus(Duration.ofMinutes(1)));
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0.plus(Duration.ofMinutes(3)));
        service.recordPageView("1.2.3.4", PublicPage.BOOK, t0.plus(Duration.ofMinutes(4)));
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(5)));

        List<PublicPage> pageOrder = service.snapshot().getFirst().pages().stream().map(PageTimeView::page).toList();
        assertThat(pageOrder).containsExactly(PublicPage.HOME, PublicPage.SERVICES, PublicPage.CONTACT, PublicPage.BOOK);
    }

    @Test
    void pageOrderIsConsistentAcrossDifferentClientsWithDifferentPagesVisited() {
        // client A: Services, Contact, Home, Book
        service.recordPageView("clientA", PublicPage.SERVICES, t0);
        service.recordPageView("clientA", PublicPage.CONTACT, t0.plus(Duration.ofMinutes(1)));
        service.recordPageView("clientA", PublicPage.HOME, t0.plus(Duration.ofMinutes(3)));
        service.recordPageView("clientA", PublicPage.BOOK, t0.plus(Duration.ofMinutes(4)));
        service.recordPageView("clientA", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(5)));
        // client B: Services, Home, Contact
        service.recordPageView("clientB", PublicPage.SERVICES, t0);
        service.recordPageView("clientB", PublicPage.HOME, t0.plus(Duration.ofMinutes(1)));
        service.recordPageView("clientB", PublicPage.CONTACT, t0.plus(Duration.ofMinutes(2)));
        service.recordPageView("clientB", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(3)));

        List<ClientTrafficView> snapshot = service.snapshot();
        List<PublicPage> clientAOrder = pagesOf(snapshot, "clientA");
        List<PublicPage> clientBOrder = pagesOf(snapshot, "clientB");

        assertThat(clientAOrder).containsExactly(PublicPage.HOME, PublicPage.SERVICES, PublicPage.CONTACT, PublicPage.BOOK);
        assertThat(clientBOrder).containsExactly(PublicPage.HOME, PublicPage.SERVICES, PublicPage.CONTACT);
    }

    private static List<PublicPage> pagesOf(List<ClientTrafficView> snapshot, String ip) {
        return snapshot.stream()
                .filter(view -> view.ip().equals(ip))
                .findFirst()
                .orElseThrow()
                .pages().stream().map(PageTimeView::page).toList();
    }

    @Test
    void differentIpsAreTrackedIndependently() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(2)));
        service.recordPageView("5.6.7.8", PublicPage.CONTACT, t0);
        service.recordPageView("5.6.7.8", PublicPage.BOOK, t0.plus(Duration.ofMinutes(9)));

        List<ClientTrafficView> snapshot = service.snapshot();
        assertThat(snapshot).hasSize(2);
        assertThat(snapshot).extracting(ClientTrafficView::ip).containsExactlyInAnyOrder("1.2.3.4", "5.6.7.8");
    }

    @Test
    void snapshotIsSortedByTotalTimeDescending() {
        service.recordPageView("busy", PublicPage.HOME, t0);
        service.recordPageView("busy", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(9)));
        service.recordPageView("quiet", PublicPage.HOME, t0);
        service.recordPageView("quiet", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(1)));

        List<ClientTrafficView> snapshot = service.snapshot();
        assertThat(snapshot).extracting(ClientTrafficView::ip).containsExactly("busy", "quiet");
    }

    @Test
    void newIpHasNoLocationUntilTheGeoLookupCallbackFires() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);

        assertThat(service.snapshot().getFirst().location()).isNull();
    }

    @Test
    void locationLookupIsTriggeredExactlyOnceForANewIp() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(1)));
        service.recordPageView("1.2.3.4", PublicPage.CONTACT, t0.plus(Duration.ofHours(2)));

        verify(geoLocationService, times(1)).resolveAsync(eq("1.2.3.4"), any());
    }

    @Test
    void locationLookupIsTriggeredOncePerDistinctIp() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("5.6.7.8", PublicPage.HOME, t0);

        verify(geoLocationService).resolveAsync(eq("1.2.3.4"), any());
        verify(geoLocationService).resolveAsync(eq("5.6.7.8"), any());
    }

    @Test
    void locationAppearsOnTheSnapshotOnceTheGeoLookupCallbackResolves() {
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<String>> callback = ArgumentCaptor.forClass(Consumer.class);
        verify(geoLocationService).resolveAsync(eq("1.2.3.4"), callback.capture());
        callback.getValue().accept("Cluj-Napoca, Romania");

        assertThat(service.snapshot().getFirst().location()).isEqualTo("Cluj-Napoca, Romania");
    }

    @Test
    void aLookupThatNeverCallsBackLeavesTheLocationUnresolvedWithoutBreakingTheSnapshot() {
        // geoLocationService is a plain mock, so resolveAsync's callback is simply never invoked -
        // this simulates a lookup that's still pending (or failed) by the time the snapshot is read.
        service.recordPageView("1.2.3.4", PublicPage.HOME, t0);
        service.recordPageView("1.2.3.4", PublicPage.SERVICES, t0.plus(Duration.ofMinutes(1)));

        ClientTrafficView view = service.snapshot().getFirst();
        assertThat(view.location()).isNull();
        assertThat(view.totalTime()).isEqualTo(Duration.ofMinutes(1));
    }
}
