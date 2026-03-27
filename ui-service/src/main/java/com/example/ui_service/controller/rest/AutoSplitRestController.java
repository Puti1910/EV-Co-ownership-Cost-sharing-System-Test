package com.example.ui_service.controller.rest;

import com.example.ui_service.client.CostPaymentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller để proxy các request Auto Split từ frontend sang backend
 */
@RestController
@RequestMapping("/api/auto-split")
public class AutoSplitRestController {

    @Autowired
    private CostPaymentClient costPaymentClient;

    /**
     * Tạo chi phí mới và tự động chia
     * POST /api/auto-split/create-and-split
     */
    @PostMapping("/create-and-split")
    public ResponseEntity<Map<String, Object>> createAndAutoSplit(@RequestBody Map<String, Object> request) {
        System.out.println("=== UI SERVICE: CREATE AND SPLIT ===");
        System.out.println("Request data: " + request);
        
        try {
            Map<String, Object> result = costPaymentClient.createAndAutoSplit(request);
            if (result != null) {
                System.out.println("Success! Result: " + result);
                return ResponseEntity.ok(result);
            } else {
                System.err.println("Result is null!");
                return ResponseEntity.status(500).body(Map.of("error", "Backend returned null"));
            }
        } catch (Exception e) {
            System.err.println("Error in UI service: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tự động chia chi phí đã tồn tại
     * POST /api/auto-split/cost/{costId}
     */
    @PostMapping("/cost/{costId}")
    public ResponseEntity<Map<String, Object>> autoSplitCost(
            @PathVariable Integer costId,
            @RequestParam Integer groupId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        if (month == null) {
            month = java.time.LocalDate.now().getMonthValue();
        }
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        Map<String, Object> result = costPaymentClient.autoSplitCost(costId, groupId, month, year);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Preview kết quả chia (không lưu)
     * POST /api/auto-split/preview
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewAutoSplit(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = costPaymentClient.previewAutoSplit(request);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
}

