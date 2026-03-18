package com.example.ui_service.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * REST Controller để proxy các request /api/funds/* từ frontend sang cost-payment-service
 * Note: Frontend đang gọi /api/funds (plural) nhưng FundRestController chỉ có /api/fund (singular)
 */
@RestController
@RequestMapping("/api/funds")
@CrossOrigin(origins = "*")
public class FundsRestController {

    private static final Logger logger = LoggerFactory.getLogger(FundsRestController.class);

    @Value("${cost-payment.service.url:http://localhost:8084}")
    private String costPaymentServiceUrl;
    
    // In Docker, this should be http://cost-payment-service:8081 (from application-docker.properties)
    // In local, this should be http://localhost:8084 (API Gateway)

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt
     * GET /api/funds/{fundId}/pending-requests
     */
    @GetMapping("/{fundId}/pending-requests")
    public ResponseEntity<?> getPendingRequests(@PathVariable Integer fundId,
                                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/" + fundId + "/pending-requests";
            logger.info("Proxying pending requests request for fundId={} to {}", fundId, url);
            
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null && !authHeader.isEmpty()) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List>() {}
            );
            
            logger.info("Received {} pending requests for fundId={}", 
                response.getBody() != null ? ((List<?>) response.getBody()).size() : 0, fundId);
            
            return ResponseEntity.ok(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Fund không tồn tại hoặc không có pending requests
            logger.info("No pending requests found for fundId={}", fundId);
            return ResponseEntity.ok(java.util.Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error getting pending requests for fundId={}: {}", fundId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error getting pending requests: " + e.getMessage()));
        }
    }

    /**
     * Lấy thống kê quỹ
     * GET /api/funds/{fundId}/statistics
     */
    @GetMapping("/{fundId}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable Integer fundId) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/" + fundId + "/statistics";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting statistics for fundId={}: {}", fundId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử giao dịch
     * GET /api/funds/{fundId}/transactions
     */
    @GetMapping("/{fundId}/transactions")
    public ResponseEntity<?> getTransactions(
        @PathVariable Integer fundId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status
    ) {
        try {
            StringBuilder urlBuilder = new StringBuilder(costPaymentServiceUrl + "/api/funds/" + fundId + "/transactions");
            boolean hasParams = false;
            
            if (type != null) {
                urlBuilder.append(hasParams ? "&" : "?").append("type=").append(type);
                hasParams = true;
            }
            if (status != null) {
                urlBuilder.append(hasParams ? "&" : "?").append("status=").append(status);
                hasParams = true;
            }
            
            String url = urlBuilder.toString();
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting transactions for fundId={}: {}", fundId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy summary của quỹ
     * GET /api/funds/{fundId}/summary
     */
    @GetMapping("/{fundId}/summary")
    public ResponseEntity<?> getSummary(@PathVariable Integer fundId) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/" + fundId + "/summary";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting summary for fundId={}: {}", fundId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

