package com.example.ui_service.controller.rest;

import com.example.ui_service.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller để xử lý các request liên quan đến xe của user
 */
@RestController
@RequestMapping("/api/users")
public class UserVehicleRestController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * Lấy danh sách xe của user
     * GET /api/users/{userId}/vehicles
     */
    @GetMapping("/{userId}/vehicles")
    public ResponseEntity<List<Map<String, Object>>> getUserVehicles(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> vehicles = vehicleService.getUserVehicles(userId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            System.err.println("Error getting user vehicles: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(List.of());
        }
    }
}

