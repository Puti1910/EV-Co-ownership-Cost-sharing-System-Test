package com.example.costpayment.controller;

import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.dto.UsageTrackingDto;
import com.example.costpayment.service.AutoCostSplitService;
import com.example.costpayment.service.CostService;
import com.example.costpayment.service.UsageTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API cho Auto Split - Tự động chia chi phí
 */
@RestController
@RequestMapping("/api/auto-split")
@CrossOrigin(origins = "*")
public class AutoSplitController {

    @Autowired
    private AutoCostSplitService autoSplitService;

    @Autowired
    private CostService costService;

    @Autowired
    private UsageTrackingService usageTrackingService;

    /**
     * Tự động chia chi phí
     * POST /api/auto-split/cost/{costId}
     */
    @PostMapping("/cost/{costId}")
    public ResponseEntity<Map<String, Object>> autoSplitCost(
            @PathVariable Integer costId,
            @RequestParam Integer groupId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        // Lấy tháng/năm hiện tại nếu không có
        if (month == null) {
            month = java.time.LocalDate.now().getMonthValue();
        }
        if (year == null) {
            year = java.time.LocalDate.now().getYear();
        }

        // Thực hiện auto split
        List<CostShare> shares = autoSplitService.autoSplitCost(costId, groupId, month, year);

        // Trả về kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("costId", costId);
        result.put("totalShares", shares.size());
        result.put("shares", shares);
        result.put("message", "Đã tự động chia chi phí thành công!");

        return ResponseEntity.ok(result);
    }

    /**
     * Tạo chi phí MỚI và tự động chia luôn HOẶC chia chi phí đã tồn tại
     * POST /api/auto-split/create-and-split
     * 
     * Request có thể có:
     * - costId: Nếu có -> chia chi phí đã tồn tại (không tạo mới)
     * - amount: Nếu có -> tạo chi phí mới và chia
     */
    @PostMapping("/create-and-split")
    public ResponseEntity<Map<String, Object>> createAndAutoSplit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            Integer costId = request.get("costId") != null ? 
                (Integer) request.get("costId") : null;
            String splitMethod = (String) request.get("splitMethod");
            Integer groupId = (Integer) request.get("groupId");
            
            // Lấy tháng/năm (mặc định là hiện tại nếu không có)
            Integer month = request.get("month") != null ? 
                (Integer) request.get("month") : 
                java.time.LocalDate.now().getMonthValue();
            Integer year = request.get("year") != null ? 
                (Integer) request.get("year") : 
                java.time.LocalDate.now().getYear();

            System.out.println("=== AUTO SPLIT REQUEST ===");
            System.out.println("Cost ID: " + costId);
            System.out.println("Split Method: " + splitMethod);
            System.out.println("Group ID: " + groupId);
            System.out.println("Month/Year: " + month + "/" + year);

            Cost savedCost;
            List<CostShare> shares;

            // Xử lý hai trường hợp: chia chi phí đã tồn tại HOẶC tạo mới và chia
            if (costId != null) {
                // Trường hợp 1: Chia chi phí đã tồn tại
                System.out.println("Splitting existing cost ID: " + costId);
                
                // Validate splitMethod và groupId
                if (splitMethod == null || splitMethod.isEmpty()) {
                    throw new IllegalArgumentException("splitMethod là bắt buộc");
                }
                if (groupId == null) {
                    throw new IllegalArgumentException("groupId là bắt buộc");
                }
                
                // Lấy cost từ database
                savedCost = costService.getCostById(costId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));
                
                System.out.println("Found cost: ID=" + savedCost.getCostId() + ", Amount=" + savedCost.getAmount());
                
                // Chia chi phí theo splitMethod
                shares = autoSplitService.autoSplitCostWithMethod(
                    costId, 
                    groupId, 
                    splitMethod, 
                    month, 
                    year,
                    token
                );
                
                System.out.println("Created " + shares.size() + " cost shares for existing cost");
                
            } else {
                // Trường hợp 2: Tạo chi phí mới và chia
                System.out.println("Creating new cost and splitting...");
                
                // Parse request để tạo cost mới
                Integer vehicleId = (Integer) request.get("vehicleId");
                String costType = (String) request.get("costType");
                Object amountObj = request.get("amount");
                
                // Validate required fields
                if (amountObj == null) {
                    throw new IllegalArgumentException("amount là bắt buộc khi tạo chi phí mới");
                }
                if (costType == null || costType.isEmpty()) {
                    throw new IllegalArgumentException("costType là bắt buộc khi tạo chi phí mới");
                }
                if (splitMethod == null || splitMethod.isEmpty()) {
                    throw new IllegalArgumentException("splitMethod là bắt buộc");
                }
                if (groupId == null) {
                    throw new IllegalArgumentException("groupId là bắt buộc");
                }
                
                Double amount;
                if (amountObj instanceof Number) {
                    amount = ((Number) amountObj).doubleValue();
                } else {
                    throw new IllegalArgumentException("amount phải là số");
                }
                
                String description = (String) request.get("description");
                
                System.out.println("Vehicle ID: " + vehicleId);
                System.out.println("Cost Type: " + costType);
                System.out.println("Amount: " + amount);

                // Tạo cost mới
                Cost cost = new Cost();
                cost.setVehicleId(vehicleId);
                cost.setCostType(Cost.CostType.valueOf(costType));
                cost.setAmount(amount);
                cost.setDescription(description);
                
                savedCost = costService.createCost(cost);
                System.out.println("Created Cost ID: " + savedCost.getCostId());

                // Tự động chia CHI PHÍ theo splitMethod
                shares = autoSplitService.autoSplitCostWithMethod(
                    savedCost.getCostId(), 
                    groupId, 
                    splitMethod, 
                    month, 
                    year,
                    token
                );

                System.out.println("Created " + shares.size() + " cost shares for new cost");
            }

            // Trả về kết quả
            Map<String, Object> result = new HashMap<>();
            result.put("cost", savedCost);
            result.put("shares", shares);
            result.put("message", costId != null ? 
                "Đã phân chia chi phí thành công!" : 
                "Đã tạo chi phí và tự động chia thành công!");

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tạo và chia chi phí: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Chia theo sở hữu
     * POST /api/auto-split/by-ownership
     */
    @PostMapping("/by-ownership")
    public ResponseEntity<List<CostShare>> splitByOwnership(
            @RequestParam Integer costId,
            @RequestParam Integer groupId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Map<Integer, Double> ownershipMap = autoSplitService.getGroupOwnership(groupId, token);
        List<CostShare> shares = autoSplitService.splitByOwnership(costId, ownershipMap);
        
        return ResponseEntity.ok(shares);
    }

    /**
     * Chia theo km (usage)
     * POST /api/auto-split/by-usage
     */
    @PostMapping("/by-usage")
    public ResponseEntity<List<CostShare>> splitByUsage(
            @RequestParam Integer costId,
            @RequestParam Integer groupId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        // Lấy km từ database
        List<UsageTrackingDto> usageList = usageTrackingService.getGroupUsageInMonth(groupId, month, year);
        
        if (usageList == null || usageList.isEmpty()) {
            throw new RuntimeException("Không có dữ liệu km cho nhóm " + groupId + 
                " trong tháng " + month + "/" + year);
        }
        
        Map<Integer, Double> usageMap = new HashMap<>();
        for (UsageTrackingDto usage : usageList) {
            if (usage.getKmDriven() != null && usage.getKmDriven() > 0) {
                usageMap.put(usage.getUserId(), usage.getKmDriven());
            }
        }
        
        if (usageMap.isEmpty()) {
            throw new RuntimeException("Không có dữ liệu km hợp lệ cho nhóm " + groupId);
        }
        
        List<CostShare> shares = autoSplitService.splitByUsage(costId, usageMap);
        
        return ResponseEntity.ok(shares);
    }

    /**
     * Chia đều
     * POST /api/auto-split/equal
     */
    @PostMapping("/equal")
    public ResponseEntity<List<CostShare>> splitEqually(
            @RequestParam Integer costId,
            @RequestBody List<Integer> userIds) {
        
        List<CostShare> shares = autoSplitService.splitEqually(costId, userIds);
        
        return ResponseEntity.ok(shares);
    }

    /**
     * Lấy ownership của nhóm
     * GET /api/auto-split/ownership/{groupId}
     */
    @GetMapping("/ownership/{groupId}")
    public ResponseEntity<Map<Integer, Double>> getGroupOwnership(@PathVariable Integer groupId,
                                                                  @RequestHeader(value = "Authorization", required = false) String token) {
        Map<Integer, Double> ownership = autoSplitService.getGroupOwnership(groupId, token);
        return ResponseEntity.ok(ownership);
    }

    /**
     * Preview kết quả chia (không lưu)
     * POST /api/auto-split/preview
     * 
     * Request có thể có:
     * - costId: Nếu có -> lấy amount từ cost đã tồn tại
     * - amount: Nếu có -> dùng amount trực tiếp
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewSplit(@RequestBody Map<String, Object> request,
                                                             @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Integer costId = request.get("costId") != null ? 
                (Integer) request.get("costId") : null;
            String splitMethod = (String) request.get("splitMethod");
            Integer groupId = (Integer) request.get("groupId");
            Integer month = request.get("month") != null ? 
                (Integer) request.get("month") : 
                java.time.LocalDate.now().getMonthValue();
            Integer year = request.get("year") != null ? 
                (Integer) request.get("year") : 
                java.time.LocalDate.now().getYear();

            // Lấy amount: từ costId hoặc từ request
            Double amount;
            if (costId != null) {
                // Lấy amount từ cost đã tồn tại
                Cost cost = costService.getCostById(costId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));
                amount = cost.getAmount();
            } else {
                // Lấy amount từ request
                Object amountObj = request.get("amount");
                if (amountObj == null) {
                    throw new IllegalArgumentException("Cần có costId hoặc amount để preview");
                }
                if (amountObj instanceof Number) {
                    amount = ((Number) amountObj).doubleValue();
                } else {
                    throw new IllegalArgumentException("amount phải là số");
                }
            }

            System.out.println("=== PREVIEW REQUEST ===");
            System.out.println("Cost ID: " + costId);
            System.out.println("Amount: " + amount);
            System.out.println("Split Method: " + splitMethod);
            System.out.println("Group ID: " + groupId);
            System.out.println("Month/Year: " + month + "/" + year);

            Map<String, Object> preview = new HashMap<>();
            preview.put("amount", amount);
            preview.put("totalAmount", amount); // Frontend expects totalAmount
            preview.put("splitMethod", splitMethod);

            // Tạo danh sách shares để preview
            List<Map<String, Object>> shares = new java.util.ArrayList<>();

            // Calculate preview based on method
            if ("BY_OWNERSHIP".equals(splitMethod)) {
                Map<Integer, Double> ownership = autoSplitService.getGroupOwnership(groupId, token);
                for (Map.Entry<Integer, Double> entry : ownership.entrySet()) {
                    Map<String, Object> share = new HashMap<>();
                    share.put("userId", entry.getKey());
                    share.put("percent", entry.getValue());
                    double shareAmount = amount * entry.getValue() / 100;
                    share.put("amountShare", shareAmount);
                    share.put("amount", shareAmount); // Frontend expects amount
                    shares.add(share);
                }
            } else if ("BY_USAGE".equals(splitMethod)) {
                // Chia theo km driven - Lấy dữ liệu thực từ database
                List<UsageTrackingDto> usageList = usageTrackingService.getGroupUsageInMonth(groupId, month, year);
                
                if (usageList == null || usageList.isEmpty()) {
                    String errorMsg = "Không có dữ liệu km cho nhóm " + groupId + 
                        " trong tháng " + month + "/" + year + ". Vui lòng nhập dữ liệu km trước.";
                    System.err.println("Error getting usage data: " + errorMsg);
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", errorMsg);
                    error.put("shares", new java.util.ArrayList<>());
                    return ResponseEntity.badRequest().body(error);
                }
                
                // Tính tổng km
                double totalKm = usageList.stream()
                    .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0)
                    .sum();
                
                if (totalKm <= 0) {
                    String errorMsg = "Tổng km phải lớn hơn 0. Vui lòng kiểm tra dữ liệu km.";
                    System.err.println("Error: " + errorMsg);
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", errorMsg);
                    error.put("shares", new java.util.ArrayList<>());
                    return ResponseEntity.badRequest().body(error);
                }
                
                // Tính phần chia cho từng user
                for (UsageTrackingDto usage : usageList) {
                    if (usage.getKmDriven() == null || usage.getKmDriven() <= 0) {
                        continue; // Bỏ qua user không có km
                    }
                    
                    Map<String, Object> share = new HashMap<>();
                    share.put("userId", usage.getUserId());
                    double percent = (usage.getKmDriven() / totalKm) * 100;
                    share.put("percent", Math.round(percent * 100.0) / 100.0);
                    double shareAmount = amount * percent / 100;
                    share.put("amountShare", shareAmount);
                    share.put("amount", shareAmount); // Frontend expects amount
                    share.put("kmDriven", usage.getKmDriven()); // Thêm km để hiển thị
                    shares.add(share);
                }
            } else if ("EQUAL".equals(splitMethod)) {
                // Chia đều
                Map<Integer, Double> ownership = autoSplitService.getGroupOwnership(groupId, token);
                int memberCount = ownership.size();
                double equalPercent = 100.0 / memberCount;
                double equalAmount = amount / memberCount;
                
                for (Integer userId : ownership.keySet()) {
                    Map<String, Object> share = new HashMap<>();
                    share.put("userId", userId);
                    share.put("percent", Math.round(equalPercent * 100.0) / 100.0);
                    share.put("amountShare", equalAmount);
                    share.put("amount", equalAmount); // Frontend expects amount
                    shares.add(share);
                }
            }

            preview.put("shares", shares);
            System.out.println("Preview shares: " + shares.size());

            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi xem trước: " + e.getMessage());
            error.put("shares", new java.util.ArrayList<>());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

