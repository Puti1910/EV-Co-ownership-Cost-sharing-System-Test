package com.example.costpayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: Tổng quan quỹ chung
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundSummaryDto {

    private Integer fundId;
    private Integer groupId;
    private Double totalContributed; // Tổng đóng góp
    private Double currentBalance; // Số dư hiện tại
    private Double totalDeposit; // Tổng nạp
    private Double totalWithdraw; // Tổng rút
    private Long pendingRequests; // Số yêu cầu chờ duyệt
    private String updatedAt;

    public FundSummaryDto(
        Integer fundId, Integer groupId, 
        Double totalContributed, Double currentBalance,
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

