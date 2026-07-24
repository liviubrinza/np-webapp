package com.brinza.notary.admin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    @GetMapping("/admin")
    public String dashboard(Authentication authentication) {
        boolean isTechnician = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_TECHNICIAN"::equals);
        return isTechnician ? "redirect:/admin/statistics" : "redirect:/admin/appointments";
    }
}
