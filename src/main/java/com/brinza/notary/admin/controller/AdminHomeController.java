package com.brinza.notary.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    private static final Logger log = LoggerFactory.getLogger(AdminHomeController.class);

    @GetMapping("/admin")
    public String dashboard(Authentication authentication) {
        boolean isTechnician = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_TECHNICIAN"::equals);
        log.debug("username={} isTechnician={}", authentication.getName(), isTechnician);
        return isTechnician ? "redirect:/admin/statistics" : "redirect:/admin/appointments";
    }
}
