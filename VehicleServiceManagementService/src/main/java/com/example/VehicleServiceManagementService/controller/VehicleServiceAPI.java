package com.example.VehicleServiceManagementService.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.VehicleServiceManagementService.dto.MaintenanceBookingRequest;
import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;
import com.example.VehicleServiceManagementService.service.VehicleServiceService;
import com.example.VehicleServiceManagementService.service.MaintenanceBookingService;
import com.example.VehicleServiceManagementService.dto.VehicleServiceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicle-services")
@CrossOrigin(origins = "*")
@Validated
public class VehicleServiceAPI {
    private static final Logger log = LoggerFactory.getLogger(VehicleServiceAPI.class);

    @Autowired
    private VehicleServiceRepository vehicleServiceRepository;

    @Autowired
    private VehicleServiceService vehicleServiceService;

    @Autowired
    private MaintenanceBookingService maintenanceBookingService;

    /**
     * Test endpoint để kiểm tra controller hoạt động
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(
            @RequestParam(required = false, defaultValue = "3") @Min(1) @Max(5) Integer retry) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "VehicleServiceAPI controller đang hoạt động",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Lấy tất cả các đăng ký dịch vụ xe
     * Sử dụng native query để đảm bảo lấy được dữ liệu
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllVehicleServices(
            @RequestParam(required = false, defaultValue = "0") @Min(0) @Max(1000) Integer page) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔵 [GET] /api/vehicleservices - Lấy tất cả đăng ký dịch vụ");
        
        try {
            List<Vehicleservice> services = vehicleServiceRepository.findAll();
            System.out.println("✅ Repository trả về " + services.size() + " records");
            
            List<Map<String, Object>> result = services.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Trả về " + result.size() + " services cho client");
            System.out.println("═══════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lấy danh sách: " + e.getMessage());
            System.err.println("   Error Type: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }
    
    /**
     * Lấy đăng ký dịch vụ theo service_id và vehicle_id (lấy bản ghi mới nhất)
     */
    @GetMapping("/service/{serviceIdStr}/vehicle/{vehicleIdStr}")
    public ResponseEntity<?> getVehicleServiceByServiceAndVehicle(
            @PathVariable String serviceIdStr,
            @PathVariable String vehicleIdStr) {
        Long serviceId;
        Long vehicleId;
        
        try {
            serviceId = Long.parseLong(serviceIdStr);
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID không đúng định dạng hoặc vượt quá giới hạn số (overflow)"));
        }

        if (serviceId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "serviceId không hợp lệ (phải >= 1)"));
        }
        if (vehicleId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "vehicleId không hợp lệ (phải >= 1)"));
        }
        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
            if (serviceOpt.isPresent()) {
                Map<String, Object> response = convertToMap(serviceOpt.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi khi lấy thông tin dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách dịch vụ của một xe
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Map<String, Object>>> getVehicleServicesByVehicleId(@PathVariable @Min(1) Long vehicleId) {
        try {
            System.out.println("🔵 [GET] /api/vehicleservices/vehicle/" + vehicleId);
            
            // Kiểm tra tồn tại của vehicle
            if (!vehicleServiceService.existsVehicleById(vehicleId)) {
                 System.err.println("❌ Không tìm thấy xe với ID: " + vehicleId);
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }
            
            List<Map<String, Object>> result = vehicleServiceRepository.findByVehicleId(vehicleId).stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Trả về " + result.size() + " services cho vehicle " + vehicleId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lấy danh sách dịch vụ: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }

    @PostMapping
    public ResponseEntity<?> registerVehicleService(@Valid @RequestBody VehicleServiceRequest request) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔵 [REGISTER SERVICE] Bắt đầu xử lý đăng ký dịch vụ");
        System.out.println("📥 Request data: " + request);
        
        Long serviceId = request.getServiceId();
        Long vehicleId = request.getVehicleId();
        Long requestedByUserId = request.getRequestedByUserId();
        String status = request.getStatus();
        String requestedByName = request.getRequestedByUserName();

        // Validate User existence
        vehicleServiceService.validateAndGetUser(requestedByUserId);

        // status custom validation (có thể đưa vào annotation @Pattern sau này)
        if (status != null && !status.trim().isEmpty()) {
            String statusLower = status.trim().toLowerCase();
            if (!statusLower.equals("pending") && !statusLower.equals("in_progress") &&
                !statusLower.equals("in progress") && !statusLower.equals("completed")) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận: pending, in_progress, completed");
            }
        }

        // Validate và lấy service, vehicle
        ServiceType service = vehicleServiceService.validateAndGetService(serviceId);
        Vehicle vehicle = vehicleServiceService.validateAndGetVehicle(vehicleId);

        // KIỂM TRA DUPLICATE
        long activeCount = vehicleServiceRepository.countActiveByServiceIdAndVehicleId(serviceId, vehicleId);
        if (activeCount > 0) {
            throw new com.example.VehicleServiceManagementService.exception.ConflictException("Dịch vụ này đã được đăng ký cho xe này và đang trong trạng thái chờ xử lý.");
        }

        // Tạo entity
        LocalDateTime preferredStart = parseDateTime(request.getPreferredStartDatetime());
        LocalDateTime preferredEnd = parseDateTime(request.getPreferredEndDatetime());

        Vehicleservice vehicleService = vehicleServiceService.createVehicleService(
            service,
            vehicle,
            request.getServiceDescription(),
            status,
            request.getGroupRefId(),
            requestedByUserId,
            requestedByName,
            preferredStart,
            preferredEnd
        );

        Vehicleservice savedService = vehicleServiceService.saveVehicleService(vehicleService);
            
        System.out.println("✅ [SUCCESS] Đã đăng ký dịch vụ thành công!");
        Map<String, Object> response = convertToMap(savedService);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Danh sách nhóm/xe người dùng có thể đặt bảo dưỡng
     */
    @GetMapping("/maintenance/options")
    public ResponseEntity<?> getMaintenanceOptions(@RequestParam @Min(1) Long userId) {
        if (userId == null || userId < 1) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "userId không hợp lệ"
            ));
        }
        try {
            List<Map<String, Object>> options = maintenanceBookingService.getUserMaintenanceOptions(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "options", options
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách tùy chọn bảo dưỡng cho user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Đã xảy ra lỗi hệ thống khi kết nối với dịch vụ quản lý nhóm",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/maintenance/book")
    public ResponseEntity<?> bookMaintenance(@Valid @RequestBody MaintenanceBookingRequest request) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔵 [BOOK MAINTENANCE] Nhận request đặt dịch vụ bảo dưỡng");
        System.out.println("📥 Request - userId: " + request.getUserId() + ", groupId: " + request.getGroupId() + 
                          ", vehicleId: " + request.getVehicleId() + ", serviceId: " + request.getServiceId());
        Map<String, Object> result = maintenanceBookingService.bookMaintenance(request);
        System.out.println("✅ [BOOK MAINTENANCE] Thành công!");
        System.out.println("═══════════════════════════════════════════════════════");
        return ResponseEntity.ok(result);
    }

    /**
     * Cập nhật đăng ký dịch vụ theo serviceId + vehicleId
     */
    @PutMapping("/service/{serviceIdStr}/vehicle/{vehicleIdStr}")
    public ResponseEntity<?> updateVehicleService(
            @PathVariable String serviceIdStr,
            @PathVariable String vehicleIdStr,
            @RequestBody Map<String, Object> requestData) {
        Long serviceId;
        Long vehicleId;
        try {
            serviceId = Long.parseLong(serviceIdStr);
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID không hợp lệ hoặc vượt quá giới hạn (overflow)"));
        }

        if (serviceId < 1 || vehicleId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID phải lớn hơn hoặc bằng 1"));
        }

        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
            if (serviceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId));
            }

            Vehicleservice service = serviceOpt.get();
            
            if (requestData.containsKey("serviceDescription")) {
                String desc = (String) requestData.get("serviceDescription");
                if (desc != null && desc.length() > 65535) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Mô tả dịch vụ quá dài (tối đa 65535 ký tự)"));
                }
                service.setServiceDescription(desc);
            }
            
            if (requestData.containsKey("serviceType")) {
                String type = (String) requestData.get("serviceType");
                if (type == null || type.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "serviceType không được để trống"));
                }
                if (type.length() > 50) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "serviceType không được vượt quá 50 ký tự"));
                }
                service.setServiceType(type);
            }
            
            if (requestData.containsKey("status")) {
                String newStatus = (String) requestData.get("status");
                if (newStatus == null || newStatus.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Trạng thái không được để trống"));
                }
                
                String statusLower = newStatus.trim().toLowerCase();
                if (!statusLower.equals("pending") && !statusLower.equals("in_progress") && 
                    !statusLower.equals("in progress") && !statusLower.equals("completed")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Trạng thái không hợp lệ. Chỉ chấp nhận: pending, in_progress, completed"));
                }

                String oldStatus = service.getStatus();
                service.setStatus(statusLower);
                
                // Tự động set completionDate khi status = completed
                if (statusLower.equals("completed")) {
                    if (service.getCompletionDate() == null) {
                        service.setCompletionDate(Instant.now());
                    }
                } else {
                    // Reset completionDate nếu chuyển về pending/in_progress
                    service.setCompletionDate(null);
                }
                
                // Đồng bộ trạng thái vehicle
                Long vehicleIdFromEntity = service.getVehicleId();
                if (vehicleIdFromEntity != null && (oldStatus == null || !oldStatus.equalsIgnoreCase(statusLower))) {
                    try {
                        vehicleServiceService.syncVehicleStatus(vehicleIdFromEntity);
                    } catch (Exception ignored) {}
                }
            }
            
            if (requestData.containsKey("completionDate")) {
                String completionDateStr = (String) requestData.get("completionDate");
                if (completionDateStr != null && !completionDateStr.isEmpty()) {
                    try {
                        Instant instant = Instant.parse(completionDateStr);
                        // Kiểm tra tầm vực của MySQL DATETIME (1000-01-01 to 9999-12-31)
                        // Khoảng 253402300799s là 9999-12-31T23:59:59Z
                        if (instant.getEpochSecond() > 253402300799L || instant.getEpochSecond() < -30610224000L) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("success", false, "message", "Năm của completionDate phải nằm trong khoảng từ 1000 đến 9999"));
                        }
                        service.setCompletionDate(instant);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", "Định dạng completionDate không hợp lệ (Expect ISO 8601, e.g. 2024-04-13T00:00:00Z)"));
                    }
                } else {
                    service.setCompletionDate(null);
                }
            }

            Vehicleservice updatedService = vehicleServiceRepository.save(service);
            Map<String, Object> response = convertToMap(updatedService);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi khi cập nhật dịch vụ: " + e.getMessage()));
        }
    }
    
    /**
     * Xóa đăng ký dịch vụ theo service_id và vehicle_id (xóa tất cả)
     */
    @DeleteMapping("/service/{serviceId}/vehicle/{vehicleId}")
    public ResponseEntity<?> deleteVehicleServiceByServiceAndVehicle(
            @PathVariable @Min(1) Long serviceId,
            @PathVariable @Min(1) Long vehicleId) {
        try {
            long count = vehicleServiceRepository.countByServiceIdAndVehicleId(serviceId, vehicleId);
            if (count > 0) {
                vehicleServiceRepository.deleteByServiceIdAndVehicleId(serviceId, vehicleId);
                
                // Đồng bộ trạng thái vehicle sau khi xóa vehicleservice
                if (vehicleId != null) {
                    try {
                        System.out.println("🔄 [DELETE] Đồng bộ vehicle status sau khi xóa vehicleservice");
                        vehicleServiceService.syncVehicleStatus(vehicleId);
                    } catch (Exception e) {
                        System.err.println("⚠️ [SYNC WARNING] Lỗi khi đồng bộ vehicle status: " + e.getMessage());
                        // Không throw exception để không ảnh hưởng đến việc xóa vehicleservice
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Đã xóa " + count + " đăng ký dịch vụ thành công");
                response.put("deletedCount", count);
                return ResponseEntity.ok(response);
            }
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Đã xảy ra lỗi khi xóa dịch vụ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/sync-vehicle-status/{vehicleIdStr}")
    public ResponseEntity<?> syncVehicleStatus(@PathVariable String vehicleIdStr) {
        Long vehicleId;
        try {
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID không hợp lệ hoặc vượt quá giới hạn (overflow)"));
        }
        if (vehicleId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "vehicleId không hợp lệ (phải >= 1)"));
        }
        
        // validateAndGetVehicle sẽ ném ResourceNotFoundException (404) nếu không tìm thấy xe
        vehicleServiceService.validateAndGetVehicle(vehicleId);
        
        System.out.println("🔄 [API] Đồng bộ trạng thái vehicle: " + vehicleId);
        vehicleServiceService.syncVehicleStatus(vehicleId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã đồng bộ trạng thái vehicle thành công",
            "vehicleId", vehicleId
        ));
    }
        
        /**
         * Đồng bộ trạng thái cho tất cả vehicles
         * @return Response với kết quả đồng bộ
         */
        @PostMapping("/sync-all-vehicle-statuses")
        public ResponseEntity<?> syncAllVehicleStatuses() {
            try {
                System.out.println("🔄 [API] Đồng bộ trạng thái cho tất cả vehicles...");
                vehicleServiceService.syncAllVehicleStatuses();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã đồng bộ trạng thái cho tất cả vehicles thành công"
                ));
            } catch (Exception e) {
                System.err.println("❌ [API] Lỗi khi đồng bộ trạng thái: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "message", "Đã xảy ra lỗi khi đồng bộ trạng thái: " + e.getMessage()
                        ));
            }
        }
        
        /**
         * Helper method để convert Vehicleservice entity sang Map
         */
        private Map<String, Object> convertToMap(Vehicleservice vs) {
            Map<String, Object> map = new HashMap<>();
            
            // Primary key
            map.put("id", vs.getId());
            
            // Other fields
            map.put("serviceId", vs.getServiceId());
            map.put("vehicleId", vs.getVehicleId());
            map.put("serviceName", vs.getServiceName());
            map.put("serviceDescription", vs.getServiceDescription());
            map.put("serviceType", vs.getServiceType());
            map.put("status", vs.getStatus());
        map.put("groupId", vs.getGroupRefId());
        map.put("requestedByUserId", vs.getRequestedByUserId());
        map.put("requestedByUserName", vs.getRequestedByUserName());
            
            if (vs.getRequestDate() != null) {
                map.put("requestDate", vs.getRequestDate().toString());
            }
            if (vs.getCompletionDate() != null) {
                map.put("completionDate", vs.getCompletionDate().toString());
            }
        if (vs.getPreferredStartDatetime() != null) {
            map.put("preferredStartDatetime", vs.getPreferredStartDatetime().toString());
        }
        if (vs.getPreferredEndDatetime() != null) {
            map.put("preferredEndDatetime", vs.getPreferredEndDatetime().toString());
        }
            
            return map;
        }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return LocalDateTime.parse(str);
            } catch (Exception e) {
                throw new IllegalArgumentException("Định dạng ngày tháng không hợp lệ (Yêu cầu ISO format, ví dụ: 2026-04-14T10:00:00Z)");
            }
        }
        return null;
    }
    }
