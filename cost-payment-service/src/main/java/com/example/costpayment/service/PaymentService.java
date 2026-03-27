package com.example.costpayment.service;

import com.example.costpayment.entity.Payment;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaymentService {
    List<Payment> getAllPayments();
    Optional<Payment> getPaymentById(Integer id);
    Payment createPayment(Payment payment);
    Optional<Payment> updatePaymentStatus(Integer paymentId, String status);
    List<Payment> getPaymentsByUserId(Integer userId);
    List<Payment> getPaymentsByCostId(Integer costId);
    
    // New methods for admin tracking
    List<Map<String, Object>> getPaymentsWithFilters(String status, String startDate, String endDate, String search);
    Map<String, Object> getPaymentDetails(Integer paymentId);
    
    // New methods for payment management
    Optional<Payment> updatePayment(Integer paymentId, Payment paymentData);
    boolean deletePayment(Integer paymentId);
}
