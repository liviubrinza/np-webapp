package com.brinza.notary.controller.web;

import com.brinza.notary.service.ServiceCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ServicesController {

    private static final Logger log = LoggerFactory.getLogger(ServicesController.class);

    private final ServiceCatalogService serviceCatalogService;

    public ServicesController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/services")
    public String showServices(Model model) {
        log.info("showServices called");
        model.addAttribute("services", serviceCatalogService.findActiveServices(LocaleContextHolder.getLocale()));
        return "public/services";
    }
}
