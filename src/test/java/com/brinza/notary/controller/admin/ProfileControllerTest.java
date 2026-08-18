package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.ProfileService;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileController.class)
@Import({AdminSessionRegistry.class, TrafficStatsService.class, GeoLocationService.class})
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProfileService profileService;
    @MockitoBean
    private AdminActivityLogger adminActivityLogger;

    @Test
    void showRendersCurrentUsername() throws Exception {
        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profile"))
                .andExpect(model().attribute("username", "titi"));
    }

    @Test
    void showRendersFullName() throws Exception {
        when(profileService.getFullName("titi")).thenReturn("Titi Full Name");

        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("fullName", "Titi Full Name"))
                .andExpect(content().string(containsString("Titi Full Name")));
    }

    @Test
    void adminPageIsNoindexed() throws Exception {
        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"robots\" content=\"noindex,nofollow\"")));
    }

    @Test
    void changePasswordValidationErrorRedisplaysForm() throws Exception {
        mockMvc.perform(post("/admin/profile/password").with(csrf())
                        .param("currentPassword", "")
                        .param("newPassword", "newpw123")
                        .param("confirmNewPassword", "newpw123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profile"));
    }

    @Test
    void changePasswordSuccessRedirectsWithFlash() throws Exception {
        mockMvc.perform(post("/admin/profile/password").with(csrf())
                        .param("currentPassword", "current")
                        .param("newPassword", "newpw123")
                        .param("confirmNewPassword", "newpw123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profile"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void changePasswordWrongCurrentPasswordRedisplaysFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Parola curenta este incorecta."))
                .when(profileService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/admin/profile/password").with(csrf())
                        .param("currentPassword", "wrong")
                        .param("newPassword", "newpw123")
                        .param("confirmNewPassword", "newpw123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profile"))
                .andExpect(model().attributeExists("error"));
    }
}
