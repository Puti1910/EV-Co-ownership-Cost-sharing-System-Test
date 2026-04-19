package com.example.costpayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

/**
 * DTO: Tổng quan quỹ chung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundSummaryDto {

    private Integer fundId;
    private Integer groupId;
<<<<<<< HEAD
    private Double totalContributed; // Tổng đóng góp
    private Double currentBalance; // Số dư hiện tại
    private Double totalDeposit; // Tổng nạp
    private Double totalWithdraw; // Tổng rút
=======
    private BigDecimal totalContributed; // Tổng đóng góp
    private BigDecimal currentBalance; // Số dư hiện tại
    private BigDecimal totalDeposit; // Tổng nạp
    private BigDecimal totalWithdraw; // Tổng rút
>>>>>>> origin/main
    private Long pendingRequests; // Số yêu cầu chờ duyệt
    private String updatedAt;

    public FundSummaryDto(
        Integer fundId, Integer groupId, 
<<<<<<< HEAD
        Double totalContributed, Double currentBalance,
=======
        BigDecimal totalContributed, BigDecimal currentBalance,
>>>>>>> origin/main
        Long pendingRequests, String updatedAt
    ) {
        this.fundId = fundId;
        this.groupId = groupId;
        this.totalContributed = totalContributed;
        this.currentBalance = currentBalance;
        this.pendingRequests = pendingRequests;
        this.updatedAt = updatedAt;
    }
}

