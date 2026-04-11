package com.example.ui_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostDto {
    private Integer id;
    private Integer costId;
    private Integer vehicleId;
    private String type;
    private String costType;
    private Double amount;
    private String description;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueDate;
    private String invoiceNumber;
    private String receiptUrl;
}
