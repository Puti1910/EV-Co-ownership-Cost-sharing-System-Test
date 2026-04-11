package com.example.user_account_service.dto; // (Thay bằng package của bạn)

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    // Tên đăng nhập (sẽ map sang fullName)
    private String fullName;

    private String email;
    private String password;
    // Chúng ta không cần 'confirmPassword' ở backend,
    // vì việc kiểm tra đó nên được xử lý ở frontend (ui-service)
}