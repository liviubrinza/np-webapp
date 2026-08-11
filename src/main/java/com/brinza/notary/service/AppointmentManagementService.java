package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import com.brinza.notary.dto.AppointmentMonthlyStatsView;
import com.brinza.notary.dto.BusyTimeSlots;
import com.brinza.notary.dto.DayAvailability;
import com.brinza.notary.dto.InternalNoteView;
import com.brinza.notary.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class AppointmentManagementService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentManagementService.class);

    private static final DateTimeFormatter CHANGE_LOG_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final LocalTime WORKDAY_START = LocalTime.of(9, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);

    private final AppointmentRepository appointmentRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final AppointmentEmailService appointmentEmailService;

    public AppointmentManagementService(AppointmentRepository appointmentRepository,
                                         ServiceCatalogService serviceCatalogService,
                                         AppointmentEmailService appointmentEmailService) {
        this.appointmentRepository = appointmentRepository;
        this.serviceCatalogService = serviceCatalogService;
        this.appointmentEmailService = appointmentEmailService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentListItemView> search(Set<AppointmentStatus> statuses, LocalDateTime from, LocalDateTime to, String clientName) {
        log.info("search called with statuses={} from={} to={} clientName={}", statuses, from, to, clientName);
        String normalizedName = (clientName == null || clientName.isBlank()) ? null : clientName.trim();
        Set<AppointmentStatus> normalizedStatuses = (statuses == null || statuses.isEmpty()) ? null : statuses;
        List<AppointmentListItemView> results = appointmentRepository.search(normalizedStatuses, from, to, normalizedName).stream()
                .map(this::toListItem)
                .toList();
        log.debug("search matched {} appointment(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public AppointmentListView searchGrouped(Set<AppointmentStatus> statuses, LocalDateTime from, LocalDateTime to, String clientName) {
        log.info("searchGrouped called with statuses={} from={} to={} clientName={}", statuses, from, to, clientName);
        List<AppointmentListItemView> all = search(statuses, from, to, clientName);

        List<AppointmentListItemView> pending = all.stream()
                .filter(a -> a.status() == AppointmentStatus.PENDING)
                .sorted(Comparator.comparing(AppointmentListItemView::createdAt))
                .toList();

        List<AppointmentListItemView> others = all.stream()
                .filter(a -> a.status() != AppointmentStatus.PENDING)
                .sorted(Comparator.comparing(AppointmentListItemView::requestedAt))
                .toList();

        return new AppointmentListView(pending, others);
    }

    @Transactional(readOnly = true)
    public List<AppointmentListItemView> findByDate(LocalDate date) {
        log.info("findByDate called for date={}", date);
        return search(null, date.atStartOfDay(), date.atTime(LocalTime.MAX), null).stream()
                .sorted(Comparator.comparing(AppointmentListItemView::requestedAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, DayAvailability> monthAvailability(YearMonth month) {
        log.info("monthAvailability called for month={}", month);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.atEndOfMonth().atTime(LocalTime.MAX);
        List<Appointment> appointments = appointmentRepository.search(null, from, to, null);

        Map<LocalDate, List<Appointment>> byDay = appointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(a -> a.getRequestedAt().toLocalDate()));
        log.debug("monthAvailability grouped {} appointment(s) across {} day(s)", appointments.size(), byDay.size());

        Map<LocalDate, DayAvailability> result = new LinkedHashMap<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            result.put(date, dayAvailability(byDay.getOrDefault(date, List.of())));
        }
        return result;
    }

    private DayAvailability dayAvailability(List<Appointment> nonCancelledAppointments) {
        log.debug("dayAvailability called for {} appointment(s)", nonCancelledAppointments.size());
        if (nonCancelledAppointments.isEmpty()) {
            return DayAvailability.FREE;
        }

        int startOfDay = WORKDAY_START.toSecondOfDay() / 60;
        int endOfDay = WORKDAY_END.toSecondOfDay() / 60;

        List<int[]> intervals = new ArrayList<>();
        for (Appointment appointment : nonCancelledAppointments) {
            int start = Math.max(appointment.getRequestedAt().toLocalTime().toSecondOfDay() / 60, startOfDay);
            int end = Math.min(appointment.getEndedAt().toLocalTime().toSecondOfDay() / 60, endOfDay);
            if (end > start) {
                intervals.add(new int[]{start, end});
            }
        }
        if (intervals.isEmpty()) {
            return DayAvailability.PARTIAL;
        }

        intervals.sort(Comparator.comparingInt(i -> i[0]));
        int mergedStart = intervals.get(0)[0];
        int mergedEnd = intervals.get(0)[1];
        for (int i = 1; i < intervals.size(); i++) {
            int[] current = intervals.get(i);
            if (current[0] > mergedEnd) {
                return DayAvailability.PARTIAL;
            }
            mergedEnd = Math.max(mergedEnd, current[1]);
        }

        return (mergedStart <= startOfDay && mergedEnd >= endOfDay) ? DayAvailability.FULL : DayAvailability.PARTIAL;
    }

    /**
     * Appointment counts by status, grouped by the month the appointment was created in
     * (not the requested/scheduled date), for the admin statistics "Cereri" tab. {@code from}/
     * {@code to} bound the creation month (inclusive on both ends); either or both may be null
     * to leave that end open, and both null returns every month on record.
     */
    @Transactional(readOnly = true)
    public List<AppointmentMonthlyStatsView> monthlyStatusSummary(YearMonth from, YearMonth to) {
        log.info("monthlyStatusSummary called for from={} to={}", from, to);
        LocalDateTime fromDateTime = from != null ? from.atDay(1).atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atEndOfMonth().atTime(LocalTime.MAX) : null;

        Map<YearMonth, List<Appointment>> byMonth = appointmentRepository.findAllByCreatedAtRange(fromDateTime, toDateTime).stream()
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getCreatedAt())));
        log.debug("Grouped appointments into {} month(s)", byMonth.size());

        return byMonth.entrySet().stream()
                .map(entry -> {
                    Map<AppointmentStatus, Long> counts = entry.getValue().stream()
                            .collect(Collectors.groupingBy(Appointment::getStatus, Collectors.counting()));
                    return new AppointmentMonthlyStatsView(
                            entry.getKey().atDay(1),
                            counts.getOrDefault(AppointmentStatus.PENDING, 0L),
                            counts.getOrDefault(AppointmentStatus.CONFIRMED, 0L),
                            counts.getOrDefault(AppointmentStatus.CANCELLED, 0L),
                            counts.getOrDefault(AppointmentStatus.COMPLETED, 0L),
                            (long) entry.getValue().size());
                })
                .sorted(Comparator.comparing(AppointmentMonthlyStatsView::month).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPendingAppointments() {
        return appointmentRepository.existsByStatus(AppointmentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public AppointmentDetailView getDetail(Long id) {
        log.info("getDetail called for id={}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        return toDetailView(appointment);
    }

    /**
     * {@code sendConfirmationEmail} only has an effect for the PENDING -&gt; CONFIRMED transition -
     * it's the admin/technician's explicit answer to the "send a confirmation email?" prompt shown
     * client-side for that specific transition (see {@code admin/appointments/detail.html}), not a
     * generic per-call override of {@link AppointmentEmailService}'s own {@code app.mail.enabled} gate.
     */
    @Transactional
    public void updateStatus(Long id, AppointmentStatus status, String authorUsername, boolean sendConfirmationEmail) {
        log.info("updateStatus called for id={} status={} author={} sendConfirmationEmail={}", id, status, authorUsername, sendConfirmationEmail);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus == status) {
            log.debug("Status unchanged for appointment id={} (already {})", id, status);
            return;
        }
        if (status == AppointmentStatus.CONFIRMED && overlapsConfirmed(appointment)) {
            log.debug("Rejected status change for id={}: overlaps an already-confirmed appointment", id);
            throw new IllegalArgumentException("Programarea se suprapune cu o altă programare confirmată și nu poate fi confirmată.");
        }
        appointment.setStatus(status);
        String note = "Stare schimbată: %s -> %s".formatted(previousStatus.getDisplayName(), status.getDisplayName());
        appointment.addInternalNote(new InternalNote(authorUsername, note));
        log.debug("Appointment id={} status changed {} -> {}", id, previousStatus, status);

        if (previousStatus == AppointmentStatus.PENDING && status == AppointmentStatus.CONFIRMED) {
            if (sendConfirmationEmail) {
                appointmentEmailService.sendConfirmedEmail(appointment);
            } else {
                log.debug("Confirmation email declined by {} for appointment id={}", authorUsername, id);
            }
        }
    }

    @Transactional
    public void updateSchedule(Long id, LocalDateTime requestedAt, LocalDateTime endedAt, String authorUsername) {
        log.info("updateSchedule called for id={} requestedAt={} endedAt={} author={}", id, requestedAt, endedAt, authorUsername);
        if (!endedAt.isAfter(requestedAt)) {
            log.debug("Rejected schedule update for id={}: endedAt not after requestedAt", id);
            throw new IllegalArgumentException("Ora de sfârșit trebuie să fie după ora de început.");
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        LocalDateTime previousRequestedAt = appointment.getRequestedAt();
        LocalDateTime previousEndedAt = appointment.getEndedAt();
        if (previousRequestedAt.equals(requestedAt) && previousEndedAt.equals(endedAt)) {
            log.debug("Schedule unchanged for appointment id={}", id);
            return;
        }
        if (appointmentRepository.existsOverlapping(AppointmentStatus.CONFIRMED, id, requestedAt, endedAt)) {
            log.debug("Rejected schedule update for id={}: new schedule overlaps a confirmed appointment", id);
            throw new IllegalArgumentException("Noua dată și oră se suprapun cu o programare deja confirmată.");
        }
        appointment.setRequestedAt(requestedAt);
        appointment.setEndedAt(endedAt);
        String note = "Stare schimbată: %s-%s -> %s-%s".formatted(
                previousRequestedAt.format(CHANGE_LOG_FORMAT), previousEndedAt.toLocalTime(),
                requestedAt.format(CHANGE_LOG_FORMAT), endedAt.toLocalTime());
        appointment.addInternalNote(new InternalNote(authorUsername, note));
        log.debug("Appointment id={} schedule changed {}-{} -> {}-{}", id, previousRequestedAt, previousEndedAt, requestedAt, endedAt);
    }

    @Transactional
    public void addInternalNote(Long id, String authorUsername, String note) {
        log.info("addInternalNote called for id={} author={}", id, authorUsername);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        appointment.addInternalNote(new InternalNote(authorUsername, note));
    }

    private AppointmentListItemView toListItem(Appointment appointment) {
        return new AppointmentListItemView(
                appointment.getId(),
                appointment.getClientName(),
                serviceCatalogService.resolveName(appointment.getService(), Locale.of("ro")),
                appointment.getRequestedAt(),
                appointment.getEndedAt(),
                appointment.getStatus(),
                overlapsConfirmedForDisplay(appointment),
                appointment.getCreatedAt()
        );
    }

    private AppointmentDetailView toDetailView(Appointment appointment) {
        return new AppointmentDetailView(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getEmail(),
                appointment.getPhone(),
                serviceCatalogService.resolveName(appointment.getService(), Locale.of("ro")),
                appointment.getRequestedAt(),
                appointment.getEndedAt(),
                appointment.getStatus(),
                overlapsConfirmedForDisplay(appointment),
                appointment.getNotes(),
                appointment.getInternalNotes().stream()
                        .map(n -> new InternalNoteView(n.getAuthorUsername(), n.getNote(), n.getCreatedAt()))
                        .toList().reversed(),
                appointment.getCreatedAt()
        );
    }

    /**
     * The overlap flag shown in the list/detail views (red "!" badge, warning box). Conflicts are
     * only meaningful for appointments still in play - PENDING (needs resolving before it can be
     * confirmed) or CONFIRMED (should never happen, kept as a safety net). A CANCELLED or COMPLETED
     * appointment is displayed as-is, with no conflict check run against it at all, regardless of
     * whether its old time range happens to clash with something now CONFIRMED.
     */
    private boolean overlapsConfirmedForDisplay(Appointment appointment) {
        AppointmentStatus status = appointment.getStatus();
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            return false;
        }
        return overlapsConfirmed(appointment);
    }

    /**
     * Whether {@code appointment}'s time range overlaps a different, already-CONFIRMED
     * appointment, regardless of {@code appointment}'s own status. Confirmed appointments never
     * overlap each other (enforced in {@link #updateStatus} and {@link #updateSchedule}), so this
     * is only ever true for a non-confirmed appointment that clashes with one that already is.
     * Used directly (not through {@link #overlapsConfirmedForDisplay}) by {@link #updateStatus},
     * which must check this on every transition into CONFIRMED - including from CANCELLED - not
     * just from PENDING.
     */
    private boolean overlapsConfirmed(Appointment appointment) {
        return appointmentRepository.existsOverlapping(AppointmentStatus.CONFIRMED, appointment.getId(),
                appointment.getRequestedAt(), appointment.getEndedAt());
    }

    /**
     * Of {@code candidateTimes} (each an "HH:mm" time-of-day, e.g. the reschedule form's
     * dropdown options), which ones the reschedule form should grey out in the start dropdown
     * and which in the end dropdown, on {@code date}. {@code excludeAppointmentId} is the
     * appointment being rescheduled, so its own current slot never blocks itself when it's the
     * one already CONFIRMED.
     *
     * <p>Start and end use different boundary rules, because touching a confirmed appointment's
     * boundary is not the same on both sides: starting exactly when a confirmed appointment
     * starts is already an overlap (your appointment then runs into it), so a start time is busy
     * when {@code otherStart <= t < otherEnd}. Ending exactly when a confirmed appointment
     * starts is fine (back-to-back, no overlap) - ending exactly when one *ends* always still
     * overlaps it (your appointment must have started before that instant), so an end time is
     * busy when {@code otherStart < t <= otherEnd}.
     *
     * <p>This only flags a candidate time that itself falls inside a busy span; it can't by
     * itself guarantee an arbitrary (start, end) pair never straddles one without either endpoint
     * landing inside it. {@link #updateSchedule} is the actual guarantee - this is just the UI hint.
     */
    @Transactional(readOnly = true)
    public BusyTimeSlots findBusyTimeSlots(LocalDate date, Long excludeAppointmentId, List<String> candidateTimes) {
        log.debug("findBusyTimeSlots called for date={} excludeAppointmentId={}", date, excludeAppointmentId);
        List<Appointment> confirmed = appointmentRepository.search(Set.of(AppointmentStatus.CONFIRMED),
                        date.atStartOfDay(), date.atTime(LocalTime.MAX), null).stream()
                .filter(a -> !a.getId().equals(excludeAppointmentId))
                .toList();

        Set<String> busyStart = new LinkedHashSet<>();
        Set<String> busyEnd = new LinkedHashSet<>();
        for (String candidate : candidateTimes) {
            LocalTime t = LocalTime.parse(candidate);
            boolean startBusy = confirmed.stream().anyMatch(a ->
                    !t.isBefore(a.getRequestedAt().toLocalTime()) && t.isBefore(a.getEndedAt().toLocalTime()));
            boolean endBusy = confirmed.stream().anyMatch(a ->
                    t.isAfter(a.getRequestedAt().toLocalTime()) && !t.isAfter(a.getEndedAt().toLocalTime()));
            if (startBusy) {
                busyStart.add(candidate);
            }
            if (endBusy) {
                busyEnd.add(candidate);
            }
        }
        return new BusyTimeSlots(busyStart, busyEnd);
    }
}
