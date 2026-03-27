package com.example.ui_service.controller.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserScheduleController {

    @GetMapping("/fair-schedule")
    public String fairSchedule(Authentication authentication, Model model) {
        UserPageModelHelper.populateCommonAttributes(authentication, model);
        model.addAttribute("pageTitle", "Lịch đặt xe công bằng");
        model.addAttribute("activePage", "fair-schedule");
        model.addAttribute("pageCss", new String[]{"/css/user-dashboard.css"});
        model.addAttribute("pageJs", new String[]{"/js/user-dashboard.js"});
        return "user-dashboard";
    }
}

