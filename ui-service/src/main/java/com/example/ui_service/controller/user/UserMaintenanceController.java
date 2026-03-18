package com.example.ui_service.controller.user;

import com.example.ui_service.external.service.ServiceRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserMaintenanceController {

    private static final Logger log = LoggerFactory.getLogger(UserMaintenanceController.class);
    private final ServiceRestClient serviceRestClient;

    public UserMaintenanceController(ServiceRestClient serviceRestClient) {
        this.serviceRestClient = serviceRestClient;
    }

    @GetMapping("/maintenance")
    public String maintenance(Authentication authentication, Model model) {
        UserPageModelHelper.populateCommonAttributes(authentication, model);
        model.addAttribute("pageTitle", "Đặt dịch vụ bảo dưỡng");
        model.addAttribute("activePage", "maintenance");

        // Lấy dịch vụ từ bảng service (template dịch vụ)
        List<Map<String, Object>> maintenanceServices;
        try {
            log.info("🔵 [UserMaintenanceController] Đang gọi serviceRestClient.getAllServices()...");
            maintenanceServices = serviceRestClient.getAllServices();
            log.info("✅ [UserMaintenanceController] Đã tải {} dịch vụ từ bảng service trong vehicle_management", 
                    maintenanceServices != null ? maintenanceServices.size() : 0);
            
            if (maintenanceServices != null && !maintenanceServices.isEmpty()) {
                log.info("✅ [UserMaintenanceController] Danh sách dịch vụ: {}", maintenanceServices);
            }
            
            // Nếu null hoặc rỗng, trả về empty list (không hiện gì)
            if (maintenanceServices == null || maintenanceServices.isEmpty()) {
                log.warn("⚠️ [UserMaintenanceController] Không có dịch vụ nào trong bảng service");
                maintenanceServices = Collections.emptyList();
            }
        } catch (Exception ex) {
            log.error("❌ [UserMaintenanceController] Không thể tải danh sách dịch vụ bảo dưỡng từ bảng service", ex);
            ex.printStackTrace();
            maintenanceServices = Collections.emptyList();
        }

        model.addAttribute("maintenanceServices", maintenanceServices);
        return "user-dashboard";
    }
}

