package com.example.costpayment.controller;

import com.example.costpayment.dto.UsageTrackingDto;
import com.example.costpayment.entity.UsageTracking;
import com.example.costpayment.service.UsageTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API cho Usage Tracking - Theo dõi mức độ sử dụng xe
 */
@RestController
@RequestMapping("/api/usage-tracking")
@CrossOrigin(origins = "*")
public class UsageTrackingController {

    @Autowired
    private UsageTrackingService usageTrackingService;

    /**
     * Lấy usage của nhóm trong tháng (kèm %)
     * GET /api/usage-tracking/group/{groupId}?month=10&year=2024
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<UsageTrackingDto>> getGroupUsage(
            @PathVariable Integer groupId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        List<UsageTrackingDto> usage = usageTrackingService.getGroupUsageInMonth(groupId, month, year);
        return ResponseEntity.ok(usage);
    }

    /**
     * Lấy usage của 1 user trong tháng
     * GET /api/usage-tracking/{groupId}/{userId}?month=10&year=2024
     */
    @GetMapping("/{groupId}/{userId}")
    public ResponseEntity<UsageTracking> getUserUsage(
            @PathVariable Integer groupId,
            @PathVariable Integer userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        UsageTracking usage = usageTrackingService.getUserUsageInMonth(groupId, userId, month, year);
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
    public ResponseEntity<UsageTracking> createOrUpdateUsage(@RequestBody UsageTracking usageTracking) {
        UsageTracking saved = usageTrackingService.saveUsageTracking(usageTracking);
        return ResponseEntity.ok(saved);
    }

    /**
     * Cập nhật nhanh km cho user
     * PUT /api/usage-tracking/update-km
     */
    @PutMapping("/update-km")
    public ResponseEntity<UsageTracking> updateKm(
            @RequestParam Integer groupId,
            @RequestParam Integer userId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Double kmDriven) {
        
        UsageTracking updated = usageTrackingService.updateKmDriven(groupId, userId, month, year, kmDriven);
        return ResponseEntity.ok(updated);
    }

    /**
     * Lấy lịch sử usage của user
     * GET /api/usage-tracking/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<UsageTracking>> getUserHistory(@PathVariable Integer userId) {
        List<UsageTracking> history = usageTrackingService.getUserUsageHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Tính % km của từng user trong nhóm
     * GET /api/usage-tracking/group/{groupId}/percentage?month=10&year=2024
     */
    @GetMapping("/group/{groupId}/percentage")
    public ResponseEntity<Map<Integer, Double>> getKmPercentage(
            @PathVariable Integer groupId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        Map<Integer, Double> percentages = usageTrackingService.calculateKmPercentage(groupId, month, year);
        return ResponseEntity.ok(percentages);
    }

    /**
     * Xóa usage tracking
     * DELETE /api/usage-tracking/{usageId}
     */
    @DeleteMapping("/{usageId}")
    public ResponseEntity<Void> deleteUsage(@PathVariable Integer usageId) {
        usageTrackingService.deleteUsageTracking(usageId);
        return ResponseEntity.ok().build();
    }
}

