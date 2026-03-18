package com.example.costpayment.service.impl;

import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.repository.CostRepository;
import com.example.costpayment.repository.CostShareRepository;
import com.example.costpayment.service.CostShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CostShareServiceImpl implements CostShareService {

    @Autowired
    private CostRepository costRepository;

    @Autowired
    private CostShareRepository costShareRepository;

    public CostShare createCostShare(Integer costId, CostShare costShare) {
        costShare.setCostId(costId);
        CostShare savedShare = costShareRepository.save(costShare);
        
        // Update cost status to SHARED after creating a share
        Optional<Cost> costOpt = costRepository.findById(costId);
        if (costOpt.isPresent()) {
            Cost cost = costOpt.get();
            cost.setStatus(Cost.CostStatus.SHARED);
            costRepository.save(cost);
        }
        
        return savedShare;
    }

    public List<CostShare> getCostSharesByCostId(Integer costId) {
        return costShareRepository.findByCostId(costId);
    }

    /**
     * 💰 Tính toán chia chi phí theo phần trăm với logic chính xác
     * Ví dụ: Chi phí 1,000,000 VNĐ
     * - Thành viên A: 50% = 500,000 VNĐ
     * - Thành viên B: 30% = 300,000 VNĐ  
     * - Thành viên C: 20% = 200,000 VNĐ
     */
    public List<CostShare> calculateCostShares(Integer costId, List<Integer> userIds, List<Double> percentages) {
        // Kiểm tra chi phí tồn tại
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        // Kiểm tra tính hợp lệ của đầu vào
        if (userIds.size() != percentages.size()) {
            throw new RuntimeException("Số lượng thành viên và phần trăm không khớp");
        }

        // Kiểm tra tổng phần trăm = 100%
        double totalPercent = percentages.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalPercent - 100.0) > 0.01) {
            throw new RuntimeException("Tổng phần trăm phải bằng 100%. Hiện tại: " + totalPercent + "%");
        }

        // Xóa các chia sẻ cũ nếu có
        List<CostShare> existingShares = costShareRepository.findByCostId(costId);
        for (CostShare existingShare : existingShares) {
            costShareRepository.delete(existingShare);
        }

        // Tính toán chia chi phí với độ chính xác cao
        List<CostShare> shares = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.valueOf(cost.getAmount());
        BigDecimal remainingAmount = totalAmount;

        for (int i = 0; i < userIds.size(); i++) {
            BigDecimal percent = BigDecimal.valueOf(percentages.get(i));
            BigDecimal shareAmount;
            
            if (i == userIds.size() - 1) {
                // Thành viên cuối cùng nhận phần còn lại để tránh sai số làm tròn
                shareAmount = remainingAmount;
            } else {
                // Tính toán chính xác với BigDecimal
                shareAmount = totalAmount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                remainingAmount = remainingAmount.subtract(shareAmount);
            }

            CostShare share = new CostShare(costId, userIds.get(i), percentages.get(i), shareAmount.doubleValue());
            shares.add(costShareRepository.save(share));
        }

        // Update cost status to SHARED after creating shares
        cost.setStatus(Cost.CostStatus.SHARED);
        costRepository.save(cost);

        return shares;
    }

    // Additional methods for UI Service compatibility
    public List<CostShare> getAllCostShares() {
        return costShareRepository.findAll();
    }

    public CostShare getCostShareById(Integer id) {
        Optional<CostShare> costShare = costShareRepository.findById(id);
        return costShare.orElse(null);
    }

    public CostShare updateCostShare(Integer id, CostShare costShare) {
        Optional<CostShare> existingCostShare = costShareRepository.findById(id);
        if (existingCostShare.isPresent()) {
            costShare.setShareId(id);
            return costShareRepository.save(costShare);
        }
        return null;
    }

    public void deleteCostShare(Integer id) {
        // Get the costId before deleting
        Optional<CostShare> shareOpt = costShareRepository.findById(id);
        if (shareOpt.isPresent()) {
            Integer costId = shareOpt.get().getCostId();
            costShareRepository.deleteById(id);
            
            // Check if there are any remaining shares for this cost
            List<CostShare> remainingShares = costShareRepository.findByCostId(costId);
            if (remainingShares.isEmpty()) {
                // No more shares, set status back to PENDING
                Optional<Cost> costOpt = costRepository.findById(costId);
                if (costOpt.isPresent()) {
                    Cost cost = costOpt.get();
                    cost.setStatus(Cost.CostStatus.PENDING);
                    costRepository.save(cost);
                }
            }
        } else {
            costShareRepository.deleteById(id);
        }
    }

    /**
     * 🔍 Tìm kiếm chia sẻ chi phí theo User ID
     */
    public List<CostShare> getCostSharesByUserId(Integer userId) {
        return costShareRepository.findByUserId(userId);
    }

    /**
     * 📊 Lấy thống kê chia sẻ chi phí cho một user
     */
    public Map<String, Object> getCostShareStatisticsByUser(Integer userId) {
        List<CostShare> userShares = getCostSharesByUserId(userId);
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("userId", userId);
        stats.put("totalShares", userShares.size());
        stats.put("totalAmount", userShares.stream().mapToDouble(CostShare::getAmountShare).sum());
        stats.put("averageAmount", userShares.stream().mapToDouble(CostShare::getAmountShare).average().orElse(0.0));
        
        // Group by cost ID để xem chi phí nào user đã chia sẻ
        Map<Integer, Double> costBreakdown = userShares.stream()
            .collect(Collectors.groupingBy(
                CostShare::getCostId,
                Collectors.summingDouble(CostShare::getAmountShare)
            ));
        stats.put("costBreakdown", costBreakdown);
        
        return stats;
    }

    /**
     * 📈 Lấy lịch sử chia sẻ chi phí với thông tin chi tiết
     */
    public List<Map<String, Object>> getCostShareHistory(Integer costId) {
        List<CostShare> shares = getCostSharesByCostId(costId);
        Optional<Cost> costOpt = costRepository.findById(costId);
        
        if (!costOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        Cost cost = costOpt.get();
        
        return shares.stream().map(share -> {
            Map<String, Object> historyItem = new java.util.HashMap<>();
            historyItem.put("shareId", share.getShareId());
            historyItem.put("userId", share.getUserId());
            historyItem.put("percent", share.getPercent());
            historyItem.put("amountShare", share.getAmountShare());
            historyItem.put("calculatedAt", share.getCalculatedAt());
            historyItem.put("costType", cost.getCostType().getDisplayName());
            historyItem.put("totalCostAmount", cost.getAmount());
            historyItem.put("description", cost.getDescription());
            return historyItem;
        }).collect(Collectors.toList());
    }

    /**
     * ✅ Lấy cost shares của user theo status
     * Note: CostShare entity không có status field, nên method này chỉ return all shares
     */
    @Override
    public List<CostShare> getCostSharesByUserIdAndStatus(Integer userId, String status) {
        // CostShare không có status, nên chỉ return tất cả shares của user
        return costShareRepository.findByUserId(userId);
    }

    /**
     * ✅ Cập nhật cost share
     */
    @Override
    public CostShare updateCostShare(CostShare costShare) {
        return costShareRepository.save(costShare);
    }

    /**
     * ✅ Kiểm tra xem một chi phí đã được chia sẻ chưa
     */
    public boolean isCostShared(Integer costId) {
        return !costShareRepository.findByCostId(costId).isEmpty();
    }

    /**
     * 🔄 Cập nhật chia sẻ chi phí với validation
     */
    public CostShare updateCostShareWithValidation(Integer id, CostShare updatedShare) {
        Optional<CostShare> existingOpt = costShareRepository.findById(id);
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("Không tìm thấy chia sẻ chi phí ID: " + id);
        }
        
        CostShare existing = existingOpt.get();
        
        // Kiểm tra tổng phần trăm không vượt quá 100%
        List<CostShare> otherShares = costShareRepository.findByCostId(existing.getCostId())
            .stream()
            .filter(share -> !share.getShareId().equals(id))
            .collect(Collectors.toList());
        
        double otherPercentTotal = otherShares.stream().mapToDouble(CostShare::getPercent).sum();
        if (otherPercentTotal + updatedShare.getPercent() > 100.0) {
            throw new RuntimeException("Tổng phần trăm không được vượt quá 100%");
        }
        
        // Cập nhật thông tin
        existing.setUserId(updatedShare.getUserId());
        existing.setPercent(updatedShare.getPercent());
        
        // Tính lại số tiền chia sẻ
        Optional<Cost> costOpt = costRepository.findById(existing.getCostId());
        if (costOpt.isPresent()) {
            Cost cost = costOpt.get();
            BigDecimal shareAmount = BigDecimal.valueOf(cost.getAmount())
                .multiply(BigDecimal.valueOf(updatedShare.getPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            existing.setAmountShare(shareAmount.doubleValue());
        }
        
        return costShareRepository.save(existing);
    }
}
