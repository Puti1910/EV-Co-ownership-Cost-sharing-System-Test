package com.example.costpayment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

@Entity
@Table(name = "costshare", catalog = "Cost_Payment_DB")
public class CostShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shareId")
    private Integer shareId;

    @Column(name = "costId", nullable = false)
    private Integer costId;
    
    @Column(name = "userId", nullable = false)
    private Integer userId;
    
<<<<<<< HEAD
    @Column(name = "percent")
    private Double percent;
    
    @Column(name = "amountShare")
    private Double amountShare;
=======
    @Column(name = "percent", precision = 5, scale = 2)
    private BigDecimal percent;
    
    @Column(name = "amountShare", precision = 15, scale = 2)
    private BigDecimal amountShare;
>>>>>>> origin/main
    
    @Column(name = "calculatedAt")
    private LocalDateTime calculatedAt = LocalDateTime.now();

    // Constructors
    public CostShare() {}
<<<<<<< HEAD
    public CostShare(Integer costId, Integer userId, Double percent, Double amountShare) {
=======
    public CostShare(Integer costId, Integer userId, BigDecimal percent, BigDecimal amountShare) {
>>>>>>> origin/main
        this.costId = costId;
        this.userId = userId;
        this.percent = percent;
        this.amountShare = amountShare;
    }

    // Getters & Setters
    public Integer getShareId() { return shareId; }
    public void setShareId(Integer shareId) { this.shareId = shareId; }

    public Integer getCostId() { return costId; }
    public void setCostId(Integer costId) { this.costId = costId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

<<<<<<< HEAD
    public Double getPercent() { return percent; }
    public void setPercent(Double percent) { this.percent = percent; }

    public Double getAmountShare() { return amountShare; }
    public void setAmountShare(Double amountShare) { this.amountShare = amountShare; }
=======
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }

    public BigDecimal getAmountShare() { return amountShare; }
    public void setAmountShare(BigDecimal amountShare) { this.amountShare = amountShare; }
>>>>>>> origin/main

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
