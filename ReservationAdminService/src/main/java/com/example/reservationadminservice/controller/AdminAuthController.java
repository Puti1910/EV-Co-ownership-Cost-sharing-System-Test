package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.service.AdminAuthService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "http://localhost:8080")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req) {
        String token = authService.login(req.get("username"), req.get("password"));
        return Map.of("token", token, "username", req.get("username"));
    }
}
