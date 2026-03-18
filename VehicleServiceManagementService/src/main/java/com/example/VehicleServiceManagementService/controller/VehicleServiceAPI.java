package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.dto.MaintenanceBookingRequest;
import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;
import com.example.VehicleServiceManagementService.service.VehicleServiceService;
import com.example.VehicleServiceManagementService.service.MaintenanceBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicle-services")
@CrossOrigin(origins = "*")
public class VehicleServiceAPI {

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
    public ResponseEntity<?> testEndpoint() {
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
    public ResponseEntity<List<Map<String, Object>>> getAllVehicleServices() {
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
    @GetMapping("/service/{serviceId}/vehicle/{vehicleId}")
    public ResponseEntity<?> getVehicleServiceByServiceAndVehicle(
            @PathVariable String serviceId,
            @PathVariable String vehicleId) {
        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
            if (serviceOpt.isPresent()) {
                Map<String, Object> response = convertToMap(serviceOpt.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lấy thông tin dịch vụ: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách dịch vụ của một xe
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Map<String, Object>>> getVehicleServicesByVehicleId(@PathVariable String vehicleId) {
        try {
            System.out.println("🔵 [GET] /api/vehicleservices/vehicle/" + vehicleId);
            
            List<Map<String, Object>> result = vehicleServiceRepository.findByVehicle_VehicleId(vehicleId).stream()
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

    /**
     * Đăng ký dịch vụ xe mới
     */
    @PostMapping
    public ResponseEntity<?> registerVehicleService(@RequestBody Map<String, Object> requestData) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔵 [REGISTER SERVICE] Bắt đầu xử lý đăng ký dịch vụ");
        System.out.println("📥 Request data: " + requestData);
        
        try {
            // Validation
            String serviceId = (String) requestData.get("serviceId");
            if (serviceId == null || serviceId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("serviceId là bắt buộc");
            }

            String vehicleId = (String) requestData.get("vehicleId");
            if (vehicleId == null || vehicleId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("vehicleId là bắt buộc");
            }

            // Validate và lấy service, vehicle
            ServiceType service;
            Vehicle vehicle;
            try {
                service = vehicleServiceService.validateAndGetService(serviceId);
                vehicle = vehicleServiceService.validateAndGetVehicle(vehicleId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }

            // KIỂM TRA DUPLICATE TRƯỚC KHI TẠO ENTITY
            // Chỉ chặn nếu có dịch vụ đang chờ (pending/in_progress) chưa completed
            System.out.println("   🔍 [CHECK DUPLICATE] Kiểm tra dịch vụ đang chờ...");
            System.out.println("   - serviceId: " + serviceId);
            System.out.println("   - vehicleId: " + vehicleId);
            
            // Kiểm tra xem có dịch vụ đang chờ (pending/in_progress) không
            long activeCount = vehicleServiceRepository.countActiveByServiceIdAndVehicleId(serviceId, vehicleId);
            if (activeCount > 0) {
                System.err.println("   ⚠️ [ACTIVE SERVICE] Đã tồn tại " + activeCount + " dịch vụ đang chờ với serviceId=" + serviceId + " và vehicleId=" + vehicleId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Dịch vụ này đã được đăng ký cho xe này và đang trong trạng thái chờ xử lý. Vui lòng hoàn thành dịch vụ trước đó hoặc hủy đăng ký cũ.");
            }
            
            // Với id làm primary key, có thể đăng ký nhiều lần
            // Chỉ kiểm tra xem có dịch vụ đang chờ (pending/in_progress) không
            // Nếu có dịch vụ completed, vẫn cho phép đăng ký lại
            
            System.out.println("   ✅ [NO CONFLICT] Không có conflict, tiếp tục tạo entity...");

            // Tạo entity
            String serviceDescription = (String) requestData.get("serviceDescription");
            String status = (String) requestData.get("status");
            
            Integer groupRefId = toInteger(requestData.getOrDefault("groupRefId", requestData.get("groupId")));
            Integer requestedByUserId = toInteger(requestData.getOrDefault("requestedByUserId", requestData.get("userId")));
            String requestedByName = toStringValue(requestData.getOrDefault("requestedByUserName", requestData.get("requestedByName")));
            LocalDateTime preferredStart = parseDateTime(requestData.get("preferredStartDatetime"));
            LocalDateTime preferredEnd = parseDateTime(requestData.get("preferredEndDatetime"));

            Vehicleservice vehicleService = vehicleServiceService.createVehicleService(
                service,
                vehicle,
                serviceDescription,
                status,
                groupRefId,
                requestedByUserId,
                requestedByName,
                preferredStart,
                preferredEnd
            );

            // Lưu vào database
            Vehicleservice savedService = vehicleServiceService.saveVehicleService(vehicleService);
                
            System.out.println("✅ [SUCCESS] Đã đăng ký dịch vụ thành công!");
            System.out.println("   - Service ID: " + savedService.getServiceId());
            System.out.println("   - Vehicle ID: " + savedService.getVehicleId());
            System.out.println("═══════════════════════════════════════════════════════");
            
            // Convert sang Map để trả về
            Map<String, Object> response = convertToMap(savedService);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                
        } catch (IllegalArgumentException e) {
            System.err.println("❌ [VALIDATION ERROR] " + e.getMessage());
            String errorMessage = e.getMessage();
            // Kiểm tra nếu là lỗi duplicate
            if (errorMessage.contains("đã được đăng ký") || errorMessage.contains("trùng lặp")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
                
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            System.err.println("❌ [DATABASE ERROR] " + e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Lỗi ràng buộc dữ liệu: " + (e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage()));
            
        } catch (Exception e) {
            System.err.println("❌ [ERROR] " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi đăng ký dịch vụ: " + e.getMessage());
        }
    }

    /**
     * Danh sách nhóm/xe người dùng có thể đặt bảo dưỡng
     */
    @GetMapping("/maintenance/options")
    public ResponseEntity<?> getMaintenanceOptions(@RequestParam Integer userId) {
        try {
            List<Map<String, Object>> options = maintenanceBookingService.getUserMaintenanceOptions(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "options", options
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Đặt lịch bảo dưỡng cho xe
     */
    @PostMapping("/maintenance/book")
    public ResponseEntity<?> bookMaintenance(@RequestBody MaintenanceBookingRequest request) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔵 [BOOK MAINTENANCE] Nhận request đặt dịch vụ bảo dưỡng");
        System.out.println("📥 Request - userId: " + request.getUserId() + ", groupId: " + request.getGroupId() + 
                          ", vehicleId: " + request.getVehicleId() + ", serviceId: " + request.getServiceId());
        try {
            Map<String, Object> result = maintenanceBookingService.bookMaintenance(request);
            System.out.println("✅ [BOOK MAINTENANCE] Thành công!");
            System.out.println("═══════════════════════════════════════════════════════");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ [BOOK MAINTENANCE] Lỗi validation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("❌ [BOOK MAINTENANCE] Lỗi không xác định: " + e.getMessage());
            System.err.println("   Error Type: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Cập nhật đăng ký dịch vụ theo serviceId + vehicleId
     */
    @PutMapping("/service/{serviceId}/vehicle/{vehicleId}")
    public ResponseEntity<?> updateVehicleService(
            @PathVariable String serviceId,
            @PathVariable String vehicleId,
            @RequestBody Map<String, Object> requestData) {
        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
            if (serviceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId);
            }

            Vehicleservice service = serviceOpt.get();
            
            if (requestData.containsKey("serviceDescription")) {
                service.setServiceDescription((String) requestData.get("serviceDescription"));
            }
            
            if (requestData.containsKey("serviceType")) {
                service.setServiceType((String) requestData.get("serviceType"));
            }
            
            if (requestData.containsKey("status")) {
                String newStatus = (String) requestData.get("status");
                String oldStatus = service.getStatus();
                service.setStatus(newStatus);
                
                // Tự động set completionDate khi status = completed
                if (newStatus != null && newStatus.equalsIgnoreCase("completed")) {
                    if (service.getCompletionDate() == null) {
                        service.setCompletionDate(Instant.now());
                        System.out.println("✅ Tự động set completionDate = " + Instant.now());
                    }
                } else if (newStatus != null && (newStatus.equalsIgnoreCase("pending") || newStatus.equalsIgnoreCase("in_progress") || newStatus.equalsIgnoreCase("in progress"))) {
                    // Reset completionDate nếu chuyển về pending/in_progress
                    service.setCompletionDate(null);
                }
                
                // Đồng bộ trạng thái vehicle sau khi cập nhật status của vehicleservice
                String vehicleIdFromEntity = service.getVehicleId();
                if (vehicleIdFromEntity != null && (oldStatus == null || !oldStatus.equalsIgnoreCase(newStatus))) {
                    try {
                        System.out.println("🔄 [UPDATE STATUS] Đồng bộ vehicle status sau khi cập nhật vehicleservice status");
                        vehicleServiceService.syncVehicleStatus(vehicleIdFromEntity);
                    } catch (Exception e) {
                        System.err.println("⚠️ [SYNC WARNING] Lỗi khi đồng bộ vehicle status: " + e.getMessage());
                        // Không throw exception để không ảnh hưởng đến việc cập nhật vehicleservice
                    }
                }
            }
            
            if (requestData.containsKey("completionDate")) {
                String completionDateStr = (String) requestData.get("completionDate");
                if (completionDateStr != null && !completionDateStr.isEmpty()) {
                    service.setCompletionDate(Instant.parse(completionDateStr));
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
                    .body("Đã xảy ra lỗi khi cập nhật dịch vụ: " + e.getMessage());
        }
    }
    
    /**
     * Xóa đăng ký dịch vụ theo service_id và vehicle_id (xóa tất cả)
     */
    @DeleteMapping("/service/{serviceId}/vehicle/{vehicleId}")
    public ResponseEntity<?> deleteVehicleServiceByServiceAndVehicle(
            @PathVariable String serviceId,
            @PathVariable String vehicleId) {
        try {
            long count = vehicleServiceRepository.countByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
            if (count > 0) {
                vehicleServiceRepository.deleteByIdServiceIdAndIdVehicleId(serviceId, vehicleId);
                
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
    
        /**
         * Đồng bộ trạng thái vehicle dựa trên vehicleservice
         * @param vehicleId ID của vehicle cần đồng bộ
         * @return Response với kết quả đồng bộ
         */
        @PostMapping("/sync-vehicle-status/{vehicleId}")
        public ResponseEntity<?> syncVehicleStatus(@PathVariable String vehicleId) {
            try {
                System.out.println("🔄 [API] Đồng bộ trạng thái vehicle: " + vehicleId);
                vehicleServiceService.syncVehicleStatus(vehicleId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã đồng bộ trạng thái vehicle thành công",
                    "vehicleId", vehicleId
                ));
            } catch (Exception e) {
                System.err.println("❌ [API] Lỗi khi đồng bộ trạng thái vehicle: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "message", "Đã xảy ra lỗi khi đồng bộ trạng thái: " + e.getMessage()
                        ));
            }
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
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    }
