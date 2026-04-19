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
<<<<<<< HEAD
=======
import java.math.BigDecimal;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
>>>>>>> origin/main

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
<<<<<<< HEAD
        
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
=======

        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);

>>>>>>> origin/main
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.format("%02d/%d", month, year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
<<<<<<< HEAD
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
        
=======
        report.put("totalAmount", costs.stream().map(Cost::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Thống kê theo loại
        Map<String, BigDecimal> byType = costs.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCostType().getDisplayName(),
                        Collectors.mapping(Cost::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
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

>>>>>>> origin/main
        return report;
    }

    @Override
    public Map<String, Object> getQuarterlyCostReport(Integer vehicleId, Integer quarter, Integer year) {
        logger.info("Generating quarterly cost report for vehicleId={}, quarter={}, year={}", vehicleId, quarter, year);
<<<<<<< HEAD
        
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDateTime startDate = LocalDateTime.of(year, startMonth, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(3).minusSeconds(1);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
=======

        int startMonth = (quarter - 1) * 3 + 1;
        LocalDateTime startDate = LocalDateTime.of(year, startMonth, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(3).minusSeconds(1);

        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);

>>>>>>> origin/main
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.format("Q%d/%d", quarter, year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
<<<<<<< HEAD
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
        
=======
        report.put("totalAmount", costs.stream().map(Cost::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Thống kê theo tháng trong quý
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        for (int m = startMonth; m < startMonth + 3; m++) {
            LocalDateTime monthStart = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            BigDecimal monthTotal = costs.stream()
                    .filter(c -> !c.getCreatedAt().isBefore(monthStart) && !c.getCreatedAt().isAfter(monthEnd))
                    .map(Cost::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            byMonth.put(String.format("%02d/%d", m, year), monthTotal);
        }
        report.put("costsByMonth", byMonth);

        // Thống kê theo loại
        Map<String, BigDecimal> byType = costs.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCostType().getDisplayName(),
                        Collectors.mapping(Cost::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        report.put("costsByType", byType);

>>>>>>> origin/main
        return report;
    }

    @Override
    public Map<String, Object> getYearlyCostReport(Integer vehicleId, Integer year) {
        logger.info("Generating yearly cost report for vehicleId={}, year={}", vehicleId, year);
<<<<<<< HEAD
        
        LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
=======

        LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);

>>>>>>> origin/main
        Map<String, Object> report = new HashMap<>();
        report.put("period", String.valueOf(year));
        report.put("vehicleId", vehicleId);
        report.put("totalCosts", costs.size());
<<<<<<< HEAD
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
=======
        report.put("totalAmount", costs.stream().map(Cost::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        // Thống kê theo tháng
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            LocalDateTime monthStart = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            BigDecimal monthTotal = costs.stream()
                    .filter(c -> !c.getCreatedAt().isBefore(monthStart) && !c.getCreatedAt().isAfter(monthEnd))
                    .map(Cost::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            byMonth.put(String.format("%02d/%d", m, year), monthTotal);
        }
        report.put("costsByMonth", byMonth);

        // Thống kê theo loại
        Map<String, BigDecimal> byType = costs.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCostType().getDisplayName(),
                        Collectors.mapping(Cost::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        report.put("costsByType", byType);

        // Thống kê theo quý
        Map<String, BigDecimal> byQuarter = new LinkedHashMap<>();
>>>>>>> origin/main
        for (int q = 1; q <= 4; q++) {
            int startMonth = (q - 1) * 3 + 1;
            LocalDateTime quarterStart = LocalDateTime.of(year, startMonth, 1, 0, 0);
            LocalDateTime quarterEnd = quarterStart.plusMonths(3).minusSeconds(1);
<<<<<<< HEAD
            double quarterTotal = costs.stream()
                .filter(c -> !c.getCreatedAt().isBefore(quarterStart) && !c.getCreatedAt().isAfter(quarterEnd))
                .mapToDouble(Cost::getAmount)
                .sum();
            byQuarter.put(String.format("Q%d/%d", q, year), quarterTotal);
        }
        report.put("costsByQuarter", byQuarter);
        
=======
            BigDecimal quarterTotal = costs.stream()
                    .filter(c -> !c.getCreatedAt().isBefore(quarterStart) && !c.getCreatedAt().isAfter(quarterEnd))
                    .map(Cost::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            byQuarter.put(String.format("Q%d/%d", q, year), quarterTotal);
        }
        report.put("costsByQuarter", byQuarter);

>>>>>>> origin/main
        return report;
    }

    @Override
    public Map<String, Object> compareUsageWithOwnership(Integer groupId, Integer month, Integer year) {
        logger.info("Comparing usage with ownership for groupId={}, month={}, year={}", groupId, month, year);
<<<<<<< HEAD
        
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
                
=======

        try {
            // Lấy thông tin thành viên từ Group Management Service
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members";
            List<Map<String, Object>> members = new ArrayList<>();
            try {
                members = exchangeForObject(
                        url,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });
            } catch (Exception e) {
                logger.warn("Could not fetch members for groupId {}: {}", groupId, e.getMessage());
            }

            if (members == null)
                members = new ArrayList<>();

            // Lấy dữ liệu sử dụng - Sử dụng Objects.equals để tránh NullPointerException
            List<UsageTracking> usageList = usageTrackingRepository.findAll().stream()
                    .filter(u -> Objects.equals(u.getGroupId(), groupId)
                            && Objects.equals(u.getMonth(), month)
                            && Objects.equals(u.getYear(), year))
                    .collect(Collectors.toList());

            // Tính tổng km - Xử lý null
            double totalKm = usageList.stream()
                    .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0.0)
                    .sum();

            // Tạo báo cáo so sánh
            List<Map<String, Object>> comparison = new ArrayList<>();

            for (Map<String, Object> member : members) {
                Integer userId = ((Number) member.get("userId")).intValue();
                Double ownershipPercent = member.get("ownershipPercent") != null
                        ? ((Number) member.get("ownershipPercent")).doubleValue()
                        : 0.0;

                // Tìm usage của user - Sử dụng Objects.equals để an toàn
                Optional<UsageTracking> usageOpt = usageList.stream()
                    .filter(u -> Objects.equals(u.getUserId(), userId))
                    .findFirst();

                double kmDriven = usageOpt.map(u -> u.getKmDriven() != null ? u.getKmDriven() : 0.0)
                        .orElse(0.0);
                double usagePercent = totalKm > 0 ? (kmDriven / totalKm) * 100 : 0.0;

                // Tính chênh lệch
                double difference = usagePercent - ownershipPercent;

>>>>>>> origin/main
                Map<String, Object> userComparison = new HashMap<>();
                userComparison.put("userId", userId);
                userComparison.put("ownershipPercent", ownershipPercent);
                userComparison.put("kmDriven", kmDriven);
                userComparison.put("usagePercent", usagePercent);
                userComparison.put("difference", difference);
<<<<<<< HEAD
                userComparison.put("status", difference > 5 ? "Sử dụng nhiều hơn" 
                    : difference < -5 ? "Sử dụng ít hơn" : "Cân bằng");
                
                comparison.add(userComparison);
            }
            
=======
                userComparison.put("status", difference > 5 ? "Sử dụng nhiều hơn"
                        : difference < -5 ? "Sử dụng ít hơn" : "Cân bằng");

                comparison.add(userComparison);
            }

>>>>>>> origin/main
            Map<String, Object> report = new HashMap<>();
            report.put("groupId", groupId);
            report.put("period", String.format("%02d/%d", month, year));
            report.put("totalKm", totalKm);
<<<<<<< HEAD
            report.put("totalMembers", members.size());
            report.put("comparison", comparison);
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error comparing usage with ownership: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể so sánh mức sử dụng với tỷ lệ sở hữu: " + e.getMessage());
=======
            report.put("totalMembers", members != null ? members.size() : 0);
            report.put("comparison", comparison);

            return report;

        } catch (Exception e) {
            logger.error("Error comparing usage with ownership: {}", e.getMessage(), e);
            // Thay vì throw RuntimeException (gây lỗi 500), ta trả về kết quả rỗng cơ bản
            Map<String, Object> emptyReport = new HashMap<>();
            emptyReport.put("groupId", groupId);
            emptyReport.put("period", String.format("%02d/%d", month, year));
            emptyReport.put("totalKm", 0.0);
            emptyReport.put("totalMembers", 0);
            emptyReport.put("comparison", new ArrayList<>());
            emptyReport.put("notice", "Dữ liệu chưa sẵn sàng hoặc Service ngoài gặp lỗi");
            return emptyReport;
>>>>>>> origin/main
        }
    }

    @Override
<<<<<<< HEAD
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
=======
    public Map<String, Object> getPersonalAnalysis(Integer userId, Integer groupId, LocalDateTime startDate,
            LocalDateTime endDate) {
        try {
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
                        if (cost == null)
                            return false;
                        LocalDateTime costDate = cost.getCreatedAt();
                        return !costDate.isBefore(startDate) && !costDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());

            // Tính tổng chi phí phải trả
            BigDecimal totalCostShare = filteredShares.stream()
                    .map(cs -> cs.getAmountShare() != null ? cs.getAmountShare() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Lấy thanh toán
            List<Payment> payments = paymentRepository.findByUserId(userId);
            List<Payment> filteredPayments = payments.stream()
                    .filter(p -> {
                        LocalDateTime paymentDate = p.getPaymentDate() != null ? p.getPaymentDate()
                                : LocalDateTime.now();
                        return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());

            BigDecimal totalPaid = filteredPayments.stream()
                    .filter(p -> "PAID".equals(p.getStatus().toString()))
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPending = filteredPayments.stream()
                    .filter(p -> "PENDING".equals(p.getStatus().toString()))
                    .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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

            Map<String, String> periodMap = new HashMap<>();
            periodMap.put("start", startDate.toString());
            periodMap.put("end", endDate.toString());
            analysis.put("period", periodMap);

            analysis.put("totalCostShare", totalCostShare);
            analysis.put("totalPaid", totalPaid);
            analysis.put("totalPending", totalPending);
            analysis.put("totalKm", totalKm);
            analysis.put("usageRecords", usageHistory.size());
            analysis.put("costShareRecords", filteredShares.size());
            analysis.put("paymentRecords", filteredPayments.size());

            // Chi tiết theo loại chi phí
            Map<String, BigDecimal> costByType = new HashMap<>();
            for (CostShare share : filteredShares) {
                Cost cost = costRepository.findById(share.getCostId()).orElse(null);
                if (cost != null) {
                    String type = cost.getCostType().getDisplayName();
                    BigDecimal current = costByType.getOrDefault(type, BigDecimal.ZERO);
                    BigDecimal addAmount = share.getAmountShare() != null ? share.getAmountShare() : BigDecimal.ZERO;
                    costByType.put(type, current.add(addAmount));
                }
            }
            analysis.put("costByType", costByType);

            return analysis;
        } catch (Exception e) {
            logger.error("Error generating personal analysis: {}", e.getMessage(), e);
            Map<String, Object> emptyAnalysis = new HashMap<>();
            emptyAnalysis.put("userId", userId);
            emptyAnalysis.put("totalCostShare", BigDecimal.ZERO);
            emptyAnalysis.put("totalPaid", BigDecimal.ZERO);
            emptyAnalysis.put("totalKm", 0.0);
            emptyAnalysis.put("notice", "Lỗi phân tích dữ liệu");
            return emptyAnalysis;
        }
>>>>>>> origin/main
    }

    @Override
    public Map<String, Object> getFinancialReport(Integer groupId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating financial report for groupId={}, period={} to {}", groupId, startDate, endDate);
<<<<<<< HEAD
        
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
=======

        try {
            // Lấy thông tin nhóm và thành viên
            Map<String, Object> group = null;
            List<Map<String, Object>> members = new ArrayList<>();
            Map<String, Object> fund = null;

            try {
                String groupUrl = groupManagementServiceUrl + "/api/groups/" + groupId;
                group = exchangeForObject(groupUrl, new ParameterizedTypeReference<Map<String, Object>>() {});

                String membersUrl = groupManagementServiceUrl + "/api/groups/" + groupId + "/members";
                members = exchangeForObject(membersUrl, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

                String fundUrl = apiGatewayUrl + "/api/funds/group/" + groupId;
                fund = exchangeForObject(fundUrl, new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("External service call failed for groupId {}: {}", groupId, e.getMessage());
            }

            // Lấy giao dịch quỹ
            List<Map<String, Object>> transactions = new ArrayList<>();
            Object fundIdObj = (fund != null) ? fund.get("fundId") : null;
            if (fundIdObj != null && fundIdObj instanceof Number) {
                Integer fundId = ((Number) fundIdObj).intValue();
                try {
                    String transactionsUrl = apiGatewayUrl + "/api/funds/" + fundId + "/transactions";
                    List<Map<String, Object>> fetched = exchangeForObject(
                            transactionsUrl,
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                    if (fetched != null) transactions.addAll(fetched);
                } catch (Exception e) {
                    logger.warn("Could not fetch transactions for fundId {}: {}", fundId, e.getMessage());
                }
            }

            // Lọc giao dịch
            List<Map<String, Object>> filteredTransactions = new ArrayList<>();
            for (Map<String, Object> transaction : transactions) {
                if (transaction != null) filteredTransactions.add(transaction);
            }

            // Tính tổng chi phí
            Object vehicleIdObj = (group != null) ? group.get("vehicleId") : null;
            Integer vehicleId = (vehicleIdObj != null && vehicleIdObj instanceof Number)
                    ? ((Number) vehicleIdObj).intValue()
                    : null;

            BigDecimal totalCosts = BigDecimal.ZERO;
            if (vehicleId != null) {
                try {
                    List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
                    if (costs != null) {
                        totalCosts = costs.stream()
                            .map(c -> (c != null && c.getAmount() != null) ? c.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                } catch (Exception e) {
                    logger.warn("Error calculating costs for vehicleId {}: {}", vehicleId, e.getMessage());
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("groupId", groupId);
            report.put("groupName", (group != null && group.get("groupName") != null) ? group.get("groupName") : "N/A");
            report.put("period", Map.of("start", startDate, "end", endDate));
            report.put("totalMembers", members != null ? members.size() : 0);

            BigDecimal fundBalance = BigDecimal.ZERO;
            Object balanceObj = (fund != null) ? fund.get("currentBalance") : null;
            if (balanceObj != null) {
                try {
                    fundBalance = new BigDecimal(String.valueOf(balanceObj));
                } catch (Exception e) {
                    logger.warn("Could not parse fund balance: {}", balanceObj);
                }
            }
            report.put("fundBalance", fundBalance);
            report.put("totalCosts", totalCosts != null ? totalCosts : BigDecimal.ZERO);
            report.put("totalTransactions", filteredTransactions.size());
            report.put("transactions", filteredTransactions);

            return report;

        } catch (Exception e) {
            logger.error("Error generating financial report for groupId {}: {}", groupId, e.getMessage(), e);
            Map<String, Object> emptyReport = new HashMap<>();
            emptyReport.put("groupId", groupId);
            emptyReport.put("groupName", "N/A");
            emptyReport.put("period", Map.of("start", startDate, "end", endDate));
            emptyReport.put("totalMembers", 0);
            emptyReport.put("fundBalance", BigDecimal.ZERO);
            emptyReport.put("totalCosts", BigDecimal.ZERO);
            emptyReport.put("totalTransactions", 0);
            emptyReport.put("transactions", new ArrayList<>());
            return emptyReport;
>>>>>>> origin/main
        }
    }

    @Override
<<<<<<< HEAD
    public Map<String, Object> getCostStatisticsByType(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting cost statistics by type for vehicleId={}, period={} to {}", vehicleId, startDate, endDate);
        
        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);
        
=======
    public Map<String, Object> getCostStatisticsByType(Integer vehicleId, LocalDateTime startDate,
            LocalDateTime endDate) {
        logger.info("Getting cost statistics by type for vehicleId={}, period={} to {}", vehicleId, startDate, endDate);

        List<Cost> costs = costRepository.findByVehicleIdAndCreatedAtBetween(vehicleId, startDate, endDate);

>>>>>>> origin/main
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("vehicleId", vehicleId);
        statistics.put("period", Map.of("start", startDate, "end", endDate));
        statistics.put("totalCosts", costs.size());
<<<<<<< HEAD
        statistics.put("totalAmount", costs.stream().mapToDouble(Cost::getAmount).sum());
        
=======
        statistics.put("totalAmount", costs.stream().map(Cost::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

>>>>>>> origin/main
        // Thống kê theo loại
        Map<String, Map<String, Object>> byType = new HashMap<>();
        for (Cost.CostType type : Cost.CostType.values()) {
            List<Cost> typeCosts = costs.stream()
<<<<<<< HEAD
                .filter(c -> c.getCostType() == type)
                .collect(Collectors.toList());
            
            if (!typeCosts.isEmpty()) {
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("count", typeCosts.size());
                typeStats.put("totalAmount", typeCosts.stream().mapToDouble(Cost::getAmount).sum());
                typeStats.put("averageAmount", typeCosts.stream().mapToDouble(Cost::getAmount).average().orElse(0.0));
                typeStats.put("minAmount", typeCosts.stream().mapToDouble(Cost::getAmount).min().orElse(0.0));
                typeStats.put("maxAmount", typeCosts.stream().mapToDouble(Cost::getAmount).max().orElse(0.0));
=======
                    .filter(c -> c.getCostType() == type)
                    .collect(Collectors.toList());

            if (!typeCosts.isEmpty()) {
                Map<String, Object> typeStats = new HashMap<>();
                typeStats.put("count", typeCosts.size());

                BigDecimal totalAmount = typeCosts.stream().map(Cost::getAmount).reduce(BigDecimal.ZERO,
                        BigDecimal::add);
                typeStats.put("totalAmount", totalAmount);

                BigDecimal averageAmount = typeCosts.isEmpty() ? BigDecimal.ZERO
                        : totalAmount.divide(new BigDecimal(typeCosts.size()), 2, java.math.RoundingMode.HALF_UP);
                typeStats.put("averageAmount", averageAmount);

                BigDecimal minAmount = typeCosts.stream().map(Cost::getAmount).min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                typeStats.put("minAmount", minAmount);

                BigDecimal maxAmount = typeCosts.stream().map(Cost::getAmount).max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                typeStats.put("maxAmount", maxAmount);

>>>>>>> origin/main
                byType.put(type.getDisplayName(), typeStats);
            }
        }
        statistics.put("statisticsByType", byType);
<<<<<<< HEAD
        
=======

>>>>>>> origin/main
        return statistics;
    }

    @Override
<<<<<<< HEAD
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
        
=======
    public Map<String, Object> getUserCostPaymentSummary(Integer userId, LocalDateTime startDate,
            LocalDateTime endDate) {
        logger.info("Getting cost payment summary for userId={}, period={} to {}", userId, startDate, endDate);

        // Lấy cost shares
        List<CostShare> costShares = costShareRepository.findByUserId(userId);
        List<CostShare> filteredShares = costShares.stream()
                .filter(cs -> {
                    Cost cost = costRepository.findById(cs.getCostId()).orElse(null);
                    if (cost == null)
                        return false;
                    LocalDateTime costDate = cost.getCreatedAt();
                    return !costDate.isBefore(startDate) && !costDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        BigDecimal totalShareAmount = filteredShares.stream()
                .map(cs -> cs.getAmountShare() != null ? cs.getAmountShare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lấy payments
        List<Payment> payments = paymentRepository.findByUserId(userId);
        List<Payment> filteredPayments = payments.stream()
                .filter(p -> {
                    LocalDateTime paymentDate = p.getPaymentDate() != null ? p.getPaymentDate() : LocalDateTime.now();
                    return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        BigDecimal totalPaid = filteredPayments.stream()
                .filter(p -> "PAID".equals(p.getStatus().toString()))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = filteredPayments.stream()
                .filter(p -> "PENDING".equals(p.getStatus().toString()))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

>>>>>>> origin/main
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("period", Map.of("start", startDate, "end", endDate));
        summary.put("totalShareAmount", totalShareAmount);
        summary.put("totalPaid", totalPaid);
        summary.put("totalPending", totalPending);
<<<<<<< HEAD
        summary.put("balance", totalShareAmount - totalPaid);
        summary.put("costShareCount", filteredShares.size());
        summary.put("paymentCount", filteredPayments.size());
        
=======
        summary.put("balance", totalShareAmount.subtract(totalPaid));
        summary.put("costShareCount", filteredShares.size());
        summary.put("paymentCount", filteredPayments.size());

>>>>>>> origin/main
        return summary;
    }

    private <T> T exchangeForObject(String url, ParameterizedTypeReference<T> typeReference) {
<<<<<<< HEAD
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, null, typeReference);
        return response.getBody();
    }
}

=======
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    headers.set("Authorization", authHeader);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not attach Authorization header: {}", e.getMessage());
        }

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("parameters",
                headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, typeReference);
        return response.getBody();
    }
}
>>>>>>> origin/main
