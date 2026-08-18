package com.brinza.notary.controller.admin;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.GeoLocationService;
import com.brinza.notary.service.TrafficStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SystemSettingsAdminController.class)
@Import({AdminSessionRegistry.class, TrafficStatsService.class, GeoLocationService.class})
@WithMockUser(roles = "TECHNICIAN")
class SystemSettingsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SystemSettings systemSettings;
    @MockitoBean
    private AdminActivityLogger adminActivityLogger;

    @Test
    void showRendersCurrentMailEnabledValue() throws Exception {
        when(systemSettings.isMailEnabled()).thenReturn(true);

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/list"))
                .andExpect(model().attribute("mailEnabled", true));
    }

    @Test
    void showRendersCurrentLoginLockoutValues() throws Exception {
        when(systemSettings.getLoginLockoutMaxAttempts()).thenReturn(5);
        when(systemSettings.getLoginLockoutLockDurationMinutes()).thenReturn(15);

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginLockoutMaxAttempts", 5))
                .andExpect(model().attribute("loginLockoutLockDurationMinutes", 15));
    }

    @Test
    void updatingLoginLockoutSettingsCallsSetters() throws Exception {
        mockMvc.perform(post("/admin/settings/login-lockout").with(csrf())
                        .param("maxAttempts", "10")
                        .param("lockDurationMinutes", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("success"));

        verify(systemSettings).setLoginLockoutMaxAttempts(10);
        verify(systemSettings).setLoginLockoutLockDurationMinutes(30);
    }

    @Test
    void invalidLoginLockoutSettingRedirectsWithFlashError() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Numărul de încercări trebuie să fie cel puțin 1."))
                .when(systemSettings).setLoginLockoutMaxAttempts(0);

        mockMvc.perform(post("/admin/settings/login-lockout").with(csrf())
                        .param("maxAttempts", "0")
                        .param("lockDurationMinutes", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void togglingOnCallsSetMailEnabledTrue() throws Exception {
        mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("success"));

        verify(systemSettings).setMailEnabled(true);
    }

    @Test
    void submittingWithoutEnabledParamSetsFalse() throws Exception {
        mockMvc.perform(post("/admin/settings/mail-enabled").with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(systemSettings).setMailEnabled(false);
    }

    @Test
    void showRendersCurrentLogLevelValue() throws Exception {
        when(systemSettings.getLogLevel()).thenReturn(LogLevel.DEBUG);

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("logLevel", LogLevel.DEBUG))
                .andExpect(model().attribute("logLevels", LogLevel.values()));
    }

    @Test
    void updatingLogLevelCallsSetter() throws Exception {
        mockMvc.perform(post("/admin/settings/log-level").with(csrf())
                        .param("level", "WARN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"))
                .andExpect(flash().attributeExists("success"));

        verify(systemSettings).setLogLevel(LogLevel.WARN);
    }
}
