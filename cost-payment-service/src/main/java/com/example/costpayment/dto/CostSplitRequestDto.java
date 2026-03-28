package com.example.costpayment.dto;

import java.util.List;
import java.math.BigDecimal;

public class CostSplitRequestDto {
    private List<Integer> userIds;
    private List<BigDecimal> percentages;

    // Constructors
    public CostSplitRequestDto() {}

    public CostSplitRequestDto(List<Integer> userIds, List<BigDecimal> percentages) {
        this.userIds = userIds;
        this.percentages = percentages;
    }

    // Getters & Setters
    public List<Integer> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Integer> userIds) {
        this.userIds = userIds;
    }

    public List<BigDecimal> getPercentages() {
        return percentages;
    }

    public void setPercentages(List<BigDecimal> percentages) {
        this.percentages = percentages;
    }

    @Override
    public String toString() {
        return "CostSplitRequestDto{" +
                "userIds=" + userIds +
                ", percentages=" + percentages +
                '}';
    }
}
