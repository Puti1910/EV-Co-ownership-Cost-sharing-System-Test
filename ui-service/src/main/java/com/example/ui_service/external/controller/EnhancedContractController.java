package com.example.ui_service.external.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EnhancedContractController {

    @GetMapping("/ext/admin/enhanced-contract")
    public String enhancedContractManagement(Model model) {
        model.addAttribute("pageTitle", "Quản Lý Hợp Đồng Điện Tử");
        model.addAttribute("pageDescription", "Quản lý hợp đồng pháp lý cho nhóm đồng sở hữu");
        return "ext/admin/enhanced-contract-management";
    }
}


