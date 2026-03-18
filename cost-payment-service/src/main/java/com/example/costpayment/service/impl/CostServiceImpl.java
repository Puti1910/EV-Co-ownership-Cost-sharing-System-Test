package com.example.costpayment.service.impl;

import com.example.costpayment.entity.Cost;
import com.example.costpayment.repository.CostRepository;
import com.example.costpayment.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CostServiceImpl implements CostService {

    @Autowired
    private CostRepository costRepository;

    // ✅ Các chức năng cơ bản CRUD
    @Override
    public List<Cost> getAllCosts() {
        return costRepository.findAll();
    }

    @Override
    public Optional<Cost> getCostById(Integer id) {
        return costRepository.findById(id);
    }

    @Override
    public Cost createCost(Cost cost) {
        return costRepository.save(cost);
    }

    @Override
    public Optional<Cost> updateCost(Integer id, Cost updatedCost) {
        return getCostById(id).map(cost -> {
            cost.setVehicleId(updatedCost.getVehicleId());
            cost.setCostType(updatedCost.getCostType());
            cost.setAmount(updatedCost.getAmount());
            cost.setDescription(updatedCost.getDescription());
            return costRepository.save(cost);
        });
    }

    @Override
    public boolean deleteCost(Integer id) {
        if (costRepository.existsById(id)) {
            costRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // ✅ Tìm kiếm chi phí theo các tiêu chí
    @Override
    public List<Cost> getCostsByVehicleId(Integer vehicleId) {
        return costRepository.findByVehicleId(vehicleId);
    }

    @Override
    public List<Cost> getCostsByType(String costType) {
        return costRepository.findByCostType(Enum.valueOf(Cost.CostType.class, costType));
    }

    @Override
    public List<Cost> getCostsByVehicleIdAndType(Integer vehicleId, String costType) {
        return costRepository.findByVehicleIdAndCostType(vehicleId, Enum.valueOf(Cost.CostType.class, costType));
    }

    // ✅ Tìm kiếm theo khoảng thời gian
    @Override
    public List<Cost> getCostsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return costRepository.findByCreatedAtBetween(startDate, endDate);
    }

    @Override
    public List<Cost> getCostsByVehicleIdAndDateRange(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        return costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
    }

    // ✅ Tìm kiếm theo khoảng số tiền
    @Override
    public List<Cost> getCostsByAmountRange(Double minAmount, Double maxAmount) {
        return costRepository.findByAmountBetween(minAmount, maxAmount);
    }

    @Override
    public List<Cost> getCostsByVehicleIdAndAmountRange(Integer vehicleId, Double minAmount, Double maxAmount) {
        return costRepository.findByVehicleIdAndAmountBetween(vehicleId, minAmount, maxAmount);
    }

    // ✅ Tìm kiếm nâng cao với nhiều điều kiện
    @Override
    public List<Cost> searchCosts(Integer vehicleId, String costType, Double minAmount, Double maxAmount, 
                                  LocalDateTime startDate, LocalDateTime endDate) {
        List<Cost> allCosts = costRepository.findAll();
        
        return allCosts.stream()
            .filter(cost -> vehicleId == null || cost.getVehicleId().equals(vehicleId))
            .filter(cost -> costType == null || cost.getCostType().name().equals(costType))
            .filter(cost -> minAmount == null || cost.getAmount() >= minAmount)
            .filter(cost -> maxAmount == null || cost.getAmount() <= maxAmount)
            .filter(cost -> startDate == null || cost.getCreatedAt().isAfter(startDate) || cost.getCreatedAt().isEqual(startDate))
            .filter(cost -> endDate == null || cost.getCreatedAt().isBefore(endDate) || cost.getCreatedAt().isEqual(endDate))
            .collect(Collectors.toList());
    }

    // ✅ Thống kê chi phí
    @Override
    public Map<String, Object> getCostStatistics() {
        List<Cost> allCosts = costRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCosts", allCosts.size());
        stats.put("totalAmount", allCosts.stream().mapToDouble(Cost::getAmount).sum());
        stats.put("averageAmount", allCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
        
        // Group by cost type
        Map<String, Double> costTypeStats = allCosts.stream()
            .collect(Collectors.groupingBy(
                cost -> cost.getCostType().name(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("costTypeBreakdown", costTypeStats);
        
        // Group by vehicle
        Map<Integer, Double> vehicleStats = allCosts.stream()
            .collect(Collectors.groupingBy(
                Cost::getVehicleId,
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("vehicleBreakdown", vehicleStats);
        
        return stats;
    }

    @Override
    public Map<String, Object> getCostStatisticsByVehicle(Integer vehicleId) {
        List<Cost> vehicleCosts = costRepository.findByVehicleId(vehicleId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("vehicleId", vehicleId);
        stats.put("totalCosts", vehicleCosts.size());
        stats.put("totalAmount", vehicleCosts.stream().mapToDouble(Cost::getAmount).sum());
        stats.put("averageAmount", vehicleCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
        
        // Group by cost type for this vehicle
        Map<String, Double> costTypeStats = vehicleCosts.stream()
            .collect(Collectors.groupingBy(
                cost -> cost.getCostType().name(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("costTypeBreakdown", costTypeStats);
        
        return stats;
    }

    @Override
    public Map<String, Object> getCostStatisticsByType(String costType) {
        List<Cost> typeCosts = getCostsByType(costType);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("costType", costType);
        stats.put("totalCosts", typeCosts.size());
        stats.put("totalAmount", typeCosts.stream().mapToDouble(Cost::getAmount).sum());
        stats.put("averageAmount", typeCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
        
        // Group by vehicle for this cost type
        Map<Integer, Double> vehicleStats = typeCosts.stream()
            .collect(Collectors.groupingBy(
                Cost::getVehicleId,
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("vehicleBreakdown", vehicleStats);
        
        return stats;
    }

    @Override
    public Map<String, Object> getCostStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Cost> rangeCosts = getCostsByDateRange(startDate, endDate);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        stats.put("totalCosts", rangeCosts.size());
        stats.put("totalAmount", rangeCosts.stream().mapToDouble(Cost::getAmount).sum());
        stats.put("averageAmount", rangeCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
        
        // Group by cost type
        Map<String, Double> costTypeStats = rangeCosts.stream()
            .collect(Collectors.groupingBy(
                cost -> cost.getCostType().name(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("costTypeBreakdown", costTypeStats);
        
        // Group by vehicle
        Map<Integer, Double> vehicleStats = rangeCosts.stream()
            .collect(Collectors.groupingBy(
                Cost::getVehicleId,
                Collectors.summingDouble(Cost::getAmount)
            ));
        stats.put("vehicleBreakdown", vehicleStats);
        
        return stats;
    }

    // ✅ Tính tổng chi phí
    @Override
    public Double getTotalAmountByVehicleId(Integer vehicleId) {
        Double total = costRepository.getTotalAmountByVehicleId(vehicleId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalAmountByCostType(String costType) {
        Double total = costRepository.getTotalAmountByCostType(Enum.valueOf(Cost.CostType.class, costType));
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Cost> rangeCosts = getCostsByDateRange(startDate, endDate);
        return rangeCosts.stream().mapToDouble(Cost::getAmount).sum();
    }

    // ✅ Đếm số lượng chi phí
    @Override
    public Long countCostsByVehicleId(Integer vehicleId) {
        return costRepository.countByVehicleId(vehicleId);
    }

    @Override
    public Long countCostsByCostType(String costType) {
        return costRepository.countByCostType(Enum.valueOf(Cost.CostType.class, costType));
    }

    @Override
    public Long countCostsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Cost> rangeCosts = getCostsByDateRange(startDate, endDate);
        return (long) rangeCosts.size();
    }
}
