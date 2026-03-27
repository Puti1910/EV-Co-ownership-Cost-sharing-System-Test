package com.example.costpayment.controller;

import com.example.costpayment.dto.CostShareDto;
import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.entity.Payment;
import com.example.costpayment.entity.PaymentStatus;
import com.example.costpayment.entity.UsageTracking;
import com.example.costpayment.repository.UsageTrackingRepository;
import com.example.costpayment.service.AutoCostSplitService;
import com.example.costpayment.service.CostService;
import com.example.costpayment.service.CostShareService;
import com.example.costpayment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for Cost Share operations
 */
@RestController
@RequestMapping("/api/cost-shares")
public class CostShareRestController {

    private static final Logger logger = LoggerFactory.getLogger(CostShareRestController.class);

    @Autowired
    private CostShareService costShareService;

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private CostService costService;
    
    @Autowired(required = false)
    private AutoCostSplitService autoCostSplitService;
    
    @Autowired(required = false)
    private UsageTrackingRepository usageTrackingRepository;

    /**
     * Get pending (unpaid) cost shares for a user
     * GET /api/cost-shares/user/{userId}/pending
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<List<CostShareDto>> getPendingCostSharesByUserId(@PathVariable Integer userId) {
        try {
            logger.info("Fetching pending cost shares for userId: {}", userId);
            
            // Get all cost shares for user
            List<CostShare> allShares = costShareService.getCostSharesByUserId(userId);
            logger.info("Found {} total cost shares for userId: {}", allShares.size(), userId);
            
            // Get all payments for user
            List<Payment> userPayments = paymentService.getPaymentsByUserId(userId);
            logger.info("Found {} payments for userId: {}", userPayments.size(), userId);
            
            // Filter out shares that have been paid
            List<CostShare> pendingShares = allShares.stream()
                    .filter(share -> {
                        boolean isPaid = userPayments.stream()
                                .anyMatch(payment -> 
                                    payment.getCostId().equals(share.getCostId()) && 
                                    payment.getStatus() == PaymentStatus.PAID);
                        return !isPaid;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} pending cost shares for userId: {}", pendingShares.size(), userId);
            
            List<CostShareDto> dtos = pendingShares.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching pending cost shares for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Return empty list on error
        }
    }

    /**
     * Confirm payment for a cost share
     * POST /api/cost-shares/{shareId}/payment
     */
    @PostMapping("/{shareId}/payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable Integer shareId,
            @RequestBody Map<String, Object> paymentInfo) {
        try {
            logger.info("Processing payment confirmation for shareId: {}", shareId);
            logger.info("Payment info: {}", paymentInfo);
            
            // Get cost share
            CostShare costShare = costShareService.getCostShareById(shareId);
            if (costShare == null) {
                logger.error("Cost share not found with id: {}", shareId);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cost share not found"));
            }
            
            // Extract payment info
            String paymentMethod = (String) paymentInfo.get("paymentMethod");
            String transactionCode = (String) paymentInfo.get("transactionCode");
            
            logger.info("Creating payment record - userId: {}, costId: {}, amount: {}, method: {}", 
                    costShare.getUserId(), costShare.getCostId(), costShare.getAmountShare(), paymentMethod);
            
            // Create payment record
            Payment payment = new Payment();
            payment.setUserId(costShare.getUserId());
            payment.setCostId(costShare.getCostId());
            payment.setAmount(costShare.getAmountShare());
            payment.setMethod(parsePaymentMethod(paymentMethod));
            payment.setTransactionCode(transactionCode);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus(PaymentStatus.PAID);
            
            Payment savedPayment = paymentService.createPayment(payment);
            logger.info("Payment created successfully with ID: {}", savedPayment.getPaymentId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment confirmed successfully",
                "paymentId", savedPayment.getPaymentId()
            ));
            
        } catch (Exception e) {
            logger.error("Error confirming payment for shareId {}: {}", shareId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment history (paid cost shares) for a user
     * GET /api/cost-shares/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<CostShareDto>> getPaymentHistoryByUserId(@PathVariable Integer userId) {
        try {
            logger.info("Fetching payment history for userId: {}", userId);
            
            // Get all paid payments for user
            List<Payment> paidPayments = paymentService.getPaymentsByUserId(userId).stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                    .collect(Collectors.toList());
            
            logger.info("Found {} paid payments for userId: {}", paidPayments.size(), userId);
            
            // Get corresponding cost shares
            List<CostShare> paidShares = paidPayments.stream()
                    .map(payment -> {
                        List<CostShare> shares = costShareService.getCostSharesByCostId(payment.getCostId());
                        return shares.stream()
                                .filter(share -> share.getUserId().equals(userId))
                                .findFirst()
                                .orElse(null);
                    })
                    .filter(share -> share != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} paid cost shares for userId: {}", paidShares.size(), userId);
            
            List<CostShareDto> dtos = paidShares.stream()
                    .map(share -> {
                        CostShareDto dto = convertToDto(share);
                        dto.setStatus("PAID");
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching payment history for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Return empty list on error
        }
    }

    /**
     * Convert CostShare entity to DTO with full information
     */
    private CostShareDto convertToDto(CostShare share) {
        CostShareDto dto = new CostShareDto();
        dto.setShareId(share.getShareId());
        dto.setUserId(share.getUserId());
        dto.setCostId(share.getCostId());
        dto.setAmountShare(share.getAmountShare());
        dto.setPercent(share.getPercent());
        dto.setCalculatedAt(share.getCalculatedAt());
        dto.setStatus("PENDING"); // Default status
        
        // Get full Cost information
        try {
            costService.getCostById(share.getCostId()).ifPresent(cost -> {
                // Basic cost info
                dto.setDescription(cost.getDescription());
                dto.setTotalAmount(cost.getAmount());
                
                // Cost type
                dto.setCostType(cost.getCostType().name());
                dto.setCostTypeDisplay(cost.getCostType().getDisplayName());
                
                // Determine split method based on cost type
                String splitMethod = determineSplitMethod(cost.getCostType());
                dto.setSplitMethod(splitMethod);
                dto.setSplitMethodDisplay(getSplitMethodDisplayName(splitMethod));
                
                // If split by ownership, get ownership percent
                if ("BY_OWNERSHIP".equals(splitMethod)) {
                    dto.setOwnershipPercent(share.getPercent());
                }
                
                // If split by usage, try to get km information
                if ("BY_USAGE".equals(splitMethod)) {
                    try {
                        // Try to get km from usage tracking
                        // Note: We need groupId and month/year, which we don't have directly
                        // For now, we'll try to get it from the calculated date
                        LocalDateTime calculatedAt = share.getCalculatedAt();
                        if (calculatedAt != null) {
                            int month = calculatedAt.getMonthValue();
                            int year = calculatedAt.getYear();
                            
                            // Try to find usage tracking for this user in the same month/year
                            // We'll need groupId, but we can try to find it from other shares
                            List<CostShare> allSharesForCost = costShareService.getCostSharesByCostId(share.getCostId());
                            if (!allSharesForCost.isEmpty()) {
                                // Try to get km if usageTrackingRepository is available
                                if (usageTrackingRepository != null) {
                                    // We need groupId - for now, we'll skip detailed km info
                                    // This can be enhanced later when we have groupId in CostShare or Cost
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Could not get km information: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to get cost information for costId {}: {}", share.getCostId(), e.getMessage());
            dto.setDescription("Chi phí #" + share.getCostId());
        }
        
        return dto;
    }
    
    /**
     * Determine split method based on cost type
     */
    private String determineSplitMethod(Cost.CostType costType) {
        switch (costType) {
            case ElectricCharge:
                return "BY_USAGE";
            case Maintenance:
            case Insurance:
            case Inspection:
                return "BY_OWNERSHIP";
            case Cleaning:
            case Other:
                return "EQUAL";
            default:
                return "BY_OWNERSHIP";
        }
    }
    
    /**
     * Get display name for split method
     */
    private String getSplitMethodDisplayName(String splitMethod) {
        switch (splitMethod) {
            case "BY_USAGE":
                return "Chia theo km";
            case "BY_OWNERSHIP":
                return "Chia theo sở hữu";
            case "EQUAL":
                return "Chia đều";
            default:
                return "Chia theo sở hữu";
        }
    }

    /**
     * Parse payment method string to enum
     */
    private Payment.Method parsePaymentMethod(String method) {
        if (method == null || method.isEmpty()) {
            return Payment.Method.EWALLET;
        }
        
        switch (method.toLowerCase()) {
            case "ewallet":
                return Payment.Method.EWALLET;
            case "banking":
                return Payment.Method.BANKING;
            case "cash":
                return Payment.Method.CASH;
            default:
                return Payment.Method.EWALLET;
        }
    }
}


