package com.brinza.notary.controller.admin;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.service.AdminActivityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class SystemSettingsAdminController {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsAdminController.class);

    private final SystemSettings systemSettings;
    private final AdminActivityLogger adminActivityLogger;

    public SystemSettingsAdminController(SystemSettings systemSettings, AdminActivityLogger adminActivityLogger) {
        this.systemSettings = systemSettings;
        this.adminActivityLogger = adminActivityLogger;
    }

    @GetMapping
    public String show(Model model) {
        model.addAttribute("mailEnabled", systemSettings.isMailEnabled());
        model.addAttribute("loginLockoutMaxAttempts", systemSettings.getLoginLockoutMaxAttempts());
        model.addAttribute("loginLockoutLockDurationMinutes", systemSettings.getLoginLockoutLockDurationMinutes());
        model.addAttribute("logLevel", systemSettings.getLogLevel());
        model.addAttribute("logLevels", LogLevel.values());
        return "admin/settings/list";
    }

    @PostMapping("/log-level")
    public String updateLogLevel(@RequestParam LogLevel level, RedirectAttributes redirectAttributes) {
        systemSettings.setLogLevel(level);
        adminActivityLogger.log("Setat nivel logging aplicație la " + level);
        redirectAttributes.addFlashAttribute("success", "Setare actualizată.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/mail-enabled")
    public String updateMailEnabled(@RequestParam(required = false, defaultValue = "false") boolean enabled,
                                     RedirectAttributes redirectAttributes) {
        log.info("Setting mail.enabled to {}", enabled);
        systemSettings.setMailEnabled(enabled);
        adminActivityLogger.log("Setat trimitere email-uri pe " + (enabled ? "activat" : "dezactivat"));
        redirectAttributes.addFlashAttribute("success", "Setare actualizată.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/login-lockout")
    public String updateLoginLockout(@RequestParam int maxAttempts, @RequestParam int lockDurationMinutes,
                                      RedirectAttributes redirectAttributes) {
        try {
            systemSettings.setLoginLockoutMaxAttempts(maxAttempts);
            systemSettings.setLoginLockoutLockDurationMinutes(lockDurationMinutes);
            adminActivityLogger.log("Setat blocare autentificare la %d încercări / %d minute".formatted(maxAttempts, lockDurationMinutes));
            redirectAttributes.addFlashAttribute("success", "Setare actualizată.");
        } catch (IllegalArgumentException e) {
            log.debug("Login lockout setting rejected: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
