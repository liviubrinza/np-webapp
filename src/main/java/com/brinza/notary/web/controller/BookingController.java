package com.brinza.notary.web.controller;

import com.brinza.notary.dto.BookingRequest;
import com.brinza.notary.service.AppointmentBookingService;
import com.brinza.notary.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/{lang:en|ro|hu}/book")
public class BookingController {

    private static final int OPENING_HOUR = 9;
    private static final int CLOSING_HOUR = 17;

    private final ServiceCatalogService serviceCatalogService;
    private final AppointmentBookingService appointmentBookingService;

    public BookingController(ServiceCatalogService serviceCatalogService,
                              AppointmentBookingService appointmentBookingService) {
        this.serviceCatalogService = serviceCatalogService;
        this.appointmentBookingService = appointmentBookingService;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("bookingRequest", new BookingRequest());
        model.addAttribute("services", serviceCatalogService.findActiveServices(LocaleContextHolder.getLocale()));
        model.addAttribute("timeSlots", buildTimeSlots());
        return "public/book";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                          BindingResult bindingResult,
                          Model model,
                          @PathVariable String lang,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("services", serviceCatalogService.findActiveServices(LocaleContextHolder.getLocale()));
            model.addAttribute("timeSlots", buildTimeSlots());
            return "public/book";
        }

        var confirmation = appointmentBookingService.book(bookingRequest, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("appointment", confirmation);
        return "redirect:/" + lang + "/book/confirmation";
    }

    @GetMapping("/confirmation")
    public String confirmation() {
        return "public/book-confirmation";
    }

    private static List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (int hour = OPENING_HOUR; hour <= CLOSING_HOUR; hour++) {
            slots.add(String.format("%02d:00", hour));
            if (hour != CLOSING_HOUR) {
                slots.add(String.format("%02d:30", hour));
            }
        }
        return slots;
    }
}
