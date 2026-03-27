package com.example.reservationadminservice.controller;

import com.example.reservationadminservice.model.ReservationAdmin;
import com.example.reservationadminservice.model.User;
import com.example.reservationadminservice.model.VehicleAdmin;
import com.example.reservationadminservice.repository.admin.AdminReservationRepository;
import com.example.reservationadminservice.repository.admin.AdminUserRepository;
import com.example.reservationadminservice.repository.admin.AdminVehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller quản lý các thao tác CRUD với bảng reservations (lịch xe)
 * Frontend (UI-service port 8080) sẽ gọi API này để hiển thị / thêm / xóa lịch xe
 */
@RestController
@RequestMapping("/api/admin/reservations/manage")
@CrossOrigin(origins = "http://localhost:8080") // Cho phép UI gọi API
public class ReservationAdminController {

    private final AdminReservationRepository reservationRepo;
    private final AdminVehicleRepository vehicleRepo;
    private final AdminUserRepository userRepo;

    public ReservationAdminController(AdminReservationRepository reservationRepo,
                                      AdminVehicleRepository vehicleRepo,
                                      AdminUserRepository userRepo) {
        this.reservationRepo = reservationRepo;
        this.vehicleRepo = vehicleRepo;
        this.userRepo = userRepo;
    }

    // =====================================================
    // 📍 1️⃣ Lấy danh sách toàn bộ lịch đặt xe
    // =====================================================
    @GetMapping
    public List<Map<String, Object>> getAllReservations() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (ReservationAdmin r : reservationRepo.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());

            // Tên xe
            VehicleAdmin v = vehicleRepo.findById(r.getVehicleId()).orElse(null);
            map.put("vehicleName", v != null ? v.getVehicleName() : "N/A");

            // Người đặt
            User u = userRepo.findById(r.getUserId()).orElse(null);
            map.put("bookedBy", u != null ? u.getUsername() : "N/A");

            // Chi tiết thời gian
            map.put("startDate", r.getStartDatetime());
            map.put("endDate", r.getEndDatetime());
            map.put("status", r.getStatus());

            result.add(map);
        }

        return result;
    }

    // =====================================================
    // 📍 2️⃣ Tạo mới một lịch đặt xe
    // =====================================================
    @PostMapping
    public Map<String, Object> createReservation(@RequestBody ReservationAdmin r) {
        ReservationAdmin saved = reservationRepo.save(r);
        return Map.of("message", "Tạo lịch thành công", "id", saved.getId());
    }

    // =====================================================
    // 📍 3️⃣ Cập nhật trạng thái hoặc thông tin đặt xe
    // =====================================================
    @PutMapping("/{id}")
    public Map<String, Object> updateReservation(@PathVariable Long id, @RequestBody ReservationAdmin req) {
        ReservationAdmin r = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch có ID: " + id));

        r.setStartDatetime(req.getStartDatetime());
        r.setEndDatetime(req.getEndDatetime());
        r.setStatus(req.getStatus());

        reservationRepo.save(r);
        return Map.of("message", "Cập nhật lịch thành công");
    }

    // =====================================================
    // 📍 4️⃣ Xóa lịch đặt xe
    // =====================================================
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteReservation(@PathVariable Long id) {
        if (!reservationRepo.existsById(id))
            return Map.of("error", "Không tìm thấy lịch cần xóa");
        reservationRepo.deleteById(id);
        return Map.of("message", "Đã xóa lịch có ID " + id);
    }
}
