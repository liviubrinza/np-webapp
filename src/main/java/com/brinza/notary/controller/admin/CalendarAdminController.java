package com.brinza.notary.controller.admin;

import com.brinza.notary.dto.DayAvailability;
import com.brinza.notary.service.AppointmentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Controller
@RequestMapping("/admin/calendar")
public class CalendarAdminController {

    private static final Logger log = LoggerFactory.getLogger(CalendarAdminController.class);

    private final AppointmentManagementService appointmentManagementService;

    public CalendarAdminController(AppointmentManagementService appointmentManagementService) {
        this.appointmentManagementService = appointmentManagementService;
    }

    @GetMapping
    public String view(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        Map<LocalDate, DayAvailability> availability = appointmentManagementService.monthAvailability(YearMonth.from(selectedDate));

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("appointments", appointmentManagementService.findByDate(selectedDate));
        model.addAttribute("dayAvailabilityJson", toJson(availability));
        return "admin/calendar";
    }

    private static String toJson(Map<LocalDate, DayAvailability> availability) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<LocalDate, DayAvailability> entry : availability.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(entry.getKey()).append("\":\"").append(entry.getValue()).append('"');
            first = false;
        }
        return json.append('}').toString();
    }
}
