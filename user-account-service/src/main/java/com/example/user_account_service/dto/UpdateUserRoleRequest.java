package com.example.user_account_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {
    @NotBlank(message = "Vai trò (role) không được để trống")
    private String role;
}

