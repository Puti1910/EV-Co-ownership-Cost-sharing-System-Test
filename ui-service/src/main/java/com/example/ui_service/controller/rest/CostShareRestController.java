package com.example.ui_service.controller.rest;

import com.example.ui_service.dto.CostSplitDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller để xử lý cost shares (phần chia chi phí)
 */
@RestController
@RequestMapping("/api/cost-shares")
public class CostShareRestController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${microservices.cost-payment.url:http://localhost:8084}")
    private String costPaymentUrl;

    /**
     * Lấy danh sách cost shares pending (chưa thanh toán) của user
     * GET /api/cost-shares/user/{userId}/pending
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<List<CostSplitDto>> getPendingCostSharesByUserId(@PathVariable Integer userId) {
        try {
            // Debug: Check authentication
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            System.out.println("=== CostShareRestController.getPendingCostSharesByUserId ===");
            System.out.println("Authentication: " + (auth != null ? auth.getName() : "null"));
            System.out.println("Authorities: " + (auth != null ? auth.getAuthorities() : "null"));
            System.out.println("Is authenticated: " + (auth != null && auth.isAuthenticated()));
            
            System.out.println("=== Fetching pending cost shares for userId: " + userId + " ===");
            String url = costPaymentUrl + "/api/cost-shares/user/" + userId + "/pending";
            System.out.println("Calling URL: " + url);
            
            // Use Map to receive response since field names might differ
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> body = response.getBody();
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response received. Count: " + (body != null ? body.size() : 0));
            
            if (body != null && !body.isEmpty()) {
                System.out.println("First item raw: " + body.get(0));
            }
            
            if (body == null || body.isEmpty()) {
                System.out.println("Returning empty list - no pending shares found");
                return ResponseEntity.ok(List.of());
            }
            
            // Convert Map to CostSplitDto
            List<CostSplitDto> result = body.stream()
                .map(this::mapToCostSplitDto)
                .collect(Collectors.toList());
            
            if (!result.isEmpty()) {
                System.out.println("First item converted: " + result.get(0));
            }
            
            System.out.println("Successfully returning " + result.size() + " pending cost shares");
            return ResponseEntity.ok(result);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("ERROR: Cannot connect to cost-payment-service at " + costPaymentUrl);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(List.of());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("ERROR: HTTP error when fetching pending cost shares");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return ResponseEntity.status(e.getStatusCode()).body(List.of());
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error fetching pending cost shares");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
        }
    }
    
    /**
     * Map response Map to CostSplitDto
     */
    private CostSplitDto mapToCostSplitDto(Map<String, Object> map) {
        try {
            CostSplitDto dto = new CostSplitDto();
            
            // Map basic fields
            if (map.get("shareId") != null) {
                Object shareIdObj = map.get("shareId");
                if (shareIdObj instanceof Number) {
                    dto.setShareId(((Number) shareIdObj).intValue());
                }
            }
            
            if (map.get("costId") != null) {
                Object costIdObj = map.get("costId");
                if (costIdObj instanceof Number) {
                    dto.setCostId(((Number) costIdObj).intValue());
                }
            }
            
            if (map.get("userId") != null) {
                Object userIdObj = map.get("userId");
                if (userIdObj instanceof Number) {
                    dto.setUserId(((Number) userIdObj).intValue());
                }
            }
            
            if (map.get("percent") != null) {
                Object percentObj = map.get("percent");
                if (percentObj instanceof Number) {
                    dto.setPercent(((Number) percentObj).doubleValue());
                }
            }
            
            if (map.get("amountShare") != null) {
                Object amountObj = map.get("amountShare");
                if (amountObj instanceof Number) {
                    dto.setAmountShare(((Number) amountObj).doubleValue());
                }
            }
            
            if (map.get("status") != null) {
                dto.setStatus(map.get("status").toString());
            }
            
            if (map.get("description") != null) {
                dto.setDescription(map.get("description").toString());
            }
            
            // Handle calculatedAt - could be LocalDateTime object, string, or Map
            if (map.get("calculatedAt") != null) {
                Object calculatedAtObj = map.get("calculatedAt");
                try {
                    if (calculatedAtObj instanceof java.time.LocalDateTime) {
                        dto.setCalculatedAt((java.time.LocalDateTime) calculatedAtObj);
                    } else if (calculatedAtObj instanceof String) {
                        String dateStr = (String) calculatedAtObj;
                        // Handle ISO format with or without Z
                        dateStr = dateStr.replace("Z", "");
                        if (dateStr.length() > 19) {
                            dateStr = dateStr.substring(0, 19);
                        }
                        dto.setCalculatedAt(java.time.LocalDateTime.parse(dateStr));
                    } else if (calculatedAtObj instanceof Map) {
                        // Handle nested date object from Jackson
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dateMap = (Map<String, Object>) calculatedAtObj;
                        if (dateMap.containsKey("year") && dateMap.containsKey("month") && dateMap.containsKey("dayOfMonth")) {
                            int year = ((Number) dateMap.get("year")).intValue();
                            int month = ((Number) dateMap.get("monthValue")).intValue();
                            int day = ((Number) dateMap.get("dayOfMonth")).intValue();
                            int hour = dateMap.containsKey("hour") ? ((Number) dateMap.get("hour")).intValue() : 0;
                            int minute = dateMap.containsKey("minute") ? ((Number) dateMap.get("minute")).intValue() : 0;
                            int second = dateMap.containsKey("second") ? ((Number) dateMap.get("second")).intValue() : 0;
                            dto.setCalculatedAt(java.time.LocalDateTime.of(year, month, day, hour, minute, second));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing calculatedAt: " + e.getMessage());
                    System.err.println("calculatedAt value: " + calculatedAtObj);
                    System.err.println("calculatedAt type: " + (calculatedAtObj != null ? calculatedAtObj.getClass().getName() : "null"));
                    // Set to current time as fallback
                    dto.setCalculatedAt(java.time.LocalDateTime.now());
                }
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("Error mapping CostSplitDto: " + e.getMessage());
            e.printStackTrace();
            // Return minimal DTO with defaults
            CostSplitDto dto = new CostSplitDto();
            dto.setCalculatedAt(java.time.LocalDateTime.now());
            return dto;
        }
    }

    /**
     * Xác nhận thanh toán cho một cost share
     * POST /api/cost-shares/{splitId}/payment
     */
    @PostMapping("/{splitId}/payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable Integer splitId,
            @RequestBody Map<String, Object> paymentInfo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentInfo, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/cost-shares/" + splitId + "/payment",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error confirming payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách cost shares đã thanh toán của user
     * GET /api/cost-shares/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<CostSplitDto>> getPaymentHistoryByUserId(@PathVariable Integer userId) {
        try {
            System.out.println("=== Fetching payment history for userId: " + userId + " ===");
            String url = costPaymentUrl + "/api/cost-shares/user/" + userId + "/history";
            System.out.println("Calling URL: " + url);
            
            // Use Map to receive response since field names might differ
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> body = response.getBody();
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response received. Count: " + (body != null ? body.size() : 0));
            
            if (body != null && !body.isEmpty()) {
                System.out.println("First item raw: " + body.get(0));
            }
            
            if (body == null || body.isEmpty()) {
                System.out.println("Returning empty list - no payment history found");
                return ResponseEntity.ok(List.of());
            }
            
            // Convert Map to CostSplitDto
            List<CostSplitDto> result = body.stream()
                .map(this::mapToCostSplitDto)
                .collect(Collectors.toList());
            
            if (!result.isEmpty()) {
                System.out.println("First item converted: " + result.get(0));
            }
            
            System.out.println("Successfully returning " + result.size() + " payment history items");
            return ResponseEntity.ok(result);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("ERROR: Cannot connect to cost-payment-service at " + costPaymentUrl);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(List.of());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("ERROR: HTTP error when fetching payment history");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return ResponseEntity.status(e.getStatusCode()).body(List.of());
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error fetching payment history");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of());
        }
    }
}

