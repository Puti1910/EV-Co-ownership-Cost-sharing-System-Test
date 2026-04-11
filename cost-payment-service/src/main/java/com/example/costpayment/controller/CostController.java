package com.example.costpayment.controller;

import com.example.costpayment.dto.CostDto;
import com.example.costpayment.dto.CostShareDto;
import com.example.costpayment.dto.CostSplitRequestDto;
import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.service.CostService;
import com.example.costpayment.service.CostShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/costs")
public class CostController {

    private static final Logger logger = LoggerFactory.getLogger(CostController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CostService costService;

    @Autowired
    private CostShareService costShareService;

    // ========== BASIC CRUD ENDPOINTS ==========

    @GetMapping
    public List<CostDto> getAllCosts() {
        logger.info("=== getAllCosts() method called ===");
        List<CostDto> costs = new ArrayList<>();
        try {
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM cost ORDER BY createdAt DESC");
            
            int count = 0;
            while (resultSet.next()) {
                count++;
                try {
                    CostDto cost = new CostDto();
                    cost.setCostId(resultSet.getInt("costId"));
                    cost.setVehicleId(resultSet.getInt("vehicleId"));
                    cost.setCostType(resultSet.getString("costType"));
                    cost.setAmount(resultSet.getDouble("amount"));
                    cost.setDescription(resultSet.getString("description"));
                    
                    // Get status from database (default to PENDING if null)
                    String statusStr = resultSet.getString("status");
                    cost.setStatus(statusStr != null ? statusStr : "PENDING");
                    
                    // Convert Timestamp to LocalDateTime
                    java.sql.Timestamp timestamp = resultSet.getTimestamp("createdAt");
                    if (timestamp != null) {
                        cost.setCreatedAt(timestamp.toLocalDateTime());
                    }
                    
                    costs.add(cost);
                    logger.info("Processed cost {}: ID={}, Amount={}", count, cost.getCostId(), cost.getAmount());
                } catch (Exception rowException) {
                    logger.error("Error processing row {}: {}", count, rowException.getMessage(), rowException);
                }
            }
            
            logger.info("Total rows processed: {}, costs added: {}", count, costs.size());
            
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            logger.error("Database error: {}", e.getMessage(), e);
        }
        return costs;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostDto> getCostById(@PathVariable Integer id) {
        logger.info("=== getCostById() method called for ID: {} ===", id);
        try {
            Optional<Cost> costOpt = costService.getCostById(id);
            if (costOpt.isPresent()) {
                Cost cost = costOpt.get();
                CostDto costDto = convertToDto(cost);
                logger.info("Found cost: ID={}, Amount={}", costDto.getCostId(), costDto.getAmount());
                return ResponseEntity.ok(costDto);
            } else {
                logger.info("No cost found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting cost by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<CostDto> createCost(@Valid @RequestBody CostDto costDto) {
        logger.info("=== createCost() method called ===");
        logger.info("Request: {}", costDto);
        
        try {
            // Convert DTO to Entity
            Cost cost = new Cost();
            cost.setVehicleId(costDto.getVehicleId());
            cost.setCostType(Cost.CostType.valueOf(costDto.getCostType()));
            cost.setAmount(costDto.getAmount());
            cost.setDescription(costDto.getDescription());
            
            // Create cost
            Cost createdCost = costService.createCost(cost);
            CostDto resultDto = convertToDto(createdCost);
            
            logger.info("Created cost: ID={}, Amount={}", resultDto.getCostId(), resultDto.getAmount());
            return ResponseEntity.ok(resultDto);
        } catch (Exception e) {
            logger.error("Error creating cost: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostDto> updateCost(@PathVariable Integer id, @RequestBody CostDto costDto) {
        logger.info("=== updateCost() method called for ID: {} ===", id);
        logger.info("Request: {}", costDto);
        
        try {
            // Convert DTO to Entity
            Cost cost = new Cost();
            cost.setVehicleId(costDto.getVehicleId());
            cost.setCostType(Cost.CostType.valueOf(costDto.getCostType()));
            cost.setAmount(costDto.getAmount());
            cost.setDescription(costDto.getDescription());
            
            // Update cost
            Optional<Cost> updatedCostOpt = costService.updateCost(id, cost);
            if (updatedCostOpt.isPresent()) {
                CostDto resultDto = convertToDto(updatedCostOpt.get());
                logger.info("Updated cost: ID={}, Amount={}", resultDto.getCostId(), resultDto.getAmount());
                return ResponseEntity.ok(resultDto);
            } else {
                logger.info("No cost found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating cost {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCost(@PathVariable Integer id) {
        logger.info("=== deleteCost() method called for ID: {} ===", id);
        
        try {
            boolean deleted = costService.deleteCost(id);
            if (deleted) {
                logger.info("Deleted cost: ID={}", id);
                return ResponseEntity.ok("Cost deleted successfully");
            } else {
                logger.info("No cost found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting cost {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/simple")
    public String getSimpleCosts() {
        try {
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM cost");
            
            int count = 0;
            if (resultSet.next()) {
                count = resultSet.getInt("count");
            }
            
            resultSet.close();
            statement.close();
            connection.close();
            
            return "Database connected! Found " + count + " costs.";
        } catch (Exception e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ========== COST SHARING ENDPOINTS ==========

    /**
     * Lấy thông tin chia chi phí cho một cost cụ thể
     */
    @GetMapping("/{costId}/splits")
    public ResponseEntity<List<CostShareDto>> getCostSplits(@PathVariable Integer costId) {
        logger.info("=== getCostSplits() method called for costId: {} ===", costId);
        try {
            List<CostShare> costShares = costShareService.getCostSharesByCostId(costId);
            List<CostShareDto> costShareDtos = costShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Found {} cost shares for costId: {}", costShareDtos.size(), costId);
            return ResponseEntity.ok(costShareDtos);
        } catch (Exception e) {
            logger.error("Error getting cost splits for costId {}: {}", costId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy thông tin chia chi phí cho một cost cụ thể (alias cho /splits để tương thích)
     */
    @GetMapping("/{costId}/shares")
    public ResponseEntity<List<CostShareDto>> getCostShares(@PathVariable Integer costId) {
        logger.info("=== getCostShares() method called for costId: {} ===", costId);
        // Delegate to getCostSplits method
        return getCostSplits(costId);
    }

    /**
     * Tạo chia chi phí mới cho một cost
     */
    @PostMapping("/{costId}/splits")
    public ResponseEntity<List<CostShareDto>> createCostSplits(
            @PathVariable Integer costId,
            @RequestBody CostSplitRequestDto request) {
        logger.info("=== createCostSplits() method called for costId: {} ===", costId);
        logger.info("Request: {}", request);
        
        try {
            // Validate request
            if (request.getUserIds() == null || request.getPercentages() == null ||
                request.getUserIds().size() != request.getPercentages().size()) {
                logger.error("Invalid request: userIds and percentages must have same size");
                return ResponseEntity.badRequest().build();
            }

            // Validate percentages sum to 100
            double totalPercent = request.getPercentages().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(totalPercent - 100.0) > 0.01) {
                logger.error("Invalid percentages: total must be 100%, got: {}%", totalPercent);
                return ResponseEntity.badRequest().build();
            }

            List<CostShare> costShares = costShareService.calculateCostShares(
                    costId, request.getUserIds(), request.getPercentages());
            
            List<CostShareDto> costShareDtos = costShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Created {} cost shares for costId: {}", costShareDtos.size(), costId);
            return ResponseEntity.ok(costShareDtos);
        } catch (Exception e) {
            logger.error("Error creating cost splits for costId {}: {}", costId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy tất cả cost shares
     */
    @GetMapping("/splits")
    public ResponseEntity<List<CostShareDto>> getAllCostSplits() {
        logger.info("=== getAllCostSplits() method called ===");
        try {
            List<CostShare> costShares = costShareService.getAllCostShares();
            List<CostShareDto> costShareDtos = costShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Found {} total cost shares", costShareDtos.size());
            return ResponseEntity.ok(costShareDtos);
        } catch (Exception e) {
            logger.error("Error getting all cost splits: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy tất cả cost shares (alias cho /splits)
     */
    @GetMapping("/shares")
    public ResponseEntity<List<CostShareDto>> getAllCostShares() {
        logger.info("=== getAllCostShares() method called ===");
        try {
            List<CostShare> costShares = costShareService.getAllCostShares();
            List<CostShareDto> costShareDtos = costShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Found {} total cost shares", costShareDtos.size());
            return ResponseEntity.ok(costShareDtos);
        } catch (Exception e) {
            logger.error("Error getting all cost shares: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tính toán chia chi phí theo phần trăm
     */
    @PostMapping("/{costId}/calculate-shares")
    public ResponseEntity<List<CostShareDto>> calculateCostShares(
            @PathVariable Integer costId,
            @RequestBody CostSplitRequestDto request) {
        logger.info("=== calculateCostShares() method called for costId: {} ===", costId);
        logger.info("Request: {}", request);
        
        try {
            // Validate request
            if (request.getUserIds() == null || request.getPercentages() == null ||
                request.getUserIds().size() != request.getPercentages().size()) {
                logger.error("Invalid request: userIds and percentages must have same size");
                return ResponseEntity.badRequest().build();
            }

            // Validate percentages sum to 100
            double totalPercent = request.getPercentages().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(totalPercent - 100.0) > 0.01) {
                logger.error("Invalid percentages: total must be 100%, got: {}%", totalPercent);
                return ResponseEntity.badRequest().build();
            }

            List<CostShare> costShares = costShareService.calculateCostShares(
                    costId, request.getUserIds(), request.getPercentages());
            
            List<CostShareDto> costShareDtos = costShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Calculated {} cost shares for costId: {}", costShareDtos.size(), costId);
            return ResponseEntity.ok(costShareDtos);
        } catch (Exception e) {
            logger.error("Error calculating cost shares for costId {}: {}", costId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy cost share theo ID
     */
    @GetMapping("/shares/{id}")
    public ResponseEntity<CostShareDto> getCostShareById(@PathVariable Integer id) {
        logger.info("=== getCostShareById() method called for ID: {} ===", id);
        try {
            CostShare costShare = costShareService.getCostShareById(id);
            if (costShare != null) {
                CostShareDto costShareDto = convertToDto(costShare);
                logger.info("Found cost share with ID: {}", id);
                return ResponseEntity.ok(costShareDto);
            } else {
                logger.warn("Cost share not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting cost share by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cập nhật cost share
     */
    @PutMapping("/shares/{id}")
    public ResponseEntity<CostShareDto> updateCostShare(@PathVariable Integer id, @RequestBody CostShareDto costShareDto) {
        logger.info("=== updateCostShare() method called for ID: {} ===", id);
        try {
            CostShare costShare = convertToEntity(costShareDto);
            CostShare updatedCostShare = costShareService.updateCostShare(id, costShare);
            if (updatedCostShare != null) {
                CostShareDto updatedDto = convertToDto(updatedCostShare);
                logger.info("Updated cost share with ID: {}", id);
                return ResponseEntity.ok(updatedDto);
            } else {
                logger.warn("Cost share not found for update with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating cost share with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xóa cost share
     */
    @DeleteMapping("/shares/{id}")
    public ResponseEntity<String> deleteCostShare(@PathVariable Integer id) {
        logger.info("=== deleteCostShare() method called for ID: {} ===", id);
        try {
            costShareService.deleteCostShare(id);
            logger.info("Deleted cost share with ID: {}", id);
            return ResponseEntity.ok("Cost share deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting cost share with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🔍 Tìm kiếm chi phí theo ID
     */
    @GetMapping("/search/{id}")
    public ResponseEntity<CostDto> searchCostById(@PathVariable Integer id) {
        logger.info("=== searchCostById() method called for ID: {} ===", id);
        try {
            CostDto cost = new CostDto();
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM cost WHERE costId = " + id);
            
            if (resultSet.next()) {
                cost.setCostId(resultSet.getInt("costId"));
                cost.setVehicleId(resultSet.getInt("vehicleId"));
                cost.setCostType(resultSet.getString("costType"));
                cost.setAmount(resultSet.getDouble("amount"));
                cost.setDescription(resultSet.getString("description"));
                
                java.sql.Timestamp timestamp = resultSet.getTimestamp("createdAt");
                if (timestamp != null) {
                    cost.setCreatedAt(timestamp.toLocalDateTime());
                }
                
                logger.info("Found cost: ID={}, Amount={}", cost.getCostId(), cost.getAmount());
                resultSet.close();
                statement.close();
                connection.close();
                return ResponseEntity.ok(cost);
            } else {
                logger.info("No cost found with ID: {}", id);
                resultSet.close();
                statement.close();
                connection.close();
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error searching cost by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📊 Lấy thống kê chia sẻ chi phí cho một user
     */
    @GetMapping("/shares/user/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getUserCostShareStatistics(@PathVariable Integer userId) {
        logger.info("=== getUserCostShareStatistics() method called for userId: {} ===", userId);
        try {
            Map<String, Object> stats = costShareService.getCostShareStatisticsByUser(userId);
            logger.info("Retrieved statistics for userId: {}", userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting user cost share statistics for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📈 Lấy lịch sử chia sẻ chi phí
     */
    @GetMapping("/{costId}/shares/history")
    public ResponseEntity<List<Map<String, Object>>> getCostShareHistory(@PathVariable Integer costId) {
        logger.info("=== getCostShareHistory() method called for costId: {} ===", costId);
        try {
            List<Map<String, Object>> history = costShareService.getCostShareHistory(costId);
            logger.info("Retrieved {} history items for costId: {}", history.size(), costId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error getting cost share history for costId {}: {}", costId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ Kiểm tra xem chi phí đã được chia sẻ chưa
     */
    @GetMapping("/{costId}/shares/status")
    public ResponseEntity<Map<String, Object>> getCostShareStatus(@PathVariable Integer costId) {
        logger.info("=== getCostShareStatus() method called for costId: {} ===", costId);
        try {
            boolean isShared = costShareService.isCostShared(costId);
            List<CostShare> shares = costShareService.getCostSharesByCostId(costId);
            
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("costId", costId);
            status.put("isShared", isShared);
            status.put("shareCount", shares.size());
            status.put("totalSharedAmount", shares.stream().mapToDouble(CostShare::getAmountShare).sum());
            
            logger.info("Cost {} share status: isShared={}, shareCount={}", costId, isShared, shares.size());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting cost share status for costId {}: {}", costId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🔄 Cập nhật chia sẻ chi phí với validation
     */
    @PutMapping("/shares/{shareId}")
    public ResponseEntity<CostShareDto> updateCostShareWithValidation(
            @PathVariable Integer shareId,
            @RequestBody CostShareDto updatedShareDto) {
        logger.info("=== updateCostShareWithValidation() method called for shareId: {} ===", shareId);
        try {
            CostShare updatedShare = new CostShare();
            updatedShare.setUserId(updatedShareDto.getUserId());
            updatedShare.setPercent(updatedShareDto.getPercent());
            
            CostShare result = costShareService.updateCostShareWithValidation(shareId, updatedShare);
            CostShareDto resultDto = convertToDto(result);
            
            logger.info("Updated cost share: shareId={}", shareId);
            return ResponseEntity.ok(resultDto);
        } catch (Exception e) {
            logger.error("Error updating cost share {}: {}", shareId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Convert Cost entity to CostDto
     */
    private CostDto convertToDto(Cost cost) {
        CostDto costDto = new CostDto();
        costDto.setCostId(cost.getCostId());
        costDto.setVehicleId(cost.getVehicleId());
        costDto.setCostType(cost.getCostType().name());
        costDto.setAmount(cost.getAmount());
        costDto.setDescription(cost.getDescription());
        costDto.setCreatedAt(cost.getCreatedAt());
        // Map status from entity (default to PENDING if null)
        if (cost.getStatus() != null) {
            costDto.setStatus(cost.getStatus().name());
        } else {
            costDto.setStatus("PENDING");
        }
        return costDto;
    }

    /**
     * Convert CostShare entity to CostShareDto
     */
    private CostShareDto convertToDto(CostShare costShare) {
        return new CostShareDto(
                costShare.getShareId(),
                costShare.getCostId(),
                costShare.getUserId(),
                costShare.getPercent(),
                costShare.getAmountShare(),
                costShare.getCalculatedAt(),
                "PENDING" // Default status for now
        );
    }

    /**
     * Convert CostShareDto to CostShare entity
     */
    private CostShare convertToEntity(CostShareDto costShareDto) {
        CostShare costShare = new CostShare();
        costShare.setShareId(costShareDto.getShareId());
        costShare.setCostId(costShareDto.getCostId());
        costShare.setUserId(costShareDto.getUserId());
        costShare.setPercent(costShareDto.getPercent());
        costShare.setAmountShare(costShareDto.getAmountShare());
        costShare.setCalculatedAt(costShareDto.getCalculatedAt());
        // Note: CostShare entity doesn't have status field, so we skip it
        return costShare;
    }
}