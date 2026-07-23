package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.dto.AppointmentDetailView;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.repository.AppointmentRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public List<AppointmentListItemView> search(AppointmentStatus status, LocalDateTime from, LocalDateTime to) {
        return appointmentRepository.search(status, from, to).stream()
                .map(this::toListItem)
                .toList();
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
    public void updateInternalNotes(Long id, String internalNotes) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + id));
        appointment.setInternalNotes(internalNotes);
    }

    private AppointmentListItemView toListItem(Appointment appointment) {
        return new AppointmentListItemView(
                appointment.getId(),
                appointment.getClientName(),
                serviceCatalogService.resolveName(appointment.getService(), Locale.ENGLISH),
                appointment.getRequestedAt(),
                appointment.getStatus()
        );
    }

    private AppointmentDetailView toDetailView(Appointment appointment) {
        return new AppointmentDetailView(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getEmail(),
                appointment.getPhone(),
                serviceCatalogService.resolveName(appointment.getService(), Locale.ENGLISH),
                appointment.getRequestedAt(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getInternalNotes(),
                appointment.getCreatedAt()
        );
    }
}
