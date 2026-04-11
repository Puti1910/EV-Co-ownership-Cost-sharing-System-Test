package com.example.costpayment.controller;

import com.example.costpayment.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller: Báo cáo và Phân tích
 * UC5: Chi phí & Thanh toán - Báo cáo
 * UC9: Quản lý Quỹ chung - Báo cáo tài chính
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    /**
     * Báo cáo chi phí theo tháng
     * GET /api/reports/costs/monthly?vehicleId={vehicleId}&month={month}&year={year}
     */
    @GetMapping("/costs/monthly")
    public ResponseEntity<?> getMonthlyCostReport(
            @RequestParam Integer vehicleId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try {
            logger.info("Getting monthly cost report: vehicleId={}, month={}, year={}", vehicleId, month, year);
            Map<String, Object> report = reportService.getMonthlyCostReport(vehicleId, month, year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting monthly cost report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo chi phí theo quý
     * GET /api/reports/costs/quarterly?vehicleId={vehicleId}&quarter={quarter}&year={year}
     */
    @GetMapping("/costs/quarterly")
    public ResponseEntity<?> getQuarterlyCostReport(
            @RequestParam Integer vehicleId,
            @RequestParam Integer quarter,
            @RequestParam Integer year) {
        try {
            logger.info("Getting quarterly cost report: vehicleId={}, quarter={}, year={}", vehicleId, quarter, year);
            Map<String, Object> report = reportService.getQuarterlyCostReport(vehicleId, quarter, year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting quarterly cost report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo chi phí theo năm
     * GET /api/reports/costs/yearly?vehicleId={vehicleId}&year={year}
     */
    @GetMapping("/costs/yearly")
    public ResponseEntity<?> getYearlyCostReport(
            @RequestParam Integer vehicleId,
            @RequestParam Integer year) {
        try {
            logger.info("Getting yearly cost report: vehicleId={}, year={}", vehicleId, year);
            Map<String, Object> report = reportService.getYearlyCostReport(vehicleId, year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting yearly cost report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * So sánh mức sử dụng với tỷ lệ sở hữu
     * GET /api/reports/usage-ownership-comparison?groupId={groupId}&month={month}&year={year}
     */
    @GetMapping("/usage-ownership-comparison")
    public ResponseEntity<?> compareUsageWithOwnership(
            @RequestParam Integer groupId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try {
            logger.info("Comparing usage with ownership: groupId={}, month={}, year={}", groupId, month, year);
            Map<String, Object> report = reportService.compareUsageWithOwnership(groupId, month, year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error comparing usage with ownership: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Phân tích chi tiết cá nhân
     * GET /api/reports/personal-analysis?userId={userId}&groupId={groupId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/personal-analysis")
    public ResponseEntity<?> getPersonalAnalysis(
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting personal analysis: userId={}, groupId={}, period={} to {}", 
                userId, groupId, startDate, endDate);
            Map<String, Object> analysis = reportService.getPersonalAnalysis(userId, groupId, startDate, endDate);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            logger.error("Error getting personal analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo tài chính minh bạch cho nhóm
     * GET /api/reports/financial?groupId={groupId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/financial")
    public ResponseEntity<?> getFinancialReport(
            @RequestParam Integer groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting financial report: groupId={}, period={} to {}", groupId, startDate, endDate);
            Map<String, Object> report = reportService.getFinancialReport(groupId, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting financial report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Thống kê chi phí theo loại
     * GET /api/reports/costs/statistics-by-type?vehicleId={vehicleId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/costs/statistics-by-type")
    public ResponseEntity<?> getCostStatisticsByType(
            @RequestParam Integer vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting cost statistics by type: vehicleId={}, period={} to {}", vehicleId, startDate, endDate);
            Map<String, Object> statistics = reportService.getCostStatisticsByType(vehicleId, startDate, endDate);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting cost statistics by type: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tổng hợp chi phí và thanh toán của user
     * GET /api/reports/user-summary?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/user-summary")
    public ResponseEntity<?> getUserCostPaymentSummary(
            @RequestParam Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting user cost payment summary: userId={}, period={} to {}", userId, startDate, endDate);
            Map<String, Object> summary = reportService.getUserCostPaymentSummary(userId, startDate, endDate);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting user cost payment summary: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

