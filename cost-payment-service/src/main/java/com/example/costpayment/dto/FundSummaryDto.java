package com.example.costpayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO: Tổng quan quỹ chung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundSummaryDto {

    private Integer fundId;
    private Integer groupId;
    private BigDecimal totalContributed; // Tổng đóng góp
    private BigDecimal currentBalance; // Số dư hiện tại
    private BigDecimal totalDeposit; // Tổng nạp
    private BigDecimal totalWithdraw; // Tổng rút
    private Long pendingRequests; // Số yêu cầu chờ duyệt
    private String updatedAt;

    public FundSummaryDto(
        Integer fundId, Integer groupId, 
        BigDecimal totalContributed, BigDecimal currentBalance,
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

