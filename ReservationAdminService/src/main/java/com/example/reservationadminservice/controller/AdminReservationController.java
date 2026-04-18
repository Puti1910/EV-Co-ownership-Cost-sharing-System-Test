package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.dto.ReservationDTO;
import com.example.reservationadminservice.service.AdminReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reservations")
@CrossOrigin(origins = "http://localhost:8080", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AdminReservationController {

    private final AdminReservationService service;

    public AdminReservationController(AdminReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationDTO> getAllReservations(@RequestParam Map<String, String> allParams) {
        System.out.println("🌐 AdminReservationController.getAllReservations() - Request received");
        
        // RS_BVA_3603: Reject unknown parameters
        if (!allParams.isEmpty()) {
            java.util.Set<String> allowedParams = java.util.Set.of("status", "userId", "vehicleId");
            for (String param : allParams.keySet()) {
                if (!allowedParams.contains(param)) {
                    throw new IllegalArgumentException("Unknown or invalid parameter: " + param);
                }
            }
        }

        List<ReservationDTO> result = service.getAllReservations();
        System.out.println("✅ AdminReservationController.getAllReservations() - Returning " + result.size() + " reservations");
        return result;
    }

    // =====================================================
    // 📍 MANAGE Endpoints (Consolidated from ReservationAdminController)
    // =====================================
    @GetMapping("/manage")
    public List<ReservationDTO> manageAllReservations(@RequestParam Map<String, String> allParams) {
        // RS_BVA_3603: Strict validation for /manage too
        if (!allParams.isEmpty()) {
            throw new IllegalArgumentException("Parameters not supported on this endpoint");
        }
        return getAllReservations(allParams);
    }

    @PostMapping("/manage")
    public ResponseEntity<?> createReservationManage(@jakarta.validation.Valid @RequestBody ReservationDTO dto) {
        try {
            // RS_BVA: Fix mã trạng thái 201 Created cho Nominal cases (TC_12_01, 12_03, 12_04, v.v.)
            ReservationDTO saved = service.createReservation(dto);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(Map.of("message", "Tạo lịch thành công", "id", saved.getReservationId()));
        } catch (IllegalArgumentException e) {
            // TC_12_14, 12_19, 12_20, 12_25: Trả về 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (com.example.reservationadminservice.exception.ResourceNotFoundException e) {
            // TC_12_05, 12_06, 12_11, 12_12: Trả về 404 Not Found
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Internal Server Error: " + e.getMessage()));
        }
    }

    @PutMapping("/manage/{id}")
    public ResponseEntity<?> updateReservationManage(
            @PathVariable String id, 
            @jakarta.validation.Valid @RequestBody ReservationDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            long longId = Long.parseLong(id);
            if (longId <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found: " + id));
            }
            
            // TC_10_08: Kiểm tra tính nhất quán của ID giữa path và body
            if (dto.getReservationId() != null && !dto.getReservationId().equals(longId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", "ID in path (" + longId + ") does not match ID in body (" + dto.getReservationId() + ")"
                ));
            }
            
            service.updateReservation(longId, dto, false, token);
            return ResponseEntity.ok(Map.of("message", "Cập nhật lịch thành công"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format"));
        } catch (com.example.reservationadminservice.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Update failed"));
        }
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Map<String, Object>> deleteReservationManage(@PathVariable String id) {
        try {
            long longId = Long.parseLong(id);
            if (longId <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Reservation not found for ID: " + id));
            }
            service.deleteReservation(longId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa lịch có ID " + id));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy lịch cần xóa"));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable String id) {
        try {
            Long longId = Long.parseLong(id);
            if (longId <= 0) return ResponseEntity.status(404).build();
            return service.getReservationById(longId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/manage/{id}")
    public ResponseEntity<?> getReservationManage(@PathVariable String id) {
        try {
            Long longId = Long.parseLong(id);
            if (longId <= 0) return ResponseEntity.status(404).build();
            return service.getReservationById(longId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody ReservationDTO dto,
            @RequestHeader(value = "X-Sync-Origin", required = false) String syncOrigin,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            long longId = Long.parseLong(id);
            if (longId <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found: " + id));
            }
            
            // TC_11_38: Kiểm tra độ dài mục đích ngay tại Controller để chắc chắn trả về 400
            if (dto.getPurpose() != null && dto.getPurpose().length() > 255) {
                return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", "Mục đích sử dụng không được quá 255 ký tự"));
            }
            
            // TC_10_08: Kiểm tra tính nhất quán của ID giữa path và body
            if (dto.getReservationId() != null && !dto.getReservationId().equals(longId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed", 
                    "message", "ID in path (" + longId + ") does not match ID in body (" + dto.getReservationId() + ")"
                ));
            }

            boolean skipBookingSync = syncOrigin != null && syncOrigin.equalsIgnoreCase("reservation-service");
            ReservationDTO updated = service.updateReservation(longId, dto, skipBookingSync, token);
            return ResponseEntity.ok(updated);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format"));
        } catch (com.example.reservationadminservice.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("403 Forbidden") || msg.contains("403")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", msg));
            }
            if (msg.contains("404 Not Found") || msg.toLowerCase().contains("not found") || msg.toLowerCase().contains("không tìm thấy")) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", msg));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        try {
            service.deleteReservation(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/sync")
    public ResponseEntity<String> syncReservation(@RequestBody Map<String, Object> payload) {
        try {
            service.syncFromReservationService(payload);
            return ResponseEntity.ok("Đồng bộ thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}
