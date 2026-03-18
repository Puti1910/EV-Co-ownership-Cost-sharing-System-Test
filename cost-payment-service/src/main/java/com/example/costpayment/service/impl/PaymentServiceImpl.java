package com.example.costpayment.service.impl;

import com.example.costpayment.entity.Payment;
import com.example.costpayment.entity.PaymentStatus;
import com.example.costpayment.repository.PaymentRepository;
import com.example.costpayment.service.PaymentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Optional<Payment> getPaymentById(Integer id) {
        return paymentRepository.findById(id);
    }

    @Override
    public Payment createPayment(Payment payment) {
        // Generate transaction code if not provided
        if (payment.getTransactionCode() == null || payment.getTransactionCode().isEmpty()) {
            payment.setTransactionCode(generateTransactionCode());
        }
        
        // Set default status if not provided
        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.PENDING);
        }
        
        // Set payment date
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> updatePaymentStatus(Integer paymentId, String status) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            
            try {
                PaymentStatus newStatus = parsePaymentStatus(status);
                payment.setStatus(newStatus);
                
                // Update payment date if status changed to PAID
                if (newStatus == PaymentStatus.PAID) {
                    payment.setPaymentDate(LocalDateTime.now());
                }
                
                return Optional.of(paymentRepository.save(payment));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid payment status: " + status);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> getPaymentsByUserId(Integer userId) {
        // Use native query to handle invalid enum values gracefully
        // This prevents Hibernate from failing when it encounters "Completed" status
        try {
            // Use native query that converts "Completed" to "PAID" in SQL
            Query query = entityManager.createNativeQuery(
                "SELECT paymentId, userId, costId, amount, transactionCode, method, " +
                "CASE WHEN status = 'Completed' THEN 'PAID' WHEN status IS NULL THEN 'PENDING' ELSE status END as status, " +
                "paymentDate " +
                "FROM payment WHERE userId = :userId");
            query.setParameter("userId", userId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            List<Payment> payments = new ArrayList<>();
            for (Object[] row : results) {
                Payment payment = new Payment();
                payment.setPaymentId(((Number) row[0]).intValue());
                payment.setUserId(((Number) row[1]).intValue());
                if (row[2] != null) {
                    payment.setCostId(((Number) row[2]).intValue());
                }
                payment.setAmount(((Number) row[3]).doubleValue());
                if (row[4] != null) {
                    payment.setTransactionCode((String) row[4]);
                }
                if (row[5] != null) {
                    payment.setMethod(parsePaymentMethod((String) row[5]));
                }
                // Handle status - convert "Completed" to "PAID" and handle various formats
                if (row[6] != null) {
                    payment.setStatus(parsePaymentStatus((String) row[6]));
                } else {
                    payment.setStatus(PaymentStatus.PENDING);
                }
                if (row[7] != null) {
                    if (row[7] instanceof java.sql.Timestamp) {
                        payment.setPaymentDate(((java.sql.Timestamp) row[7]).toLocalDateTime());
                    } else if (row[7] instanceof java.time.LocalDateTime) {
                        payment.setPaymentDate((java.time.LocalDateTime) row[7]);
                    }
                }
                payments.add(payment);
            }
            
            return payments;
        } catch (Exception ex) {
            System.err.println("Error loading payments for userId " + userId + ": " + ex.getMessage());
            ex.printStackTrace();
            return new ArrayList<>(); // Return empty list rather than failing
        }
    }

    @Override
    public List<Payment> getPaymentsByCostId(Integer costId) {
        return paymentRepository.findByCostId(costId);
    }
    
    /**
     * Get pending payments by user ID
     */
    public List<Payment> getPendingPaymentsByUserId(Integer userId) {
        return paymentRepository.findPendingPaymentsByUserId(userId);
    }
    
    /**
     * Get payment history by user ID (completed payments)
     */
    public List<Payment> getPaymentHistoryByUserId(Integer userId) {
        return paymentRepository.findPaymentHistoryByUserId(userId);
    }
    
    /**
     * Generate unique transaction code
     */
    private String generateTransactionCode() {
        return "TXN" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Get payments with filters for admin tracking
     */
    @Override
    public List<Map<String, Object>> getPaymentsWithFilters(String status, String startDate, String endDate, String search) {
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT p.paymentId, p.userId, p.costId, p.amount, p.transactionCode, " +
                "p.method, CASE WHEN p.status = 'Completed' THEN 'PAID' ELSE p.status END as status, " +
                "p.paymentDate, c.costType, c.description as costDescription " +
                "FROM payment p " +
                "LEFT JOIN cost c ON p.costId = c.costId " +
                "WHERE 1=1 "
            );
            
            // Add status filter
            if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                if ("PAID".equalsIgnoreCase(status)) {
                    sql.append("AND (p.status = 'PAID' OR p.status = 'Completed') ");
                } else {
                    sql.append("AND p.status = '").append(status).append("' ");
                }
            }
            
            // Add date range filter
            if (startDate != null && !startDate.isEmpty()) {
                sql.append("AND p.paymentDate >= '").append(startDate).append(" 00:00:00' ");
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append("AND p.paymentDate <= '").append(endDate).append(" 23:59:59' ");
            }
            
            // Add search filter (search by userId or transactionCode)
            if (search != null && !search.isEmpty()) {
                sql.append("AND (p.userId LIKE '%").append(search).append("%' ")
                   .append("OR p.transactionCode LIKE '%").append(search).append("%' ")
                   .append("OR c.costType LIKE '%").append(search).append("%') ");
            }
            
            sql.append("ORDER BY p.paymentDate DESC");
            
            Query query = entityManager.createNativeQuery(sql.toString());
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            List<Map<String, Object>> payments = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> payment = new java.util.HashMap<>();
                payment.put("paymentId", row[0]);
                payment.put("userId", row[1]);
                payment.put("costId", row[2]);
                payment.put("amount", row[3]);
                payment.put("transactionCode", row[4]);
                payment.put("method", row[5]);
                payment.put("status", row[6]);
                
                // Handle payment date - convert to ISO-8601 string format for JSON serialization
                if (row[7] != null) {
                    LocalDateTime dateTime = null;
                    if (row[7] instanceof java.sql.Timestamp) {
                        dateTime = ((java.sql.Timestamp) row[7]).toLocalDateTime();
                    } else if (row[7] instanceof LocalDateTime) {
                        dateTime = (LocalDateTime) row[7];
                    }
                    if (dateTime != null) {
                        // Convert to ISO-8601 string format (e.g., "2024-01-15T10:30:00")
                        payment.put("paymentDate", dateTime.toString());
                    }
                }
                
                payment.put("costType", row[8]);
                payment.put("costDescription", row[9]);
                
                payments.add(payment);
            }
            
            return payments;
        } catch (Exception e) {
            System.err.println("Error getting payments with filters: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Get payment details with related information
     */
    @Override
    public Map<String, Object> getPaymentDetails(Integer paymentId) {
        try {
            String sql = 
                "SELECT p.paymentId, p.userId, p.costId, p.amount, p.transactionCode, " +
                "p.method, CASE WHEN p.status = 'Completed' THEN 'PAID' ELSE p.status END as status, " +
                "p.paymentDate, " +
                "c.costType, c.description as costDescription, c.amount as costAmount, c.createdAt as costDate " +
                "FROM payment p " +
                "LEFT JOIN cost c ON p.costId = c.costId " +
                "WHERE p.paymentId = :paymentId";
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("paymentId", paymentId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            if (results.isEmpty()) {
                return null;
            }
            
            Object[] row = results.get(0);
            Map<String, Object> details = new java.util.HashMap<>();
            
            // Payment info
            details.put("paymentId", row[0]);
            details.put("userId", row[1]);
            details.put("costId", row[2]);
            details.put("amount", row[3]);
            details.put("transactionCode", row[4]);
            details.put("method", row[5]);
            details.put("status", row[6]);
            
            // Handle payment date
            if (row[7] != null) {
                if (row[7] instanceof java.sql.Timestamp) {
                    details.put("paymentDate", ((java.sql.Timestamp) row[7]).toLocalDateTime());
                } else if (row[7] instanceof LocalDateTime) {
                    details.put("paymentDate", row[7]);
                }
            }
            
            // Cost info
            Map<String, Object> costInfo = new java.util.HashMap<>();
            costInfo.put("costType", row[8]);
            costInfo.put("description", row[9]);
            costInfo.put("amount", row[10]);
            if (row[11] != null) {
                if (row[11] instanceof java.sql.Timestamp) {
                    costInfo.put("date", ((java.sql.Timestamp) row[11]).toLocalDateTime());
                } else if (row[11] instanceof LocalDateTime) {
                    costInfo.put("date", row[11]);
                }
            }
            details.put("cost", costInfo);
            
            // User info (no User table available, set to null)
            Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("name", null);
            userInfo.put("email", null);
            details.put("user", userInfo);
            
            return details;
        } catch (Exception e) {
            System.err.println("Error getting payment details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<Payment> updatePayment(Integer paymentId, Payment paymentData) {
        try {
            Optional<Payment> existingPaymentOpt = paymentRepository.findById(paymentId);
            
            if (!existingPaymentOpt.isPresent()) {
                return Optional.empty();
            }
            
            Payment existingPayment = existingPaymentOpt.get();
            
            // Update fields
            if (paymentData.getUserId() != null) {
                existingPayment.setUserId(paymentData.getUserId());
            }
            
            if (paymentData.getCostId() != null) {
                existingPayment.setCostId(paymentData.getCostId());
            }
            
            if (paymentData.getAmount() != null) {
                existingPayment.setAmount(paymentData.getAmount());
            }
            
            if (paymentData.getMethod() != null) {
                existingPayment.setMethod(paymentData.getMethod());
            }
            
            if (paymentData.getTransactionCode() != null) {
                existingPayment.setTransactionCode(paymentData.getTransactionCode());
            }
            
            if (paymentData.getPaymentDate() != null) {
                existingPayment.setPaymentDate(paymentData.getPaymentDate());
            }
            
            if (paymentData.getStatus() != null) {
                System.out.println("PaymentServiceImpl: Setting status to: " + paymentData.getStatus() + " (enum name: " + paymentData.getStatus().name() + ")");
                existingPayment.setStatus(paymentData.getStatus());
                
                // If status is changed to PAID and payment date is not set, set it now
                if (paymentData.getStatus() == PaymentStatus.PAID && existingPayment.getPaymentDate() == null) {
                    existingPayment.setPaymentDate(LocalDateTime.now());
                }
            }
            
            // Log status before saving
            System.out.println("PaymentServiceImpl: About to save payment with status: " + existingPayment.getStatus() + " (enum name: " + existingPayment.getStatus().name() + ")");
            
            // Save updated payment
            Payment updatedPayment = paymentRepository.save(existingPayment);
            return Optional.of(updatedPayment);
            
        } catch (Exception e) {
            System.err.println("Error updating payment: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean deletePayment(Integer paymentId) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            
            if (!paymentOpt.isPresent()) {
                return false;
            }
            
            paymentRepository.deleteById(paymentId);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Parse payment method string to enum safely
     */
    private Payment.Method parsePaymentMethod(String methodStr) {
        if (methodStr == null || methodStr.isEmpty()) {
            return Payment.Method.EWALLET;
        }
        
        String normalized = methodStr.trim();
        
        // Handle various formats
        if (normalized.equalsIgnoreCase("EWallet") || normalized.equalsIgnoreCase("EWALLET")) {
            return Payment.Method.EWALLET;
        } else if (normalized.equalsIgnoreCase("Banking") || normalized.equalsIgnoreCase("BANKING") ||
                   normalized.equalsIgnoreCase("BankTransfer") || normalized.equalsIgnoreCase("BANKTRANSFER")) {
            return Payment.Method.BANKING;
        } else if (normalized.equalsIgnoreCase("Cash") || normalized.equalsIgnoreCase("CASH")) {
            return Payment.Method.CASH;
        }
        
        // Try direct enum value match
        try {
            return Payment.Method.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Payment.Method.EWALLET; // Default fallback
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

