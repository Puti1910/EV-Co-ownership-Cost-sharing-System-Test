package com.example.costpayment.entity;

public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    OVERDUE("Quá hạn"),
    CANCELLED("Đã hủy");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}