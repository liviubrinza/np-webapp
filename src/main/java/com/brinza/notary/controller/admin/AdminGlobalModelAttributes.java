package com.brinza.notary.controller.admin;

import com.brinza.notary.service.AppointmentManagementService;
import org.springframework.beans.factory.ObjectProvider;
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
 */
@ControllerAdvice(basePackages = "com.brinza.notary.controller.admin")
public class AdminGlobalModelAttributes {

    private final ObjectProvider<AppointmentManagementService> appointmentManagementService;

    public AdminGlobalModelAttributes(ObjectProvider<AppointmentManagementService> appointmentManagementService) {
        this.appointmentManagementService = appointmentManagementService;
    }

    @ModelAttribute("hasPendingAppointments")
    public boolean hasPendingAppointments() {
        AppointmentManagementService service = appointmentManagementService.getIfAvailable();
        return service != null && service.hasPendingAppointments();
    }
}
