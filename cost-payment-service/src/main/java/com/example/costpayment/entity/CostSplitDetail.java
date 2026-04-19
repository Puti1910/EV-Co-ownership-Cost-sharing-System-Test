package com.example.costpayment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

@Entity
@Table(name = "costsplitdetail", catalog = "Cost_Payment_DB")
public class CostSplitDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "splitDetailId")
    private Integer splitDetailId;

    @Column(name = "costId", nullable = false)
    private Integer costId;

    @Column(name = "memberId", nullable = false)
    private Integer memberId;

<<<<<<< HEAD
    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "percentage", nullable = false)
    private Double percentage;
=======
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;
>>>>>>> origin/main

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    // Constructors
    public CostSplitDetail() {
        this.createdAt = LocalDateTime.now();
    }

<<<<<<< HEAD
    public CostSplitDetail(Integer costId, Integer memberId, Double amount, Double percentage) {
=======
    public CostSplitDetail(Integer costId, Integer memberId, BigDecimal amount, BigDecimal percentage) {
>>>>>>> origin/main
        this();
        this.costId = costId;
        this.memberId = memberId;
        this.amount = amount;
        this.percentage = percentage;
    }

    // Getters and Setters
    public Integer getSplitDetailId() {
        return splitDetailId;
    }

    public void setSplitDetailId(Integer splitDetailId) {
        this.splitDetailId = splitDetailId;
    }

    public Integer getCostId() {
        return costId;
    }

    public void setCostId(Integer costId) {
        this.costId = costId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

<<<<<<< HEAD
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
=======
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
>>>>>>> origin/main
        this.percentage = percentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}