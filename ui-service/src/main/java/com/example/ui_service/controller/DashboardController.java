package com.example.ui_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    /**
     * Redirect root path to auth login
     */
    @GetMapping("/")
    public String redirectRoot() {
        return "redirect:/auth/login";
    }
    
    /**
     * Redirect /dashboard to admin dashboard
     */
    @GetMapping("/dashboard")
    public String redirectDashboard() {
        return "redirect:/admin";
    }
    
    /**
     * Redirect từ URL cũ /ext/admin/vehicle-manager đến URL mới /admin/vehicle-services
     */
    @GetMapping("/ext/admin/vehicle-manager")
    public String redirectVehicleManager() {
        return "redirect:/admin/vehicle-services";
    }
    
    /**
     * Redirect từ URL cũ /ext/admin/vehicle-services đến URL mới /admin/vehicle-services
     */
    @GetMapping("/ext/admin/vehicle-services")
    public String redirectVehicleServices() {
        return "redirect:/admin/vehicle-services";
    }
}