package com.example.ui_service.controller.rest;

import com.example.ui_service.external.service.VehicleServiceRestClient;
import com.example.ui_service.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-maintenance")
public class VehicleMaintenanceRestController {

    private final VehicleServiceRestClient vehicleServiceRestClient;

    public VehicleMaintenanceRestController(VehicleServiceRestClient vehicleServiceRestClient) {
        this.vehicleServiceRestClient = vehicleServiceRestClient;
    }

    @GetMapping("/options")
    public ResponseEntity<?> getMaintenanceOptions(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        List<Map<String, Object>> options = vehicleServiceRestClient.getMaintenanceOptions(userId);
        return ResponseEntity.ok(Map.of("success", true, "options", options));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookMaintenance(Authentication authentication,
                                             @RequestBody Map<String, Object> payload) {
        Integer userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        payload.put("userId", userId);
        Map<String, Object> result = vehicleServiceRestClient.bookMaintenance(payload);
        return ResponseEntity.ok(result);
    }

    private Integer extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getUserId() != null ? user.getUserId().intValue() : null;
        }
        return null;
    }
}

