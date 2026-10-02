package com.brinza.notary.controller.admin;

import com.brinza.notary.domain.AdminTheme;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes whether any PENDING appointment exists, so the navbar can show a
 * notification mark on the "Programări" link across every admin page.
 *
 * <p>{@code @WebMvcTest} slices pick up every {@code @ControllerAdvice} bean in the
 * application regardless of its {@code basePackages}, including slices for controllers
 * that never provide an {@link AppointmentManagementService} bean. An {@link ObjectProvider}
 * defers that lookup to call time instead of constructor injection, so bean creation never
 * fails in those slices - it just resolves to no pending-appointments notification.
 *
 * <p>Also exposes the logged-in user's {@code data-bs-theme} value (light/dark), read from
 * their profile, so every admin page renders in that user's theme. The same
 * {@link ObjectProvider} pattern applies; anonymous requests (the login page) get none.
 */
@ControllerAdvice(basePackages = "com.brinza.notary.controller.admin")
public class AdminGlobalModelAttributes {

    private final ObjectProvider<AppointmentManagementService> appointmentManagementService;
    private final ObjectProvider<ProfileService> profileService;

    public AdminGlobalModelAttributes(ObjectProvider<AppointmentManagementService> appointmentManagementService,
                                      ObjectProvider<ProfileService> profileService) {
        this.appointmentManagementService = appointmentManagementService;
        this.profileService = profileService;
    }

    @ModelAttribute("hasPendingAppointments")
    public boolean hasPendingAppointments() {
        AppointmentManagementService service = appointmentManagementService.getIfAvailable();
        return service != null && service.hasPendingAppointments();
    }

    @ModelAttribute("adminTheme")
    public String adminTheme(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        ProfileService service = profileService.getIfAvailable();
        AdminTheme theme = service != null ? service.getTheme(authentication.getName()) : AdminTheme.LIGHT;
        return theme.attributeValue();
    }
}
