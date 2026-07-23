package com.brinza.notary.admin.controller;

import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.service.AppointmentManagementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/appointments")
public class AppointmentAdminController {

    private final AppointmentManagementService appointmentManagementService;

    public AppointmentAdminController(AppointmentManagementService appointmentManagementService) {
        this.appointmentManagementService = appointmentManagementService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AppointmentStatus status,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        model.addAttribute("appointments", appointmentManagementService.search(status, fromDateTime, toDateTime));
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/appointments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("appointment", appointmentManagementService.getDetail(id));
        model.addAttribute("statuses", AppointmentStatus.values());
        return "admin/appointments/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                                RedirectAttributes redirectAttributes) {
        appointmentManagementService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "Status updated.");
        return "redirect:/admin/appointments/" + id;
    }

    @PostMapping("/{id}/notes")
    public String updateNotes(@PathVariable Long id, @RequestParam(required = false) String internalNotes,
                               RedirectAttributes redirectAttributes) {
        appointmentManagementService.updateInternalNotes(id, internalNotes);
        redirectAttributes.addFlashAttribute("success", "Notes saved.");
        return "redirect:/admin/appointments/" + id;
    }
}
