package com.example.ui_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Collections;

@Controller
public class AuthUIController {

    // --- Trang Public ---

    /**
     * 1. Trang Đăng nhập (Điểm vào chính)
     */
    @GetMapping("/auth/login")
    public String showLoginPage() {
        return "auth/login";
    }

    /**
     * 2. Trang Đăng ký
     */
    @GetMapping("/auth/register")
    public String showRegisterPage() {
        return "auth/register";
    }

    // --- Trang ADMIN ---

    /**
     * 3. Trang Admin Dashboard (Quản lý Nhóm) - Đổi tên để tránh xung đột
     */
    @GetMapping("/admin/auth-groups")
    public String showGroupManagement(Model model) {
        model.addAttribute("pageTitle", "Quản Lí Nhóm Đồng Sở Hữu");
        model.addAttribute("currentPage", "auth-groups"); // Dành cho sidebar admin

        // Dữ liệu mẫu (Dummy data)
        model.addAttribute("adminName", "Admin");
        model.addAttribute("totalGroups", 12);
        model.addAttribute("activeGroups", 8);
        model.addAttribute("brokenCars", 5);
        model.addAttribute("activeHosts", 10);
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 5);
        model.addAttribute("startIndex", 1);
        model.addAttribute("endIndex", 10);
        model.addAttribute("groups", Collections.emptyList());

        return "auth/group-management";
    }

    /**
     * 4. Trang Admin Duyệt Hồ Sơ - Đổi tên để tránh xung đột
     */
    @GetMapping("/admin/auth-profile-approval")
    public String showProfileApprovalPage(Model model) {
        model.addAttribute("pageTitle", "Duyệt Hồ Sơ Người Dùng");
        model.addAttribute("currentPage", "auth-approval"); // Dành cho sidebar admin
        return "auth/admin/profile-approval";
    }

    // --- Trang USER ---

    /**
     * 5. Trang User Onboarding (Đăng ký hồ sơ)
     */
    @GetMapping("/user/auth-onboarding")
    public String showUserOnboardingPage(Model model) {
        model.addAttribute("pageTitle", "Hoàn tất đăng ký");
        model.addAttribute("currentPage", "auth-onboarding"); // Dành cho sidebar user
        return "auth/user-onboarding";
    }

    /**
     * 6. Trang xem Tình trạng Hồ sơ (User)
     */
    @GetMapping("/user/auth-profile-status")
    public String showProfileStatusPage(Model model) {
        model.addAttribute("pageTitle", "Tình trạng Hồ sơ");
        model.addAttribute("currentPage", "auth-status"); // Dành cho sidebar user
        return "auth/profile-status";
    }

    /**
     * 7. Trang Quản lý Hợp đồng (User)
     */
    @GetMapping("/user/auth-contracts")
    public String showContractsPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Hợp đồng");
        model.addAttribute("currentPage", "auth-contracts"); // Dành cho sidebar user
        return "auth/user-contracts";
    }
}

