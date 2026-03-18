package com.example.ui_service.controller.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Profile của User
 */
@Controller
@RequestMapping("/user")
public class UserProfileController {

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        UserPageModelHelper.populateCommonAttributes(authentication, model);
        model.addAttribute("activePage", "profile");
        model.addAttribute("pageCss", new String[]{"/css/user-dashboard.css"});
        model.addAttribute("pageJs", new String[]{"/js/user-profile.js"});

        return "user-dashboard";
    }
}

