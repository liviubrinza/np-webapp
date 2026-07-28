package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class AppointmentManagementService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentManagementService.class);

    private static final DateTimeFormatter CHANGE_LOG_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final LocalTime WORKDAY_START = LocalTime.of(9, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);

    private final AppointmentRepository appointmentRepository;
    private final ServiceCatalogService serviceCatalogService;

    public AppointmentManagementService(AppointmentRepository appointmentRepository,
                                         ServiceCatalogService serviceCatalogService) {
        this.appointmentRepository = appointmentRepository;
        this.serviceCatalogService = serviceCatalogService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentListItemView> search(AppointmentStatus status, LocalDateTime from, LocalDateTime to, String clientName) {
        log.info("search called with status={} from={} to={} clientName={}", status, from, to, clientName);
        String normalizedName = (clientName == null || clientName.isBlank()) ? null : clientName.trim();
        List<AppointmentListItemView> results = appointmentRepository.search(status, from, to, normalizedName).stream()
                .map(this::toListItem)
                .toList();
        log.debug("search matched {} appointment(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public AppointmentListView searchGrouped(AppointmentStatus status, LocalDateTime from, LocalDateTime to, String clientName) {
        log.info("searchGrouped called with status={} from={} to={} clientName={}", status, from, to, clientName);
        List<AppointmentListItemView> all = search(status, from, to, clientName);

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

    @Transactional(readOnly = true)
    public AppointmentDetailView getDetail(Long id) {
        log.info("getDetail called for id={}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        return toDetailView(appointment);
    }

    @Transactional
    public void updateStatus(Long id, AppointmentStatus status, String authorUsername) {
        log.info("updateStatus called for id={} status={} author={}", id, status, authorUsername);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        AppointmentStatus previousStatus = appointment.getStatus();
        if (previousStatus == status) {
            log.debug("Status unchanged for appointment id={} (already {})", id, status);
            return;
        }
        appointment.setStatus(status);
        String note = "Stare schimbată: %s -> %s".formatted(previousStatus.getDisplayName(), status.getDisplayName());
        appointment.addInternalNote(new InternalNote(authorUsername, note));
        log.debug("Appointment id={} status changed {} -> {}", id, previousStatus, status);
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
                appointment.getNotes(),
                appointment.getInternalNotes().stream()
                        .map(n -> new InternalNoteView(n.getAuthorUsername(), n.getNote(), n.getCreatedAt()))
                        .toList().reversed(),
                appointment.getCreatedAt()
        );
    }
}
