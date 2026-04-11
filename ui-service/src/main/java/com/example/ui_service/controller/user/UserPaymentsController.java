package com.example.ui_service.controller.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Thanh toán của User
 */
@Controller
@RequestMapping("/user")
public class UserPaymentsController {

    @GetMapping("/payments")
    public String payments(Authentication authentication, Model model) {

        UserPageModelHelper.populateCommonAttributes(authentication, model);
        model.addAttribute("activePage", "payments");
        model.addAttribute("pageCss", new String[]{"/css/user-dashboard.css"});
        model.addAttribute("pageJs", new String[]{"/js/user-dashboard.js"});

        return "user-dashboard";
    }
}

