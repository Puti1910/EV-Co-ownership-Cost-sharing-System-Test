package com.example.user_account_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String profileStatus;
}