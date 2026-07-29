package com.brinza.notary.admin.controller;

import com.brinza.notary.config.SystemSettings;
import com.brinza.notary.service.AdminActivityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        return "admin/settings/list";
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
}
