package com.brinza.notary.controller.web;

import com.brinza.notary.config.AdminSessionRegistry;
import com.brinza.notary.config.SecurityConfig;
import com.brinza.notary.dto.AppointmentConfirmationView;
import com.brinza.notary.service.AppointmentBookingService;
import com.brinza.notary.service.ServiceCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BookingController.class)
@Import({AdminSessionRegistry.class, SecurityConfig.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ServiceCatalogService serviceCatalogService;
    @MockitoBean
    private AppointmentBookingService appointmentBookingService;

    @Test
    void showFormRendersEmptyBookingRequestAndServices() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());

        mockMvc.perform(get("/ro/book"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/book"))
                .andExpect(model().attributeExists("bookingRequest", "services", "timeSlots"));
    }

    @Test
    void submitWithValidationErrorsRedisplaysFormWithoutBooking() throws Exception {
        when(serviceCatalogService.findActiveServices(any())).thenReturn(List.of());

        mockMvc.perform(post("/ro/book").with(csrf())
                        .param("clientName", "")
                        .param("email", "not-an-email")
                        .param("phone", "0700000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/book"));

        verify(appointmentBookingService, never()).book(any(), any());
    }

    @Test
    void submitWithValidDataRedirectsToConfirmation() throws Exception {
        when(appointmentBookingService.book(any(), any())).thenReturn(
                new AppointmentConfirmationView("Ion Popescu", "Autentificare", LocalDateTime.of(2026, 8, 1, 10, 0)));

        mockMvc.perform(post("/ro/book").with(csrf())
                        .param("clientName", "Ion Popescu")
                        .param("email", "ion@example.com")
                        .param("phone", "0700000000")
                        .param("serviceId", "1")
                        .param("requestedAt", "2099-08-01T10:00")
                        .param("notes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ro/book/confirmation"));
    }

    @Test
    void confirmationRendersConfirmationView() throws Exception {
        mockMvc.perform(get("/ro/book/confirmation"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/book-confirmation"));
    }
}
