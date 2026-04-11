package com.example.user_account_service.controller;

import com.example.user_account_service.dto.UpdateUserRoleRequest;
import com.example.user_account_service.entity.User;
import com.example.user_account_service.enums.ProfileStatus;
import com.example.user_account_service.enums.Role;
import com.example.user_account_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin") // Tiền tố chung cho API Admin
@CrossOrigin(origins = "http://localhost:8080") // Cho phép UI Admin gọi
public class AdminController {

    @Autowired
    private UserService userService; // UserService của user-account-service

    /**
     * API lấy danh sách User đang chờ duyệt
     * URL: GET http://localhost:8081/api/admin/pending-users
     * Yêu cầu: ROLE_ADMIN
     */
    @GetMapping("/pending-users")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> users = userService.getProfilesByStatus(ProfileStatus.PENDING);
        return ResponseEntity.ok(users);
    }

    /**
     * API lấy danh sách theo trạng thái
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsersByStatus(@RequestParam(defaultValue = "PENDING") String status) {
        try {
            ProfileStatus profileStatus = ProfileStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(userService.getProfilesByStatus(profileStatus));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API Duyệt hồ sơ
     * URL: PUT http://localhost:8081/api/admin/approve/{userId}
     * Yêu cầu: ROLE_ADMIN
     */
    @PutMapping("/approve/{userId}")
    public ResponseEntity<User> approveUser(@PathVariable Long userId) {
        User updatedUser = userService.approveProfile(userId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * API Từ chối hồ sơ
     * URL: PUT http://localhost:8081/api/admin/reject/{userId}
     * Yêu cầu: ROLE_ADMIN
     */
    @PutMapping("/reject/{userId}")
    public ResponseEntity<User> rejectUser(@PathVariable Long userId) {
        User updatedUser = userService.rejectProfile(userId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * API cập nhật vai trò người dùng
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long userId,
                                               @RequestBody UpdateUserRoleRequest request) {
        try {
            Role role = Role.valueOf(request.getRole().toUpperCase());
            User updatedUser = userService.updateUserRole(userId, role);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}