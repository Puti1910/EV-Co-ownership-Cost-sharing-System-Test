package com.example.ui_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Auto Split (Chia tự động)
 */
@Controller
@RequestMapping("/costs")
public class AutoSplitController {

    /**
     * Trang tạo chi phí và chia tự động
     */
    @GetMapping("/auto-split")
    public String autoSplitPage(Model model) {
        model.addAttribute("pageTitle", "Tạo Chi Phí Tự Động");
        
        return "costs/auto-split";
    }
}

