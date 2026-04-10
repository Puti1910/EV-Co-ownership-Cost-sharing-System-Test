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
     * URL: PUT http://localhost:8081/api/auth/admin/approve/{userId}
     * Yêu cầu: ROLE_ADMIN
     */
    @PutMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(400).body("Lỗi: ID người dùng không hợp lệ (phải > 0)");
        }
        try {
            User updatedUser = userService.approveProfile(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (com.example.user_account_service.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body("Lỗi: Không tìm thấy người dùng với ID: " + userId);
        } catch (Exception ex) {
            return ResponseEntity.status(400).body("Lỗi nghiệp vụ: " + ex.getMessage());
        }
    }

    /**
     * API Từ chối hồ sơ
     * URL: PUT http://localhost:8081/api/auth/admin/reject/{userId}
     * Yêu cầu: ROLE_ADMIN
     */
    @PutMapping("/reject/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(400).body("Lỗi: ID người dùng không hợp lệ (phải > 0)");
        }
        try {
            User updatedUser = userService.rejectProfile(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (com.example.user_account_service.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body("Lỗi: Không tìm thấy người dùng với ID: " + userId);
        } catch (Exception ex) {
            return ResponseEntity.status(400).body("Lỗi nghiệp vụ: " + ex.getMessage());
        }
    }

    /**
     * API cập nhật vai trò người dùng
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
                                            @jakarta.validation.Valid @RequestBody UpdateUserRoleRequest request) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(400).body("Lỗi: ID người dùng không hợp lệ (phải > 0)");
        }
        try {
            Role role = Role.valueOf(request.getRole().toUpperCase());
            User updatedUser = userService.updateUserRole(userId, role);
            return ResponseEntity.ok(updatedUser);
        } catch (com.example.user_account_service.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body("Lỗi: Không tìm thấy người dùng với ID: " + userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(400).body("Lỗi: Vai trò (role) không hợp lệ");
        } catch (Exception ex) {
            return ResponseEntity.status(400).body("Lỗi nghiệp vụ: " + ex.getMessage());
        }
    }
}