package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Tổng quan Admin
 */
@Controller
@RequestMapping("/admin")
public class AdminOverviewController {

    @GetMapping({"", "/overview"})
    public String overview(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Quản lý hệ thống chia sẻ chi phí xe điện");
        return "admin-overview";
    }
}

