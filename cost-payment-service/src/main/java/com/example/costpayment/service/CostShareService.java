package com.example.costpayment.service;

import com.example.costpayment.entity.CostShare;
import java.util.List;
import java.util.Map;

public interface CostShareService {
    // ✅ Các chức năng cơ bản CRUD
    CostShare createCostShare(Integer costId, CostShare costShare);
    List<CostShare> getCostSharesByCostId(Integer costId);
    List<CostShare> getAllCostShares();
    CostShare getCostShareById(Integer id);
    CostShare updateCostShare(Integer id, CostShare costShare);
    void deleteCostShare(Integer id);
    
    // 💰 Tính toán chia chi phí theo phần trăm
<<<<<<< HEAD
    List<CostShare> calculateCostShares(Integer costId, List<Integer> userIds, List<Double> percentages);
=======
    List<CostShare> calculateCostShares(Integer costId, List<Integer> userIds, List<java.math.BigDecimal> percentages);
>>>>>>> origin/main
    
    // 🔍 Tìm kiếm và thống kê
    List<CostShare> getCostSharesByUserId(Integer userId);
    List<CostShare> getCostSharesByUserIdAndStatus(Integer userId, String status);
    Map<String, Object> getCostShareStatisticsByUser(Integer userId);
    List<Map<String, Object>> getCostShareHistory(Integer costId);
    
    // ✅ Kiểm tra và validation
    boolean isCostShared(Integer costId);
    CostShare updateCostShareWithValidation(Integer id, CostShare updatedShare);
    CostShare updateCostShare(CostShare costShare);
}
