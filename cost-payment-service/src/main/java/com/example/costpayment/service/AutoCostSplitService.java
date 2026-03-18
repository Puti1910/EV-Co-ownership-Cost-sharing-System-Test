package com.example.costpayment.service;

import com.example.costpayment.entity.CostShare;

import java.util.List;
import java.util.Map;

/**
 * Service tự động chia chi phí
 */
public interface AutoCostSplitService {

    /**
     * Tự động chia chi phí dựa trên split method
     */
    List<CostShare> autoSplitCost(Integer costId, Integer groupId, Integer month, Integer year);

    /**
     * Tự động chia chi phí với splitMethod chỉ định
     */
    List<CostShare> autoSplitCostWithMethod(Integer costId, Integer groupId, String splitMethod, Integer month, Integer year);
    
    /**
     * Tự động chia chi phí với splitMethod chỉ định (có token)
     */
    List<CostShare> autoSplitCostWithMethod(Integer costId, Integer groupId, String splitMethod, Integer month, Integer year, String token);

    /**
     * Chia theo tỉ lệ sở hữu
     */
    List<CostShare> splitByOwnership(Integer costId, Map<Integer, Double> ownershipMap);

    /**
     * Chia theo mức độ sử dụng (km)
     */
    List<CostShare> splitByUsage(Integer costId, Map<Integer, Double> usageMap);

    /**
     * Chia đều
     */
    List<CostShare> splitEqually(Integer costId, List<Integer> userIds);

    /**
     * Lấy ownership % của nhóm
     */
    Map<Integer, Double> getGroupOwnership(Integer groupId);
    
    /**
     * Lấy ownership % của nhóm (có token)
     */
    Map<Integer, Double> getGroupOwnership(Integer groupId, String token);
}


