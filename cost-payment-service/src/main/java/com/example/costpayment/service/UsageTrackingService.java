package com.example.costpayment.service;

import com.example.costpayment.dto.UsageTrackingDto;
import com.example.costpayment.entity.UsageTracking;

import java.util.List;
import java.util.Map;

/**
 * Service cho UsageTracking
 */
public interface UsageTrackingService {

    /**
     * Lưu usage tracking
     */
    UsageTracking saveUsageTracking(UsageTracking usageTracking);

    /**
     * Lấy usage của nhóm trong tháng (kèm %)
     */
    List<UsageTrackingDto> getGroupUsageInMonth(Integer groupId, Integer month, Integer year);

    /**
     * Lấy usage của 1 user trong tháng
     */
    UsageTracking getUserUsageInMonth(Integer groupId, Integer userId, Integer month, Integer year);

    /**
     * Cập nhật km cho user
     */
    UsageTracking updateKmDriven(Integer groupId, Integer userId, Integer month, Integer year, Double kmDriven);

    /**
     * Lấy lịch sử usage của user
     */
    List<UsageTracking> getUserUsageHistory(Integer userId);

    /**
     * Tính % km của từng user trong nhóm
     */
    Map<Integer, Double> calculateKmPercentage(Integer groupId, Integer month, Integer year);

    /**
     * Xóa usage tracking
     */
    void deleteUsageTracking(Integer usageId);
}


