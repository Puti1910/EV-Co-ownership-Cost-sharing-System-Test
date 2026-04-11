package com.example.costpayment.repository;

import com.example.costpayment.entity.Payment;
import com.example.costpayment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    
    /**
     * Find payments by userId
     */
    List<Payment> findByUserId(Integer userId);
    
    /**
     * Find payments by costId
     */
    List<Payment> findByCostId(Integer costId);
    
    /**
     * Find payments by userId and status
     */
    List<Payment> findByUserIdAndStatus(Integer userId, PaymentStatus status);
    
    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Find pending payments by userId (for user dashboard)
     */
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = 'PENDING' ORDER BY p.paymentDate DESC")
    List<Payment> findPendingPaymentsByUserId(@Param("userId") Integer userId);
    
    /**
     * Find payment history by userId (completed payments)
     */
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = 'PAID' ORDER BY p.paymentDate DESC")
    List<Payment> findPaymentHistoryByUserId(@Param("userId") Integer userId);
}

