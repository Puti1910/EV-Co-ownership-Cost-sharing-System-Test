package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.service.ExternalApiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private final ExternalApiService externalApiService;
    
    public TestController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }
    
    @GetMapping("/user/{userId}")
    public String testGetUser(@PathVariable Long userId) {
        // Sử dụng ExternalApiService thay vì BookingUserService
        // Hiển thị "User#n" để tránh lỗi encoding, giống như ReservationService
        String userName = externalApiService.getUserName(userId);
        return "User ID: " + userId + ", User Name: " + userName;
    }
}

