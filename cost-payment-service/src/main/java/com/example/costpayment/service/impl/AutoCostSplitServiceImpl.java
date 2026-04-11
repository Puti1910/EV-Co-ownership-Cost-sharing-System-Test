package com.example.costpayment.service.impl;

import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.entity.SplitMethod;
import com.example.costpayment.entity.UsageTracking;
import com.example.costpayment.repository.CostRepository;
import com.example.costpayment.repository.CostShareRepository;
import com.example.costpayment.repository.UsageTrackingRepository;
import com.example.costpayment.service.AutoCostSplitService;
import com.example.costpayment.service.CostShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service tự động chia chi phí theo các phương thức khác nhau
 */
@Service
public class AutoCostSplitServiceImpl implements AutoCostSplitService {

    @Autowired
    private CostRepository costRepository;

    @Autowired
    private CostShareRepository costShareRepository;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Autowired
    private CostShareService costShareService;

    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @org.springframework.beans.factory.annotation.Value("${group-management.service.url:http://api-gateway:8084}")
    private String groupManagementServiceUrl;

    /**
     * Tự động chia chi phí dựa trên split method
     */
    @Override
    @Transactional
    public List<CostShare> autoSplitCost(Integer costId, Integer groupId, Integer month, Integer year) {
        // Lấy thông tin chi phí
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        // Xác định split method (nếu có trong entity hoặc dựa vào costType)
        SplitMethod splitMethod = determineSplitMethod(cost);

        // Chia chi phí theo phương thức tương ứng
        switch (splitMethod) {
            case BY_OWNERSHIP:
                Map<Integer, Double> ownershipMap = getGroupOwnership(groupId);
                return splitByOwnership(costId, ownershipMap);

            case BY_USAGE:
                Map<Integer, Double> usageMap = getGroupUsageKm(groupId, month, year);
                return splitByUsage(costId, usageMap);

            case EQUAL:
                List<Integer> userIds = getUserIdsByGroup(groupId);
                return splitEqually(costId, userIds);

            case CUSTOM:
                throw new RuntimeException("CUSTOM split method requires manual input");

            default:
                throw new RuntimeException("Unknown split method: " + splitMethod);
        }
    }

    /**
     * Tự động chia chi phí với splitMethod được chỉ định (từ form)
     */
    @Override
    @Transactional
    public List<CostShare> autoSplitCostWithMethod(Integer costId, Integer groupId, String splitMethodStr, Integer month, Integer year) {
        return autoSplitCostWithMethod(costId, groupId, splitMethodStr, month, year, null);
    }
    
    /**
     * Tự động chia chi phí với splitMethod được chỉ định (từ form) - có token
     */
    @Override
    @Transactional
    public List<CostShare> autoSplitCostWithMethod(Integer costId, Integer groupId, String splitMethodStr, Integer month, Integer year, String token) {
        System.out.println("=== AUTO SPLIT WITH METHOD ===");
        System.out.println("Cost ID: " + costId);
        System.out.println("Group ID: " + groupId);
        System.out.println("Split Method: " + splitMethodStr);
        System.out.println("Month/Year: " + month + "/" + year);

        // Validate cost exists
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        // Parse split method từ String
        SplitMethod splitMethod;
        try {
            splitMethod = SplitMethod.valueOf(splitMethodStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Split method không hợp lệ: " + splitMethodStr);
        }

        // Chia chi phí theo phương thức tương ứng
        switch (splitMethod) {
            case BY_OWNERSHIP:
                System.out.println("Splitting by OWNERSHIP...");
                Map<Integer, Double> ownershipMap = getGroupOwnership(groupId, token);
                System.out.println("Ownership map: " + ownershipMap);
                return splitByOwnership(costId, ownershipMap);

            case BY_USAGE:
                System.out.println("Splitting by USAGE...");
                Map<Integer, Double> usageMap = getGroupUsageKm(groupId, month, year);
                System.out.println("Usage map: " + usageMap);
                return splitByUsage(costId, usageMap);

            case EQUAL:
                System.out.println("Splitting EQUALLY...");
                List<Integer> userIds = getUserIdsByGroup(groupId, token);
                System.out.println("User IDs: " + userIds);
                return splitEqually(costId, userIds);

            case CUSTOM:
                throw new RuntimeException("CUSTOM split method requires manual input");

            default:
                throw new RuntimeException("Unknown split method: " + splitMethod);
        }
    }

    /**
     * Chia chi phí theo tỉ lệ sở hữu
     */
    @Override
    @Transactional
    public List<CostShare> splitByOwnership(Integer costId, Map<Integer, Double> ownershipMap) {
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        // Validate và normalize tổng ownership
        double totalOwnership = ownershipMap.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Nếu tổng ownership = 0 hoặc quá nhỏ, báo lỗi
        if (totalOwnership <= 0) {
            throw new RuntimeException("Tổng ownership phải lớn hơn 0%, hiện tại: " + totalOwnership + "%");
        }
        
        // Nếu tổng ownership không bằng 100%, tự động normalize (trong phạm vi hợp lý: 50-150%)
        if (Math.abs(totalOwnership - 100.0) > 0.01) {
            if (totalOwnership < 50.0 || totalOwnership > 150.0) {
                throw new RuntimeException("Tổng ownership không hợp lệ: " + totalOwnership + "%. Vui lòng kiểm tra dữ liệu nhóm.");
            }
            // Normalize: chia tất cả giá trị cho totalOwnership và nhân với 100
            System.out.println("Warning: Tổng ownership = " + totalOwnership + "%, đang normalize về 100%");
            Map<Integer, Double> normalizedMap = new HashMap<>();
            for (Map.Entry<Integer, Double> entry : ownershipMap.entrySet()) {
                normalizedMap.put(entry.getKey(), (entry.getValue() / totalOwnership) * 100.0);
            }
            ownershipMap = normalizedMap;
            totalOwnership = 100.0; // Sau khi normalize, tổng sẽ là 100%
        }

        // Xóa các chia sẻ cũ
        deletePreviousShares(costId);

        // Tạo danh sách userId và % từ map
        List<Integer> userIds = new ArrayList<>(ownershipMap.keySet());
        List<Double> percentages = new ArrayList<>(ownershipMap.values());

        // Sử dụng service có sẵn
        return costShareService.calculateCostShares(costId, userIds, percentages);
    }

    /**
     * Chia chi phí theo mức độ sử dụng (km)
     */
    @Override
    @Transactional
    public List<CostShare> splitByUsage(Integer costId, Map<Integer, Double> usageMap) {
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        // Tính tổng km
        double totalKm = usageMap.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalKm <= 0) {
            throw new RuntimeException("Tổng km phải lớn hơn 0");
        }

        // Tính % cho mỗi user
        Map<Integer, Double> percentageMap = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : usageMap.entrySet()) {
            double percent = (entry.getValue() / totalKm) * 100.0;
            percentageMap.put(entry.getKey(), percent);
        }

        // Xóa các chia sẻ cũ
        deletePreviousShares(costId);

        // Tạo danh sách userId và %
        List<Integer> userIds = new ArrayList<>(percentageMap.keySet());
        List<Double> percentages = new ArrayList<>(percentageMap.values());

        // Sử dụng service có sẵn
        return costShareService.calculateCostShares(costId, userIds, percentages);
    }

    /**
     * Chia đều chi phí
     */
    @Override
    @Transactional
    public List<CostShare> splitEqually(Integer costId, List<Integer> userIds) {
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi phí ID: " + costId));

        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("Danh sách user không được rỗng");
        }

        // Xóa các chia sẻ cũ
        deletePreviousShares(costId);

        // Tính % đều cho mỗi người
        double percentPerPerson = 100.0 / userIds.size();
        List<Double> percentages = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            percentages.add(percentPerPerson);
        }

        // Sử dụng service có sẵn
        return costShareService.calculateCostShares(costId, userIds, percentages);
    }

    /**
     * Lấy ownership % của nhóm từ Group Management Service
     */
    @Override
    public Map<Integer, Double> getGroupOwnership(Integer groupId) {
        return getGroupOwnership(groupId, null);
    }
    
    /**
     * Lấy ownership % của nhóm từ Group Management Service (có token)
     */
    @Override
    public Map<Integer, Double> getGroupOwnership(Integer groupId, String token) {
        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members/view";
            System.out.println("Calling Group Management Service: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("members")) {
                throw new RuntimeException("Không lấy được thông tin thành viên từ Group Management Service");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> members = (List<Map<String, Object>>) responseBody.get("members");
            
            Map<Integer, Double> ownershipMap = new HashMap<>();
            for (Map<String, Object> member : members) {
                Integer userId = (Integer) member.get("userId");
                Object ownershipPercentObj = member.get("ownershipPercent");
                Double ownershipPercent = ownershipPercentObj != null ? 
                    (ownershipPercentObj instanceof Number ? 
                        ((Number) ownershipPercentObj).doubleValue() : 
                        Double.parseDouble(ownershipPercentObj.toString())) : 
                    0.0;
                ownershipMap.put(userId, ownershipPercent);
            }
            
            System.out.println("Ownership map: " + ownershipMap);
            return ownershipMap;
            
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Group Management Service: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lấy thông tin ownership từ Group Management Service: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Xác định split method dựa vào cost hoặc cost type
     */
    private SplitMethod determineSplitMethod(Cost cost) {
        // Nếu cost có splitMethod field (cần thêm vào Cost entity)
        // return cost.getSplitMethod();

        // Tạm thời dựa vào costType
        switch (cost.getCostType()) {
            case ElectricCharge:
                return SplitMethod.BY_USAGE;
            case Maintenance:
            case Insurance:
            case Inspection:
                return SplitMethod.BY_OWNERSHIP;
            case Cleaning:
            case Other:
                return SplitMethod.EQUAL;
            default:
                return SplitMethod.BY_OWNERSHIP;
        }
    }

    /**
     * Lấy km của nhóm trong tháng
     */
    private Map<Integer, Double> getGroupUsageKm(Integer groupId, Integer month, Integer year) {
        List<UsageTracking> usageList = usageTrackingRepository
                .findByGroupIdAndMonthAndYear(groupId, month, year);

        Map<Integer, Double> usageMap = new HashMap<>();
        for (UsageTracking usage : usageList) {
            usageMap.put(usage.getUserId(), usage.getKmDriven());
        }

        if (usageMap.isEmpty()) {
            throw new RuntimeException("Không có dữ liệu km cho nhóm " + groupId +
                    " trong tháng " + month + "/" + year);
        }

        return usageMap;
    }

    /**
     * Lấy danh sách userId của nhóm từ Group Management Service
     */
    private List<Integer> getUserIdsByGroup(Integer groupId) {
        return getUserIdsByGroup(groupId, null);
    }
    
    /**
     * Lấy danh sách userId của nhóm từ Group Management Service (có token)
     */
    private List<Integer> getUserIdsByGroup(Integer groupId, String token) {
        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members/view";
            System.out.println("Calling Group Management Service: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("members")) {
                throw new RuntimeException("Không lấy được thông tin thành viên từ Group Management Service");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> members = (List<Map<String, Object>>) responseBody.get("members");
            
            List<Integer> userIds = new ArrayList<>();
            for (Map<String, Object> member : members) {
                Integer userId = (Integer) member.get("userId");
                if (userId != null) {
                    userIds.add(userId);
                }
            }
            
            System.out.println("User IDs: " + userIds);
            return userIds;
            
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Group Management Service: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lấy danh sách thành viên từ Group Management Service: " + e.getMessage());
        }
    }

    /**
     * Xóa các chia sẻ cũ của cost
     */
    private void deletePreviousShares(Integer costId) {
        List<CostShare> existingShares = costShareRepository.findByCostId(costId);
        if (!existingShares.isEmpty()) {
            costShareRepository.deleteAll(existingShares);
        }
    }
}

