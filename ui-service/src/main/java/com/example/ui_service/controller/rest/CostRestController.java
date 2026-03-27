package com.example.ui_service.controller.rest;

import com.example.ui_service.client.CostPaymentClient;
import com.example.ui_service.dto.CostDto;
import com.example.ui_service.dto.CostSplitDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller để proxy các request Costs từ frontend sang backend
 */
@RestController
@RequestMapping("/api/costs")
public class CostRestController {

    @Autowired
    private CostPaymentClient costPaymentClient;

    /**
     * Lấy tất cả costs
     * GET /api/costs
     */
    @GetMapping
    public ResponseEntity<List<CostDto>> getAllCosts() {
        List<CostDto> costs = costPaymentClient.getAllCosts();
        return ResponseEntity.ok(costs);
    }

    /**
     * Lấy cost theo ID
     * GET /api/costs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CostDto> getCostById(@PathVariable Integer id) {
        CostDto cost = costPaymentClient.getCostById(id);
        if (cost != null) {
            return ResponseEntity.ok(cost);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy cost shares của một cost
     * GET /api/costs/{costId}/shares
     */
    @GetMapping("/{costId}/shares")
    public ResponseEntity<List<CostSplitDto>> getCostShares(@PathVariable Integer costId) {
        List<CostSplitDto> shares = costPaymentClient.getCostSharesByCostId(costId);
        return ResponseEntity.ok(shares);
    }

    /**
     * Tạo cost mới
     * POST /api/costs
     */
    @PostMapping
    public ResponseEntity<CostDto> createCost(@RequestBody CostDto costDto) {
        CostDto created = costPaymentClient.createCost(costDto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cập nhật cost
     * PUT /api/costs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CostDto> updateCost(@PathVariable Integer id, @RequestBody CostDto costDto) {
        CostDto updated = costPaymentClient.updateCost(id, costDto);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xóa cost
     * DELETE /api/costs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCost(@PathVariable Integer id) {
        boolean deleted = costPaymentClient.deleteCost(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Tạo cost splits (chia chi phí)
     * POST /api/costs/{costId}/splits
     */
    @PostMapping("/{costId}/splits")
    public ResponseEntity<?> createCostSplits(
            @PathVariable Integer costId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> userIds = (List<Integer>) request.get("userIds");
            @SuppressWarnings("unchecked")
            List<Double> percentages = ((List<?>) request.get("percentages"))
                    .stream()
                    .map(p -> p instanceof Number ? ((Number) p).doubleValue() : Double.parseDouble(p.toString()))
                    .collect(java.util.stream.Collectors.toList());
            
            List<CostSplitDto> shares = costPaymentClient.calculateCostShares(costId, userIds, percentages);
            
            // After calculating, we need to actually create them in the backend
            // The calculateCostShares method only calculates, we need to call the create endpoint
            // Use API Gateway instead of direct service call
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String costPaymentUrl = System.getenv("API_GATEWAY_URL");
            if (costPaymentUrl == null || costPaymentUrl.isEmpty()) {
                costPaymentUrl = "http://localhost:8084"; // Default to Gateway
            }
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            Map<String, Object> splitRequest = new java.util.HashMap<>();
            splitRequest.put("userIds", userIds);
            splitRequest.put("percentages", percentages);
            
            org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                new org.springframework.http.HttpEntity<>(splitRequest, headers);
            
            org.springframework.core.ParameterizedTypeReference<List<CostSplitDto>> responseType = 
                new org.springframework.core.ParameterizedTypeReference<List<CostSplitDto>>() {};
            
            org.springframework.http.ResponseEntity<List<CostSplitDto>> response = restTemplate.exchange(
                costPaymentUrl + "/api/costs/" + costId + "/splits",
                org.springframework.http.HttpMethod.POST,
                entity,
                responseType
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error creating cost splits: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

