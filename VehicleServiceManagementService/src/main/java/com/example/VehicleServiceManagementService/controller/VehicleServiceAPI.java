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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllVehicleServices(
            @RequestParam(required = false, defaultValue = "0") @Min(0) @Max(1000) Integer page) {
        try {
            List<Vehicleservice> services = vehicleServiceRepository.findAll();
            List<Map<String, Object>> result = services.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
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
                    .body(Map.of("success", false, "message", "ID không đúng định dạng"));
        }

        if (serviceId < 1 || vehicleId < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID phải lớn hơn hoặc bằng 1"));
        }

        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(serviceId, vehicleId);
            if (serviceOpt.isPresent()) {
                Map<String, Object> response = convertToMap(serviceOpt.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Không tìm thấy đăng ký dịch vụ"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách dịch vụ của một xe
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<Map<String, Object>>> getVehicleServicesByVehicleId(@PathVariable @Min(1) Long vehicleId) {
        try {
            if (!vehicleServiceService.existsVehicleById(vehicleId)) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
            }
            
            List<Map<String, Object>> result = vehicleServiceRepository.findByVehicleId(vehicleId).stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * Đăng ký dịch vụ xe mới
     */
    @PostMapping
    public ResponseEntity<?> registerVehicleService(@Valid @RequestBody VehicleServiceRequest request) {
        log.info("🔵 [REGISTER SERVICE] Bắt đầu xử lý đăng ký dịch vụ: {}", request);
        
        Long serviceId = request.getServiceId();
        Long vehicleId = request.getVehicleId();
        Long requestedByUserId = request.getRequestedByUserId();
        String status = request.getStatus();
        String requestedByName = request.getRequestedByUserName();

        // Validate User existence
        vehicleServiceService.validateAndGetUser(requestedByUserId);

        if (status != null && !status.trim().isEmpty()) {
            String statusLower = status.trim().toLowerCase();
            if (!statusLower.equals("pending") && !statusLower.equals("in_progress") &&
                !statusLower.equals("in progress") && !statusLower.equals("completed")) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận: pending, in_progress, completed");
            }
        }

        ServiceType service = vehicleServiceService.validateAndGetService(serviceId);
        Vehicle vehicle = vehicleServiceService.validateAndGetVehicle(vehicleId);

        long activeCount = vehicleServiceRepository.countActiveByServiceIdAndVehicleId(serviceId, vehicleId);
        if (activeCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "Dịch vụ đang trong trạng thái chờ xử lý."));
        }

        LocalDateTime preferredStart = parseDateTime(request.getPreferredStartDatetime());
        LocalDateTime preferredEnd = parseDateTime(request.getPreferredEndDatetime());

        Vehicleservice vehicleService = vehicleServiceService.createVehicleService(
            service,
            vehicle,
            request.getServiceDescription(),
            status,
            request.getGroupRefId() != null ? request.getGroupRefId().intValue() : null,
            requestedByUserId.intValue(),
            requestedByName,
            preferredStart,
            preferredEnd
        );

        Vehicleservice savedService = vehicleServiceService.saveVehicleService(vehicleService);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToMap(savedService));
    }

    /**
     * Danh sách nhóm/xe người dùng có thể đặt bảo dưỡng
     */
    @GetMapping("/maintenance/options")
    public ResponseEntity<?> getMaintenanceOptions(@RequestParam @Min(1) Long userId) {
        try {
            List<Map<String, Object>> options = maintenanceBookingService.getUserMaintenanceOptions(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "options", options
            ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy maintenance options: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/maintenance/book")
    public ResponseEntity<?> bookMaintenance(@Valid @RequestBody MaintenanceBookingRequest request) {
        Map<String, Object> result = maintenanceBookingService.bookMaintenance(request);
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
                    .body(Map.of("success", false, "message", "ID không hợp lệ"));
        }

        try {
            Optional<Vehicleservice> serviceOpt = vehicleServiceRepository.findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(serviceId, vehicleId);
            if (serviceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Không tìm thấy đăng ký dịch vụ"));
            }

            Vehicleservice service = serviceOpt.get();
            
            if (requestData.containsKey("serviceDescription")) {
                service.setServiceDescription((String) requestData.get("serviceDescription"));
            }
            
            if (requestData.containsKey("status")) {
                String newStatus = (String) requestData.get("status");
                if (newStatus != null) {
                    String statusLower = newStatus.trim().toLowerCase();
                    String oldStatus = service.getStatus();
                    service.setStatus(statusLower);
                    
                    if (statusLower.equals("completed")) {
                        if (service.getCompletionDate() == null) {
                            service.setCompletionDate(Instant.now());
                        }
                    } else {
                        service.setCompletionDate(null);
                    }
                    
                    if (oldStatus == null || !oldStatus.equalsIgnoreCase(statusLower)) {
                        try {
                            vehicleServiceService.syncVehicleStatus(vehicleId);
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            if (requestData.containsKey("completionDate")) {
                String completionDateStr = (String) requestData.get("completionDate");
                if (completionDateStr != null && !completionDateStr.isEmpty()) {
                    try {
                        service.setCompletionDate(Instant.parse(completionDateStr));
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("success", false, "message", " completionDate không hợp lệ"));
                    }
                } else {
                    service.setCompletionDate(null);
                }
            }

            Vehicleservice updatedService = vehicleServiceRepository.save(service);
            return ResponseEntity.ok(convertToMap(updatedService));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Xóa đăng ký dịch vụ theo service_id và vehicle_id
     */
    @DeleteMapping("/service/{serviceId}/vehicle/{vehicleId}")
    public ResponseEntity<?> deleteVehicleServiceByServiceAndVehicle(
            @PathVariable @Min(1) Long serviceId,
            @PathVariable @Min(1) Long vehicleId) {
        try {
            long count = vehicleServiceRepository.countActiveByServiceIdAndVehicleId(serviceId, vehicleId);
            if (count > 0) {
                vehicleServiceRepository.deleteByServiceIdAndVehicleId(serviceId, vehicleId);
                try {
                    vehicleServiceService.syncVehicleStatus(vehicleId);
                } catch (Exception ignored) {}
                
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa thành công"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }
    
    @PostMapping("/sync-vehicle-status/{vehicleIdStr}")
    public ResponseEntity<?> syncVehicleStatus(@PathVariable String vehicleIdStr) {
        Long vehicleId;
        try {
            vehicleId = Long.parseLong(vehicleIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "ID không hợp lệ"));
        }
        
        vehicleServiceService.validateAndGetVehicle(vehicleId);
        vehicleServiceService.syncVehicleStatus(vehicleId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã đồng bộ trạng thái xe", "vehicleId", vehicleId));
    }
    
    @PostMapping("/sync-all-vehicle-statuses")
    public ResponseEntity<?> syncAllVehicleStatuses() {
        try {
            vehicleServiceService.syncAllVehicleStatuses();
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã đồng bộ tất cả"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToMap(Vehicleservice vs) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vs.getId());
        map.put("serviceId", vs.getServiceId());
        map.put("vehicleId", vs.getVehicleId());
        map.put("serviceName", vs.getServiceName());
        map.put("serviceDescription", vs.getServiceDescription());
        map.put("serviceType", vs.getServiceType());
        map.put("status", vs.getStatus());
        map.put("groupId", vs.getGroupRefId());
        map.put("requestedByUserId", vs.getRequestedByUserId());
        map.put("requestedByUserName", vs.getRequestedByUserName());
        if (vs.getRequestDate() != null) map.put("requestDate", vs.getRequestDate().toString());
        if (vs.getCompletionDate() != null) map.put("completionDate", vs.getCompletionDate().toString());
        if (vs.getPreferredStartDatetime() != null) map.put("preferredStartDatetime", vs.getPreferredStartDatetime().toString());
        if (vs.getPreferredEndDatetime() != null) map.put("preferredEndDatetime", vs.getPreferredEndDatetime().toString());
        return map;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(value); } catch (Exception e) { return null; }
    }
}
