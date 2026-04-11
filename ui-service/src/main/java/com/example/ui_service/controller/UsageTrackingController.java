package com.example.ui_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Usage Tracking (Nhập km)
 */
@Controller
@RequestMapping("/costs")
public class UsageTrackingController {

    /**
     * Trang nhập km sử dụng
     */
    @GetMapping("/usage-tracking")
    public String usageTrackingPage(Model model) {
        // Có thể thêm dữ liệu mẫu vào model nếu cần
        model.addAttribute("pageTitle", "Nhập Km Sử Dụng");
        
        return "costs/usage-tracking";
    }
}

