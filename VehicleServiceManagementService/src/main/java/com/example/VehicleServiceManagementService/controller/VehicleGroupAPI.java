package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.service.VehicleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-groups")
@CrossOrigin(origins = "*")
public class VehicleGroupAPI {

    @Autowired
    private VehicleGroupService vehicleGroupService;

    // Lấy danh sách tất cả nhóm xe
    @GetMapping
    public List<Vehiclegroup> getAllVehicleGroups() {
        // Gọi phương thức getAllVehicleGroups() trong service
        return vehicleGroupService.getAllVehicleGroups();
    }

    /**
     * Lấy danh sách nhóm xe chưa có xe nào
     * @return Danh sách nhóm xe chưa có xe
     */
    @GetMapping("/available")
    public List<Vehiclegroup> getAvailableVehicleGroups(
            @RequestParam(value = "currentGroupId", required = false) String currentGroupId) {
        if (currentGroupId != null && !currentGroupId.trim().isEmpty()) {
            // Nếu có currentGroupId, trả về nhóm chưa có xe + nhóm hiện tại
            return vehicleGroupService.getAvailableVehicleGroups(currentGroupId);
        } else {
            // Nếu không có currentGroupId, chỉ trả về nhóm chưa có xe
            return vehicleGroupService.getVehicleGroupsWithoutVehicles();
        }
    }

    /**
     * Lấy chi tiết nhóm xe theo groupId
     * @param groupId ID của nhóm xe
     * @return ResponseEntity với Vehiclegroup hoặc thông báo lỗi
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getVehicleGroupById(@PathVariable String groupId) {
        try {
            Vehiclegroup group = vehicleGroupService.getVehicleGroupById(groupId);
            if (group != null) {
                return ResponseEntity.ok(group);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy nhóm xe với ID: " + groupId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lấy thông tin nhóm xe: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách xe trong nhóm theo groupId
     * @param groupId ID của nhóm xe
     * @return ResponseEntity với danh sách xe hoặc thông báo lỗi
     */
    @GetMapping("/{groupId}/vehicles")
    public ResponseEntity<?> getVehiclesByGroupId(@PathVariable String groupId) {
        try {
            List<Vehicle> vehicles = vehicleGroupService.getVehiclesByGroupId(groupId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lấy danh sách xe: " + e.getMessage());
        }
    }

    // Thêm nhóm xe mới
    @PostMapping
    public Vehiclegroup addVehicleGroup(@RequestBody Vehiclegroup vehicleGroup) {
        return vehicleGroupService.addVehicleGroup(vehicleGroup);
    }

    /**
     * Sửa thông tin nhóm xe
     * Có thể sửa: tên, trạng thái, mô tả
     * 
     * @param groupId ID của nhóm xe cần sửa
     * @param vehicleGroup Đối tượng chứa thông tin cần cập nhật
     * @return ResponseEntity với Vehiclegroup đã được cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateVehicleGroup(@PathVariable String groupId, @RequestBody Vehiclegroup vehicleGroup) {
        try {
            Vehiclegroup updatedGroup = vehicleGroupService.updateVehicleGroup(groupId, vehicleGroup);
            if (updatedGroup != null) {
                return ResponseEntity.ok(updatedGroup);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy nhóm xe với ID: " + groupId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi cập nhật nhóm xe: " + e.getMessage());
        }
    }

    /**
     * Xóa nhóm xe theo groupId
     * Tự động xóa tất cả xe trong nhóm trước khi xóa nhóm
     * @param groupId ID của nhóm xe cần xóa
     * @return ResponseEntity với thông báo kết quả
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> deleteVehicleGroup(@PathVariable String groupId) {
        try {
            String resultMessage = vehicleGroupService.deleteVehicleGroup(groupId);
            if (resultMessage != null) {
                return ResponseEntity.ok(resultMessage);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy nhóm xe với ID: " + groupId);
        } catch (Exception e) {
            // Xử lý các lỗi khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi xóa nhóm xe: " + e.getMessage());
        }
    }


    // Lọc nhóm xe theo tên và trạng thái
    @GetMapping("/filter")
    public List<Vehiclegroup> filterVehicleGroups(
            @RequestParam(value = "searchQuery", required = false, defaultValue = "") String searchQuery,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "Tất cả") String statusFilter) {
        return vehicleGroupService.filterVehicleGroups(searchQuery, statusFilter);
    }
}
