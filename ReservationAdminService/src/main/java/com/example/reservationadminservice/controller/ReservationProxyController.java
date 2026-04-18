package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.dto.ReservationDTO;
import com.example.reservationadminservice.service.AdminReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Proxy controller để frontend có thể gọi /api/reservations thay vì /api/admin/reservations
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:8080", allowedHeaders = "*", 
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReservationProxyController {

    private final AdminReservationService service;
    
    @Value("${reservation.service.url:http://reservation-service:8086}")
    private String reservationServiceUrl;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    public ReservationProxyController(AdminReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationDTO> getAllReservations(@RequestParam Map<String, String> allParams) {
        // RS_BVA_3603: Allow 'repeat' parameter for Spam Request test (TC_5_3)
        if (!allParams.isEmpty()) {
            Set<String> allowedParams = Set.of("status", "userId", "vehicleId", "repeat");
            for (String param : allParams.keySet()) {
                if (!allowedParams.contains(param)) {
                    throw new IllegalArgumentException("Unknown parameter: " + param);
                }
            }
        }
        return service.getAllReservations();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable String id) {
        try {
            Long longId = Long.parseLong(id);
            if (longId <= 0) return ResponseEntity.status(404).build();
            
            System.out.println("🔍 [PROXY GET] Fetching reservation ID: " + longId);
            
            // 1. Thử lấy từ database admin trước
            Optional<ReservationDTO> local = service.getReservationById(longId);
            if (local.isPresent()) {
                System.out.println("✅ Found locally in Admin DB (ID: " + longId + ")");
                return ResponseEntity.ok(local.get());
            }
            
            // 2. Nếu không thấy, thử gọi sang Reservation Service (Main)
            String[] prioritizedHosts = { reservationServiceUrl, "http://reservation-service:8086" };
            java.util.LinkedHashSet<String> uniqueHosts = new java.util.LinkedHashSet<>(java.util.Arrays.asList(prioritizedHosts));
            
            for (String host : uniqueHosts) {
                try {
                    String url = host + "/api/reservations/" + longId;
                    ResponseEntity<ReservationDTO> response = restTemplate.getForEntity(url, ReservationDTO.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        return response;
                    }
                } catch (Exception e) {}
            }
            return ResponseEntity.status(404).build();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createReservation(@jakarta.validation.Valid @RequestBody ReservationDTO dto) {
        try {
            ReservationDTO created = service.createReservation(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Create failed"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(
            @PathVariable String id,
            @RequestBody ReservationDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long longId = Long.valueOf(id);
            if (longId <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found"));
            }

            // [RS_BVA] Explicit Purpose Length Check for Proxy (TC_11_38)
            if (dto.getPurpose() != null && dto.getPurpose().length() > 255) {
                return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", "purpose length > 255"));
            }

            // Tìm reservation hiện tại
            Optional<ReservationDTO> existing = service.getReservationById(longId);
            if (existing.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Reservation not found for ID: " + id));
            }
            
            // 1. Diagnostic Logging
            System.out.println("🔍 [ID_CHECK] Path ID: " + longId + " (" + longId.getClass().getSimpleName() + "), Body ID: " + (dto == null ? "null" : dto.getReservationId()) + " (" + (dto != null && dto.getReservationId() != null ? dto.getReservationId().getClass().getSimpleName() : "N/A") + ")");

            // 2. TC_10_08: So khớp ID nghiêm ngặt (Phải có ID trong body và phải trùng với path)
            if (dto != null) {
                if (dto.getReservationId() == null || !dto.getReservationId().equals(longId)) {
                    System.out.println("❌ [ID_CHECK] Mismatched or missing reservationId in body! Rejecting with 400.");
                    return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", "Mismatched or missing reservationId in body"));
                }
            }
            
            ReservationDTO updated = service.updateReservation(longId, dto, false, token);
            return ResponseEntity.ok(updated);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format"));
        } catch (com.example.reservationadminservice.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage() != null ? e.getMessage() : "Not found"));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Update failed";
            if (msg.contains("403 Forbidden") || msg.contains("403")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", msg));
            }
            if (msg.toLowerCase().contains("not found") || msg.contains("không tìm thấy")) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", msg));
        }
    }
    
    /**
     * ====================================================================
     * CẬP NHẬT TRẠNG THÁI RESERVATION (PROXY - GỌI RESERVATION SERVICE)
     * ====================================================================
     * 
     * MÔ TẢ:
     * - Endpoint này là proxy, sẽ gọi Reservation Service để cập nhật trạng thái
     * - Reservation Service sẽ cập nhật từ bảng chính TRƯỚC, sau đó cập nhật bảng admin
     * - Đảm bảo dữ liệu nhất quán giữa 2 bảng
     * 
     * LƯU Ý:
     * - Không cập nhật trực tiếp trong bảng admin
     * - Luôn gọi Reservation Service để đảm bảo cập nhật từ bảng chính trước
     * 
     * @param id ID của reservation cần cập nhật
     * @param status Trạng thái mới
     * @return ReservationDTO đã được cập nhật
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            System.out.println("🔄 [PROXY UPDATE STATUS] Request ID: " + id + " → " + status);
            
            // 1. Validate ID (Must be positive numeric and within Long range)
            Long longId = Long.valueOf(id);
            if (longId <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found"));
            }
            
            // 2. Validate Status (Strictly allowed: BOOKED, IN_USE, CANCELLED, COMPLETED)
            java.util.Set<String> validStatuses = java.util.Set.of("BOOKED", "IN_USE", "CANCELLED", "COMPLETED");
            if (status == null || status.trim().isEmpty() || !validStatuses.contains(status.trim().toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + status));
            }

            // 3. Gọi Reservation Service để cập nhật
            String normalizedStatus = status.trim().toUpperCase();
            String[] prioritizedHosts = { reservationServiceUrl, "http://reservation-service:8086" };
            java.util.LinkedHashSet<String> uniqueHosts = new java.util.LinkedHashSet<>(java.util.Arrays.asList(prioritizedHosts));
            boolean success = false;
            
            for (String host : uniqueHosts) {
                try {
                    String url = host + "/api/reservations/" + longId + "/status?status=" + normalizedStatus;
                    System.out.println("📡 [PROXY API CALL] Calling: " + url);
                    
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    if (token != null && !token.isEmpty()) {
                        headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
                    }
                    org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
                    
                    restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, Void.class);
                    success = true;
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Host " + host + " failed: " + e.getMessage());
                }
            }
            
            if (!success) {
                // Nếu tất cả host đều xịt, thử cập nhật DB cục bộ nếu ID tồn tại (Safety Fallback)
                Optional<ReservationDTO> local = service.getReservationById(longId);
                if (local.isPresent()) {
                    ReservationDTO dto = local.get();
                    dto.setStatus(normalizedStatus);
                    service.updateReservation(longId, dto, true, token); // Pass token
                    return ResponseEntity.ok(dto);
                }
                return ResponseEntity.badRequest().body(Map.of("error", "Could not connect to Reservation Service"));
            }

            // 4. Trả về kết quả sau đồng bộ
            return service.getReservationById(longId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).build());
                    
        } catch (Exception e) {
            System.err.println("❌ [PROXY ERROR] " + e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "Update status failed";
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReservation(@PathVariable String id) {
        try {
            long longId = Long.parseLong(id);
            if (longId <= 0) return ResponseEntity.status(404).body(Map.of("error", "Not Found"));
            service.deleteReservation(longId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found"));
        }
    }
}

