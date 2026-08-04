package com.brinza.notary.controller.web;

import com.brinza.notary.dto.ServiceView;
import com.brinza.notary.service.ServiceCatalogService;
import com.brinza.notary.service.StructuredDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/{lang:en|ro|hu}")
public class ServicesController {

    private static final Logger log = LoggerFactory.getLogger(ServicesController.class);

    private final ServiceCatalogService serviceCatalogService;
    private final StructuredDataService structuredDataService;

    public ServicesController(ServiceCatalogService serviceCatalogService, StructuredDataService structuredDataService) {
        this.serviceCatalogService = serviceCatalogService;
        this.structuredDataService = structuredDataService;
    }

    @GetMapping("/services")
    public String showServices(Model model) {
        log.info("showServices called");
        Locale locale = LocaleContextHolder.getLocale();
        List<ServiceView> services = serviceCatalogService.findActiveServices(locale);
        model.addAttribute("services", services);
        model.addAttribute("servicesJsonLd", structuredDataService.servicesJsonLd(services, locale));
        return "public/services";
    }
}
