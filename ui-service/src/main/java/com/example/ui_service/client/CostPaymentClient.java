package com.example.ui_service.client;

import com.example.ui_service.dto.CostDto;
import com.example.ui_service.dto.CostSplitDto;
import com.example.ui_service.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class CostPaymentClient {

    @Value("${microservices.cost-payment.url:http://localhost:8084}")
    private String costPaymentUrl;

    @Autowired
    private RestTemplate restTemplate;

    public List<CostDto> getAllCosts() {
        try {
            ResponseEntity<List<CostDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CostDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching costs: " + e.getMessage());
            return List.of();
        }
    }

    public CostDto createCost(CostDto costDto) {
        try {
            return restTemplate.postForObject(costPaymentUrl + "/api/costs", costDto, CostDto.class);
        } catch (Exception e) {
            System.err.println("Error creating cost: " + e.getMessage());
            return null;
        }
    }

    public List<CostSplitDto> getCostSplits(Integer costId) {
        try {
            // Use /splits endpoint which is the primary endpoint
            ResponseEntity<List<CostSplitDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/" + costId + "/splits",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CostSplitDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching cost splits: " + e.getMessage());
            return List.of();
        }
    }

    public CostSplitDto createCostSplit(Integer costId, CostSplitDto splitDto) {
        try {
            return restTemplate.postForObject(costPaymentUrl + "/api/costs/" + costId + "/shares", splitDto, CostSplitDto.class);
        } catch (Exception e) {
            System.err.println("Error creating cost split: " + e.getMessage());
            return null;
        }
    }

    public List<PaymentDto> getAllPayments() {
        try {
            ResponseEntity<List<PaymentDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/payments",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PaymentDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching payments: " + e.getMessage());
            return List.of();
        }
    }

    public PaymentDto createPayment(PaymentDto paymentDto) {
        try {
            return restTemplate.postForObject(costPaymentUrl + "/api/costs/payments", paymentDto, PaymentDto.class);
        } catch (Exception e) {
            System.err.println("Error creating payment: " + e.getMessage());
            return null;
        }
    }

    // Additional CRUD operations for Cost management
    public CostDto getCostById(Integer id) {
        try {
            return restTemplate.getForObject(costPaymentUrl + "/api/costs/" + id, CostDto.class);
        } catch (Exception e) {
            System.err.println("Error fetching cost by ID: " + e.getMessage());
            return null;
        }
    }

    public CostDto updateCost(Integer id, CostDto costDto) {
        try {
            restTemplate.put(costPaymentUrl + "/api/costs/" + id, costDto);
            return getCostById(id);
        } catch (Exception e) {
            System.err.println("Error updating cost: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteCost(Integer id) {
        try {
            restTemplate.delete(costPaymentUrl + "/api/costs/" + id);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting cost: " + e.getMessage());
            return false;
        }
    }

    public List<CostDto> searchCosts(String query, String costType, Integer vehicleId) {
        try {
            StringBuilder url = new StringBuilder(costPaymentUrl + "/api/costs/search?");
            if (query != null && !query.isEmpty()) {
                url.append("query=").append(query).append("&");
            }
            if (costType != null && !costType.isEmpty()) {
                url.append("costType=").append(costType).append("&");
            }
            if (vehicleId != null) {
                url.append("vehicleId=").append(vehicleId).append("&");
            }
            
            ResponseEntity<List<CostDto>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CostDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error searching costs: " + e.getMessage());
            return List.of();
        }
    }

    // Cost Sharing APIs
    public List<CostSplitDto> getAllCostShares() {
        try {
            ResponseEntity<List<CostSplitDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/shares",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CostSplitDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching cost shares: " + e.getMessage());
            return List.of();
        }
    }

    public List<CostSplitDto> getCostSharesByCostId(Integer costId) {
        try {
            // Use /splits endpoint which is the primary endpoint
            ResponseEntity<List<CostSplitDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/" + costId + "/splits",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CostSplitDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching cost shares by cost ID: " + e.getMessage());
            return List.of();
        }
    }

    public List<CostSplitDto> calculateCostShares(Integer costId, List<Integer> userIds, List<Double> percentages) {
        try {
            // Create request object
            CostShareRequest request = new CostShareRequest();
            request.setUserIds(userIds);
            request.setPercentages(percentages);
            
            ResponseEntity<List<CostSplitDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/" + costId + "/calculate-shares",
                HttpMethod.POST,
                org.springframework.http.HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<CostSplitDto>>() {},
                request
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error calculating cost shares: " + e.getMessage());
            return List.of();
        }
    }

    public CostSplitDto getCostShareById(Integer id) {
        try {
            return restTemplate.getForObject(costPaymentUrl + "/api/costs/shares/" + id, CostSplitDto.class);
        } catch (Exception e) {
            System.err.println("Error fetching cost share by ID: " + e.getMessage());
            return null;
        }
    }

    public CostSplitDto updateCostShare(Integer id, CostSplitDto costShareDto) {
        try {
            restTemplate.put(costPaymentUrl + "/api/costs/shares/" + id, costShareDto);
            return getCostShareById(id);
        } catch (Exception e) {
            System.err.println("Error updating cost share: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteCostShare(Integer id) {
        try {
            restTemplate.delete(costPaymentUrl + "/api/costs/shares/" + id);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting cost share: " + e.getMessage());
            return false;
        }
    }

    // Auto Split APIs
    public Map<String, Object> createAndAutoSplit(Map<String, Object> request) {
        try {
            System.out.println("=== FEIGN CLIENT: CREATE AND SPLIT ===");
            System.out.println("Calling: " + costPaymentUrl + "/api/auto-split/create-and-split");
            System.out.println("Request: " + request);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/auto-split/create-and-split",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
            
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error creating and auto-splitting cost: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> autoSplitCost(Integer costId, Integer groupId, Integer month, Integer year) {
        try {
            String url = String.format("%s/api/auto-split/cost/%d?groupId=%d&month=%d&year=%d",
                costPaymentUrl, costId, groupId, month, year);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error auto-splitting cost: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> previewAutoSplit(Map<String, Object> request) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/auto-split/preview",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error getting preview: " + e.getMessage());
            return null;
        }
    }

    // Usage Tracking APIs
    public List<Map<String, Object>> getGroupUsage(Integer groupId, Integer month, Integer year) {
        try {
            String url = String.format("%s/api/usage-tracking/group/%d?month=%d&year=%d",
                costPaymentUrl, groupId, month, year);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching group usage: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getUserUsage(Integer groupId, Integer userId, Integer month, Integer year) {
        try {
            String url = String.format("%s/api/usage-tracking/%d/%d?month=%d&year=%d",
                costPaymentUrl, groupId, userId, month, year);
            
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            System.err.println("Error fetching user usage: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> updateUsageKm(Integer groupId, Integer userId, Integer month, Integer year, Double kmDriven) {
        try {
            // Sử dụng format số để tránh vấn đề với số thập phân
            String url = String.format("%s/api/usage-tracking/update-km?groupId=%d&userId=%d&month=%d&year=%d&kmDriven=%s",
                costPaymentUrl, groupId, userId, month, year, kmDriven.toString());
            
            // Backend trả về UsageTracking entity, RestTemplate sẽ tự động convert thành Map
            Map<String, Object> result = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                null,
                Map.class
            ).getBody();
            
            return result;
        } catch (Exception e) {
            System.err.println("Error updating usage km: " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug
            return null;
        }
    }

    public List<Map<String, Object>> getUserHistory(Integer userId) {
        try {
            String url = String.format("%s/api/usage-tracking/user/%d/history", costPaymentUrl, userId);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching user history: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> saveUsageTracking(Map<String, Object> usageTracking) {
        try {
            return restTemplate.postForObject(
                costPaymentUrl + "/api/usage-tracking",
                usageTracking,
                Map.class
            );
        } catch (Exception e) {
            System.err.println("Error saving usage tracking: " + e.getMessage());
            return null;
        }
    }

    // Inner class for cost share request
    public static class CostShareRequest {
        private List<Integer> userIds;
        private List<Double> percentages;

        public List<Integer> getUserIds() { return userIds; }
        public void setUserIds(List<Integer> userIds) { this.userIds = userIds; }
        public List<Double> getPercentages() { return percentages; }
        public void setPercentages(List<Double> percentages) { this.percentages = percentages; }
    }
}