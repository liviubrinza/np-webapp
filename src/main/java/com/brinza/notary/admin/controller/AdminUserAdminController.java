package com.brinza.notary.admin.controller;

import com.brinza.notary.domain.AdminRole;
import com.brinza.notary.dto.AdminUserForm;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.AdminUserManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserAdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserAdminController.class);

    private final AdminUserManagementService adminUserManagementService;
    private final AdminActivityLogger adminActivityLogger;

    public AdminUserAdminController(AdminUserManagementService adminUserManagementService, AdminActivityLogger adminActivityLogger) {
        this.adminUserManagementService = adminUserManagementService;
        this.adminActivityLogger = adminActivityLogger;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("admins", adminUserManagementService.listAdmins());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        AdminUserForm form = new AdminUserForm();
        form.setRole(AdminRole.ADMIN);
        model.addAttribute("adminUserForm", form);
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("adminUserForm") AdminUserForm adminUserForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            log.debug("Admin user form has {} validation error(s)", bindingResult.getErrorCount());
            return "admin/users/form";
        }
        log.info("Creating new user for username={} role={}", adminUserForm.getUsername(), adminUserForm.getRole());
        try {
            adminUserManagementService.create(adminUserForm);
            adminActivityLogger.log("Created %s user '%s'".formatted(adminUserForm.getRole(), adminUserForm.getUsername()));
        } catch (IllegalArgumentException e) {
            log.debug("User creation rejected: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "admin/users/form";
        }
        redirectAttributes.addFlashAttribute("success", "Cont admin creat.");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AdminUserView admin = adminUserManagementService.getAdmin(id);
        AdminUserForm form = new AdminUserForm();
        form.setUsername(admin.username());
        form.setRole(admin.role());
        model.addAttribute("adminUserForm", form);
        model.addAttribute("adminId", id);
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("adminUserForm") AdminUserForm adminUserForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            log.debug("Admin user form has {} validation error(s)", bindingResult.getErrorCount());
            model.addAttribute("adminId", id);
            return "admin/users/form";
        }
        try {
            adminUserManagementService.update(id, adminUserForm);
            adminActivityLogger.log("Updated user #%d (username '%s', role %s)".formatted(id, adminUserForm.getUsername(), adminUserForm.getRole()));
        } catch (IllegalArgumentException e) {
            log.debug("Could not update user for id={}: {}", id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("adminId", id);
            return "admin/users/form";
        }
        redirectAttributes.addFlashAttribute("success", "Cont admin actualizat.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.delete(id, authentication.getName());
            adminActivityLogger.log("Deleted user #%d".formatted(id));
            redirectAttributes.addFlashAttribute("success", "Cont șters.");
        } catch (IllegalArgumentException e) {
            log.debug("Could not delete user for id={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
