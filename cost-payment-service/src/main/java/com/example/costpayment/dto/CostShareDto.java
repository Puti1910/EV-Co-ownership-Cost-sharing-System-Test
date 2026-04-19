package com.example.costpayment.dto;

import java.time.LocalDateTime;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

public class CostShareDto {
    private Integer shareId;
    private Integer costId;
    private Integer userId;
<<<<<<< HEAD
    private Double percent;
    private Double amountShare;
=======
    private BigDecimal percent;
    private BigDecimal amountShare;
>>>>>>> origin/main
    private LocalDateTime calculatedAt;
    private String status;
    private String description; // Added for displaying cost description
    private String costType; // Loại chi phí (ElectricCharge, Maintenance, etc.)
    private String costTypeDisplay; // Tên hiển thị (Sạc điện, Bảo dưỡng, etc.)
<<<<<<< HEAD
    private Double totalAmount; // Tổng chi phí gốc
=======
    private BigDecimal totalAmount; // Tổng chi phí gốc
>>>>>>> origin/main
    private String splitMethod; // Phương thức chia (BY_OWNERSHIP, BY_USAGE, EQUAL)
    private String splitMethodDisplay; // Tên hiển thị phương thức chia
    private Double kmDriven; // Số km đã chạy (nếu chia theo usage)
    private Double totalKm; // Tổng km của nhóm (nếu chia theo usage)
<<<<<<< HEAD
    private Double ownershipPercent; // Tỷ lệ sở hữu (nếu chia theo ownership)
=======
    private BigDecimal ownershipPercent; // Tỷ lệ sở hữu (nếu chia theo ownership)
>>>>>>> origin/main

    // Constructors
    public CostShareDto() {}

<<<<<<< HEAD
    public CostShareDto(Integer shareId, Integer costId, Integer userId, Double percent, Double amountShare, LocalDateTime calculatedAt) {
=======
    public CostShareDto(Integer shareId, Integer costId, Integer userId, BigDecimal percent, BigDecimal amountShare, LocalDateTime calculatedAt) {
>>>>>>> origin/main
        this.shareId = shareId;
        this.costId = costId;
        this.userId = userId;
        this.percent = percent;
        this.amountShare = amountShare;
        this.calculatedAt = calculatedAt;
        this.status = "PENDING"; // Default status
    }

<<<<<<< HEAD
    public CostShareDto(Integer shareId, Integer costId, Integer userId, Double percent, Double amountShare, LocalDateTime calculatedAt, String status) {
=======
    public CostShareDto(Integer shareId, Integer costId, Integer userId, BigDecimal percent, BigDecimal amountShare, LocalDateTime calculatedAt, String status) {
>>>>>>> origin/main
        this.shareId = shareId;
        this.costId = costId;
        this.userId = userId;
        this.percent = percent;
        this.amountShare = amountShare;
        this.calculatedAt = calculatedAt;
        this.status = status;
    }

    // Getters & Setters
    public Integer getShareId() {
        return shareId;
    }

    public void setShareId(Integer shareId) {
        this.shareId = shareId;
    }

    public Integer getCostId() {
        return costId;
    }

    public void setCostId(Integer costId) {
        this.costId = costId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

<<<<<<< HEAD
    public Double getPercent() {
        return percent;
    }

    public void setPercent(Double percent) {
        this.percent = percent;
    }

    public Double getAmountShare() {
        return amountShare;
    }

    public void setAmountShare(Double amountShare) {
=======
    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }

    public BigDecimal getAmountShare() {
        return amountShare;
    }

    public void setAmountShare(BigDecimal amountShare) {
>>>>>>> origin/main
        this.amountShare = amountShare;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCostType() {
        return costType;
    }

    public void setCostType(String costType) {
        this.costType = costType;
    }

    public String getCostTypeDisplay() {
        return costTypeDisplay;
    }

    public void setCostTypeDisplay(String costTypeDisplay) {
        this.costTypeDisplay = costTypeDisplay;
    }

<<<<<<< HEAD
    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
=======
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
>>>>>>> origin/main
        this.totalAmount = totalAmount;
    }

    public String getSplitMethod() {
        return splitMethod;
    }

    public void setSplitMethod(String splitMethod) {
        this.splitMethod = splitMethod;
    }

    public String getSplitMethodDisplay() {
        return splitMethodDisplay;
    }

    public void setSplitMethodDisplay(String splitMethodDisplay) {
        this.splitMethodDisplay = splitMethodDisplay;
    }

    public Double getKmDriven() {
        return kmDriven;
    }

    public void setKmDriven(Double kmDriven) {
        this.kmDriven = kmDriven;
    }

    public Double getTotalKm() {
        return totalKm;
    }

    public void setTotalKm(Double totalKm) {
        this.totalKm = totalKm;
    }

<<<<<<< HEAD
    public Double getOwnershipPercent() {
        return ownershipPercent;
    }

    public void setOwnershipPercent(Double ownershipPercent) {
=======
    public BigDecimal getOwnershipPercent() {
        return ownershipPercent;
    }

    public void setOwnershipPercent(BigDecimal ownershipPercent) {
>>>>>>> origin/main
        this.ownershipPercent = ownershipPercent;
    }

    @Override
    public String toString() {
        return "CostShareDto{" +
                "shareId=" + shareId +
                ", costId=" + costId +
                ", userId=" + userId +
                ", percent=" + percent +
                ", amountShare=" + amountShare +
                ", calculatedAt=" + calculatedAt +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", costType='" + costType + '\'' +
                ", splitMethod='" + splitMethod + '\'' +
                '}';
    }
}
