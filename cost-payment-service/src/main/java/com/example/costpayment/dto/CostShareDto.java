package com.example.costpayment.dto;

import java.time.LocalDateTime;

public class CostShareDto {
    private Integer shareId;
    private Integer costId;
    private Integer userId;
    private Double percent;
    private Double amountShare;
    private LocalDateTime calculatedAt;
    private String status;
    private String description; // Added for displaying cost description
    private String costType; // Loại chi phí (ElectricCharge, Maintenance, etc.)
    private String costTypeDisplay; // Tên hiển thị (Sạc điện, Bảo dưỡng, etc.)
    private Double totalAmount; // Tổng chi phí gốc
    private String splitMethod; // Phương thức chia (BY_OWNERSHIP, BY_USAGE, EQUAL)
    private String splitMethodDisplay; // Tên hiển thị phương thức chia
    private Double kmDriven; // Số km đã chạy (nếu chia theo usage)
    private Double totalKm; // Tổng km của nhóm (nếu chia theo usage)
    private Double ownershipPercent; // Tỷ lệ sở hữu (nếu chia theo ownership)

    // Constructors
    public CostShareDto() {}

    public CostShareDto(Integer shareId, Integer costId, Integer userId, Double percent, Double amountShare, LocalDateTime calculatedAt) {
        this.shareId = shareId;
        this.costId = costId;
        this.userId = userId;
        this.percent = percent;
        this.amountShare = amountShare;
        this.calculatedAt = calculatedAt;
        this.status = "PENDING"; // Default status
    }

    public CostShareDto(Integer shareId, Integer costId, Integer userId, Double percent, Double amountShare, LocalDateTime calculatedAt, String status) {
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

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
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

    public Double getOwnershipPercent() {
        return ownershipPercent;
    }

    public void setOwnershipPercent(Double ownershipPercent) {
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
