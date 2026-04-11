package com.example.costpayment.service.impl;

import com.example.costpayment.entity.Cost;
import com.example.costpayment.entity.CostShare;
import com.example.costpayment.entity.Payment;
import com.example.costpayment.entity.UsageTracking;
import com.example.costpayment.repository.CostRepository;
import com.example.costpayment.repository.CostShareRepository;
import com.example.costpayment.repository.PaymentRepository;
import com.example.costpayment.repository.UsageTrackingRepository;
import com.example.costpayment.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private CostRepository costRepository;

    @Autowired
    private CostShareRepository costShareRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${group-management.service.url:${API_GATEWAY_URL:http://localhost:8084}}")
    private String groupManagementServiceUrl;

    @Value("${API_GATEWAY_URL:http://localhost:8084}")
    private String apiGatewayUrl;

    @Override
    public Map<String, Object> getMonthlyCostReport(Integer vehicleId, Integer month, Integer year) {
        logger.info("Generating monthly cost report for vehicleId={}, month={}, year={}", vehicleId, month, year);
        
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.format("%02d/%d", month, year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
        report.put("totalAmount", costs.stream().mapToDouble(Cost::getAmount).sum());
        
        // Thống kê theo loại
        Map<String, Double> byType = costs.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCostType().getDisplayName(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        report.put("costsByType", byType);
        
        // Chi tiết từng chi phí
        List<Map<String, Object>> costDetails = costs.stream()
            .map(cost -> {
                Map<String, Object> detail = new HashMap<>();
                detail.put("costId", cost.getCostId());
                detail.put("costType", cost.getCostType().getDisplayName());
                detail.put("amount", cost.getAmount());
                detail.put("description", cost.getDescription());
                detail.put("status", cost.getStatus().getDisplayName());
                detail.put("createdAt", cost.getCreatedAt());
                return detail;
            })
            .collect(Collectors.toList());
        report.put("costDetails", costDetails);
        
        return report;
    }

    @Override
    public Map<String, Object> getQuarterlyCostReport(Integer vehicleId, Integer quarter, Integer year) {
        logger.info("Generating quarterly cost report for vehicleId={}, quarter={}, year={}", vehicleId, quarter, year);
        
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDateTime startDate = LocalDateTime.of(year, startMonth, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(3).minusSeconds(1);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.format("Q%d/%d", quarter, year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
        report.put("totalAmount", costs.stream().mapToDouble(Cost::getAmount).sum());
        
        // Thống kê theo tháng trong quý
        Map<String, Double> byMonth = new LinkedHashMap<>();
        for (int m = startMonth; m < startMonth + 3; m++) {
            LocalDateTime monthStart = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            double monthTotal = costs.stream()
                .filter(c -> !c.getCreatedAt().isBefore(monthStart) && !c.getCreatedAt().isAfter(monthEnd))
                .mapToDouble(Cost::getAmount)
                .sum();
            byMonth.put(String.format("%02d/%d", m, year), monthTotal);
        }
        report.put("costsByMonth", byMonth);
        
        // Thống kê theo loại
        Map<String, Double> byType = costs.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCostType().getDisplayName(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        report.put("costsByType", byType);
        
        return report;
    }

    @Override
    public Map<String, Object> getYearlyCostReport(Integer vehicleId, Integer year) {
        logger.info("Generating yearly cost report for vehicleId={}, year={}", vehicleId, year);
        
        LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.valueOf(year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
        report.put("totalAmount", costs.stream().mapToDouble(Cost::getAmount).sum());
        
        // Thống kê theo tháng
        Map<String, Double> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            LocalDateTime monthStart = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            double monthTotal = costs.stream()
                .filter(c -> !c.getCreatedAt().isBefore(monthStart) && !c.getCreatedAt().isAfter(monthEnd))
                .mapToDouble(Cost::getAmount)
                .sum();
            byMonth.put(String.format("%02d/%d", m, year), monthTotal);
        }
        report.put("costsByMonth", byMonth);
        
        // Thống kê theo loại
        Map<String, Double> byType = costs.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCostType().getDisplayName(),
                Collectors.summingDouble(Cost::getAmount)
            ));
        report.put("costsByType", byType);
        
        // Thống kê theo quý
        Map<String, Double> byQuarter = new LinkedHashMap<>();
        for (int q = 1; q <= 4; q++) {
            int startMonth = (q - 1) * 3 + 1;
            LocalDateTime quarterStart = LocalDateTime.of(year, startMonth, 1, 0, 0);
            LocalDateTime quarterEnd = quarterStart.plusMonths(3).minusSeconds(1);
            double quarterTotal = costs.stream()
                .filter(c -> !c.getCreatedAt().isBefore(quarterStart) && !c.getCreatedAt().isAfter(quarterEnd))
                .mapToDouble(Cost::getAmount)
                .sum();
            byQuarter.put(String.format("Q%d/%d", q, year), quarterTotal);
        }
        report.put("costsByQuarter", byQuarter);
        
        return report;
    }

    @Override
    public Map<String, Object> compareUsageWithOwnership(Integer groupId, Integer month, Integer year) {
        logger.info("Comparing usage with ownership for groupId={}, month={}, year={}", groupId, month, year);
        
        try {
            // Lấy thông tin thành viên từ Group Management Service
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members";
            List<Map<String, Object>> members = exchangeForObject(
                url,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (members == null || members.isEmpty()) {
                throw new RuntimeException("Không tìm thấy thành viên trong nhóm");
            }
            
            // Lấy dữ liệu sử dụng
            List<UsageTracking> usageList = usageTrackingRepository.findAll().stream()
                .filter(u -> u.getGroupId().equals(groupId) 
                    && u.getMonth().equals(month) 
                    && u.getYear().equals(year))
                .collect(Collectors.toList());
            
            // Tính tổng km
            double totalKm = usageList.stream()
                .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0.0)
                .sum();
            
            // Tạo báo cáo so sánh
            List<Map<String, Object>> comparison = new ArrayList<>();
            
            for (Map<String, Object> member : members) {
                Integer userId = ((Number) member.get("userId")).intValue();
                Double ownershipPercent = member.get("ownershipPercent") != null 
                    ? ((Number) member.get("ownershipPercent")).doubleValue() : 0.0;
                
                // Tìm usage của user
                Optional<UsageTracking> usageOpt = usageList.stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst();
                
                double kmDriven = usageOpt.map(u -> u.getKmDriven() != null ? u.getKmDriven() : 0.0)
                    .orElse(0.0);
                double usagePercent = totalKm > 0 ? (kmDriven / totalKm) * 100 : 0.0;
                
                // Tính chênh lệch
                double difference = usagePercent - ownershipPercent;
                
                Map<String, Object> userComparison = new HashMap<>();
                userComparison.put("userId", userId);
                userComparison.put("ownershipPercent", ownershipPercent);
                userComparison.put("kmDriven", kmDriven);
                userComparison.put("usagePercent", usagePercent);
                userComparison.put("difference", difference);
                userComparison.put("status", difference > 5 ? "Sử dụng nhiều hơn" 
                    : difference < -5 ? "Sử dụng ít hơn" : "Cân bằng");
                
                comparison.add(userComparison);
            }
            
            Map<String, Object> report = new HashMap<>();
            report.put("groupId", groupId);
            report.put("period", String.format("%02d/%d", month, year));
            report.put("totalKm", totalKm);
            report.put("totalMembers", members.size());
            report.put("comparison", comparison);
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error comparing usage with ownership: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể so sánh mức sử dụng với tỷ lệ sở hữu: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPersonalAnalysis(Integer userId, Integer groupId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating personal analysis for userId={}, groupId={}, period={} to {}", 
            userId, groupId, startDate, endDate);
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Lấy chi phí đã chia cho user
        List<CostShare> costShares = costShareRepository.findByUserId(userId);
        if (groupId != null) {
            // Filter by group if provided (need to get vehicleId from group)
            // For now, we'll get all cost shares
        }
        
        // Lọc theo thời gian
        List<CostShare> filteredShares = costShares.stream()
            .filter(cs -> {
                // Get cost creation date
                Cost cost = costRepository.findById(cs.getCostId()).orElse(null);
                if (cost == null) return false;
                LocalDateTime costDate = cost.getCreatedAt();
                return !costDate.isBefore(startDate) && !costDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        // Tính tổng chi phí phải trả
        double totalCostShare = filteredShares.stream()
            .mapToDouble(cs -> cs.getAmountShare() != null ? cs.getAmountShare() : 0.0)
            .sum();
        
        // Lấy thanh toán
        List<Payment> payments = paymentRepository.findByUserId(userId);
        List<Payment> filteredPayments = payments.stream()
            .filter(p -> {
                LocalDateTime paymentDate = p.getPaymentDate() != null ? p.getPaymentDate() : LocalDateTime.now();
                return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        double totalPaid = filteredPayments.stream()
            .filter(p -> "PAID".equals(p.getStatus().toString()))
            .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
            .sum();
        
        double totalPending = filteredPayments.stream()
            .filter(p -> "PENDING".equals(p.getStatus().toString()))
            .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
            .sum();
        
        // Lấy lịch sử sử dụng
        List<UsageTracking> usageHistory = usageTrackingRepository.findAll().stream()
            .filter(u -> u.getUserId().equals(userId))
            .filter(u -> {
                LocalDateTime usageDate = u.getRecordedAt() != null ? u.getRecordedAt() : LocalDateTime.now();
                return !usageDate.isBefore(startDate) && !usageDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        double totalKm = usageHistory.stream()
            .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0.0)
            .sum();
        
        analysis.put("userId", userId);
        analysis.put("groupId", groupId);
        analysis.put("period", Map.of("start", startDate, "end", endDate));
        analysis.put("totalCostShare", totalCostShare);
        analysis.put("totalPaid", totalPaid);
        analysis.put("totalPending", totalPending);
        analysis.put("totalKm", totalKm);
        analysis.put("usageRecords", usageHistory.size());
        analysis.put("costShareRecords", filteredShares.size());
        analysis.put("paymentRecords", filteredPayments.size());
        
        // Chi tiết theo loại chi phí
        Map<String, Double> costByType = new HashMap<>();
        for (CostShare share : filteredShares) {
            Cost cost = costRepository.findById(share.getCostId()).orElse(null);
            if (cost != null) {
                String type = cost.getCostType().getDisplayName();
                costByType.put(type, costByType.getOrDefault(type, 0.0) + share.getAmountShare());
            }
        }
        analysis.put("costByType", costByType);
        
        return analysis;
    }

    @Override
    public Map<String, Object> getFinancialReport(Integer groupId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating financial report for groupId={}, period={} to {}", groupId, startDate, endDate);
        
        try {
            // Lấy thông tin nhóm và thành viên
            String groupUrl = groupManagementServiceUrl + "/api/groups/" + groupId;
            Map<String, Object> group = exchangeForObject(
                groupUrl,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            String membersUrl = groupManagementServiceUrl + "/api/groups/" + groupId + "/members";
            List<Map<String, Object>> members = exchangeForObject(
                membersUrl,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            // Lấy quỹ chung thông qua API Gateway
            String fundUrl = apiGatewayUrl + "/api/funds/group/" + groupId;
            Map<String, Object> fund = exchangeForObject(
                fundUrl,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Lấy giao dịch quỹ (nếu xác định được fundId)
            List<Map<String, Object>> transactions = null;
            if (fund != null && fund.containsKey("fundId")) {
                Integer fundId = ((Number) fund.get("fundId")).intValue();
                String transactionsUrl = apiGatewayUrl + "/api/funds/" + fundId + "/transactions";
                transactions = exchangeForObject(
                    transactionsUrl,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
            }
            
            // Lọc giao dịch theo thời gian
            List<Map<String, Object>> filteredTransactions = new ArrayList<>();
            if (transactions != null) {
                for (Map<String, Object> transaction : transactions) {
                    // Parse transaction date (assuming it's in the transaction map)
                    // This is a simplified version - you may need to adjust based on actual structure
                    filteredTransactions.add(transaction);
                }
            }
            
            // Tính tổng chi phí của nhóm (cần vehicleId từ group)
            Integer vehicleId = group != null && group.containsKey("vehicleId") 
                ? ((Number) group.get("vehicleId")).intValue() : null;
            
            double totalCosts = 0.0;
            if (vehicleId != null) {
                List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
                totalCosts = costs.stream().mapToDouble(Cost::getAmount).sum();
            }
            
            Map<String, Object> report = new HashMap<>();
            report.put("groupId", groupId);
            report.put("groupName", group != null ? group.get("groupName") : "N/A");
            report.put("period", Map.of("start", startDate, "end", endDate));
            report.put("totalMembers", members != null ? members.size() : 0);
            report.put("fundBalance", fund != null && fund.containsKey("currentBalance") 
                ? fund.get("currentBalance") : 0.0);
            report.put("totalCosts", totalCosts);
            report.put("totalTransactions", filteredTransactions.size());
            report.put("transactions", filteredTransactions);
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating financial report: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo báo cáo tài chính: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getCostStatisticsByType(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting cost statistics by type for vehicleId={}, period={} to {}", vehicleId, startDate, endDate);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("vehicleId", vehicleId);
        statistics.put("period", Map.of("start", startDate, "end", endDate));
        statistics.put("totalCosts", costs.size());
        statistics.put("totalAmount", costs.stream().mapToDouble(Cost::getAmount).sum());
        
        // Thống kê theo loại
        Map<String, Map<String, Object>> byType = new HashMap<>();
        for (Cost.CostType type : Cost.CostType.values()) {
            List<Cost> typeCosts = costs.stream()
                .filter(c -> c.getCostType() == type)
                .collect(Collectors.toList());
            
            if (!typeCosts.isEmpty()) {
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("count", typeCosts.size());
                typeStats.put("totalAmount", typeCosts.stream().mapToDouble(Cost::getAmount).sum());
                typeStats.put("averageAmount", typeCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
                typeStats.put("minAmount", typeCosts.stream().mapToDouble(Cost::getAmount).min().orElse(0.0));
                typeStats.put("maxAmount", typeCosts.stream().mapToDouble(Cost::getAmount).max().orElse(0.0));
                byType.put(type.getDisplayName(), typeStats);
            }
        }
        statistics.put("statisticsByType", byType);
        
        return statistics;
    }

    @Override
    public Map<String, Object> getUserCostPaymentSummary(Integer userId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting cost payment summary for userId={}, period={} to {}", userId, startDate, endDate);
        
        // Lấy cost shares
        List<CostShare> costShares = costShareRepository.findByUserId(userId);
        List<CostShare> filteredShares = costShares.stream()
            .filter(cs -> {
                Cost cost = costRepository.findById(cs.getCostId()).orElse(null);
                if (cost == null) return false;
                LocalDateTime costDate = cost.getCreatedAt();
                return !costDate.isBefore(startDate) && !costDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        double totalShareAmount = filteredShares.stream()
            .mapToDouble(cs -> cs.getAmountShare() != null ? cs.getAmountShare() : 0.0)
            .sum();
        
        // Lấy payments
        List<Payment> payments = paymentRepository.findByUserId(userId);
        List<Payment> filteredPayments = payments.stream()
            .filter(p -> {
                LocalDateTime paymentDate = p.getPaymentDate() != null ? p.getPaymentDate() : LocalDateTime.now();
                return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        double totalPaid = filteredPayments.stream()
            .filter(p -> "PAID".equals(p.getStatus().toString()))
            .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
            .sum();
        
        double totalPending = filteredPayments.stream()
            .filter(p -> "PENDING".equals(p.getStatus().toString()))
            .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
            .sum();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("period", Map.of("start", startDate, "end", endDate));
        summary.put("totalShareAmount", totalShareAmount);
        summary.put("totalPaid", totalPaid);
        summary.put("totalPending", totalPending);
        summary.put("balance", totalShareAmount - totalPaid);
        summary.put("costShareCount", filteredShares.size());
        summary.put("paymentCount", filteredPayments.size());
        
        return summary;
    }

    private <T> T exchangeForObject(String url, ParameterizedTypeReference<T> typeReference) {
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, null, typeReference);
        return response.getBody();
    }
}

