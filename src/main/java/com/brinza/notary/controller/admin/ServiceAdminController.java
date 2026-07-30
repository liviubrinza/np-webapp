package com.brinza.notary.controller.admin;

import com.brinza.notary.dto.ServiceAdminDetailView;
import com.brinza.notary.dto.ServiceForm;
import com.brinza.notary.service.AdminActivityLogger;
import com.brinza.notary.service.ServiceAdminManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/admin/settings/services")
public class ServiceAdminController {

    private static final Logger log = LoggerFactory.getLogger(ServiceAdminController.class);

    private final ServiceAdminManagementService serviceAdminManagementService;
    private final AdminActivityLogger adminActivityLogger;

    public ServiceAdminController(ServiceAdminManagementService serviceAdminManagementService,
                                   AdminActivityLogger adminActivityLogger) {
        this.serviceAdminManagementService = serviceAdminManagementService;
        this.adminActivityLogger = adminActivityLogger;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("services", serviceAdminManagementService.listAll());
        return "admin/settings/services/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        ServiceForm form = new ServiceForm();
        form.setActive(true);
        model.addAttribute("serviceForm", form);
        return "admin/settings/services/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("serviceForm") ServiceForm serviceForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            log.debug("Service form has {} validation error(s)", bindingResult.getErrorCount());
            return "admin/settings/services/form";
        }
        try {
            serviceAdminManagementService.create(serviceForm);
            adminActivityLogger.log("Created service '%s'".formatted(serviceForm.getCode()));
        } catch (IllegalArgumentException e) {
            log.debug("Service creation rejected: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "admin/settings/services/form";
        }
        redirectAttributes.addFlashAttribute("success", "Serviciu creat.");
        return "redirect:/admin/settings/services";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ServiceAdminDetailView view = serviceAdminManagementService.getForEdit(id);
        ServiceForm form = new ServiceForm();
        form.setCode(view.code());
        form.setDurationMinutes(view.durationMinutes());
        form.setActive(view.active());
        form.setNameEn(view.nameEn());
        form.setDescriptionEn(view.descriptionEn());
        form.setNameRo(view.nameRo());
        form.setDescriptionRo(view.descriptionRo());
        form.setNameHu(view.nameHu());
        form.setDescriptionHu(view.descriptionHu());
        model.addAttribute("serviceForm", form);
        model.addAttribute("serviceId", id);
        return "admin/settings/services/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("serviceForm") ServiceForm serviceForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            log.debug("Service form has {} validation error(s)", bindingResult.getErrorCount());
            model.addAttribute("serviceId", id);
            return "admin/settings/services/form";
        }
        try {
            serviceAdminManagementService.update(id, serviceForm);
            adminActivityLogger.log("Updated service #%d (code '%s')".formatted(id, serviceForm.getCode()));
        } catch (IllegalArgumentException e) {
            log.debug("Could not update service for id={}: {}", id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("serviceId", id);
            return "admin/settings/services/form";
        }
        redirectAttributes.addFlashAttribute("success", "Serviciu actualizat.");
        return "redirect:/admin/settings/services";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            serviceAdminManagementService.delete(id);
            adminActivityLogger.log("Deleted service #%d".formatted(id));
            redirectAttributes.addFlashAttribute("success", "Serviciu șters.");
        } catch (IllegalArgumentException e) {
            log.debug("Could not delete service for id={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings/services";
    }
}
