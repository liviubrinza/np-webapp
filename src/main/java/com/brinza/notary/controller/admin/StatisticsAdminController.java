package com.brinza.notary.controller.admin;

import com.brinza.notary.dto.AdminActivityEntryView;
import com.brinza.notary.dto.AppointmentMonthlyStatsView;
import com.brinza.notary.dto.LogEntryView;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.service.LogViewerService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/statistics")
public class StatisticsAdminController {

    private static final Logger log = LoggerFactory.getLogger(StatisticsAdminController.class);

    private static final List<String> LOG_LEVELS = List.of("DEBUG", "INFO", "WARN", "ERROR");

    private final LogViewerService logViewerService;
    private final AppointmentManagementService appointmentManagementService;

    public StatisticsAdminController(LogViewerService logViewerService, AppointmentManagementService appointmentManagementService) {
        this.logViewerService = logViewerService;
        this.appointmentManagementService = appointmentManagementService;
    }

    @GetMapping
    public String showRequests(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
                                Model model) {
        List<AppointmentMonthlyStatsView> monthlyStats = appointmentManagementService.monthlyStatusSummary(from, to);
        log.debug("Rendering {} month(s) of request statistics", monthlyStats.size());

        model.addAttribute("monthlyStats", monthlyStats);
        model.addAttribute("monthlyStatsChartJson", toChartJson(monthlyStats));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/statistics/requests";
    }

    /**
     * Builds the bar-chart data as JSON, chronological (oldest first) rather than the table's
     * newest-first order, since that's the natural reading direction for a time-axis chart.
     */
    private static String toChartJson(List<AppointmentMonthlyStatsView> monthlyStats) {
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("ro"));
        StringBuilder labels = new StringBuilder();
        StringBuilder confirmed = new StringBuilder();
        StringBuilder cancelled = new StringBuilder();
        StringBuilder completed = new StringBuilder();
        boolean first = true;
        for (AppointmentMonthlyStatsView m : monthlyStats.reversed()) {
            if (!first) {
                labels.append(',');
                confirmed.append(',');
                cancelled.append(',');
                completed.append(',');
            }
            labels.append('"').append(monthFormat.format(m.month())).append('"');
            confirmed.append(m.confirmed());
            cancelled.append(m.cancelled());
            completed.append(m.completed());
            first = false;
        }
        return "{\"labels\":[" + labels + "],\"confirmed\":[" + confirmed + "],\"cancelled\":[" + cancelled + "],\"completed\":[" + completed + "]}";
    }

    @GetMapping("/traffic")
    public String showTraffic() {
        return "admin/statistics/traffic";
    }

    @GetMapping("/activity")
    public String showActivity(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String username,
                            @RequestParam(required = false) String correlationId,
                            @RequestParam(required = false) String text,
                            @RequestParam(required = false, defaultValue = "time") String sort,
                            Model model) {
        List<LocalDate> availableDates = logViewerService.availableDates();
        LocalDate selectedDate = date != null ? date : availableDates.stream().findFirst().orElse(null);
        List<AdminActivityEntryView> entries = selectedDate != null
                ? logViewerService.readActivityEntries(selectedDate, username, correlationId, text, "username".equals(sort))
                : List.of();
        log.debug("Rendering {} admin activity entr(ies) for selectedDate={}", entries.size(), selectedDate);

        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("entries", entries);
        model.addAttribute("username", username);
        model.addAttribute("correlationId", correlationId);
        model.addAttribute("text", text);
        model.addAttribute("sort", sort);
        return "admin/statistics/activity";
    }

    @GetMapping("/logs")
    public String showLogs(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) String correlationId,
                        @RequestParam(required = false) String level,
                        @RequestParam(required = false) String className,
                        @RequestParam(required = false) String text,
                        Model model) {
        List<LocalDate> availableDates = logViewerService.availableDates();
        LocalDate selectedDate = date != null ? date : availableDates.stream().findFirst().orElse(null);
        List<LogEntryView> entries = selectedDate != null
                ? logViewerService.readEntries(selectedDate, correlationId, level, className, text)
                : List.of();
        log.debug("Rendering {} log entr(ies) for selectedDate={}", entries.size(), selectedDate);

        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("entries", entries);
        model.addAttribute("correlationId", correlationId);
        model.addAttribute("level", level);
        model.addAttribute("className", className);
        model.addAttribute("text", text);
        model.addAttribute("levels", LOG_LEVELS);
        return "admin/statistics/logs";
    }
}
