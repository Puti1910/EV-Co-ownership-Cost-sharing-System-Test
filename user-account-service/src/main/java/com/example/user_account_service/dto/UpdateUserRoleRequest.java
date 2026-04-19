package com.example.user_account_service.dto;

<<<<<<< HEAD
=======
import jakarta.validation.constraints.NotBlank;
>>>>>>> origin/main
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {
<<<<<<< HEAD
=======
    @NotBlank(message = "Vai trò (role) không được để trống")
>>>>>>> origin/main
    private String role;
}

