package com.example.reservationadminservice.service;

import com.example.reservationadminservice.model.ReservationAdmin;
import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.model.booking.BookingReservation;
import com.example.reservationadminservice.model.booking.BookingVehicle;
import com.example.reservationadminservice.repository.admin.AdminReservationRepository;
import com.example.reservationadminservice.repository.admin.AdminVehicleRepository;
import com.example.reservationadminservice.repository.booking.BookingReservationRepository;
import com.example.reservationadminservice.repository.booking.BookingVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service đồng bộ dữ liệu từ booking database sang admin database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final BookingReservationRepository bookingReservationRepository;
    private final BookingVehicleRepository bookingVehicleRepository;
    private final AdminReservationRepository adminReservationRepository;
    private final AdminVehicleRepository adminVehicleRepository;
    private final ExternalApiService externalApiService;

    /**
     * Đồng bộ tất cả dữ liệu từ booking DB sang admin DB
     * Chạy mỗi 5 phút
     */
    @Scheduled(cron = "${sync.schedule.cron}")
    public void syncAllData() {
        log.info("🔄 Bắt đầu đồng bộ dữ liệu từ booking DB sang admin DB...");
        
        try {
            syncVehicles();
            syncReservations();
            log.info("✅ Đồng bộ dữ liệu thành công lúc: {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ dữ liệu: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Đồng bộ vehicles
     */
    public void syncVehicles() {
        log.info("🚗 Đồng bộ vehicles...");
        
        try {
            // Đọc từ booking DB
            List<BookingVehicle> bookingVehicles = bookingVehicleRepository.findAll();
            log.info("📖 Đọc được {} vehicles từ booking DB", bookingVehicles.size());
            
            // Convert sang admin entities
            List<VehicleAdmin> adminVehicles = bookingVehicles.stream()
                    .map(this::convertToAdminVehicle)
                    .collect(Collectors.toList());
            
            // Xóa dữ liệu cũ trong admin DB
            adminVehicleRepository.deleteAll();
            log.info("🗑️ Đã xóa vehicles cũ trong admin DB");
            
            // Lưu vào admin DB
            adminVehicleRepository.saveAll(adminVehicles);
            
            log.info("✅ Đã đồng bộ {} vehicles", adminVehicles.size());
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ vehicles: {}", e.getMessage(), e);
        }
    }

    /**
     * Đồng bộ reservations - CHỈ CẬP NHẬT VÀ THÊM MỚI, KHÔNG XÓA
     * 
     * LÝ DO:
     * - Không xóa dữ liệu trong admin DB để tránh mất dữ liệu đã xóa thủ công
     * - Chỉ cập nhật các reservation đã tồn tại và thêm mới các reservation chưa có
     * - Nếu reservation đã bị xóa trong admin DB, sẽ không được tự động thêm lại
     */
    public void syncReservations() {
        log.info("📅 Đồng bộ reservations (chỉ cập nhật và thêm mới, không xóa)...");
        
        try {
            // Đọc từ booking DB
            List<BookingReservation> bookingReservations = bookingReservationRepository.findAll();
            log.info("📖 Đọc được {} reservations từ booking DB", bookingReservations.size());
            
            int updatedCount = 0;
            int createdCount = 0;
            
            // Duyệt qua từng reservation từ booking DB
            for (BookingReservation booking : bookingReservations) {
                ReservationAdmin adminReservation = convertToAdminReservation(booking);
                
                // Kiểm tra reservation đã tồn tại trong admin DB chưa
                ReservationAdmin existing = adminReservationRepository.findById(adminReservation.getId()).orElse(null);
                
                if (existing != null) {
                    // Cập nhật reservation đã tồn tại
                    existing.setVehicleId(adminReservation.getVehicleId());
                    existing.setUserId(adminReservation.getUserId());
                    existing.setStartDatetime(adminReservation.getStartDatetime());
                    existing.setEndDatetime(adminReservation.getEndDatetime());
                    existing.setPurpose(adminReservation.getPurpose());
                    existing.setStatus(adminReservation.getStatus());
                    adminReservationRepository.save(existing);
                    updatedCount++;
                } else {
                    // Thêm mới reservation chưa có
                    adminReservationRepository.save(adminReservation);
                    createdCount++;
                }
            }
            
            log.info("✅ Đã đồng bộ: {} cập nhật, {} thêm mới", updatedCount, createdCount);
            log.info("ℹ️ Lưu ý: Các reservation đã bị xóa trong admin DB sẽ không được tự động thêm lại");
        } catch (Exception e) {
            log.error("❌ Lỗi khi đồng bộ reservations: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Đồng bộ thủ công (để test)
     */
    public void syncManually() {
        log.info("🔧 Đồng bộ thủ công được kích hoạt...");
        syncAllData();
    }
    
    /**
     * Convert BookingVehicle sang VehicleAdmin
     */
    private VehicleAdmin convertToAdminVehicle(BookingVehicle booking) {
        VehicleAdmin admin = new VehicleAdmin();
        admin.setId(booking.getVehicleId());
        admin.setVehicleName(booking.getVehicleName());
        admin.setVehicleType(booking.getVehicleType());
        admin.setLicensePlate(booking.getLicensePlate());
        
        // Convert groupId từ String (booking DB) sang Long (admin DB)
        // Booking DB có group_id là varchar(20) như "GRP001"
        // Admin DB có group_id là bigint(20) - cần lấy từ group-management-service
        Long groupId = convertGroupIdToLong(booking.getVehicleId(), booking.getGroupId());
        admin.setGroupId(groupId);
        
        admin.setStatus(booking.getStatus());
        return admin;
    }
    
    /**
     * Convert groupId từ String sang Long
     * Nếu groupId là string như "GRP001", cần lấy groupId thực từ group-management-service
     */
    private Long convertGroupIdToLong(Long vehicleId, String groupIdString) {
        if (groupIdString == null || groupIdString.isEmpty()) {
            // Nếu không có groupId string, thử lấy từ group-management-service
            return getGroupIdFromApi(vehicleId);
        }
        
        // Thử parse trực tiếp nếu là số
        try {
            return Long.parseLong(groupIdString);
        } catch (NumberFormatException e) {
            // Nếu không phải số, lấy từ group-management-service
            log.info("⚠️ groupId '{}' không phải số, đang lấy từ group-management-service cho vehicleId={}", groupIdString, vehicleId);
            return getGroupIdFromApi(vehicleId);
        }
    }
    
    /**
     * Lấy groupId từ group-management-service theo vehicleId
     */
    private Long getGroupIdFromApi(Long vehicleId) {
        try {
            java.util.Optional<java.util.Map<String, Object>> groupOpt = externalApiService.getGroupByVehicleId(vehicleId);
            if (groupOpt.isPresent()) {
                Object groupIdObj = groupOpt.get().get("groupId");
                if (groupIdObj != null) {
                    if (groupIdObj instanceof Number) {
                        return ((Number) groupIdObj).longValue();
                    } else {
                        return Long.parseLong(groupIdObj.toString());
                    }
                }
            }
            log.warn("⚠️ Không tìm thấy group cho vehicleId={}", vehicleId);
            return null;
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy groupId từ API cho vehicleId={}: {}", vehicleId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert BookingReservation sang ReservationAdmin
     */
    private ReservationAdmin convertToAdminReservation(BookingReservation booking) {
        ReservationAdmin admin = new ReservationAdmin();
        admin.setId(booking.getReservationId());
        admin.setVehicleId(booking.getVehicleId());
        admin.setUserId(booking.getUserId());
        admin.setStartDatetime(booking.getStartDatetime());
        admin.setEndDatetime(booking.getEndDatetime());
        admin.setPurpose(booking.getPurpose());
        admin.setStatus(booking.getStatus() != null ? booking.getStatus() : "BOOKED");
        // Set createdAt từ startDatetime (vì bảng reservations không có cột created_at)
        // Dùng startDatetime làm giá trị mặc định cho createdAt
        if (booking.getStartDatetime() != null) {
            admin.setCreatedAt(Timestamp.valueOf(booking.getStartDatetime()));
        } else {
            admin.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        return admin;
    }
}


