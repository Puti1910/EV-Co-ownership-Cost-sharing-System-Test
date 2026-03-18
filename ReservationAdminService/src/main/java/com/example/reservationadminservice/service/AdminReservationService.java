package com.example.reservationadminservice.service;

import com.example.reservationadminservice.dto.ReservationDTO;
import com.example.reservationadminservice.model.ReservationAdmin;
import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.repository.admin.AdminReservationRepository;
import com.example.reservationadminservice.repository.admin.AdminVehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminReservationService {

    private final AdminReservationRepository repository;
    private final AdminVehicleRepository vehicleRepository;
    private final ExternalApiService externalApiService;
    private final RestTemplate restTemplate;
    
    @Value("${reservation.service.url:http://localhost:8081}")
    private String reservationServiceUrl;

    public AdminReservationService(AdminReservationRepository repository,
                                   AdminVehicleRepository vehicleRepository,
                                   ExternalApiService externalApiService,
                                   RestTemplate restTemplate) {
        this.repository = repository;
        this.vehicleRepository = vehicleRepository;
        this.externalApiService = externalApiService;
        this.restTemplate = restTemplate;
    }

    public List<ReservationDTO> getAllReservations() {
        System.out.println("🔍 AdminReservationService.getAllReservations() called");
        List<ReservationAdmin> allReservations = repository.findAll();
        System.out.println("📊 Found " + allReservations.size() + " reservations in database");
        
        List<ReservationDTO> result = allReservations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        System.out.println("✅ Converted to " + result.size() + " DTOs");
        if (!result.isEmpty()) {
            System.out.println("📝 First DTO: reservationId=" + result.get(0).getReservationId() + 
                             ", vehicleId=" + result.get(0).getVehicleId() + 
                             ", userId=" + result.get(0).getUserId());
        }
        
        return result;
    }
    
    private ReservationDTO convertToDTO(ReservationAdmin reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setReservationId(reservation.getId());
        dto.setVehicleId(reservation.getVehicleId());
        dto.setUserId(reservation.getUserId());
        dto.setStartDatetime(reservation.getStartDatetime());
        dto.setEndDatetime(reservation.getEndDatetime());
        dto.setPurpose(reservation.getPurpose());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt() != null ? 
            reservation.getCreatedAt().toLocalDateTime() : null);
        
        // Lấy tên xe từ external API (vehicle-service) hoặc admin database
        VehicleAdmin vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
        if (vehicle != null) {
            dto.setVehicleName(vehicle.getVehicleName());
        } else {
            // Gọi API từ vehicle-service
            dto.setVehicleName(externalApiService.getVehicleName(reservation.getVehicleId()));
        }
        
        // Lấy tên người dùng từ external API (user-account-service)
        // Hiển thị "User#n" để tránh lỗi encoding tiếng Việt, giống như ReservationService
        dto.setUserName(externalApiService.getUserName(reservation.getUserId()));
        
        return dto;
    }
    
    public Optional<ReservationDTO> getReservationById(Long id) {
        return repository.findById(id).map(this::convertToDTO);
    }
    
    public ReservationDTO createReservation(ReservationDTO dto) {
        ReservationAdmin reservation = new ReservationAdmin();
        reservation.setVehicleId(dto.getVehicleId());
        reservation.setUserId(dto.getUserId());
        reservation.setStartDatetime(dto.getStartDatetime());
        reservation.setEndDatetime(dto.getEndDatetime());
        reservation.setPurpose(dto.getPurpose());
        reservation.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        
        ReservationAdmin saved = repository.save(reservation);
        return convertToDTO(saved);
    }
    
    public List<ReservationAdmin> getReservationsByStatus(String status) {
        return repository.findByStatus(status);
    }
    
    public List<ReservationAdmin> getReservationsByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
    
    /**
     * ====================================================================
     * CẬP NHẬT RESERVATION (ĐƯỢC GỌI TỪ RESERVATION SERVICE)
     * ====================================================================
     * 
     * MÔ TẢ:
     * - Cập nhật reservation trong bảng admin: co_ownership_admin.reservations
     * - Method này được gọi từ Reservation Service sau khi đã cập nhật bảng chính
     * - Đảm bảo dữ liệu nhất quán giữa 2 bảng
     * - Nếu reservation không tồn tại, sẽ tạo mới (upsert)
     * 
     * LƯU Ý:
     * - Không nên gọi trực tiếp method này từ admin panel
     * - Luôn gọi qua Reservation Service để đảm bảo cập nhật từ bảng chính trước
     * 
     * @param id ID của reservation cần cập nhật
     * @param dto ReservationDTO chứa thông tin cần cập nhật
     * @return ReservationDTO đã được cập nhật
     */
    public ReservationDTO updateReservation(Long id, ReservationDTO dto) {
        return updateReservation(id, dto, false);
    }
    
    public ReservationDTO updateReservation(Long id, ReservationDTO dto, boolean skipBookingSync) {
        System.out.println("🔄 [ADMIN SERVICE UPDATE] Cập nhật reservation ID: " + id + " (đồng bộ cả 2 hệ thống)");
        System.out.println("   → Status từ DTO: " + dto.getStatus());
        
        if (skipBookingSync) {
            System.out.println("⚙️ [ADMIN SERVICE UPDATE] Bỏ qua bước gọi Booking Service (nguồn: Reservation Service)");
        } else {
        callBookingServiceUpdate(id, dto);
        }
        
        // Tìm reservation trong admin database, nếu không tồn tại thì tạo mới
        ReservationAdmin reservation = repository.findById(id).orElse(null);
        
        if (reservation == null) {
            System.out.println("ℹ️ [INFO] Reservation ID: " + id + " không tồn tại trong bảng admin, sẽ tạo mới");
            reservation = new ReservationAdmin();
            reservation.setId(id);
        }
        
        // Cập nhật các field - ưu tiên dữ liệu từ DTO, nếu không có thì giữ nguyên giá trị cũ
        if (dto.getVehicleId() != null) {
            reservation.setVehicleId(dto.getVehicleId());
        }
        if (dto.getUserId() != null) {
            reservation.setUserId(dto.getUserId());
        }
        if (dto.getStartDatetime() != null) {
            reservation.setStartDatetime(dto.getStartDatetime());
        }
        if (dto.getEndDatetime() != null) {
            reservation.setEndDatetime(dto.getEndDatetime());
        }
        if (dto.getPurpose() != null) {
            reservation.setPurpose(dto.getPurpose());
        }
        // QUAN TRỌNG: Luôn cập nhật status nếu có trong DTO
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            String newStatus = dto.getStatus().trim().toUpperCase();
            reservation.setStatus(newStatus);
            System.out.println("✅ [ADMIN SERVICE UPDATE] Đã cập nhật trạng thái: " + newStatus);
        } else {
            // Nếu không có status trong DTO, đặt mặc định là BOOKED
            if (reservation.getStatus() == null || reservation.getStatus().trim().isEmpty()) {
                reservation.setStatus("BOOKED");
                System.out.println("ℹ️ [INFO] Không có status trong DTO, đặt mặc định: BOOKED");
            }
        }
        
        // Lưu vào database
        ReservationAdmin saved = repository.save(reservation);
        
        System.out.println("✅ [ADMIN SERVICE UPDATE] Đã cập nhật reservation ID: " + id + " trong bảng admin");
        System.out.println("   → Final status: " + saved.getStatus());
        return convertToDTO(saved);
    }
    
    /**
     * ====================================================================
     * XÓA RESERVATION TỪ CẢ 2 BẢNG DATABASE
     * ====================================================================
     * 
     * MÔ TẢ:
     * - Xóa reservation từ bảng admin: co_ownership_admin.reservations
     * - Gọi Reservation Service để xóa từ bảng chính: co_ownership_booking.reservations
     * 
     * LÝ DO:
     * - Hệ thống sử dụng 2 database riêng biệt
     * - Scheduled job sync dữ liệu từ booking → admin mỗi 5 phút
     * - Nếu chỉ xóa từ bảng admin, scheduled job sẽ sync lại dữ liệu từ bảng chính
     * - Để đảm bảo dữ liệu nhất quán, cần xóa từ cả 2 bảng
     * 
     * QUY TRÌNH:
     * 1. Xóa từ bảng admin (co_ownership_admin.reservations)
     * 2. Gọi Reservation Service API để xóa từ bảng chính (co_ownership_booking.reservations)
     * 
     * @param id ID của reservation cần xóa
     * @throws RuntimeException nếu không tìm thấy reservation hoặc xóa thất bại
     */
    public void deleteReservation(Long id) {
        System.out.println("🗑️ [ADMIN SERVICE DELETE] Bắt đầu xóa reservation ID: " + id);
        
        // Kiểm tra reservation có tồn tại trong bảng admin không
        if (!repository.existsById(id)) {
            System.out.println("❌ [ERROR] Không tìm thấy reservation ID: " + id + " trong bảng admin");
            throw new RuntimeException("Reservation not found");
        }
        
        try {
            // ============================================================
            // BƯỚC 1: XÓA TỪ BẢNG ADMIN (co_ownership_admin.reservations)
            // ============================================================
            System.out.println("🔄 [STEP 1] Xóa từ bảng admin (co_ownership_admin.reservations)...");
            repository.deleteById(id);
            System.out.println("✅ [SUCCESS] Đã xóa reservation ID: " + id + " từ bảng admin");
            
            // ============================================================
            // BƯỚC 2: GỌI RESERVATION SERVICE ĐỂ XÓA TỪ BẢNG CHÍNH
            // ============================================================
            try {
                System.out.println("🔄 [STEP 2] Gọi Reservation Service để xóa từ bảng chính (co_ownership_booking.reservations)...");
                deleteFromBookingDatabase(id);
                System.out.println("✅ [SUCCESS] Đã xóa reservation ID: " + id + " từ bảng chính");
            } catch (Exception e) {
                // Nếu xóa từ bảng chính thất bại, log warning nhưng không throw exception
                // Vì đã xóa thành công từ bảng admin
                System.err.println("⚠️ [WARNING] Không thể xóa từ bảng chính: " + e.getMessage());
                System.err.println("   → Scheduled job sẽ tự động sync lại sau 5 phút");
                e.printStackTrace();
            }
            
            System.out.println("✅ [COMPLETE] Hoàn tất xóa reservation ID: " + id + " từ cả 2 bảng");
            
        } catch (Exception e) {
            System.err.println("❌ [ERROR] Lỗi khi xóa reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * ====================================================================
     * XÓA RESERVATION TỪ BẢNG CHÍNH (BOOKING DATABASE)
     * ====================================================================
     * 
     * MÔ TẢ:
     * - Gọi Reservation Service API để xóa reservation từ bảng chính
     * - Endpoint: DELETE /api/reservations/{id}
     * 
     * @param reservationId ID của reservation cần xóa từ bảng chính
     * @throws Exception nếu gọi API thất bại
     */
    private void deleteFromBookingDatabase(Long reservationId) {
        try {
            // Tạo URL endpoint của Reservation Service
            String url = reservationServiceUrl + "/api/reservations/" + reservationId;
            System.out.println("📡 [API CALL] Gọi Reservation Service API: " + url);
            
            // Gọi DELETE API đến Reservation Service
            // Reservation Service sẽ xóa từ bảng chính và cũng gọi lại Admin Service để xóa từ bảng admin
            // Nhưng vì đã xóa từ bảng admin rồi, nên sẽ không có vấn đề gì
            restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
            
            System.out.println("✅ [API SUCCESS] Đã gọi thành công Reservation Service để xóa reservation ID: " + reservationId);
        } catch (Exception e) {
            System.err.println("❌ [API ERROR] Lỗi khi gọi Reservation Service để xóa reservation " + reservationId + ": " + e.getMessage());
            throw e;
        }
    }
    
    public void syncFromReservationService(Map<String, Object> payload) {
        try {
            ReservationAdmin reservation = new ReservationAdmin();
            
            // Parse dữ liệu từ payload
            reservation.setId(((Number) payload.get("reservationId")).longValue());
            reservation.setVehicleId(((Number) payload.get("vehicleId")).longValue());
            reservation.setUserId(((Number) payload.get("userId")).longValue());
            
            // Parse datetime
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            reservation.setStartDatetime(LocalDateTime.parse((String) payload.get("startDatetime"), formatter));
            reservation.setEndDatetime(LocalDateTime.parse((String) payload.get("endDatetime"), formatter));
            
            reservation.setPurpose((String) payload.get("purpose"));
            reservation.setStatus((String) payload.get("status"));
            
            // Lưu vào database
            repository.save(reservation);
            
            System.out.println("✓ Đã lưu booking ID " + reservation.getId() + " vào Admin Database");
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi lưu vào Admin Database: " + e.getMessage());
            throw new RuntimeException("Không thể đồng bộ dữ liệu: " + e.getMessage());
        }
    }
    
    private void callBookingServiceUpdate(Long reservationId, ReservationDTO dto) {
        try {
            String url = reservationServiceUrl + "/api/reservations/" + reservationId;
            System.out.println("📡 [API CALL] PUT " + url);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            
            // Đảm bảo có ít nhất một field để update
            boolean hasAnyField = false;
            
            if (dto.getVehicleId() != null) {
                body.put("vehicleId", dto.getVehicleId().intValue());
                hasAnyField = true;
            }
            if (dto.getUserId() != null) {
                body.put("userId", dto.getUserId().intValue());
                hasAnyField = true;
            }
            if (dto.getStartDatetime() != null) {
                body.put("startDatetime", dto.getStartDatetime().toString());
                hasAnyField = true;
            }
            if (dto.getEndDatetime() != null) {
                body.put("endDatetime", dto.getEndDatetime().toString());
                hasAnyField = true;
            }
            if (dto.getPurpose() != null) {
                body.put("purpose", dto.getPurpose());
                hasAnyField = true;
            }
            
            // Normalize status: uppercase và trim
            if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
                String normalizedStatus = dto.getStatus().trim().toUpperCase();
                body.put("status", normalizedStatus);
                hasAnyField = true;
                System.out.println("   → Normalized status: " + dto.getStatus() + " → " + normalizedStatus);
            }
            
            if (!hasAnyField) {
                System.out.println("⚠️ [WARNING] No fields to update in DTO, skipping API call");
                return;
            }
            
            System.out.println("   → Request body: " + body);
            
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<?> response = restTemplate.exchange(
                url, 
                HttpMethod.PUT, 
                entity, 
                Object.class
            );
            
            System.out.println("✅ [API SUCCESS] Đã cập nhật reservation ID: " + reservationId + " ở bảng chính (status: " + response.getStatusCode() + ")");
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println("❌ [API ERROR] HTTP " + e.getStatusCode() + " - Không thể cập nhật reservation " + reservationId + " ở bảng chính");
            System.err.println("   → Error body: " + errorBody);
            e.printStackTrace();
            throw new RuntimeException("Không thể cập nhật dữ liệu trên hệ thống chính: HTTP " + e.getStatusCode() + " - " + errorBody, e);
        } catch (Exception e) {
            System.err.println("❌ [API ERROR] Không thể cập nhật reservation " + reservationId + " ở bảng chính: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể cập nhật dữ liệu trên hệ thống chính: " + e.getMessage(), e);
        }
    }
}