package com.example.reservationadminservice.service;

import com.example.reservationadminservice.model.User;
import com.example.reservationadminservice.repository.admin.AdminUserRepository;
import com.example.reservationadminservice.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminUserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AdminAuthService(AdminUserRepository repository, BCryptPasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepository = repository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }


    public String login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        User user = userOpt.get();

        // ✅ So sánh trực tiếp vì bạn không dùng mã hóa
        if (!password.equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // ✅ Trả token nếu đúng (dùng bean jwtUtil thay vì static)
        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }


}
