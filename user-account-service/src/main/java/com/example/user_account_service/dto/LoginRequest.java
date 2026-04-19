package com.example.user_account_service.dto;

<<<<<<< HEAD
=======
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
>>>>>>> origin/main
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
<<<<<<< HEAD
    private String email;
=======
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
>>>>>>> origin/main
    private String password;
}