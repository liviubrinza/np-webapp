package com.brinza.notary.admin.controller;

import com.brinza.notary.dto.ChangePasswordForm;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final AdminActivityLogger adminActivityLogger;

    public ProfileController(ProfileService profileService, AdminActivityLogger adminActivityLogger) {
        this.profileService = profileService;
        this.adminActivityLogger = adminActivityLogger;
    }

    @GetMapping
    public String show(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "admin/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                  BindingResult bindingResult,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.debug("Change-password form has {} validation error(s)", bindingResult.getErrorCount());
            model.addAttribute("username", authentication.getName());
            return "admin/profile";
        }
        try {
            profileService.changePassword(authentication.getName(), form.getCurrentPassword(), form.getNewPassword());
            adminActivityLogger.log("Changed own password");
        } catch (IllegalArgumentException e) {
            log.debug("Could not change password for username={}: {}", authentication.getName(), e.getMessage());
            model.addAttribute("username", authentication.getName());
            model.addAttribute("error", e.getMessage());
            return "admin/profile";
        }
        redirectAttributes.addFlashAttribute("success", "Parolă actualizată cu succes.");
        return "redirect:/admin/profile";
    }
}
