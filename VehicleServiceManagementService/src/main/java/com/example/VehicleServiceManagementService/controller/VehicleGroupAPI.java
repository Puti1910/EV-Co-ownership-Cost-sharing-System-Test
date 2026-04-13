package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.service.VehicleGroupService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-groups")
@CrossOrigin(origins = "*")
@Validated
public class VehicleGroupAPI {

    @Autowired
    private VehicleGroupService vehicleGroupService;

    // Lấy danh sách tất cả nhóm xe
    @GetMapping
    public List<Vehiclegroup> getAllVehicleGroups(
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(30) Integer size) {
        // Gọi phương thức getAllVehicleGroups() trong service
        return vehicleGroupService.getAllVehicleGroups();
    }

    /**
     * Lấy danh sách nhóm xe chưa có xe nào
     * @return Danh sách nhóm xe chưa có xe
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVehicleGroups(
            @RequestParam(value = "currentGroupId", required = false) @Min(1) Long currentGroupId) {
        if (currentGroupId != null) {
            if (currentGroupId < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("currentGroupId phải lớn hơn hoặc bằng 1");
            }
            if (vehicleGroupService.getVehicleGroupById(currentGroupId) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy nhóm xe với ID: " + currentGroupId);
            }
            return ResponseEntity.ok(vehicleGroupService.getAvailableVehicleGroups(currentGroupId));
        } else {
            return ResponseEntity.ok(vehicleGroupService.getVehicleGroupsWithoutVehicles());
        }
    }

    /**
     * Lấy chi tiết nhóm xe theo groupId
     * @param groupId ID của nhóm xe
     * @return ResponseEntity với Vehiclegroup hoặc thông báo lỗi
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getVehicleGroupById(@PathVariable @Min(1) Long groupId) {
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
    public ResponseEntity<?> getVehiclesByGroupId(@PathVariable @Min(1) Long groupId) {
        try {
            // Kiểm tra tồn tại của nhóm
            Vehiclegroup group = vehicleGroupService.getVehicleGroupById(groupId);
            if (group == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy nhóm xe với ID: " + groupId);
            }
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
    public ResponseEntity<?> updateVehicleGroup(@PathVariable @Min(1) Long groupId, @RequestBody Vehiclegroup vehicleGroup) {
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
    public ResponseEntity<String> deleteVehicleGroup(@PathVariable @Min(1) Long groupId) {
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
    public ResponseEntity<?> filterVehicleGroups(
            @RequestParam(value = "searchQuery") @NotBlank @Size(max = 100) String searchQuery,
            @RequestParam(value = "statusFilter") @NotBlank @Size(max = 20) String statusFilter) {
        return ResponseEntity.ok(vehicleGroupService.filterVehicleGroups(searchQuery, statusFilter));
    }
}
