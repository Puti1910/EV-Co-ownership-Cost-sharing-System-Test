package com.example.costpayment.service;

import com.example.costpayment.entity.Cost;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
import java.util.Optional;
>>>>>>> origin/main

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
<<<<<<< HEAD
    List<Cost> getCostsByAmountRange(Double minAmount, Double maxAmount);
    List<Cost> getCostsByVehicleIdAndAmountRange(Integer vehicleId, Double minAmount, Double maxAmount);
    
    // ✅ Tìm kiếm nâng cao với nhiều điều kiện
    List<Cost> searchCosts(Integer vehicleId, String costType, Double minAmount, Double maxAmount, 
=======
    List<Cost> getCostsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount);
    List<Cost> getCostsByVehicleIdAndAmountRange(Integer vehicleId, BigDecimal minAmount, BigDecimal maxAmount);
    
    // ✅ Tìm kiếm nâng cao với nhiều điều kiện
    List<Cost> searchCosts(Integer vehicleId, String costType, BigDecimal minAmount, BigDecimal maxAmount, 
>>>>>>> origin/main
                          LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Thống kê chi phí
    Map<String, Object> getCostStatistics();
    Map<String, Object> getCostStatisticsByVehicle(Integer vehicleId);
    Map<String, Object> getCostStatisticsByType(String costType);
    Map<String, Object> getCostStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    // ✅ Tính tổng chi phí
<<<<<<< HEAD
    Double getTotalAmountByVehicleId(Integer vehicleId);
    Double getTotalAmountByCostType(String costType);
    Double getTotalAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate);
=======
    BigDecimal getTotalAmountByVehicleId(Integer vehicleId);
    BigDecimal getTotalAmountByCostType(String costType);
    BigDecimal getTotalAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate);
>>>>>>> origin/main
    
    // ✅ Đếm số lượng chi phí
    Long countCostsByVehicleId(Integer vehicleId);
    Long countCostsByCostType(String costType);
    Long countCostsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
