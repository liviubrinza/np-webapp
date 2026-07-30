package com.brinza.notary.service;

import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.dto.ServiceAdminDetailView;
import com.brinza.notary.dto.ServiceAdminListItemView;
import com.brinza.notary.dto.ServiceForm;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAdminManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private ServiceAdminManagementService service() {
        return new ServiceAdminManagementService(serviceRepository, appointmentRepository);
    }

    @Test
    void createRejectsDuplicateCode() {
        ServiceAdminManagementService service = service();
        when(serviceRepository.findByCode("taken")).thenReturn(Optional.of(new Service(10, true)));

        assertThatThrownBy(() -> service.create(formFor("taken"))).isInstanceOf(IllegalArgumentException.class);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void createSavesServiceWithAllTranslations() {
        ServiceAdminManagementService service = service();
        when(serviceRepository.findByCode("new-code")).thenReturn(Optional.empty());

        service.create(formFor("new-code"));

        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void updateRejectsCodeAlreadyUsedByAnotherService() {
        ServiceAdminManagementService service = service();
        Service existing = new Service(10, true);
        setId(existing, 1L);
        Service other = new Service(10, true);
        setId(other, 2L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.findByCode("taken")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, formFor("taken"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAppliesTranslationsAndFields() {
        ServiceAdminManagementService service = service();
        Service existing = new Service(10, true);
        existing.setCode("code");
        existing.addTranslation(new ServiceTranslation("ro", "Vechi", "Descriere veche"));
        setId(existing, 1L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.findByCode("code")).thenReturn(Optional.of(existing));

        ServiceForm form = formFor("code");
        form.setDurationMinutes(60);
        form.setActive(false);
        service.update(1L, form);

        assertThat(existing.getDurationMinutes()).isEqualTo(60);
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getTranslations()).hasSize(3);
        ServiceTranslation ro = existing.getTranslations().stream()
                .filter(t -> t.getLocale().equals("ro")).findFirst().orElseThrow();
        assertThat(ro.getName()).isEqualTo("Nume RO");
    }

    @Test
    void deleteRejectsServiceWithAppointments() {
        ServiceAdminManagementService service = service();
        Service existing = new Service(10, true);
        setId(existing, 1L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByServiceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(IllegalArgumentException.class);
        verify(serviceRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesServiceWithoutAppointments() {
        ServiceAdminManagementService service = service();
        Service existing = new Service(10, true);
        setId(existing, 1L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByServiceId(1L)).thenReturn(false);

        service.delete(1L);

        verify(serviceRepository).delete(existing);
    }

    @Test
    void getForEditThrowsWhenNotFound() {
        ServiceAdminManagementService service = service();
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForEdit(99L)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void getForEditMapsTranslationsByLocale() {
        ServiceAdminManagementService service = service();
        Service existing = new Service(30, true);
        existing.setCode("code");
        existing.addTranslation(new ServiceTranslation("en", "Name EN", "Desc EN"));
        existing.addTranslation(new ServiceTranslation("ro", "Nume RO", "Desc RO"));
        setId(existing, 1L);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));

        ServiceAdminDetailView view = service.getForEdit(1L);

        assertThat(view.nameEn()).isEqualTo("Name EN");
        assertThat(view.nameRo()).isEqualTo("Nume RO");
        assertThat(view.nameHu()).isEqualTo("");
    }

    @Test
    void listAllSortsByCodeAndUsesRomanianName() {
        ServiceAdminManagementService service = service();
        Service b = new Service(10, true);
        b.setCode("b-code");
        b.addTranslation(new ServiceTranslation("ro", "B Nume", "desc"));
        Service a = new Service(10, true);
        a.setCode("a-code");
        a.addTranslation(new ServiceTranslation("ro", "A Nume", "desc"));
        when(serviceRepository.findAll()).thenReturn(List.of(b, a));

        List<ServiceAdminListItemView> views = service.listAll();

        assertThat(views).extracting(ServiceAdminListItemView::code).containsExactly("a-code", "b-code");
        assertThat(views.get(0).nameRo()).isEqualTo("A Nume");
    }

    private static ServiceForm formFor(String code) {
        ServiceForm form = new ServiceForm();
        form.setCode(code);
        form.setDurationMinutes(30);
        form.setActive(true);
        form.setNameEn("Name EN");
        form.setDescriptionEn("Desc EN");
        form.setNameRo("Nume RO");
        form.setDescriptionRo("Desc RO");
        form.setNameHu("Nev HU");
        form.setDescriptionHu("Leiras HU");
        return form;
    }

    private static void setId(Service serviceEntity, Long id) {
        try {
            var field = Service.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(serviceEntity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
