package com.example.user_account_service.enums;

public enum ProfileStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED;

    public boolean isApproved() {
        return this == APPROVED;
    }
}

