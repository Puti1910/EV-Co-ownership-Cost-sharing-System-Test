package com.example.ui_service.controller.rest;

import com.example.ui_service.client.CostPaymentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller để proxy các request Usage Tracking từ frontend sang backend
 */
@RestController
@RequestMapping("/api/usage-tracking")
public class UsageTrackingRestController {

    @Autowired
    private CostPaymentClient costPaymentClient;

    /**
     * Lấy usage của nhóm trong tháng (kèm %)
     * GET /api/usage-tracking/group/{groupId}?month=10&year=2024
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Map<String, Object>>> getGroupUsage(
            @PathVariable Integer groupId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        List<Map<String, Object>> usage = costPaymentClient.getGroupUsage(groupId, month, year);
        return ResponseEntity.ok(usage);
    }

    /**
     * Lấy usage của 1 user trong tháng
     * GET /api/usage-tracking/{groupId}/{userId}?month=10&year=2024
     */
    @GetMapping("/{groupId}/{userId}")
    public ResponseEntity<Map<String, Object>> getUserUsage(
            @PathVariable Integer groupId,
            @PathVariable Integer userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        Map<String, Object> usage = costPaymentClient.getUserUsage(groupId, userId, month, year);
        if (usage != null) {
            return ResponseEntity.ok(usage);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Tạo hoặc cập nhật km cho user
     * POST /api/usage-tracking
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrUpdateUsage(@RequestBody Map<String, Object> usageTracking) {
        Map<String, Object> saved = costPaymentClient.saveUsageTracking(usageTracking);
        if (saved != null) {
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cập nhật nhanh km cho user
     * PUT /api/usage-tracking/update-km
     */
    @PutMapping("/update-km")
    public ResponseEntity<Map<String, Object>> updateKm(
            @RequestParam Integer groupId,
            @RequestParam Integer userId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Double kmDriven) {
        
        try {
            Map<String, Object> updated = costPaymentClient.updateUsageKm(groupId, userId, month, year, kmDriven);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update usage tracking"));
            }
        } catch (Exception e) {
            System.err.println("Error in updateKm endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử usage của user
     * GET /api/usage-tracking/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(@PathVariable Integer userId) {
        List<Map<String, Object>> history = costPaymentClient.getUserHistory(userId);
        return ResponseEntity.ok(history);
    }
}

