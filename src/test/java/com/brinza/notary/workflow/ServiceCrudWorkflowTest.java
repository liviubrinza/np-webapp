package com.brinza.notary.workflow;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Service;
import com.brinza.notary.domain.ServiceTranslation;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class ServiceCrudWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    private static java.util.Map<String, String> formParams(String code) {
        return java.util.Map.of(
                "code", code,
                "durationMinutes", "30",
                "nameEn", "Name EN",
                "nameRo", "Nume RO",
                "nameHu", "Nev HU");
    }

    @Test
    void createPersistsServiceWithAllTranslations() throws Exception {
        var request = post("/admin/settings/services").with(csrf());
        formParams("workflow-new-service").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings/services"));

        Service created = serviceRepository.findByCode("workflow-new-service").orElseThrow();
        assertThat(created.getDurationMinutes()).isEqualTo(30);
        assertThat(created.isActive()).isTrue();
        assertThat(created.getTranslations()).hasSize(3);
    }

    @Test
    void createDuplicateCodeRedisplaysFormAndDoesNotDuplicate() throws Exception {
        serviceRepository.save(serviceWithCode("existing-code"));

        var request = post("/admin/settings/services").with(csrf());
        formParams("existing-code").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/form"));

        assertThat(serviceRepository.findAll().stream().filter(s -> s.getCode().equals("existing-code")).count())
                .isEqualTo(1);
    }

    @Test
    void editUpdatesDurationAndTranslations() throws Exception {
        Service existing = serviceRepository.save(serviceWithCode("workflow-editable"));

        var request = post("/admin/settings/services/" + existing.getId()).with(csrf());
        java.util.Map<String, String> params = new java.util.HashMap<>(formParams("workflow-editable"));
        params.put("durationMinutes", "90");
        params.put("nameRo", "Nume Actualizat");
        params.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings/services"));

        Service reloaded = serviceRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getDurationMinutes()).isEqualTo(90);
        assertThat(reloaded.getTranslations().stream()
                .filter(t -> t.getLocale().equals("ro")).findFirst().orElseThrow().getName())
                .isEqualTo("Nume Actualizat");
    }

    @Test
    void deletingServiceWithAppointmentsIsRejected() throws Exception {
        Service existing = serviceRepository.save(serviceWithCode("workflow-with-appointment"));
        appointmentRepository.save(new Appointment("Client", "client@example.com", "0700000000", existing,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30), null));

        mockMvc.perform(post("/admin/settings/services/" + existing.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(serviceRepository.findById(existing.getId())).isPresent();
    }

    @Test
    void deletingServiceWithoutAppointmentsRemovesIt() throws Exception {
        Service existing = serviceRepository.save(serviceWithCode("workflow-deletable"));

        mockMvc.perform(post("/admin/settings/services/" + existing.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertThat(serviceRepository.findById(existing.getId())).isEmpty();
    }

    private static Service serviceWithCode(String code) {
        Service service = new Service(30, true);
        service.setCode(code);
        service.addTranslation(new ServiceTranslation("en", "Name EN", "Desc EN"));
        service.addTranslation(new ServiceTranslation("ro", "Nume RO", "Desc RO"));
        service.addTranslation(new ServiceTranslation("hu", "Nev HU", "Leiras HU"));
        return service;
    }
}
