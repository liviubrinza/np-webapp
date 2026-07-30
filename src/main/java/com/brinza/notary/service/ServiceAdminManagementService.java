package com.brinza.notary.service;

import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.dto.ServiceAdminDetailView;
import com.brinza.notary.dto.ServiceAdminListItemView;
import com.brinza.notary.dto.ServiceForm;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Backs the admin Services editor (Configurare &gt; Servicii). Named ServiceAdminManagementService,
 * not ServiceService, for the same import-clash reason documented on {@link ServiceCatalogService}.
 */
@org.springframework.stereotype.Service
public class ServiceAdminManagementService {

    private static final Logger log = LoggerFactory.getLogger(ServiceAdminManagementService.class);

    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    public ServiceAdminManagementService(ServiceRepository serviceRepository, AppointmentRepository appointmentRepository) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceAdminListItemView> listAll() {
        return serviceRepository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(service -> new ServiceAdminListItemView(
                        service.getId(), service.getCode(), translationName(service, "ro"),
                        service.getDurationMinutes(), service.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceAdminDetailView getForEdit(Long id) {
        Service service = getEntity(id);
        return new ServiceAdminDetailView(
                service.getId(), service.getCode(), service.getDurationMinutes(), service.isActive(),
                translationName(service, "en"), translationDescription(service, "en"),
                translationName(service, "ro"), translationDescription(service, "ro"),
                translationName(service, "hu"), translationDescription(service, "hu"));
    }

    @Transactional
    public void create(ServiceForm form) {
        if (serviceRepository.findByCode(form.getCode()).isPresent()) {
            throw new IllegalArgumentException("Acest cod de serviciu este deja folosit.");
        }
        Service service = new Service(form.getDurationMinutes(), form.isActive());
        service.setCode(form.getCode());
        applyTranslations(service, form);
        serviceRepository.save(service);
        log.debug("Created service code={}", service.getCode());
    }

    @Transactional
    public void update(Long id, ServiceForm form) {
        Service service = getEntity(id);
        serviceRepository.findByCode(form.getCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Acest cod de serviciu este deja folosit.");
                });
        service.setCode(form.getCode());
        service.setDurationMinutes(form.getDurationMinutes());
        service.setActive(form.isActive());
        applyTranslations(service, form);
        log.debug("Updated service id={} code={}", id, service.getCode());
    }

    @Transactional
    public void delete(Long id) {
        Service service = getEntity(id);
        if (appointmentRepository.existsByServiceId(id)) {
            throw new IllegalArgumentException(
                    "Acest serviciu are programări asociate și nu poate fi șters. Îl puteți dezactiva în schimb.");
        }
        serviceRepository.delete(service);
        log.debug("Deleted service id={}", id);
    }

    private Service getEntity(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No service with id " + id));
    }

    private void applyTranslations(Service service, ServiceForm form) {
        setTranslation(service, "en", form.getNameEn(), form.getDescriptionEn());
        setTranslation(service, "ro", form.getNameRo(), form.getDescriptionRo());
        setTranslation(service, "hu", form.getNameHu(), form.getDescriptionHu());
    }

    private void setTranslation(Service service, String locale, String name, String description) {
        service.getTranslations().stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            existing.setName(name);
                            existing.setDescription(description);
                        },
                        () -> service.addTranslation(new ServiceTranslation(locale, name, description)));
    }

    private static String translationName(Service service, String locale) {
        return service.getTranslations().stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .map(ServiceTranslation::getName)
                .orElse("");
    }

    private static String translationDescription(Service service, String locale) {
        return service.getTranslations().stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .map(ServiceTranslation::getDescription)
                .orElse("");
    }
}
