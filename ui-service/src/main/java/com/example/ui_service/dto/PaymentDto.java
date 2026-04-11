package com.example.ui_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Integer paymentId;
    private Integer userId;
    private Integer costId;
    private String method;
    private Double amount;
    private String transactionCode;
    private String status;
}