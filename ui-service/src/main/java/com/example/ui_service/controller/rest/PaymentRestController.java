package com.example.ui_service.controller.rest;

import com.example.ui_service.client.CostPaymentClient;
import com.example.ui_service.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Payment operations (proxy to backend)
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentRestController {

    @Value("${microservices.cost-payment.url:http://localhost:8084}")
    private String costPaymentUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get all payments
     * GET /api/payments
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPayments() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching payments: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get payments by user ID
     * GET /api/payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getPaymentsByUserId(@PathVariable Integer userId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching user payments: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get pending payments by user ID
     * GET /api/payments/user/{userId}/pending
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingPaymentsByUserId(@PathVariable Integer userId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/user/" + userId + "/pending",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching pending payments: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get payment history by user ID
     * GET /api/payments/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Map<String, Object>>> getPaymentHistoryByUserId(@PathVariable Integer userId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/user/" + userId + "/history",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching payment history: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Create payment
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> payment) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments",
                HttpMethod.POST,
                new HttpEntity<>(payment),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error creating payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi tạo thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Process payment (mark as PAID)
     * POST /api/payments/{id}/process
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, Object>> processPayment(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id + "/process",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi xử lý thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payment by ID
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable Integer id) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching payment: " + e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Không tìm thấy thanh toán"
            ));
        }
    }

    /**
     * Confirm payment with QR code
     * POST /api/payments/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id + "/confirm",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error confirming payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi xác nhận thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payments with filters for admin tracking
     * GET /api/payments/admin/tracking
     */
    @GetMapping("/admin/tracking")
    public ResponseEntity<Map<String, Object>> getPaymentsForTracking(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {
        try {
            StringBuilder url = new StringBuilder(costPaymentUrl + "/api/payments/admin/tracking?");
            
            if (status != null && !status.isEmpty()) {
                url.append("status=").append(status).append("&");
            }
            if (startDate != null && !startDate.isEmpty()) {
                url.append("startDate=").append(startDate).append("&");
            }
            if (endDate != null && !endDate.isEmpty()) {
                url.append("endDate=").append(endDate).append("&");
            }
            if (search != null && !search.isEmpty()) {
                url.append("search=").append(search).append("&");
            }
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching payments for tracking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                "payments", List.of(),
                "statistics", Map.of("total", 0, "totalAmount", 0.0, "paidCount", 0, "pendingCount", 0)
            ));
        }
    }

    /**
     * Get payment details with related information
     * GET /api/payments/{id}/details
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable Integer id) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id + "/details",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error fetching payment details: " + e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "error", "Không tìm thấy thanh toán"
            ));
        }
    }

    /**
     * Admin confirm payment manually
     * POST /api/payments/{id}/admin-confirm
     */
    @PostMapping("/{id}/admin-confirm")
    public ResponseEntity<Map<String, Object>> adminConfirmPayment(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id + "/admin-confirm",
                HttpMethod.POST,
                new HttpEntity<>(request != null ? request : Map.of()),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error in admin confirm payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi xác nhận thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Send payment reminder to user
     * POST /api/payments/{id}/remind
     */
    @PostMapping("/{id}/remind")
    public ResponseEntity<Map<String, Object>> sendPaymentReminder(@PathVariable Integer id) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id + "/remind",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error sending reminder: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi gửi nhắc nhở: " + e.getMessage()
            ));
        }
    }

    /**
     * Update payment information
     * PUT /api/payments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentData) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(paymentData),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error updating payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi cập nhật thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete payment
     * DELETE /api/payments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePayment(@PathVariable Integer id) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                costPaymentUrl + "/api/payments/" + id,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.err.println("Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi khi xóa thanh toán: " + e.getMessage()
            ));
        }
    }
}

