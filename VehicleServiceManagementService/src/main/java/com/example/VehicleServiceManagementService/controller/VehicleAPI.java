package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import com.example.VehicleServiceManagementService.repository.VehicleGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleAPI {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleGroupRepository vehicleGroupRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lấy tất cả các xe
     * @return Danh sách tất cả xe
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
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
    public ResponseEntity<?> getVehicleById(@PathVariable String vehicleId) {
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy xe với ID: " + vehicleId);
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
            String groupId = (String) requestData.get("groupId");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vehiclesData = (List<Map<String, Object>>) requestData.get("vehicles");
            
            if (vehiclesData == null || vehiclesData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Danh sách xe không được để trống");
            }
            
            Vehiclegroup group = null;
            
            // Nếu có groupId, kiểm tra và lấy nhóm xe
            if (groupId != null && !groupId.isEmpty()) {
                Optional<Vehiclegroup> groupOpt = vehicleGroupRepository.findById(groupId);
                if (groupOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Không tìm thấy nhóm xe với ID: " + groupId);
                }
                group = groupOpt.get();
                
                // Kiểm tra số lượng xe hiện tại trong nhóm
                long currentVehicleCount = vehicleRepository.countByGroupId(groupId);
                
                // Đảm bảo mỗi nhóm chỉ có đúng 1 xe
                if (currentVehicleCount >= 1) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Nhóm này đã có xe. Mỗi nhóm chỉ được có đúng 1 xe duy nhất.");
                }
                
                if (vehiclesData.size() > 1) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Mỗi nhóm chỉ được có đúng 1 xe. Không thể thêm " + vehiclesData.size() + " xe cùng lúc.");
                }
            }
            
            List<Vehicle> vehicles = new ArrayList<>();
            for (Map<String, Object> vehicleData : vehiclesData) {
                Vehicle vehicle = new Vehicle();
                
                // Kiểm tra và set vehicleId
                String vehicleId = null;
                if (vehicleData.containsKey("vehicleId")) {
                    vehicleId = (String) vehicleData.get("vehicleId");
                    if (vehicleId != null && !vehicleId.trim().isEmpty()) {
                        vehicleId = vehicleId.trim();
                        // Kiểm tra trùng mã xe
                        if (vehicleRepository.existsById(vehicleId)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Mã xe \"" + vehicleId + "\" đã tồn tại trong hệ thống. Vui lòng nhập mã xe khác.");
                        }
                        vehicle.setVehicleId(vehicleId);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Mã xe là bắt buộc và không được để trống.");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Mã xe là bắt buộc.");
                }
                
                if (vehicleData.containsKey("type")) {
                    vehicle.setVehicleType((String) vehicleData.get("type"));
                } else if (vehicleData.containsKey("vehicleType")) {
                    vehicle.setVehicleType((String) vehicleData.get("vehicleType"));
                }
                
                // Kiểm tra biển số xe trùng lặp
                String vehicleNumber = null;
                if (vehicleData.containsKey("vehicleNumber")) {
                    vehicleNumber = (String) vehicleData.get("vehicleNumber");
                    if (vehicleNumber != null && !vehicleNumber.trim().isEmpty()) {
                        // Kiểm tra trùng biển số xe
                        if (vehicleRepository.existsByVehicleNumber(vehicleNumber.trim())) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Biển số xe \"" + vehicleNumber + "\" đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.");
                        }
                        vehicle.setVehicleNumber(vehicleNumber.trim());
                    }
                }
                
                if (vehicleData.containsKey("vehicleName") || vehicleData.containsKey("vehiclename")) {
                    String name = (String) vehicleData.getOrDefault("vehicleName", 
                                                                    vehicleData.get("vehiclename"));
                    vehicle.setVehicleName(name);
                }
                
                if (vehicleData.containsKey("status")) {
                    vehicle.setStatus((String) vehicleData.get("status"));
                } else {
                    vehicle.setStatus("ready"); // Default status: ready (sẵn sàng)
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
            // Kiểm tra nếu lỗi do unique constraint
            if (errorMessage != null) {
                if (errorMessage.contains("vehicle_number") || errorMessage.contains("uk_vehicle_number")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Biển số xe đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.");
                }
                if (errorMessage.contains("vehicle_id") || errorMessage.contains("PRIMARY")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Mã xe đã tồn tại trong hệ thống. Vui lòng nhập mã xe khác.");
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Đã xảy ra lỗi khi thêm xe: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi thêm xe: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin xe
     * @param vehicleId ID của xe cần cập nhật
     * @param vehicleData Map chứa thông tin cần cập nhật
     * @return ResponseEntity với Vehicle đã được cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{vehicleId}")
    @Transactional
    public ResponseEntity<?> updateVehicle(@PathVariable String vehicleId, @RequestBody Map<String, Object> vehicleData) {
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy xe với ID: " + vehicleId);
            }
            
            Vehicle vehicle = vehicleOpt.get();
            
            // Kiểm tra nếu cố gắng thay đổi group_id
            if (vehicleData.containsKey("groupId")) {
                String newGroupId = (String) vehicleData.get("groupId");
                String currentGroupId = vehicle.getGroup() != null ? vehicle.getGroup().getGroupId() : null;
                
                // Nếu newGroupId là null hoặc rỗng, xóa nhóm khỏi xe
                if (newGroupId == null || newGroupId.trim().isEmpty()) {
                    vehicle.setGroup(null);
                } else if (currentGroupId == null || !newGroupId.equals(currentGroupId)) {
                    // Nếu group_id thay đổi (hoặc xe chưa có nhóm)
                    // Kiểm tra nhóm mới đã có xe chưa
                    long vehicleCountInNewGroup = vehicleRepository.countByGroupId(newGroupId);
                    if (vehicleCountInNewGroup >= 1) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Không thể chuyển xe sang nhóm này. Nhóm đã có xe. Mỗi nhóm chỉ được có đúng 1 xe.");
                    }
                    
                    // Cập nhật group_id
                    Optional<Vehiclegroup> newGroupOpt = vehicleGroupRepository.findById(newGroupId);
                    if (newGroupOpt.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Không tìm thấy nhóm xe với ID: " + newGroupId);
                    }
                    vehicle.setGroup(newGroupOpt.get());
                }
            }
            
            // Cập nhật loại xe
            if (vehicleData.containsKey("type")) {
                vehicle.setVehicleType((String) vehicleData.get("type"));
            } else if (vehicleData.containsKey("vehicleType")) {
                vehicle.setVehicleType((String) vehicleData.get("vehicleType"));
            }
            
            // Cập nhật biển số xe và kiểm tra trùng lặp
            if (vehicleData.containsKey("vehicleNumber")) {
                String newVehicleNumber = (String) vehicleData.get("vehicleNumber");
                if (newVehicleNumber != null && !newVehicleNumber.trim().isEmpty()) {
                    String currentVehicleNumber = vehicle.getVehicleNumber();
                    // Chỉ kiểm tra nếu biển số mới khác biển số hiện tại
                    if (currentVehicleNumber == null || !currentVehicleNumber.equals(newVehicleNumber.trim())) {
                        // Kiểm tra trùng biển số xe (trừ xe hiện tại)
                        if (vehicleRepository.existsByVehicleNumberAndVehicleIdNot(newVehicleNumber.trim(), vehicleId)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Biển số xe \"" + newVehicleNumber + "\" đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.");
                        }
                    }
                    vehicle.setVehicleNumber(newVehicleNumber.trim());
                } else {
                    vehicle.setVehicleNumber(null);
                }
            }
            
            // Cập nhật tên xe
            if (vehicleData.containsKey("vehicleName") || vehicleData.containsKey("vehiclename")) {
                String name = (String) vehicleData.getOrDefault("vehicleName", 
                                                                vehicleData.get("vehiclename"));
                vehicle.setVehicleName(name);
            }
            
            // Cập nhật trạng thái xe
            if (vehicleData.containsKey("status")) {
                vehicle.setStatus((String) vehicleData.get("status"));
            }
            
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            System.out.println("DEBUG: Đã cập nhật xe " + vehicleId);
            
            return ResponseEntity.ok(updatedVehicle);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            e.printStackTrace();
            String errorMessage = e.getMessage();
            // Kiểm tra nếu lỗi do unique constraint
            if (errorMessage != null) {
                if (errorMessage.contains("vehicle_number") || errorMessage.contains("uk_vehicle_number")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Biển số xe đã tồn tại trong hệ thống. Vui lòng nhập biển số khác.");
                }
                if (errorMessage.contains("vehicle_id") || errorMessage.contains("PRIMARY")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Mã xe đã tồn tại trong hệ thống. Vui lòng nhập mã xe khác.");
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Đã xảy ra lỗi khi cập nhật xe: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()));
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
    public ResponseEntity<?> deleteVehicle(@PathVariable String vehicleId) {
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy xe với ID: " + vehicleId);
            }
            
            Vehicle vehicle = vehicleOpt.get();
            String groupId = vehicle.getGroup() != null ? vehicle.getGroup().getGroupId() : null;
            
            // Xóa tất cả các bản ghi checkinoutlog liên quan đến xe trước
            // (nằm trong database legal_contract)
            // Lưu ý: vehicle_id trong checkinoutlog là INT, cần convert vehicleId (String) sang INT
            try {
                int deletedCheckInOutLogs = entityManager.createNativeQuery(
                    "DELETE FROM legal_contract.checkinoutlog WHERE vehicle_id = CAST(:vehicleId AS UNSIGNED)"
                ).setParameter("vehicleId", vehicleId).executeUpdate();
                System.out.println("DEBUG: Đã xóa " + deletedCheckInOutLogs + " bản ghi checkinoutlog liên quan đến xe " + vehicleId);
            } catch (Exception e) {
                System.err.println("DEBUG: Lỗi khi xóa checkinoutlog liên quan: " + e.getMessage());
                // Tiếp tục xóa các dữ liệu khác dù có lỗi khi xóa checkinoutlog
            }
            
            // Xóa tất cả các dịch vụ liên quan đến xe trước
            // Với id làm primary key, có thể xóa bằng cách xóa theo vehicle_id
            // Lưu ý: vehicle_id trong vehicleservice là INT, cần convert vehicleId (String) sang INT
            try {
                int deletedServices = entityManager.createNativeQuery(
                    "DELETE FROM vehicle_management.vehicleservice WHERE vehicle_id = CAST(:vehicleId AS UNSIGNED)"
                ).setParameter("vehicleId", vehicleId).executeUpdate();
                System.out.println("DEBUG: Đã xóa " + deletedServices + " dịch vụ liên quan đến xe " + vehicleId);
            } catch (Exception e) {
                System.err.println("DEBUG: Lỗi khi xóa dịch vụ liên quan: " + e.getMessage());
                // Tiếp tục xóa xe dù có lỗi khi xóa dịch vụ
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

