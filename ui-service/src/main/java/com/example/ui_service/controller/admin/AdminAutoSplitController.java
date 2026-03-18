package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Chia tự động Admin
 */
@Controller
@RequestMapping("/admin/auto-split")
public class AdminAutoSplitController {

    @GetMapping
    public String autoSplit(Model model) {
        model.addAttribute("pageTitle", "Chia tự động");
        model.addAttribute("pageSubtitle", "Tạo chi phí và tự động chia cho các thành viên trong nhóm");
        return "admin-auto-split";
    }
}

