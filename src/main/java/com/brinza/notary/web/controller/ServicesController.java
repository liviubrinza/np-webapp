package com.brinza.notary.web.controller;

import com.brinza.notary.service.ServiceCatalogService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ServicesController {

    private final ServiceCatalogService serviceCatalogService;

    public ServicesController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", serviceCatalogService.findActiveServices(LocaleContextHolder.getLocale()));
        return "public/services";
    }
}
