package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.dto.ServiceAdminDetailView;
import com.brinza.notary.dto.ServiceAdminListItemView;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.ServiceAdminManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ServiceAdminController.class)
@Import(AdminSessionRegistry.class)
@WithMockUser(roles = "TECHNICIAN")
class ServiceAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ServiceAdminManagementService serviceAdminManagementService;
    @MockitoBean
    private AdminActivityLogger adminActivityLogger;

    private static java.util.Map<String, String> formParams(String code) {
        return java.util.Map.of(
                "code", code,
                "durationMinutes", "30",
                "nameEn", "Name EN",
                "nameRo", "Nume RO",
                "nameHu", "Nev HU");
    }

    @Test
    void listRendersServicesFromService() throws Exception {
        when(serviceAdminManagementService.listAll()).thenReturn(
                List.of(new ServiceAdminListItemView(1L, "code", "Nume RO", 30, true)));

        mockMvc.perform(get("/admin/settings/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/list"))
                .andExpect(model().attribute("services",
                        List.of(new ServiceAdminListItemView(1L, "code", "Nume RO", 30, true))));
    }

    @Test
    void newFormRendersEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/settings/services/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/form"));
    }

    @Test
    void createWithBlankCodeRedisplaysFormWithoutCallingService() throws Exception {
        var request = post("/admin/settings/services").with(csrf());
        formParams("").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/form"));
    }

    @Test
    void createSuccessRedirectsWithFlashMessage() throws Exception {
        var request = post("/admin/settings/services").with(csrf());
        formParams("new-code").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings/services"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void createDuplicateCodeRedisplaysFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Acest cod de serviciu este deja folosit."))
                .when(serviceAdminManagementService).create(any());

        var request = post("/admin/settings/services").with(csrf());
        formParams("dup-code").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/form"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void editFormPrefillsFromService() throws Exception {
        when(serviceAdminManagementService.getForEdit(1L)).thenReturn(
                new ServiceAdminDetailView(1L, "code", 30, true,
                        "Name EN", "Desc EN", "Nume RO", "Desc RO", "Nev HU", "Leiras HU"));

        mockMvc.perform(get("/admin/settings/services/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/services/form"));
    }

    @Test
    void deleteSuccessRedirectsWithFlashMessage() throws Exception {
        mockMvc.perform(post("/admin/settings/services/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings/services"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void deleteBlockedByAppointmentsRedirectsWithFlashError() throws Exception {
        doThrow(new IllegalArgumentException("Acest serviciu are programari asociate."))
                .when(serviceAdminManagementService).delete(anyLong());

        mockMvc.perform(post("/admin/settings/services/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings/services"))
                .andExpect(flash().attributeExists("error"));
    }
}
