package com.example.ui_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostItemDto {
    private Long id;
    private Long categoryId;
    private String groupId;
    private String vehicleId;
    private String title;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String currency;
    private String status;
    private LocalDateTime incurredDate;
    private LocalDateTime dueDate;
    private String invoiceNumber;
    private String receiptUrl;
    private List<CostSplitDto> costSplits;
    private List<PaymentDto> payments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
