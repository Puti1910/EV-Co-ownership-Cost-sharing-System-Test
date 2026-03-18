package com.example.ui_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostSplitDto {
    private Integer shareId;
    private Integer costId;
    private Integer userId;
    private Double percent;
    private Double amountShare;
    private String status;
    private LocalDateTime calculatedAt;
    private String description;
    
    // Constructor without status for backward compatibility
    public CostSplitDto(Integer shareId, Integer costId, Integer userId, Double percent, Double amountShare) {
        this.shareId = shareId;
        this.costId = costId;
        this.userId = userId;
        this.percent = percent;
        this.amountShare = amountShare;
        this.status = "PENDING"; // Default status
    }
}