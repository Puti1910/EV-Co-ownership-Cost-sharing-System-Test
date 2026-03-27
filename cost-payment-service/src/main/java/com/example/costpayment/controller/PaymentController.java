package com.example.costpayment.controller;

import com.example.costpayment.entity.Payment;
import com.example.costpayment.entity.PaymentStatus;
import com.example.costpayment.service.impl.PaymentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Payment Management
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    /**
     * Get all payments
     * GET /api/payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment by ID
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Integer id) {
        Optional<Payment> payment = paymentService.getPaymentById(id);
        return payment.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get payments by user ID
     * GET /api/payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable Integer userId) {
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get pending payments by user ID
     * GET /api/payments/user/{userId}/pending
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<List<Payment>> getPendingPaymentsByUserId(@PathVariable Integer userId) {
        List<Payment> payments = paymentService.getPendingPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment history by user ID (completed payments)
     * GET /api/payments/user/{userId}/history
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Payment>> getPaymentHistoryByUserId(@PathVariable Integer userId) {
        List<Payment> payments = paymentService.getPaymentHistoryByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments by cost ID
     * GET /api/payments/cost/{costId}
     */
    @GetMapping("/cost/{costId}")
    public ResponseEntity<List<Payment>> getPaymentsByCostId(@PathVariable Integer costId) {
        List<Payment> payments = paymentService.getPaymentsByCostId(costId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Create new payment
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        try {
            Payment createdPayment = paymentService.createPayment(payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
        } catch (Exception e) {
            System.err.println("Error creating payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update payment status
     * PUT /api/payments/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Payment> updatedPayment = paymentService.updatePaymentStatus(id, status);
        return updatedPayment.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Process payment (mark as PAID)
     * POST /api/payments/{id}/process
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Integer id) {
        try {
            Optional<Payment> paymentOpt = paymentService.updatePaymentStatus(id, "PAID");
            
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán thành công",
                    "payment", payment,
                    "transactionCode", payment.getTransactionCode()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy thanh toán"
                ));
            }
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Lỗi khi xử lý thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Confirm payment with QR code
     * POST /api/payments/{id}/confirm
     * Body: { userId, method, transactionCode }
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        try {
            // Get payment
            Optional<Payment> paymentOpt = paymentService.getPaymentById(id);
            
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy thanh toán"
                ));
            }
            
            Payment payment = paymentOpt.get();
            
            // Update payment information
            String method = (String) request.get("method");
            String transactionCode = (String) request.get("transactionCode");
            
            if (method != null) {
                try {
                    payment.setMethod(Payment.Method.valueOf(method));
                } catch (IllegalArgumentException e) {
                    // If invalid method, ignore or use default
                    System.out.println("Invalid payment method: " + method);
                }
            }
            
            if (transactionCode != null) {
                payment.setTransactionCode(transactionCode);
            }
            
            // Mark as PAID
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(java.time.LocalDateTime.now());
            
            // Save payment
            Payment updatedPayment = paymentService.createPayment(payment);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xác nhận thanh toán thành công",
                "payment", updatedPayment,
                "transactionCode", updatedPayment.getTransactionCode()
            ));
            
        } catch (Exception e) {
            System.err.println("Error confirming payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Lỗi khi xác nhận thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payments with filters for admin tracking
     * GET /api/payments/admin/tracking
     * Params: status, startDate, endDate, search
     */
    @GetMapping("/admin/tracking")
    public ResponseEntity<Map<String, Object>> getPaymentsForTracking(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {
        try {
            List<Map<String, Object>> payments = paymentService.getPaymentsWithFilters(status, startDate, endDate, search);
            
            // Calculate statistics
            double totalAmount = payments.stream()
                .mapToDouble(p -> ((Number) p.get("amount")).doubleValue())
                .sum();
            
            long paidCount = payments.stream()
                .filter(p -> "PAID".equals(p.get("status")))
                .count();
            
            long pendingCount = payments.stream()
                .filter(p -> "PENDING".equals(p.get("status")))
                .count();
            
            return ResponseEntity.ok(Map.of(
                "payments", payments,
                "statistics", Map.of(
                    "total", payments.size(),
                    "totalAmount", totalAmount,
                    "paidCount", paidCount,
                    "pendingCount", pendingCount
                )
            ));
        } catch (Exception e) {
            System.err.println("Error fetching payments for tracking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "payments", List.of(),
                "statistics", Map.of("total", 0, "totalAmount", 0.0, "paidCount", 0, "pendingCount", 0),
                "error", e.getMessage()
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
            Map<String, Object> details = paymentService.getPaymentDetails(id);
            
            if (details != null) {
                return ResponseEntity.ok(details);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Không tìm thấy thanh toán"
                ));
            }
        } catch (Exception e) {
            System.err.println("Error fetching payment details: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Lỗi khi lấy chi tiết thanh toán: " + e.getMessage()
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
            Optional<Payment> paymentOpt = paymentService.getPaymentById(id);
            
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy thanh toán"
                ));
            }
            
            Payment payment = paymentOpt.get();
            
            // Add note if provided
            String note = request != null ? request.get("note") : null;
            
            // Mark as PAID
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(java.time.LocalDateTime.now());
            
            // Save payment
            Payment updatedPayment = paymentService.createPayment(payment);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xác nhận thanh toán thành công",
                "payment", updatedPayment
            ));
            
        } catch (Exception e) {
            System.err.println("Error in admin confirm payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
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
            Optional<Payment> paymentOpt = paymentService.getPaymentById(id);
            
            if (!paymentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy thanh toán"
                ));
            }
            
            Payment payment = paymentOpt.get();
            
            // TODO: Implement actual notification/email sending
            // For now, just return success
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi nhắc nhở thanh toán tới user ID: " + payment.getUserId()
            ));
            
        } catch (Exception e) {
            System.err.println("Error sending reminder: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
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
    public ResponseEntity<?> updatePayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentDataMap) {
        try {
            // Convert Map to Payment object with proper enum handling
            Payment paymentData = new Payment();
            
            if (paymentDataMap.containsKey("userId")) {
                paymentData.setUserId(((Number) paymentDataMap.get("userId")).intValue());
            }
            
            if (paymentDataMap.containsKey("costId")) {
                Object costIdObj = paymentDataMap.get("costId");
                if (costIdObj != null) {
                    paymentData.setCostId(((Number) costIdObj).intValue());
                }
            }
            
            if (paymentDataMap.containsKey("amount")) {
                Object amountObj = paymentDataMap.get("amount");
                if (amountObj != null) {
                    paymentData.setAmount(((Number) amountObj).doubleValue());
                }
            }
            
            // Handle payment method with mapping
            if (paymentDataMap.containsKey("method")) {
                String methodStr = (String) paymentDataMap.get("method");
                if (methodStr != null && !methodStr.isEmpty()) {
                    try {
                        // Map common variations to correct enum values
                        String normalizedMethod = methodStr.toUpperCase()
                            .replace("_", "")
                            .replace("-", "");
                        
                        // Handle specific mappings for common variations
                        if (normalizedMethod.equals("BANKTRANSFER") || normalizedMethod.equals("BANKING")) {
                            paymentData.setMethod(Payment.Method.BANKING);
                        } else if (normalizedMethod.equals("EWALLET") || normalizedMethod.equals("EWALLET")) {
                            paymentData.setMethod(Payment.Method.EWALLET);
                        } else if (normalizedMethod.equals("CASH")) {
                            paymentData.setMethod(Payment.Method.CASH);
                        } else {
                            // Try direct enum value (should be one of: BANKING, EWALLET, CASH)
                            paymentData.setMethod(Payment.Method.valueOf(normalizedMethod));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid payment method: " + methodStr + ", using default EWALLET");
                        paymentData.setMethod(Payment.Method.EWALLET);
                    }
                }
            }
            
            if (paymentDataMap.containsKey("transactionCode")) {
                paymentData.setTransactionCode((String) paymentDataMap.get("transactionCode"));
            }
            
            if (paymentDataMap.containsKey("status")) {
                String statusStr = (String) paymentDataMap.get("status");
                if (statusStr != null && !statusStr.isEmpty()) {
                    paymentData.setStatus(parsePaymentStatus(statusStr));
                }
            }
            
            if (paymentDataMap.containsKey("paymentDate")) {
                String dateStr = (String) paymentDataMap.get("paymentDate");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        paymentData.setPaymentDate(java.time.LocalDateTime.parse(dateStr));
                    } catch (Exception e) {
                        // Try parsing as LocalDate and convert
                        try {
                            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                            paymentData.setPaymentDate(date.atStartOfDay());
                        } catch (Exception e2) {
                            System.err.println("Invalid payment date format: " + dateStr);
                        }
                    }
                }
            }
            
            Optional<Payment> updatedPayment = paymentService.updatePayment(id, paymentData);
            
            if (updatedPayment.isPresent()) {
                return ResponseEntity.ok(updatedPayment.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy thanh toán với ID: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error updating payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật thanh toán: " + e.getMessage());
        }
    }

    /**
     * Delete payment
     * DELETE /api/payments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Integer id) {
        try {
            boolean deleted = paymentService.deletePayment(id);
            
            if (deleted) {
                return ResponseEntity.ok()
                    .body(Map.of(
                        "success", true,
                        "message", "Xóa thanh toán thành công"
                    ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy thanh toán với ID: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi xóa thanh toán: " + e.getMessage());
        }
    }
    
    /**
     * Parse payment status string to enum safely
     */
    private PaymentStatus parsePaymentStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return PaymentStatus.PENDING;
        }
        
        // Normalize: trim whitespace and convert to uppercase
        String normalized = statusStr.trim().toUpperCase();
        
        // Map to enum values (database ENUM: 'PENDING','PAID','OVERDUE','CANCELLED')
        switch (normalized) {
            case "PENDING":
                return PaymentStatus.PENDING;
            case "PAID":
            case "COMPLETED": // Handle legacy values
                return PaymentStatus.PAID;
            case "OVERDUE":
                return PaymentStatus.OVERDUE;
            case "CANCELLED":
            case "CANCELED": // Handle both spellings
                return PaymentStatus.CANCELLED;
            default:
                // Try direct enum value match as fallback
                try {
                    return PaymentStatus.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid payment status value: '" + statusStr + "'. Using PENDING as default.");
                    return PaymentStatus.PENDING; // Default fallback
                }
        }
    }
}

