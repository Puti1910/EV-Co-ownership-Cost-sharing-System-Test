package com.example.user_account_service.enums;

import java.util.Set;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN;

    public boolean isOneOf(Set<Role> roles) {
        return roles.contains(this);
    }
}

