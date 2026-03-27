package com.example.costpayment.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO cho UsageTracking kèm % km
 */
@Data
public class UsageTrackingDto {
    private Integer usageId;
    private Integer groupId;
    private Integer userId;
    private Integer month;
    private Integer year;
    private Double kmDriven;
    private LocalDateTime recordedAt;
    
    // Thêm field tính toán
    private Double percentKm;      // % km so với tổng nhóm
}


