package com.example.costpayment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost", catalog = "Cost_Payment_DB")
public class Cost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "costId")
    private Integer costId;

    @Column(name = "vehicleId", nullable = false)
    private Integer vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "costType", nullable = false)
    private CostType costType;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CostStatus status;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    // Constructors
    public Cost() {
        this.createdAt = LocalDateTime.now();
        this.status = CostStatus.PENDING;
    }

    public Cost(Integer vehicleId, CostType costType, Double amount, String description) {
        this();
        this.vehicleId = vehicleId;
        this.costType = costType;
        this.amount = amount;
        this.description = description;
    }

    // Getters and Setters
    public Integer getCostId() {
        return costId;
    }

    public void setCostId(Integer costId) {
        this.costId = costId;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public CostType getCostType() {
        return costType;
    }

    public void setCostType(CostType costType) {
        this.costType = costType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CostStatus getStatus() {
        return status;
    }

    public void setStatus(CostStatus status) {
        this.status = status;
    }

    // CostType enum
    public enum CostType {
        ElectricCharge("Sạc điện"),
        Maintenance("Bảo dưỡng"),
        Insurance("Bảo hiểm"),
        Inspection("Kiểm định"),
        Cleaning("Vệ sinh"),
        Other("Khác");

        private final String displayName;

        CostType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // CostStatus enum
    public enum CostStatus {
        PENDING("Chưa chia"),
        SHARED("Đã chia");

        private final String displayName;

        CostStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "Cost{" +
                "costId=" + costId +
                ", vehicleId=" + vehicleId +
                ", costType=" + costType +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}