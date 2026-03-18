package com.example.reservationservice.controller;

import com.example.reservationservice.dto.ReservationRequest;
import com.example.reservationservice.model.Reservation;
import com.example.reservationservice.repository.ReservationRepository;
import com.example.reservationservice.service.BookingService;
import com.example.reservationservice.service.GroupManagementApiService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8084"}, allowCredentials = "true")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final BookingService bookingService;
    private final ReservationRepository reservationRepo;
    private final GroupManagementApiService groupManagementApiService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Value("${admin.service.url:http://localhost:8082}")
    private String adminServiceUrl;
    
    @Value("${vehicle.service.url:http://localhost:8085}")
    private String vehicleServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * ====================================================================
     * LẤY DANH SÁCH RESERVATIONS THEO VEHICLE ID
     * ====================================================================
     * 
     * MÔ TẢ:
     * - Lấy danh sách reservations từ bảng chính (co_ownership_booking.reservations)
     * - Clear cache trước khi query để đảm bảo lấy dữ liệu mới nhất
     * - Dữ liệu này được hiển thị trên UI người dùng
     * 
     * LƯU Ý:
     * - Luôn clear cache trước khi query để đảm bảo dữ liệu mới nhất
     * - Query từ bảng chính, không phải bảng admin
     * 
     * @param vehicleId ID của vehicle cần lấy danh sách reservations
     * @return Danh sách reservations của vehicle
     */
    @GetMapping("/vehicles/{vehicleId}/reservations")
    public List<Reservation> vehicleCalendar(@PathVariable Integer vehicleId) {
        System.out.println("📋 [FETCH RESERVATIONS] Lấy danh sách reservations cho vehicle: " + vehicleId);
        
        // Clear EntityManager cache trước khi query để đảm bảo lấy dữ liệu mới nhất
        // Điều này đảm bảo khi admin cập nhật status, UI người dùng sẽ thấy ngay
        entityManager.clear();
        System.out.println("🧹 [CACHE CLEARED] Đã clear EntityManager cache");
        
        List<Reservation> reservations = reservationRepo.findByVehicleIdOrderByStartDatetimeAsc(vehicleId);
        System.out.println("✅ [FETCH SUCCESS] Tìm thấy " + (reservations != null ? reservations.size() : 0) + " reservations cho vehicle " + vehicleId);
        
        if (reservations != null && !reservations.isEmpty()) {
            reservations.forEach(r -> System.out.println("   - ID: " + r.getReservationId() + ", Status: " + r.getStatus() + ", Start: " + r.getStartDatetime()));
        }
        
        return reservations;
    }

    /**
     * Lấy danh sách lịch theo user + vehicle để phục vụ check-in/check-out
     */
    @GetMapping("/users/{userId}/vehicles/{vehicleId}/reservations")
    public List<Reservation> getUserVehicleReservations(@PathVariable Integer userId,
                                                        @PathVariable Integer vehicleId) {
        return reservationRepo.findByUserIdAndVehicleIdOrderByStartDatetimeAsc(userId, vehicleId);
    }

    /**
     * Lấy danh sách xe mà user đồng sở hữu
     */
    @GetMapping("/users/{userId}/vehicles")
    public ResponseEntity<?> getUserVehicles(
            @PathVariable Integer userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Lấy JWT token từ header để pass xuống service calls
            String token = authHeader != null ? authHeader : null;
            
            List<Map<String, Object>> groups = groupManagementApiService.getGroupsByUserId(userId, token);
            if (groups.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            Map<Long, Map<String, Object>> adminVehicles = loadAdminVehicles(token);
            List<Map<String, Object>> results = new ArrayList<>();

            for (Map<String, Object> group : groups) {
                Integer groupId = toInteger(group.get("groupId"));
                Integer vehicleId = toInteger(group.get("vehicleId"));
                if (groupId == null || vehicleId == null) {
                    continue;
                }

                Map<String, Object> vehicleInfo = adminVehicles.get(vehicleId.longValue());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("groupId", groupId);
                item.put("groupName", group.getOrDefault("groupName", "Group#" + groupId));
                item.put("vehicleId", vehicleId);
                item.put("vehicleName", vehicleInfo != null ? vehicleInfo.getOrDefault("vehicleName", "Vehicle#" + vehicleId) : "Vehicle#" + vehicleId);
                // Lấy biển số xe: ưu tiên vehicleNumber (từ Vehicle Service), sau đó licensePlate (từ Admin Service)
                String licensePlate = "";
                if (vehicleInfo != null) {
                    licensePlate = vehicleInfo.getOrDefault("vehicleNumber", vehicleInfo.getOrDefault("licensePlate", "")).toString();
                }
                item.put("licensePlate", licensePlate);
                // Lấy loại xe: ưu tiên type (từ Vehicle Service), sau đó vehicleType (từ Admin Service)
                String vehicleType = null;
                if (vehicleInfo != null) {
                    Object typeValue = vehicleInfo.getOrDefault("type", vehicleInfo.getOrDefault("vehicleType", null));
                    vehicleType = typeValue != null ? typeValue.toString() : null;
                }
                item.put("vehicleType", vehicleType);
                item.put("status", vehicleInfo != null ? vehicleInfo.getOrDefault("status", null) : null);

                groupManagementApiService.getMembershipInfo(groupId, userId, token)
                    .ifPresent(member -> {
                        item.put("ownershipPercentage", member.get("ownershipPercent"));
                        item.put("role", member.get("role"));
                    });

                results.add(item);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Không thể lấy danh sách xe của user {}", userId, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Không thể lấy danh sách xe cho userId=" + userId));
        }
    }

    /**
     * Lấy thông tin nhóm sở hữu của một vehicle (phục vụ UI booking-form)
     */
    @GetMapping("/vehicles/{vehicleId}/group")
    public ResponseEntity<?> getVehicleGroupInfo(
            @PathVariable Integer vehicleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader != null ? authHeader : null;
            Optional<Map<String, Object>> groupOpt = groupManagementApiService.getGroupByVehicleId(vehicleId, token);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "group_not_found", "message", "Không tìm thấy nhóm cho vehicleId=" + vehicleId));
            }

            Map<String, Object> group = groupOpt.get();
            Integer groupId = toInteger(group.get("groupId"));
            Map<String, Object> payload = new HashMap<>();
            payload.put("groupId", groupId);
            payload.put("groupName", group.getOrDefault("groupName", group.getOrDefault("name", "Group#" + groupId)));
            payload.put("description", group.getOrDefault("description", group.getOrDefault("note", "")));
            payload.put("status", group.getOrDefault("status", "UNKNOWN"));
            payload.put("adminId", group.getOrDefault("adminId", null));

            List<Map<String, Object>> members = groupManagementApiService.getGroupMembers(groupId != null ? groupId : -1, token);
            List<Map<String, Object>> memberViews = members.stream().map(member -> {
                Map<String, Object> info = new HashMap<>();
                info.put("userId", member.get("userId"));
                info.put("fullName", member.getOrDefault("fullName", member.getOrDefault("name", "User#" + member.get("userId"))));
                info.put("ownershipPercentage", member.getOrDefault("ownershipPercent", member.getOrDefault("ownershipPercentage", 0)));
                info.put("role", member.getOrDefault("role", "Member"));
                return info;
            }).collect(Collectors.toList());

            payload.put("members", memberViews);
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            logger.error("Không thể lấy group info cho vehicle {}", vehicleId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "internal_error", "message", "Không thể lấy thông tin nhóm: " + e.getMessage()));
        }
    }


    @GetMapping("/availability")
    public boolean isAvailable(@RequestParam Integer vehicleId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return bookingService.isAvailable(vehicleId, start, end);
    }

    @PostMapping("/reservations")
    public ResponseEntity<?> create(
            @RequestBody(required = false) ReservationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            logger.info("=== POST /api/reservations ===");
            logger.info("Request body: {}", request);
            logger.info("Header X-User-Id: {}", headerUserId);
            logger.info("Header Authorization present: {}", authHeader != null);
            logger.info("Query param userId: {}", paramUserId);
            
            // Log tất cả headers để debug
            logger.info("All headers will be logged by Spring");
            
            // Xử lý request null
            if (request == null) {
                logger.warn("Request body is null, creating empty request");
                request = new ReservationRequest();
            }
            
            // Chuyển đổi từ Long sang Integer nếu cần
            Integer vehicleId = null;
            if (request.getVehicleId() != null) {
                vehicleId = request.getVehicleId() instanceof Integer 
                    ? (Integer) request.getVehicleId() 
                    : ((Number) request.getVehicleId()).intValue();
                logger.info("VehicleId from body: {}", vehicleId);
            }
            
            // Lấy userId từ nhiều nguồn: request body -> header -> query param
            Integer userId = null;
            if (request.getUserId() != null) {
                userId = request.getUserId() instanceof Integer 
                    ? (Integer) request.getUserId() 
                    : ((Number) request.getUserId()).intValue();
                logger.info("UserId from body: {}", userId);
            } else if (headerUserId != null && !headerUserId.isEmpty()) {
                try {
                    userId = Integer.parseInt(headerUserId.trim());
                    logger.info("UserId from header X-User-Id: {}", userId);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid userId in header: {}", headerUserId);
                }
            } else if (paramUserId != null && !paramUserId.isEmpty()) {
                try {
                    userId = Integer.parseInt(paramUserId.trim());
                    logger.info("UserId from query param: {}", userId);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid userId in param: {}", paramUserId);
                }
            }
            
            if (vehicleId == null) {
                logger.error("VehicleId is missing");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "vehicleId is required",
                    "message", "Vui lòng cung cấp vehicleId"
                ));
            }
            
            if (userId == null) {
                logger.error("UserId is missing from all sources");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "userId is required",
                    "message", "Vui lòng cung cấp userId (trong body, header X-User-Id, hoặc query param userId)"
                ));
            }
            
            logger.info("Creating reservation: vehicleId={}, userId={}, start={}, end={}", 
                vehicleId, userId, request.getStartDatetime(), request.getEndDatetime());
            
            // Lấy token từ Authorization header
            String token = authHeader != null ? authHeader : null;
            
            Reservation reservation = bookingService.create(
                    vehicleId,
                    userId,
                request.getStartDatetime(),
                request.getEndDatetime(),
                request.getPurpose(),
                token
        );
            
            return ResponseEntity.ok(reservation);
        } catch (IllegalStateException e) {
            // Lỗi overlap
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.startsWith("OVERLAP:")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "OVERLAP",
                    "message", errorMsg.substring(8) // Bỏ "OVERLAP:" prefix
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cannot create reservation",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all reservations (for admin)
     */
    @GetMapping("/reservations")
    public List<Reservation> getAllReservations() {
        return reservationRepo.findAll();
    }

    /**
     * Get reservation by ID
     */
    @GetMapping("/reservations/{id}")
    public Reservation getReservation(@PathVariable Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }

    /**
     * Cập nhật reservation
     */
    @PutMapping("/reservations/{id}")
    public Reservation updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequest request) {
        try {
            Reservation reservation = reservationRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

            // Cập nhật các field nếu có trong request
            if (request.getStartDatetime() != null) {
                reservation.setStartDatetime(request.getStartDatetime());
            }
            if (request.getEndDatetime() != null) {
                reservation.setEndDatetime(request.getEndDatetime());
            }
            if (request.getPurpose() != null) {
                reservation.setPurpose(request.getPurpose());
            }
            
            // Xử lý status với error handling
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                try {
                    String statusStr = request.getStatus().trim().toUpperCase();
                    reservation.setStatus(Reservation.Status.valueOf(statusStr));
                    System.out.println("✅ Updated reservation " + id + " status to: " + statusStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ Invalid status: " + request.getStatus() + ". Valid values: BOOKED, IN_USE, COMPLETED, CANCELLED");
                    throw new RuntimeException("Invalid status: " + request.getStatus() + ". Valid values: BOOKED, IN_USE, COMPLETED, CANCELLED", e);
                }
            }

            Reservation updated = reservationRepo.save(reservation);
            
            // Đồng bộ sang admin database
            try {
                syncToAdmin(id, updated);
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not sync to admin database: " + e.getMessage());
                // Không throw exception vì đã cập nhật thành công trong database chính
            }
            
            return updated;
        } catch (Exception e) {
            System.err.println("❌ Error updating reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Đồng bộ reservation sang admin database
     */
    private void syncToAdmin(Long id, Reservation reservation) {
        try {
            String url = adminServiceUrl + "/api/admin/reservations/" + id;
            Map<String, Object> body = new HashMap<>();
            body.put("reservationId", id);
            body.put("status", reservation.getStatus() != null ? reservation.getStatus().toString() : "BOOKED");
            body.put("startDatetime", reservation.getStartDatetime() != null ? reservation.getStartDatetime().toString() : null);
            body.put("endDatetime", reservation.getEndDatetime() != null ? reservation.getEndDatetime().toString() : null);
            body.put("purpose", reservation.getPurpose());
            body.put("vehicleId", reservation.getVehicleId());
            body.put("userId", reservation.getUserId());
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("X-Sync-Origin", "reservation-service");
            org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể đồng bộ sang admin: " + e.getMessage());
        }
    }


    /**
     * Xóa reservation
     */
    @DeleteMapping("/reservations/{id}")
    public void deleteReservation(@PathVariable Long id) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        reservationRepo.delete(reservation);
        
        // Xóa từ admin database
        try {
            String url = adminServiceUrl + "/api/admin/reservations/" + id;
            restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể xóa từ admin: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật trạng thái reservation
     */
    @PutMapping("/reservations/{id}/status")
    public Reservation updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.setStatus(Reservation.Status.valueOf(status.toUpperCase()));
        Reservation updated = reservationRepo.save(reservation);
        
        // Đồng bộ sang admin database
        syncToAdmin(id, updated);
        
        return updated;
    }

    private Map<Long, Map<String, Object>> loadAdminVehicles() {
        return loadAdminVehicles(null);
    }
    
    private Map<Long, Map<String, Object>> loadAdminVehicles(String token) {
        try {
            // Ưu tiên lấy từ Vehicle Service (giống admin)
            String vehicleUrl = vehicleServiceUrl + "/api/vehicles";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            Map<Long, Map<String, Object>> vehicleMap = new LinkedHashMap<>();
            
            // Thử lấy từ Vehicle Service trước
            try {
                ResponseEntity<List<Map<String, Object>>> vehicleResponse = restTemplate.exchange(
                    vehicleUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> vehicles = vehicleResponse.getBody();
                if (vehicles != null) {
                    for (Map<String, Object> vehicle : vehicles) {
                        Object idObj = vehicle.get("vehicleId");
                        Long id = null;
                        if (idObj instanceof Number) {
                            id = ((Number) idObj).longValue();
                        } else if (idObj != null) {
                            try {
                                id = Long.parseLong(idObj.toString());
                            } catch (NumberFormatException e) {
                                // Skip invalid ID
                                continue;
                            }
                        }
                        if (id != null) {
                            vehicleMap.put(id, vehicle);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Không thể tải danh sách xe từ vehicle service: {}", e.getMessage());
            }
            
            // Nếu không có dữ liệu, fallback về Admin Service
            if (vehicleMap.isEmpty()) {
                String adminUrl = adminServiceUrl + "/api/admin/vehicles";
                ResponseEntity<List<Map<String, Object>>> adminResponse = restTemplate.exchange(
                    adminUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> adminVehicles = adminResponse.getBody();
                if (adminVehicles != null) {
                    for (Map<String, Object> vehicle : adminVehicles) {
                        Object idObj = vehicle.get("id");
                        Long id = null;
                        if (idObj instanceof Number) {
                            id = ((Number) idObj).longValue();
                        } else if (idObj != null) {
                            try {
                                id = Long.parseLong(idObj.toString());
                            } catch (NumberFormatException e) {
                                // Skip invalid ID
                                continue;
                            }
                        }
                        if (id != null) {
                            vehicleMap.put(id, vehicle);
                        }
                    }
                }
            }
            
            return vehicleMap;
        } catch (Exception e) {
            logger.warn("Không thể tải danh sách xe: {}", e.getMessage());
            return Map.of();
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
