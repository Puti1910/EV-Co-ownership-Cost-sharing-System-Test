package com.example.user_account_service.dto;

<<<<<<< HEAD
=======
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
>>>>>>> origin/main
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
<<<<<<< HEAD
=======
    @NotBlank(message = "Refresh token không được để trống")
    @Size(min = 36, max = 36, message = "Refresh token phải có đúng 36 ký tự")
>>>>>>> origin/main
    private String refreshToken;
}

