package com.brinza.notary.admin.controller;

import com.brinza.notary.dto.ChangePasswordForm;
import com.brinza.notary.service.ProfileService;
import jakarta.validation.Valid;
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

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
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
            model.addAttribute("username", authentication.getName());
            return "admin/profile";
        }
        try {
            profileService.changePassword(authentication.getName(), form.getCurrentPassword(), form.getNewPassword());
        } catch (IllegalArgumentException e) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("error", e.getMessage());
            return "admin/profile";
        }
        redirectAttributes.addFlashAttribute("success", "Parolă actualizată cu succes.");
        return "redirect:/admin/profile";
    }
}
