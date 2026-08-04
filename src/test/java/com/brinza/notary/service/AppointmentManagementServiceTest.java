package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import com.brinza.notary.dto.AppointmentMonthlyStatsView;
import com.brinza.notary.dto.BusyTimeSlots;
import com.brinza.notary.dto.DayAvailability;
import com.brinza.notary.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentManagementServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ServiceCatalogService serviceCatalogService;
    @Mock
    private AppointmentEmailService appointmentEmailService;

    private AppointmentManagementService service() {
        return new AppointmentManagementService(appointmentRepository, serviceCatalogService, appointmentEmailService);
    }

    private final Service notaryService = new Service(30, true);

    // ---- search / searchGrouped ----

    @Test
    void searchGroupedSeparatesPendingFromOthersAndSortsEach() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment pendingOld = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        pendingOld.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        Appointment pendingNew = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 2, 9, 0));
        pendingNew.setCreatedAt(LocalDateTime.of(2026, 7, 2, 9, 0));
        Appointment confirmedLater = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 5, 9, 0));
        Appointment confirmedEarlier = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 3, 9, 0));

        when(appointmentRepository.search(null, null, null, null))
                .thenReturn(List.of(pendingNew, pendingOld, confirmedLater, confirmedEarlier));

        AppointmentListView view = service().searchGrouped(null, null, null, null);

        assertThat(view.pending()).extracting(AppointmentListItemView::requestedAt)
                .containsExactly(LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 2, 9, 0));
        assertThat(view.others()).extracting(AppointmentListItemView::requestedAt)
                .containsExactly(LocalDateTime.of(2026, 8, 3, 9, 0), LocalDateTime.of(2026, 8, 5, 9, 0));
    }

    @Test
    void searchBlankNameIsNormalizedToNull() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        when(appointmentRepository.search(isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        service().search(null, null, null, "   ");

        verify(appointmentRepository).search(isNull(), isNull(), isNull(), isNull());
    }

    // ---- findByDate ----

    @Test
    void findByDateSortsByRequestedAt() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment late = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 15, 0));
        Appointment early = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(late, early));

        List<AppointmentListItemView> result = service().findByDate(LocalDate.of(2026, 8, 1));

        assertThat(result).extracting(AppointmentListItemView::requestedAt)
                .containsExactly(LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 15, 0));
    }

    // ---- monthAvailability ----

    @Test
    void dayWithNoAppointmentsIsFree() {
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of());

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.FREE);
    }

    @Test
    void cancelledAppointmentsAreExcludedLeavingDayFree() {
        Appointment cancelled = appointmentWith(AppointmentStatus.CANCELLED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 17, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(cancelled));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.FREE);
    }

    @Test
    void appointmentCoveringEntireWorkdayIsFull() {
        Appointment fullDay = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 17, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(fullDay));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.FULL);
    }

    @Test
    void partialCoverageLeavesDayPartial() {
        Appointment morning = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(morning));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.PARTIAL);
    }

    @Test
    void adjacentAppointmentsMergeToFillEntireWorkday() {
        Appointment morning = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 12, 0));
        Appointment afternoon = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 12, 0),
                LocalDateTime.of(2026, 8, 1, 17, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(morning, afternoon));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.FULL);
    }

    @Test
    void gapBetweenAppointmentsLeavesDayPartial() {
        Appointment morning = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0));
        Appointment afternoon = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 15, 0),
                LocalDateTime.of(2026, 8, 1, 17, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(morning, afternoon));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.PARTIAL);
    }

    @Test
    void appointmentEntirelyOutsideWorkdayIsClippedAwayButStillReportsPartial() {
        Appointment beforeOpening = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 6, 0),
                LocalDateTime.of(2026, 8, 1, 8, 0));
        when(appointmentRepository.search(isNull(), any(), any(), isNull())).thenReturn(List.of(beforeOpening));

        Map<LocalDate, DayAvailability> availability = service().monthAvailability(YearMonth.of(2026, 8));

        // The appointment clips to a zero-length interval since it's entirely before the workday
        // starts, so no interval is recorded - but the day isn't reported FREE, since a
        // non-cancelled appointment does exist. This documents current behavior.
        assertThat(availability.get(LocalDate.of(2026, 8, 1))).isEqualTo(DayAvailability.PARTIAL);
    }

    // ---- monthlyStatusSummary ----

    @Test
    void monthlyStatusSummaryCountsByStatusPerCreationMonth() {
        Appointment a1 = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        a1.setCreatedAt(LocalDateTime.of(2026, 7, 15, 9, 0));
        Appointment a2 = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 2, 9, 0));
        a2.setCreatedAt(LocalDateTime.of(2026, 7, 20, 9, 0));
        Appointment a3 = appointmentWith(AppointmentStatus.CANCELLED, LocalDateTime.of(2026, 8, 3, 9, 0));
        a3.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        when(appointmentRepository.findAllByCreatedAtRange(null, null)).thenReturn(List.of(a1, a2, a3));

        List<AppointmentMonthlyStatsView> summary = service().monthlyStatusSummary(null, null);

        assertThat(summary).hasSize(2);
        AppointmentMonthlyStatsView julySummary = summary.stream()
                .filter(s -> s.month().equals(LocalDate.of(2026, 7, 1))).findFirst().orElseThrow();
        assertThat(julySummary.pending()).isEqualTo(1);
        assertThat(julySummary.confirmed()).isEqualTo(1);
        assertThat(julySummary.total()).isEqualTo(2);
        // newest month first
        assertThat(summary.get(0).month()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    // ---- hasPendingAppointments ----

    @Test
    void hasPendingAppointmentsReflectsRepository() {
        when(appointmentRepository.existsByStatus(AppointmentStatus.PENDING)).thenReturn(true);

        assertThat(service().hasPendingAppointments()).isTrue();
    }

    @Test
    void hasPendingAppointmentsFalseWhenNoneExist() {
        when(appointmentRepository.existsByStatus(AppointmentStatus.PENDING)).thenReturn(false);

        assertThat(service().hasPendingAppointments()).isFalse();
    }

    // ---- getDetail ----

    @Test
    void getDetailThrowsWhenNotFound() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getDetail(1L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getDetailReturnsNotesNewestFirst() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        appointment.addInternalNote(new com.brinza.notary.domain.InternalNote("titi", "first"));
        appointment.addInternalNote(new com.brinza.notary.domain.InternalNote("titi", "second"));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        AppointmentDetailView detail = service().getDetail(1L);

        assertThat(detail.internalNotes()).extracting(com.brinza.notary.dto.InternalNoteView::note)
                .containsExactly("second", "first");
    }

    // ---- overlap detection ----

    @Test
    void searchFlagsPendingAppointmentThatOverlapsAConfirmedAppointment() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment pending = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.search(null, null, null, null)).thenReturn(List.of(pending));
        when(appointmentRepository.existsOverlapping(eq(AppointmentStatus.CONFIRMED), isNull(),
                eq(pending.getRequestedAt()), eq(pending.getEndedAt()))).thenReturn(true);

        List<AppointmentListItemView> result = service().search(null, null, null, null);

        assertThat(result).singleElement().extracting(AppointmentListItemView::overlapsConfirmed).isEqualTo(true);
    }

    @Test
    void searchDoesNotFlagAppointmentWithNoOverlap() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment pending = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.search(null, null, null, null)).thenReturn(List.of(pending));

        List<AppointmentListItemView> result = service().search(null, null, null, null);

        assertThat(result).singleElement().extracting(AppointmentListItemView::overlapsConfirmed).isEqualTo(false);
    }

    @Test
    void getDetailFlagsOverlapWithConfirmedAppointment() {
        lenient().when(serviceCatalogService.resolveName(any(), eq(Locale.of("ro")))).thenReturn("Svc");
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlapping(eq(AppointmentStatus.CONFIRMED), any(), any(), any())).thenReturn(true);

        AppointmentDetailView detail = service().getDetail(1L);

        assertThat(detail.overlapsConfirmed()).isTrue();
    }

    // ---- updateStatus ----

    @Test
    void updateStatusIsNoOpWhenStatusUnchanged() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateStatus(1L, AppointmentStatus.PENDING, "titi", false);

        assertThat(appointment.getInternalNotes()).isEmpty();
    }

    @Test
    void updateStatusAddsInternalNoteOnChange() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateStatus(1L, AppointmentStatus.CANCELLED, "titi", false);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getInternalNotes()).hasSize(1);
        assertThat(appointment.getInternalNotes().get(0).getNote()).contains("În așteptare").contains("Anulată");
    }

    @Test
    void updateStatusSendsConfirmationEmailOnlyWhenTransitionAndFlagBothMatch() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateStatus(1L, AppointmentStatus.CONFIRMED, "titi", true);

        verify(appointmentEmailService).sendConfirmedEmail(appointment);
    }

    @Test
    void updateStatusDoesNotSendEmailWhenFlagFalse() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateStatus(1L, AppointmentStatus.CONFIRMED, "titi", false);

        verify(appointmentEmailService, never()).sendConfirmedEmail(any());
    }

    @Test
    void updateStatusDoesNotSendEmailForOtherTransitions() {
        Appointment appointment = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateStatus(1L, AppointmentStatus.COMPLETED, "titi", true);

        verify(appointmentEmailService, never()).sendConfirmedEmail(any());
    }

    @Test
    void updateStatusRejectsConfirmingWhenOverlappingAnAlreadyConfirmedAppointment() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlapping(eq(AppointmentStatus.CONFIRMED), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service().updateStatus(1L, AppointmentStatus.CONFIRMED, "titi", false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getInternalNotes()).isEmpty();
        verify(appointmentEmailService, never()).sendConfirmedEmail(any());
    }

    @Test
    void updateStatusConfirmsWhenNoOverlapExists() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlapping(eq(AppointmentStatus.CONFIRMED), any(), any(), any())).thenReturn(false);

        service().updateStatus(1L, AppointmentStatus.CONFIRMED, "titi", false);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    // ---- updateSchedule ----

    @Test
    void updateScheduleRejectsEndNotAfterStart() {
        AppointmentManagementService service = service();
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);

        assertThatThrownBy(() -> service.updateSchedule(1L, start, start, "titi"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateScheduleIsNoOpWhenUnchanged() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 8, 1, 9, 30);
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, requestedAt, endedAt);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().updateSchedule(1L, requestedAt, endedAt, "titi");

        assertThat(appointment.getInternalNotes()).isEmpty();
    }

    @Test
    void updateScheduleAddsInternalNoteOnChange() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 9, 30));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        LocalDateTime newStart = LocalDateTime.of(2026, 8, 1, 11, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 1, 11, 30);

        service().updateSchedule(1L, newStart, newEnd, "titi");

        assertThat(appointment.getRequestedAt()).isEqualTo(newStart);
        assertThat(appointment.getInternalNotes()).hasSize(1);
    }

    @Test
    void updateScheduleRejectsReschedulingConfirmedAppointmentIntoOverlapWithAnotherConfirmed() {
        Appointment appointment = appointmentWith(AppointmentStatus.CONFIRMED, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 9, 30));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        LocalDateTime newStart = LocalDateTime.of(2026, 8, 1, 11, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 1, 11, 30);
        when(appointmentRepository.existsOverlapping(AppointmentStatus.CONFIRMED, 1L, newStart, newEnd)).thenReturn(true);

        assertThatThrownBy(() -> service().updateSchedule(1L, newStart, newEnd, "titi"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(appointment.getRequestedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 0));
        assertThat(appointment.getInternalNotes()).isEmpty();
    }

    @Test
    void updateScheduleRejectsReschedulingPendingAppointmentIntoOverlapWithAConfirmedOne() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 9, 30));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        LocalDateTime newStart = LocalDateTime.of(2026, 8, 1, 11, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 1, 11, 30);
        when(appointmentRepository.existsOverlapping(AppointmentStatus.CONFIRMED, 1L, newStart, newEnd)).thenReturn(true);

        assertThatThrownBy(() -> service().updateSchedule(1L, newStart, newEnd, "titi"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(appointment.getRequestedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 0));
        assertThat(appointment.getInternalNotes()).isEmpty();
    }

    @Test
    void updateSchedulePermitsReschedulingPendingAppointmentWhenNoOverlapExists() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 9, 30));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        LocalDateTime newStart = LocalDateTime.of(2026, 8, 1, 11, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 1, 11, 30);
        when(appointmentRepository.existsOverlapping(AppointmentStatus.CONFIRMED, 1L, newStart, newEnd)).thenReturn(false);

        service().updateSchedule(1L, newStart, newEnd, "titi");

        assertThat(appointment.getRequestedAt()).isEqualTo(newStart);
    }

    // ---- findBusyTimeSlots ----

    @Test
    void findBusyTimeSlotsMarksStartSlotsInsideAConfirmedAppointmentAsBusy() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Appointment confirmed = appointmentWith(AppointmentStatus.CONFIRMED,
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0));
        ReflectionTestUtils.setField(confirmed, "id", 2L);
        when(appointmentRepository.search(eq(Set.of(AppointmentStatus.CONFIRMED)), any(), any(), isNull()))
                .thenReturn(List.of(confirmed));

        BusyTimeSlots busy = service().findBusyTimeSlots(date, 1L,
                List.of("09:30", "10:00", "10:30", "11:00", "11:30"));

        // Starting exactly when the confirmed appointment starts (10:00) is already an overlap;
        // starting exactly when it ends (11:00) is not (it's after, back-to-back).
        assertThat(busy.startTimes()).containsExactlyInAnyOrder("10:00", "10:30");
    }

    @Test
    void findBusyTimeSlotsMarksEndSlotsUsingTheOppositeBoundary() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Appointment confirmed = appointmentWith(AppointmentStatus.CONFIRMED,
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0));
        ReflectionTestUtils.setField(confirmed, "id", 2L);
        when(appointmentRepository.search(eq(Set.of(AppointmentStatus.CONFIRMED)), any(), any(), isNull()))
                .thenReturn(List.of(confirmed));

        BusyTimeSlots busy = service().findBusyTimeSlots(date, 1L,
                List.of("09:30", "10:00", "10:30", "11:00", "11:30"));

        // Ending exactly when the confirmed appointment starts (10:00) is fine (back-to-back, no
        // overlap); ending exactly when it ends (11:00) always overlaps it, since your own start
        // must be before that instant.
        assertThat(busy.endTimes()).containsExactlyInAnyOrder("10:30", "11:00");
    }

    @Test
    void findBusyTimeSlotsExcludesTheAppointmentBeingRescheduledItself() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        Appointment confirmed = appointmentWith(AppointmentStatus.CONFIRMED,
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 11, 0));
        ReflectionTestUtils.setField(confirmed, "id", 5L);
        when(appointmentRepository.search(eq(Set.of(AppointmentStatus.CONFIRMED)), any(), any(), isNull()))
                .thenReturn(List.of(confirmed));

        BusyTimeSlots busy = service().findBusyTimeSlots(date, 5L,
                List.of("09:30", "10:00", "10:30", "11:00", "11:30"));

        assertThat(busy.startTimes()).isEmpty();
        assertThat(busy.endTimes()).isEmpty();
    }

    // ---- addInternalNote ----

    @Test
    void addInternalNoteAppendsNote() {
        Appointment appointment = appointmentWith(AppointmentStatus.PENDING, LocalDateTime.of(2026, 8, 1, 9, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        service().addInternalNote(1L, "titi", "a note");

        assertThat(appointment.getInternalNotes()).hasSize(1);
        assertThat(appointment.getInternalNotes().get(0).getNote()).isEqualTo("a note");
    }

    @Test
    void addInternalNoteThrowsWhenAppointmentNotFound() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().addInternalNote(1L, "titi", "note"))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Appointment appointmentWith(AppointmentStatus status, LocalDateTime requestedAt) {
        return appointmentWith(status, requestedAt, requestedAt.plusMinutes(30));
    }

    private Appointment appointmentWith(AppointmentStatus status, LocalDateTime requestedAt, LocalDateTime endedAt) {
        Appointment appointment = new Appointment("Client", "client@example.com", "0700000000", notaryService,
                requestedAt, endedAt, null);
        appointment.setStatus(status);
        return appointment;
    }
}
