package com.example.user_account_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token không được để trống")
    @Size(min = 36, max = 36, message = "Refresh token phải có đúng 36 ký tự")
    private String refreshToken;
}

