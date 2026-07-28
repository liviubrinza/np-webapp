package com.brinza.notary.admin.controller;

import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.DocumentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/appointments")
public class AppointmentAdminController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentAdminController.class);

    private static final LocalTime SCHEDULE_START = LocalTime.of(9, 0);
    private static final LocalTime SCHEDULE_END = LocalTime.of(18, 0);

    private final AppointmentManagementService appointmentManagementService;
    private final DocumentManagementService documentManagementService;
    private final AdminActivityLogger adminActivityLogger;
    private final boolean mailEnabled;

    public AppointmentAdminController(AppointmentManagementService appointmentManagementService,
                                       DocumentManagementService documentManagementService,
                                       AdminActivityLogger adminActivityLogger,
                                       @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.appointmentManagementService = appointmentManagementService;
        this.documentManagementService = documentManagementService;
        this.adminActivityLogger = adminActivityLogger;
        this.mailEnabled = mailEnabled;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AppointmentStatus status,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) String name,
                        Model model) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        var grouped = appointmentManagementService.searchGrouped(status, fromDateTime, toDateTime, name);
        model.addAttribute("pendingAppointments", grouped.pending());
        model.addAttribute("otherAppointments", grouped.others());
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("name", name);
        return "admin/appointments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String back, Model model) {
        model.addAttribute("appointment", appointmentManagementService.getDetail(id));
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("timeSlots", buildTimeSlots());
        model.addAttribute("documents", documentManagementService.listForAppointment(id));
        model.addAttribute("backUrl", sanitizeBack(back));
        model.addAttribute("mailEnabled", mailEnabled);
        return "admin/appointments/detail";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocuments(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files,
                                   @RequestParam(required = false) String back,
                                   Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            documentManagementService.upload(id, files, authentication.getName());
            adminActivityLogger.log("Uploaded %d document(s) to appointment #%d".formatted(files == null ? 0 : files.size(), id));
            redirectAttributes.addFlashAttribute("success", "Document(s) uploaded.");
        } catch (IllegalArgumentException e) {
            log.debug("Could not upload documents for id={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToDetail(id, back);
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
        return documentManagementService.download(id, documentId);
    }

    @PostMapping("/{id}/documents/{documentId}/delete")
    public String deleteDocument(@PathVariable Long id, @PathVariable Long documentId,
                                  @RequestParam(required = false) String back,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        documentManagementService.delete(id, documentId, authentication.getName());
        adminActivityLogger.log("Deleted document #%d from appointment #%d".formatted(documentId, id));
        redirectAttributes.addFlashAttribute("success", "Document șters.");
        return redirectToDetail(id, back);
    }

    @PostMapping("/{id}/schedule")
    public String updateSchedule(@PathVariable Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                                  @RequestParam(required = false) String back,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            appointmentManagementService.updateSchedule(id, LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime), authentication.getName());
            adminActivityLogger.log("Rescheduled appointment #%d to %s %s-%s".formatted(id, date, startTime, endTime));
            redirectAttributes.addFlashAttribute("success", "Programare actualizată.");
        } catch (IllegalArgumentException e) {
            log.debug("Could not update schedule for id={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToDetail(id, back);
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                                @RequestParam(required = false, defaultValue = "false") boolean sendConfirmationEmail,
                                @RequestParam(required = false) String back,
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        appointmentManagementService.updateStatus(id, status, authentication.getName(), sendConfirmationEmail);
        adminActivityLogger.log("Changed appointment #%d status to %s%s".formatted(id, status, sendConfirmationEmail ? " (confirmation email sent)" : ""));
        redirectAttributes.addFlashAttribute("success", "Stare actualizată.");
        return redirectToDetail(id, back);
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String note,
                           @RequestParam(required = false) String back,
                           Authentication authentication, RedirectAttributes redirectAttributes) {
        if (note == null || note.isBlank()) {
            log.debug("Could not add note for id={}: blank note", id);
            redirectAttributes.addFlashAttribute("error", "Nota nu poate fi goală.");
            return redirectToDetail(id, back);
        }
        appointmentManagementService.addInternalNote(id, authentication.getName(), note);
        adminActivityLogger.log("Added an internal note to appointment #%d".formatted(id));
        redirectAttributes.addFlashAttribute("success", "Notă adăugată.");
        return redirectToDetail(id, back);
    }

    private static String redirectToDetail(Long id, String back) {
        String encodedBack = URLEncoder.encode(sanitizeBack(back), StandardCharsets.UTF_8);
        return "redirect:/admin/appointments/" + id + "?back=" + encodedBack;
    }

    /**
     * Only ever redirects back into the admin appointments list or calendar - {@code back} is
     * client-supplied (query param), so it must never be trusted as an arbitrary redirect target.
     */
    private static String sanitizeBack(String back) {
        if (back != null && (back.equals("/admin/appointments") 
             || back.startsWith("/admin/appointments?")
             || back.equals("/admin/calendar") 
             || back.startsWith("/admin/calendar?"))) {
            return back;
        }
        return "/admin/appointments";
    }

    private static List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (LocalTime t = SCHEDULE_START; !t.isAfter(SCHEDULE_END); t = t.plusMinutes(30)) {
            slots.add(t.toString());
        }
        return slots;
    }
}
