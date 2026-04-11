package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Quản lý nhóm Admin
 */
@Controller
@RequestMapping("/admin/groups")
public class AdminGroupsController {

    @GetMapping
    public String groups(Model model) {
        model.addAttribute("pageTitle", "Quản lý nhóm");
        model.addAttribute("pageSubtitle", "Quản lý các nhóm đồng sở hữu xe");
        return "admin-groups";
    }
}

