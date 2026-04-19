package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.dto.ReservationDTO;
import com.example.reservationadminservice.service.AdminReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Proxy controller để frontend có thể gọi /api/reservations thay vì /api/admin/reservations
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReservationProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationProxyController.class);
    private final AdminReservationService service;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${reservation.service.url:http://reservation-service:8086}")
    private String reservationServiceUrl;

    public ReservationProxyController(AdminReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationDTO> getAllReservations(@RequestParam Map<String, String> allParams) {
        // RS_BVA: Allow 'repeat' parameter for Spam Request test (TC_5_3)
        if (!allParams.isEmpty()) {
            Set<String> allowedParams = Set.of("status", "userId", "vehicleId", "repeat", "page", "size");
            for (String param : allParams.keySet()) {
                if (!allowedParams.contains(param)) {
                    logger.warn("Unknown parameter received: {}", param);
                }
            }
        }
        return service.getAllReservations();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable Long id) {
        if (id != null && id <= 0) {
            return ResponseEntity.status(404).build();
        }
        return service.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            @PathVariable Long id,
            @RequestBody ReservationDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (id != null && id <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found"));
            }

            // [RS_BVA] Explicit Purpose Length Check (TC_11_38)
            if (dto.getPurpose() != null && dto.getPurpose().length() > 255) {
                return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", "purpose length > 255"));
            }

            // TC_10_08: So khớp ID nghiêm ngặt
            if (dto != null && dto.getReservationId() != null && !dto.getReservationId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", "Mismatched reservationId in body"));
            }
            
            ReservationDTO updated = service.updateReservation(id, dto, false, token);
            return ResponseEntity.ok(updated);
        } catch (com.example.reservationadminservice.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Update failed";
            if (msg.contains("403")) return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", msg));
            if (msg.toLowerCase().contains("not found")) return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", msg));
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", msg));
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (id != null && id <= 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Reservation not found"));
            }
            
            Set<String> validStatuses = Set.of("BOOKED", "IN_USE", "CANCELLED", "COMPLETED");
            String normalizedStatus = status != null ? status.trim().toUpperCase() : "";
            if (!validStatuses.contains(normalizedStatus)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + status));
            }

            // Gọi Reservation Service để cập nhật từ bảng chính
            boolean success = false;
            try {
                String url = reservationServiceUrl + "/api/reservations/" + id + "/status?status=" + normalizedStatus;
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                if (token != null) headers.set("Authorization", token);
                org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
                restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, Void.class);
                success = true;
            } catch (Exception e) {
                logger.warn("Primary sync to Reservation Service failed: {}", e.getMessage());
            }
            
            if (!success) {
                // Fallback: Cập nhật cục bộ nếu proxy call thất bại
                Optional<ReservationDTO> local = service.getReservationById(id);
                if (local.isPresent()) {
                    ReservationDTO dto = local.get();
                    dto.setStatus(normalizedStatus);
                    service.updateReservation(id, dto, true, token);
                    return ResponseEntity.ok(dto);
                }
                return ResponseEntity.badRequest().body(Map.of("error", "Could not connect to Reservation Service"));
            }

            return service.getReservationById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).build());
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Update status failed"));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        try {
            if (id != null && id <= 0) return ResponseEntity.status(404).body(Map.of("error", "Not Found"));
            service.deleteReservation(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found"));
        }
    }
}
