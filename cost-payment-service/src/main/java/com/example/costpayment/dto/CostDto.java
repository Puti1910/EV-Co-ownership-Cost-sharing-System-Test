package com.example.costpayment.dto;

import java.time.LocalDateTime;

public class CostDto {
    private Integer costId;
    private Integer vehicleId;
    private String costType;
    private Double amount;
    private String description;
    private LocalDateTime createdAt;
    private String status = "PENDING";

    // Constructors
    public CostDto() {}

    public CostDto(Integer costId, Integer vehicleId, String costType, Double amount, String description, LocalDateTime createdAt) {
        this.costId = costId;
        this.vehicleId = vehicleId;
        this.costType = costType;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getCostId() { return costId; }
    public void setCostId(Integer costId) { this.costId = costId; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public String getCostType() { return costType; }
    public void setCostType(String costType) { this.costType = costType; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

