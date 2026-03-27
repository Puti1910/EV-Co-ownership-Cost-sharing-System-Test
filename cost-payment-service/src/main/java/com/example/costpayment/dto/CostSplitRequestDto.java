package com.example.costpayment.dto;

import java.util.List;

public class CostSplitRequestDto {
    private List<Integer> userIds;
    private List<Double> percentages;

    // Constructors
    public CostSplitRequestDto() {}

    public CostSplitRequestDto(List<Integer> userIds, List<Double> percentages) {
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

    public List<Double> getPercentages() {
        return percentages;
    }

    public void setPercentages(List<Double> percentages) {
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
