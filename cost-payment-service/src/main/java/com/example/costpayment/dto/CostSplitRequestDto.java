package com.example.costpayment.dto;

import java.util.List;
<<<<<<< HEAD

public class CostSplitRequestDto {
    private List<Integer> userIds;
    private List<Double> percentages;
=======
import java.math.BigDecimal;

public class CostSplitRequestDto {
    private List<Integer> userIds;
    private List<BigDecimal> percentages;
>>>>>>> origin/main

    // Constructors
    public CostSplitRequestDto() {}

<<<<<<< HEAD
    public CostSplitRequestDto(List<Integer> userIds, List<Double> percentages) {
=======
    public CostSplitRequestDto(List<Integer> userIds, List<BigDecimal> percentages) {
>>>>>>> origin/main
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

<<<<<<< HEAD
    public List<Double> getPercentages() {
        return percentages;
    }

    public void setPercentages(List<Double> percentages) {
=======
    public List<BigDecimal> getPercentages() {
        return percentages;
    }

    public void setPercentages(List<BigDecimal> percentages) {
>>>>>>> origin/main
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
