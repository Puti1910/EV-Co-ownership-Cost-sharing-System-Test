package com.example.costpayment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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
    
    @Column(name = "percent", precision = 5, scale = 2)
    private BigDecimal percent;
    
    @Column(name = "amountShare", precision = 15, scale = 2)
    private BigDecimal amountShare;
    
    @Column(name = "calculatedAt")
    private LocalDateTime calculatedAt = LocalDateTime.now();

    // Constructors
    public CostShare() {}
    public CostShare(Integer costId, Integer userId, BigDecimal percent, BigDecimal amountShare) {
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

    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }

    public BigDecimal getAmountShare() { return amountShare; }
    public void setAmountShare(BigDecimal amountShare) { this.amountShare = amountShare; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
