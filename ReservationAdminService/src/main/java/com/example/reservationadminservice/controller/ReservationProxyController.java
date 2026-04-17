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
    
    @Value("${reservation.service.url:http://localhost:8086}")
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
    public ResponseEntity<ReservationDTO> getReservation(@PathVariable Long id) {
        System.out.println("🔍 [PROXY GET] Fetching reservation ID: " + id);
        
        // 1. Thử lấy từ database admin trước
        Optional<ReservationDTO> local = service.getReservationById(id);
        if (local.isPresent()) {
            System.out.println("✅ Found locally in Admin DB (ID: " + id + ")");
            return ResponseEntity.ok(local.get());
        }
        
        // 2. Nếu không thấy, thử gọi sang Reservation Service (Main)
        // Dùng tên service trong Docker network hoặc localhost tùy môi trường
        String[] possibleHosts = {reservationServiceUrl, "http://reservation-service:8086", "http://localhost:8086"};
        
        for (String host : possibleHosts) {
            try {
                String url = host + "/api/reservations/" + id;
                System.out.println("📡 Fallback call to: " + url);
                ResponseEntity<ReservationDTO> response = restTemplate.getForEntity(url, ReservationDTO.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    System.out.println("✅ Found in Main Service at " + host);
                    return response;
                }
            } catch (Exception e) {
                // Tiếp tục thử host khác
            }
        }
        
        System.err.println("❌ ID " + id + " not found anywhere.");
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<ReservationDTO> createReservation(@jakarta.validation.Valid @RequestBody ReservationDTO dto) {
        try {
            ReservationDTO created = service.createReservation(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ReservationDTO> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationDTO dto) {
        try {
            ReservationDTO updated = service.updateReservation(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
    public ResponseEntity<ReservationDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            System.out.println("🔄 [PROXY UPDATE STATUS] Proxy request cập nhật trạng thái reservation ID: " + id + " → " + status);
            
            // Gọi Reservation Service để cập nhật từ bảng chính TRƯỚC
            // Reservation Service sẽ tự động cập nhật cả 2 bảng
            String url = reservationServiceUrl + "/api/reservations/" + id + "/status?status=" + status;
            
            System.out.println("📡 [PROXY API CALL] Gọi Reservation Service: " + url);
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, null, Void.class);
            
            System.out.println("✅ [PROXY SUCCESS] Đã gọi thành công Reservation Service để cập nhật trạng thái");
            
            // Lấy reservation đã cập nhật từ Admin Service (sẽ được sync từ bảng chính)
            ReservationDTO reservation = service.getReservationById(id)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));
            
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            System.err.println("❌ [PROXY ERROR] Lỗi khi cập nhật trạng thái: " + e.getMessage());
            e.printStackTrace();
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
}

