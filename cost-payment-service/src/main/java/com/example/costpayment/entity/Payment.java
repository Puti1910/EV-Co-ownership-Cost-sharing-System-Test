package com.example.costpayment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment", catalog = "Cost_Payment_DB")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentId")
    private Integer paymentId;

    @Column(name = "userId", nullable = false)
    private Integer userId;
    
    @Column(name = "costId")
    private Integer costId;
    
    @Column(name = "amount", nullable = false)
    private Double amount;
    
    @Column(name = "transactionCode")
    private String transactionCode;

    @Convert(converter = com.example.costpayment.converter.PaymentMethodConverter.class)
    @Column(name = "method")
    private Method method = Method.EWALLET;

    @Convert(converter = com.example.costpayment.converter.PaymentStatusConverter.class)
    @Column(name = "status")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paymentDate")
    private LocalDateTime paymentDate = LocalDateTime.now();

    public enum Method { EWALLET, BANKING, CASH }

    // Constructors
    public Payment() {}
    public Payment(Integer userId, Integer costId, Double amount, Method method) {
        this.userId = userId;
        this.costId = costId;
        this.amount = amount;
        this.method = method;
    }

    // Getters & Setters
    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCostId() { return costId; }
    public void setCostId(Integer costId) { this.costId = costId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
