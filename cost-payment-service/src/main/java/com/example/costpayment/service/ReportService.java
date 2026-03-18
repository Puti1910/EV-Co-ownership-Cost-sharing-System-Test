package com.example.costpayment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service cho báo cáo và phân tích
 * UC5: Chi phí & Thanh toán - Báo cáo
 */
public interface ReportService {
    
    /**
     * Báo cáo chi phí theo tháng
     * @param vehicleId ID của xe
     * @param month Tháng (1-12)
     * @param year Năm
     * @return Báo cáo chi tiết
     */
    Map<String, Object> getMonthlyCostReport(Integer vehicleId, Integer month, Integer year);
    
    /**
     * Báo cáo chi phí theo quý
     * @param vehicleId ID của xe
     * @param quarter Quý (1-4)
     * @param year Năm
     * @return Báo cáo chi tiết
     */
    Map<String, Object> getQuarterlyCostReport(Integer vehicleId, Integer quarter, Integer year);
    
    /**
     * Báo cáo chi phí theo năm
     * @param vehicleId ID của xe
     * @param year Năm
     * @return Báo cáo chi tiết
     */
    Map<String, Object> getYearlyCostReport(Integer vehicleId, Integer year);
    
    /**
     * So sánh mức sử dụng với tỷ lệ sở hữu
     * @param groupId ID của nhóm
     * @param month Tháng
     * @param year Năm
     * @return Báo cáo so sánh
     */
    Map<String, Object> compareUsageWithOwnership(Integer groupId, Integer month, Integer year);
    
    /**
     * Phân tích chi tiết cá nhân
     * @param userId ID của user
     * @param groupId ID của nhóm (optional)
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Phân tích chi tiết
     */
    Map<String, Object> getPersonalAnalysis(Integer userId, Integer groupId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Báo cáo tài chính minh bạch cho nhóm
     * @param groupId ID của nhóm
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Báo cáo tài chính
     */
    Map<String, Object> getFinancialReport(Integer groupId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Thống kê chi phí theo loại trong khoảng thời gian
     * @param vehicleId ID của xe
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Thống kê theo loại
     */
    Map<String, Object> getCostStatisticsByType(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Tổng hợp chi phí và thanh toán của user
     * @param userId ID của user
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Tổng hợp
     */
    Map<String, Object> getUserCostPaymentSummary(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
}

