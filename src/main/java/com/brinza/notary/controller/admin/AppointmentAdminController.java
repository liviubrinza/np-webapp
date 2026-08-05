package com.brinza.notary.controller.admin;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.dto.BookingRequest;
import com.brinza.notary.dto.BusyTimeSlots;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AppointmentBookingService;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.DocumentManagementService;
import com.brinza.notary.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/appointments")
public class AppointmentAdminController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentAdminController.class);

    private static final LocalTime SCHEDULE_START = LocalTime.of(9, 0);
    private static final LocalTime SCHEDULE_END = LocalTime.of(18, 0);

    private static final int BOOKING_OPENING_HOUR = 9;
    private static final int BOOKING_CLOSING_HOUR = 17;

    private final AppointmentManagementService appointmentManagementService;
    private final DocumentManagementService documentManagementService;
    private final AdminActivityLogger adminActivityLogger;
    private final SystemSettings systemSettings;
    private final ServiceCatalogService serviceCatalogService;
    private final AppointmentBookingService appointmentBookingService;

    public AppointmentAdminController(AppointmentManagementService appointmentManagementService,
                                       DocumentManagementService documentManagementService,
                                       AdminActivityLogger adminActivityLogger,
                                       SystemSettings systemSettings,
                                       ServiceCatalogService serviceCatalogService,
                                       AppointmentBookingService appointmentBookingService) {
        this.appointmentManagementService = appointmentManagementService;
        this.documentManagementService = documentManagementService;
        this.adminActivityLogger = adminActivityLogger;
        this.systemSettings = systemSettings;
        this.serviceCatalogService = serviceCatalogService;
        this.appointmentBookingService = appointmentBookingService;
    }

    @GetMapping
    public String showList(@RequestParam(required = false) Set<AppointmentStatus> status,
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
        model.addAttribute("selectedStatuses", status == null ? Set.of() : status);
        model.addAttribute("statusFilterLabel", statusFilterLabel(status));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("name", name);
        return "admin/appointments/list";
    }

    private static String statusFilterLabel(Set<AppointmentStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "Toate";
        }
        return EnumSet.copyOf(statuses).stream()
                .map(AppointmentStatus::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("bookingRequest", new BookingRequest());
        populateNewFormModel(model, LocalDate.now(), null);
        return "admin/appointments/new";
    }

    @PostMapping("/new")
    public String createAppointment(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                                     BindingResult bindingResult,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                                     Model model,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        LocalDate formDate = bookingRequest.getRequestedAt() != null
                ? bookingRequest.getRequestedAt().toLocalDate() : LocalDate.now();
        if (bindingResult.hasErrors()) {
            log.debug("New appointment form has {} validation error(s)", bindingResult.getErrorCount());
            populateNewFormModel(model, formDate, endTime);
            return "admin/appointments/new";
        }

        try {
            Long id = appointmentBookingService.bookAsAdmin(bookingRequest, endTime);
            adminActivityLogger.log("Created appointment #%d for %s".formatted(id, bookingRequest.getClientName()));
            redirectAttributes.addFlashAttribute("success", "Programare creată.");
            return "redirect:/admin/appointments/" + id;
        } catch (IllegalArgumentException e) {
            log.debug("Could not create appointment: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            populateNewFormModel(model, formDate, endTime);
            return "admin/appointments/new";
        }
    }

    private void populateNewFormModel(Model model, LocalDate date, LocalTime selectedEndTime) {
        model.addAttribute("services", serviceCatalogService.findActiveServices(Locale.of("ro")));
        model.addAttribute("selectedDate", date);
        model.addAttribute("startTimeSlots", buildBookingTimeSlots());
        List<String> timeSlots = buildTimeSlots();
        model.addAttribute("endTimeSlots", timeSlots);
        model.addAttribute("selectedEndTime", selectedEndTime == null ? null : selectedEndTime.toString());
        model.addAttribute("dayAppointments", appointmentManagementService.findByDate(date));
        model.addAttribute("busyTimeSlots", appointmentManagementService.findBusyTimeSlots(date, null, timeSlots));
    }

    @GetMapping("/new/busy-times")
    @ResponseBody
    public BusyTimeSlots newBusyTimes(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentManagementService.findBusyTimeSlots(date, null, buildTimeSlots());
    }

    @GetMapping("/new/day-schedule")
    public String newDaySchedule(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        model.addAttribute("appointments", appointmentManagementService.findByDate(date));
        return "admin/fragments :: appointmentsTimeline(appointments=${appointments})";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, @RequestParam(required = false) String back, Model model) {
        var appointment = appointmentManagementService.getDetail(id);
        List<String> timeSlots = buildTimeSlots();
        model.addAttribute("appointment", appointment);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("busyTimeSlots",
                appointmentManagementService.findBusyTimeSlots(appointment.requestedAt().toLocalDate(), id, timeSlots));
        model.addAttribute("documents", documentManagementService.listForAppointment(id));
        model.addAttribute("backUrl", sanitizeBack(back));
        model.addAttribute("mailEnabled", systemSettings.isMailEnabled());
        return "admin/appointments/detail";
    }

    @GetMapping("/{id}/busy-times")
    @ResponseBody
    public BusyTimeSlots busyTimes(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentManagementService.findBusyTimeSlots(date, id, buildTimeSlots());
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
        adminActivityLogger.log("Downloading document id=%d from appointment id=%d".formatted(documentId, id));
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
        try {
            appointmentManagementService.updateStatus(id, status, authentication.getName(), sendConfirmationEmail);
            adminActivityLogger.log("Changed appointment #%d status to %s%s".formatted(id, status, sendConfirmationEmail ? " (confirmation email sent)" : ""));
            redirectAttributes.addFlashAttribute("success", "Stare actualizată.");
        } catch (IllegalArgumentException e) {
            log.debug("Could not update status for id={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
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

    /**
     * Matches {@link BookingRequest#isRequestedAtOnHalfHour()} exactly (9:00-17:00), unlike
     * {@link #buildTimeSlots()} above which covers the wider 9:00-18:00 schedule range used for
     * appointment start/end times.
     */
    private static List<String> buildBookingTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (int hour = BOOKING_OPENING_HOUR; hour <= BOOKING_CLOSING_HOUR; hour++) {
            slots.add(String.format("%02d:00", hour));
            if (hour != BOOKING_CLOSING_HOUR) {
                slots.add(String.format("%02d:30", hour));
            }
        }
        return slots;
    }
}
