package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.service.VehicleCleanupService;

import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import com.example.VehicleServiceManagementService.repository.VehicleGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
@Validated
public class VehicleAPI {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleGroupRepository vehicleGroupRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private VehicleCleanupService vehicleCleanupService;

    /**
     * Lấy tất cả các xe
     * @return Danh sách tất cả xe
     */
    @GetMapping
    public ResponseEntity<?> getAllVehicles(
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(200) Integer size) {
        
        // Manual validation to guarantee 400 Bad Request
        if (size != null && (size < 1 || size > 200)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Dữ liệu không hợp lệ: size must be between 1 and 200");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        try {
            List<Vehicle> vehicles = vehicleRepository.findAll();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Lấy chi tiết xe theo ID
     * @param vehicleId ID của xe
     * @return ResponseEntity với Vehicle hoặc thông báo lỗi
     */
    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> getVehicleById(@PathVariable @Min(1) Long vehicleId) {
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Không tìm thấy xe với ID: " + vehicleId));
            }
            return ResponseEntity.ok(vehicleOpt.get());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lấy thông tin xe: " + e.getMessage());
        }
    }

    /**
     * Thêm nhiều xe vào nhóm
     * @param requestData Map chứa groupId và danh sách vehicles
     * @return ResponseEntity với danh sách xe đã được tạo hoặc thông báo lỗi
     */
    @PostMapping("/batch")
    @Transactional
    public ResponseEntity<?> addVehicles(@RequestBody Map<String, Object> requestData) {
        try {
            Object groupIdObj = requestData.get("groupId");
            Long groupId = null;
            if (groupIdObj != null && !groupIdObj.toString().trim().isEmpty()) {
                try {
                    groupId = Long.valueOf(groupIdObj.toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "groupId không đúng định dạng số"));
                }
            }
            
            if (groupId != null && groupId < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "groupId không hợp lệ (phải >= 1)"));
            }
            
            Object vehiclesObj = requestData.get("vehicles");
            if (!(vehiclesObj instanceof List)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Trường 'vehicles' phải là một danh sách các xe"));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vehiclesData = (List<Map<String, Object>>) vehiclesObj;
            
            if (vehiclesData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Danh sách xe không được để trống"));
            }
            
            Vehiclegroup group = null;
            
            // Nếu có groupId, kiểm tra và lấy nhóm xe
            if (groupId != null) {
                Optional<Vehiclegroup> groupOpt = vehicleGroupRepository.findById(groupId);
                if (groupOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Không tìm thấy nhóm xe với ID: " + groupId);
                }
                group = groupOpt.get();
                
                // Restriction removed: groups can now have multiple vehicles

            }
            
            List<Vehicle> vehicles = new ArrayList<>();
            int index = 0;
            for (Map<String, Object> vehicleData : vehiclesData) {
                index++;
                Vehicle vehicle = new Vehicle();
                
                // ── vehicleId validation ──
                Object vehicleIdObj = vehicleData.get("vehicleId");
                if (vehicleIdObj != null) {
                    try {
                        Long vehicleId = Long.valueOf(vehicleIdObj.toString());
                        if (vehicleId < 1) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("success", false, "message", "Xe thứ " + index + ": vehicleId phải lớn hơn hoặc bằng 1"));
                        }
                        if (vehicleRepository.existsById(vehicleId)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("success", false, "message", "Mã xe \"" + vehicleId + "\" đã tồn tại trong hệ thống."));
                        }
                        vehicle.setVehicleId(vehicleId);
                    } catch (NumberFormatException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Xe thứ " + index + ": vehicleId không hợp lệ hoặc vượt quá giới hạn (overflow)"));
                    }
                }
                
                // ── vehicleType validation ──
                String type = null;
                if (vehicleData.containsKey("type")) {
                    type = (String) vehicleData.get("type");
                } else if (vehicleData.containsKey("vehicleType")) {
                    type = (String) vehicleData.get("vehicleType");
                }
                
                if (type == null || type.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Xe thứ " + index + ": Loại xe không được để trống"));
                }
                if (type.length() > 50) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Xe thứ " + index + ": Loại xe không được vượt quá 50 ký tự"));
                }
                vehicle.setVehicleType(type.trim());
                
                // ── vehicleNumber validation ──
                if (vehicleData.containsKey("vehicleNumber")) {
                    String vehicleNumber = (String) vehicleData.get("vehicleNumber");
                    if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Xe thứ " + index + ": Biển số xe không được để trống"));
                    }
                    String trimmedNum = vehicleNumber.trim();
                    if (trimmedNum.length() > 20) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Xe thứ " + index + ": Biển số xe không được vượt quá 20 ký tự"));
                    }
                    if (vehicleRepository.existsByVehicleNumber(trimmedNum)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Biển số xe \"" + trimmedNum + "\" đã tồn tại trong hệ thống."));
                    }
                    vehicle.setVehicleNumber(trimmedNum);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Xe thứ " + index + ": Thiếu biển số xe (vehicleNumber)"));
                }
                
                // ── vehicleName validation ──
                if (vehicleData.containsKey("vehicleName") || vehicleData.containsKey("vehiclename")) {
                    String name = (String) vehicleData.getOrDefault("vehicleName", 
                                                                    vehicleData.get("vehiclename"));
                    if (name == null || name.trim().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Xe thứ " + index + ": Tên xe không được để trống"));
                    }
                    if (name.length() > 100) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Xe thứ " + index + ": Tên xe không được vượt quá 100 ký tự"));
                    }
                    vehicle.setVehicleName(name.trim());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Xe thứ " + index + ": Thiếu tên xe (vehicleName)"));
                }
                
                // ── status validation ──
                if (vehicleData.containsKey("status")) {
                    String status = (String) vehicleData.get("status");
                    if (status != null && !status.trim().isEmpty()) {
                         String statusLower = status.trim().toLowerCase();
                         if (!statusLower.equals("ready") && !statusLower.equals("maintenance")) {
                             return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(Map.of("success", false, "message", "Xe thứ " + index + ": Trạng thái không hợp lệ (ready/maintenance)"));
                         }
                         vehicle.setStatus(statusLower);
                    } else {
                        vehicle.setStatus("ready");
                    }
                } else {
                    vehicle.setStatus("ready");
                }
                
                vehicle.setGroup(group);
                vehicles.add(vehicle);
            }
            
            List<Vehicle> savedVehicles = vehicleRepository.saveAll(vehicles);
            
            System.out.println("DEBUG: Đã thêm " + savedVehicles.size() + " xe" + 
                             (groupId != null ? " vào nhóm " + groupId : " (không thuộc nhóm nào)"));
            
            return ResponseEntity.ok(savedVehicles);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            e.printStackTrace();
            String errorMessage = e.getMessage();
            String resMessage = "Đã xảy ra lỗi khi thêm xe: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName());
            
            // Kiểm tra nếu lỗi do unique constraint
            if (errorMessage != null) {
                if (errorMessage.contains("vehicle_number") || errorMessage.contains("uk_vehicle_number")) {
                    resMessage = "Biển số xe đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.";
                } else if (errorMessage.contains("vehicle_id") || errorMessage.contains("PRIMARY")) {
                    resMessage = "Mã xe đã tồn tại trong hệ thống. Vui lòng nhập mã xe khác.";
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", resMessage));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi khi thêm xe: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật thông tin xe
     * @param vehicleIdStr ID của xe cần cập nhật
     * @param vehicleData Map chứa thông tin cần cập nhật
     * @return ResponseEntity với Vehicle đã được cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{vehicleIdStr}")
    @Transactional
    public ResponseEntity<?> updateVehicle(
            @PathVariable String vehicleIdStr, 
            @RequestBody Map<String, Object> vehicleData) {
        Long vehicleId;
        try {
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID không hợp lệ hoặc vượt quá giới hạn (overflow)"));
        }

        if (vehicleId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "vehicleId phải lớn hơn hoặc bằng 1"));
        }

        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy xe với ID: " + vehicleId);
            }
            
            Vehicle vehicle = vehicleOpt.get();
            
            // Kiểm tra nếu cố gắng thay đổi group_id
            if (vehicleData.containsKey("groupId")) {
                Object newGroupIdObj = vehicleData.get("groupId");
                if (newGroupIdObj == null) {
                    vehicle.setGroup(null);
                } else {
                    try {
                        Long newGroupId = Long.valueOf(newGroupIdObj.toString());
                        if (newGroupId < 1) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("groupId không hợp lệ (phải >= 1)");
                        }
                        
                        Long currentGroupId = vehicle.getGroup() != null ? vehicle.getGroup().getGroupId() : null;
                        
                        // Nếu group_id thay đổi (hoặc xe chưa có nhóm)
                        if (currentGroupId == null || !newGroupId.equals(currentGroupId)) {
                            // Kiểm tra nhóm mới đã có xe chưa
                            // Restriction removed: groups can now have multiple vehicles

                            
                            // Cập nhật group_id
                            Optional<Vehiclegroup> newGroupOpt = vehicleGroupRepository.findById(newGroupId);
                            if (newGroupOpt.isEmpty()) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body("Không tìm thấy nhóm xe với ID: " + newGroupId);
                            }
                            vehicle.setGroup(newGroupOpt.get());
                        }
                        // Nếu newGroupId == currentGroupId, không cần làm gì thêm (Bỏ qua lỗi chặn nhầm ID 2)
                    } catch (NumberFormatException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("groupId không đúng định dạng số hoặc vượt quá giới hạn");
                    }
                }
            }
            
            // Cập nhật loại xe
            if (vehicleData.containsKey("type") || vehicleData.containsKey("vehicleType")) {
                String type = (String) vehicleData.getOrDefault("type", vehicleData.get("vehicleType"));
                if (type == null || type.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Loại xe không được để trống");
                }
                if (type.length() > 50) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Loại xe không được vượt quá 50 ký tự");
                }
                vehicle.setVehicleType(type.trim());
            }
            
            // Cập nhật biển số xe và kiểm tra trùng lặp
            if (vehicleData.containsKey("vehicleNumber")) {
                String newVehicleNumber = (String) vehicleData.get("vehicleNumber");
                if (newVehicleNumber == null || newVehicleNumber.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Biển số xe không được để trống");
                }
                
                String trimmedNum = newVehicleNumber.trim();
                if (trimmedNum.length() > 20) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Biển số xe không được vượt quá 20 ký tự");
                }

                String currentVehicleNumber = vehicle.getVehicleNumber();
                // Chỉ kiểm tra nếu biển số mới khác biển số hiện tại
                if (currentVehicleNumber == null || !currentVehicleNumber.equals(trimmedNum)) {
                    if (vehicleRepository.existsByVehicleNumberAndVehicleIdNot(trimmedNum, vehicleId)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Biển số xe \"" + trimmedNum + "\" đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.");
                    }
                }
                vehicle.setVehicleNumber(trimmedNum);
            }
            
            // Cập nhật tên xe
            if (vehicleData.containsKey("vehicleName") || vehicleData.containsKey("vehiclename")) {
                String name = (String) vehicleData.getOrDefault("vehicleName", vehicleData.get("vehiclename"));
                if (name == null || name.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên xe không được để trống");
                }
                if (name.length() > 100) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tên xe không được vượt quá 100 ký tự");
                }
                vehicle.setVehicleName(name.trim());
            }
            
            // Cập nhật trạng thái xe
            if (vehicleData.containsKey("status")) {
                String status = (String) vehicleData.get("status");
                if (status == null || status.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái không được để trống");
                }
                String statusLower = status.trim().toLowerCase();
                if (!statusLower.equals("ready") && !statusLower.equals("maintenance")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái không hợp lệ. Chỉ chấp nhận: ready, maintenance");
                }
                vehicle.setStatus(statusLower);
            }
            
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(updatedVehicle);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("vehicle_number") || errorMessage.contains("uk_vehicle_number")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Biển số xe đã tồn tại trong hệ thống.");
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi cập nhật xe: " + e.getMessage());
        }
    }

    /**
     * Xóa xe khỏi nhóm
     * @param vehicleId ID của xe cần xóa
     * @return ResponseEntity với thông báo kết quả
     */
    @DeleteMapping("/{vehicleId}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> deleteVehicle(@PathVariable @Min(1) Long vehicleId) {
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Không tìm thấy xe với ID: " + vehicleId));
            }
            
            Vehicle vehicle = vehicleOpt.get();
            Long groupId = vehicle.getGroup() != null ? vehicle.getGroup().getGroupId() : null;
            
            // Xóa tất cả các bản ghi checkinoutlog liên quan đến xe trước
            // Sử dụng VehicleCleanupService với REQUIRES_NEW để tránh làm hỏng transaction chính
            try {
                vehicleCleanupService.deleteCheckInOutLogs(vehicleId);
            } catch (Exception e) {
                System.err.println("DEBUG: Lỗi khi xóa checkinoutlog liên quan (xe vẫn sẽ được xóa): " + e.getMessage());
            }
            
            // Xóa tất cả các dịch vụ liên quan đến xe trước
            // Sử dụng VehicleCleanupService với REQUIRES_NEW để tránh làm hỏng transaction chính
            try {
                vehicleCleanupService.deleteVehicleServices(vehicleId);
            } catch (Exception e) {
                System.err.println("DEBUG: Lỗi khi xóa dịch vụ liên quan (xe vẫn sẽ được xóa): " + e.getMessage());
            }
            
            // Xóa xe
            vehicleRepository.deleteById(vehicleId);
            vehicleRepository.flush(); // Đảm bảo xóa được thực thi ngay
            
            System.out.println("DEBUG: Đã xóa xe " + vehicleId + " thành công");
            
            return ResponseEntity.ok("Xe đã được xóa thành công");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Không thể xóa xe vì xe đang được sử dụng trong hệ thống. Vui lòng xóa các bản ghi liên quan trước.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi xóa xe: " + e.getMessage());
        }
    }
}

