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
    public List<ReservationDTO> getAllReservations() {
        System.out.println("🌐 AdminReservationController.getAllReservations() - Request received");
        List<ReservationDTO> result = service.getAllReservations();
        System.out.println("✅ AdminReservationController.getAllReservations() - Returning " + result.size() + " reservations");
        return result;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable Long id) {
        return service.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
