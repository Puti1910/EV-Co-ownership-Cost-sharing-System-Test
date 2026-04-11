package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminProfileApprovalController {

    @GetMapping("/profile-approval")
    public String profileApproval(Model model) {
        model.addAttribute("pageTitle", "Duyệt hồ sơ KYC");
        model.addAttribute("activePage", "profile-approval");
        return "admin/profile-approval";
    }
}

