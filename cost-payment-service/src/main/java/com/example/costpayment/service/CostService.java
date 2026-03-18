package com.example.costpayment.service;

import com.example.costpayment.entity.Cost;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CostService {
    
    // ✅ Các chức năng cơ bản CRUD
    List<Cost> getAllCosts();
    Optional<Cost> getCostById(Integer id);
    Cost createCost(Cost cost);
    Optional<Cost> updateCost(Integer id, Cost cost);
    boolean deleteCost(Integer id);
    
    // ✅ Tìm kiếm chi phí theo các tiêu chí
    List<Cost> getCostsByVehicleId(Integer vehicleId);
    List<Cost> getCostsByType(String costType);
    List<Cost> getCostsByVehicleIdAndType(Integer vehicleId, String costType);
    
    // ✅ Tìm kiếm theo khoảng thời gian
    List<Cost> getCostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<Cost> getCostsByVehicleIdAndDateRange(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Tìm kiếm theo khoảng số tiền
    List<Cost> getCostsByAmountRange(Double minAmount, Double maxAmount);
    List<Cost> getCostsByVehicleIdAndAmountRange(Integer vehicleId, Double minAmount, Double maxAmount);
    
    // ✅ Tìm kiếm nâng cao với nhiều điều kiện
    List<Cost> searchCosts(Integer vehicleId, String costType, Double minAmount, Double maxAmount, 
                          LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Thống kê chi phí
    Map<String, Object> getCostStatistics();
    Map<String, Object> getCostStatisticsByVehicle(Integer vehicleId);
    Map<String, Object> getCostStatisticsByType(String costType);
    Map<String, Object> getCostStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Tính tổng chi phí
    Double getTotalAmountByVehicleId(Integer vehicleId);
    Double getTotalAmountByCostType(String costType);
    Double getTotalAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Đếm số lượng chi phí
    Long countCostsByVehicleId(Integer vehicleId);
    Long countCostsByCostType(String costType);
    Long countCostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
