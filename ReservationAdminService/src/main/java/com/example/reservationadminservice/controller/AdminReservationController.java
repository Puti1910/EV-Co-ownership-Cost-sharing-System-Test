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
    public Map<String, Object> createReservationManage(@jakarta.validation.Valid @RequestBody ReservationDTO dto) {
        ReservationDTO saved = service.createReservation(dto);
        return Map.of("message", "Tạo lịch thành công", "id", saved.getReservationId());
    }

    @PutMapping("/manage/{id}")
    public Map<String, Object> updateReservationManage(@PathVariable Long id, @RequestBody ReservationDTO dto) {
        service.updateReservation(id, dto, false);
        return Map.of("message", "Cập nhật lịch thành công");
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Map<String, Object>> deleteReservationManage(@PathVariable Long id) {
        try {
            service.deleteReservation(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa lịch có ID " + id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy lịch cần xóa"));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable String id) {
        try {
            Long longId = Long.parseLong(id);
            return service.getReservationById(longId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            // RS_BVA_3603: Return 400 for non-numeric ID
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/manage/{id}")
    public ResponseEntity<?> getReservationManage(@PathVariable String id) {
        try {
            Long longId = Long.parseLong(id);
            return service.getReservationById(longId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            // FIX Iteration 12: Return 404 (or 400) for path errors
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ReservationDTO> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationDTO dto,
            @RequestHeader(value = "X-Sync-Origin", required = false) String syncOrigin) {
        try {
            boolean skipBookingSync = syncOrigin != null && syncOrigin.equalsIgnoreCase("reservation-service");
            ReservationDTO updated = service.updateReservation(id, dto, skipBookingSync);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
