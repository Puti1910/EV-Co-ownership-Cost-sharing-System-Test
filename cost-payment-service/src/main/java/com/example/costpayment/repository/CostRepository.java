package com.example.costpayment.repository;

import com.example.costpayment.entity.Cost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CostRepository extends JpaRepository<Cost, Integer> {
    
    // Tìm chi phí theo Vehicle ID
    List<Cost> findByVehicleId(Integer vehicleId);
    
    // Tìm chi phí theo loại
    List<Cost> findByCostType(Cost.CostType costType);
    
    // Tìm chi phí theo Vehicle ID và loại
    List<Cost> findByVehicleIdAndCostType(Integer vehicleId, Cost.CostType costType);
    
    // Tìm chi phí trong khoảng thời gian
    List<Cost> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Tìm chi phí theo Vehicle ID trong khoảng thời gian
    List<Cost> findByVehicleIdAndCreatedAtBetween(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Tìm chi phí có số tiền lớn hơn hoặc bằng
    List<Cost> findByAmountGreaterThanEqual(Double amount);
    
    // Tìm chi phí có số tiền nhỏ hơn hoặc bằng
    List<Cost> findByAmountLessThanEqual(Double amount);
    
    // Tìm chi phí trong khoảng số tiền
    List<Cost> findByAmountBetween(Double minAmount, Double maxAmount);
    
    // Tìm chi phí theo Vehicle ID và khoảng số tiền
    List<Cost> findByVehicleIdAndAmountBetween(Integer vehicleId, Double minAmount, Double maxAmount);
    
    // Query tùy chỉnh để tính tổng chi phí theo Vehicle ID
    @Query("SELECT SUM(c.amount) FROM Cost c WHERE c.vehicleId = :vehicleId")
    Double getTotalAmountByVehicleId(@Param("vehicleId") Integer vehicleId);
    
    // Query tùy chỉnh để tính tổng chi phí theo loại
    @Query("SELECT SUM(c.amount) FROM Cost c WHERE c.costType = :costType")
    Double getTotalAmountByCostType(@Param("costType") Cost.CostType costType);
    
    // Query tùy chỉnh để đếm số lượng chi phí theo Vehicle ID
    @Query("SELECT COUNT(c) FROM Cost c WHERE c.vehicleId = :vehicleId")
    Long countByVehicleId(@Param("vehicleId") Integer vehicleId);
    
    // Query tùy chỉnh để đếm số lượng chi phí theo loại
    @Query("SELECT COUNT(c) FROM Cost c WHERE c.costType = :costType")
    Long countByCostType(@Param("costType") Cost.CostType costType);
}
