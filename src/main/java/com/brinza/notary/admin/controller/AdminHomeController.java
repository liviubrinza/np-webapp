package com.brinza.notary.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    @GetMapping("/admin")
    public String dashboard() {
        return "redirect:/admin/appointments";
    }
}
