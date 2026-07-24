package com.brinza.notary.admin.controller;

import com.brinza.notary.dto.AdminUserForm;
import com.brinza.notary.dto.AdminUserView;
import com.brinza.notary.service.AdminUserManagementService;
import jakarta.validation.Valid;
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

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserAdminController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("admins", adminUserManagementService.listAdmins());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("adminUserForm", new AdminUserForm());
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("adminUserForm") AdminUserForm adminUserForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            return "admin/users/form";
        }
        try {
            adminUserManagementService.create(adminUserForm);
        } catch (IllegalArgumentException e) {
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
        model.addAttribute("adminUserForm", form);
        model.addAttribute("adminId", id);
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("adminUserForm") AdminUserForm adminUserForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("adminId", id);
            return "admin/users/form";
        }
        try {
            adminUserManagementService.update(id, adminUserForm);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("adminId", id);
            return "admin/users/form";
        }
        redirectAttributes.addFlashAttribute("success", "Cont admin actualizat.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminUserManagementService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Cont admin șters.");
        return "redirect:/admin/users";
    }
}
