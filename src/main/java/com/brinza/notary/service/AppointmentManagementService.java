package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import com.brinza.notary.dto.InternalNoteView;
import com.brinza.notary.repository.AppointmentRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@org.springframework.stereotype.Service
public class AppointmentManagementService {

    private final AppointmentRepository appointmentRepository;
    private final ServiceCatalogService serviceCatalogService;

    public AppointmentManagementService(AppointmentRepository appointmentRepository,
                                         ServiceCatalogService serviceCatalogService) {
        this.appointmentRepository = appointmentRepository;
        this.serviceCatalogService = serviceCatalogService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentListItemView> search(AppointmentStatus status, LocalDateTime from, LocalDateTime to, String clientName) {
        String normalizedName = (clientName == null || clientName.isBlank()) ? null : clientName.trim();
        return appointmentRepository.search(status, from, to, normalizedName).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentListView searchGrouped(AppointmentStatus status, LocalDateTime from, LocalDateTime to, String clientName) {
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
    public AppointmentDetailView getDetail(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        return toDetailView(appointment);
    }

    @Transactional
    public void updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        appointment.setStatus(status);
    }

    @Transactional
    public void addInternalNote(Long id, String authorUsername, String note) {
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
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getInternalNotes().stream()
                        .map(n -> new InternalNoteView(n.getAuthorUsername(), n.getNote(), n.getCreatedAt()))
                        .toList(),
                appointment.getCreatedAt()
        );
    }
}
