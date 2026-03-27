package com.example.user_account_service.controller;

import com.example.user_account_service.entity.User;
import com.example.user_account_service.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class UserUIController {

    @Autowired
    private UserService userService;

    /**
     * Endpoint trung gian để nhận JWT từ query parameter và set vào cookie
     */
    @GetMapping("/user/auth")
    public String authenticateUser(@RequestParam(required = false) String token,
                                   @RequestParam(required = false, defaultValue = "/user/home") String redirect,
                                   HttpServletResponse response) {
        if (token != null && !token.isEmpty()) {
            // Set JWT vào cookie
            Cookie jwtCookie = new Cookie("jwtToken", token);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400); // 24 giờ
            jwtCookie.setHttpOnly(false); // Cho phép JavaScript đọc được (cho môi trường dev)
            jwtCookie.setSecure(false); // Không yêu cầu HTTPS (cho môi trường dev)
            response.addCookie(jwtCookie);
        }
        return "redirect:" + redirect;
    }

    /**
     * Trang User Onboarding (Đăng ký hồ sơ)
     */
    @GetMapping("/user/onboarding")
    public String showUserOnboardingPage(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElse(null);
            if (user != null) {
                model.addAttribute("userName", user.getFullName());
                model.addAttribute("userEmail", user.getEmail());
            }
        }
        model.addAttribute("pageTitle", "Hoàn tất đăng ký");
        model.addAttribute("currentPage", "onboarding");
        return "user/onboarding";
    }

    /**
     * Trang xem Tình trạng Hồ sơ (User)
     */
    @GetMapping("/user/profile-status")
    public String showProfileStatusPage(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElse(null);
            if (user != null) {
                model.addAttribute("userName", user.getFullName());
                model.addAttribute("userEmail", user.getEmail());
                model.addAttribute("profileStatus", user.getProfileStatus().name());
                model.addAttribute("isVerified", user.isVerified());
            }
        }
        model.addAttribute("pageTitle", "Tình trạng Hồ sơ");
        model.addAttribute("currentPage", "profile-status");
        return "user/profile-status";
    }

    /**
     * Trang Quản lý Hợp đồng (User)
     */
    @GetMapping("/user/contracts")
    public String showContractsPage(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElse(null);
            if (user != null) {
                model.addAttribute("userName", user.getFullName());
                model.addAttribute("userEmail", user.getEmail());
            }
        }
        model.addAttribute("pageTitle", "Quản lý Hợp đồng");
        model.addAttribute("currentPage", "contracts");
        return "user/contracts";
    }

    /**
     * Trang Dashboard/Home của User
     */
    @GetMapping({"/user", "/user/home", "/user/dashboard"})
    public String showUserHomePage(Model model, Authentication authentication) {
        if (authentication != null) {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElse(null);
            if (user != null) {
                model.addAttribute("userName", user.getFullName());
                model.addAttribute("userEmail", user.getEmail());
                model.addAttribute("userId", user.getUserId());
            }
        }
        model.addAttribute("pageTitle", "Trang chủ");
        model.addAttribute("currentPage", "home");
        return "user/home";
    }
}

