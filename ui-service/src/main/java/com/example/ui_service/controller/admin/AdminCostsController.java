package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Quản lý chi phí Admin
 */
@Controller
@RequestMapping("/admin/costs")
public class AdminCostsController {

    @GetMapping
    public String costs(Model model) {
        model.addAttribute("pageTitle", "Quản lý chi phí");
        model.addAttribute("pageSubtitle", "Quản lý và theo dõi các chi phí liên quan đến xe");
        return "admin-costs";
    }
}

