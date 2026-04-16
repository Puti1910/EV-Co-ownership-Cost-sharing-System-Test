package com.example.VehicleServiceManagementService.controller;

import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.service.ServiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
@Validated
public class ServiceAPI {

    @Autowired
    private ServiceService serviceService;

    /**
     * Lấy tất cả các dịch vụ từ bảng service
     * @return Danh sách tất cả dịch vụ dưới dạng Map
     */
    @GetMapping
    public ResponseEntity<?> getAllServices(
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) Integer size) {
        
        List<ServiceType> services = serviceService.getAllServices();
        System.out.println("✅ API: Đã lấy " + services.size() + " dịch vụ từ bảng service");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceType service : services) {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceId", service.getServiceId());
            map.put("serviceName", service.getServiceName());
            map.put("serviceType", service.getServiceType());
            result.add(map);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy dịch vụ theo ID
     * @param serviceId ID của dịch vụ
     * @return ResponseEntity với ServiceType hoặc thông báo lỗi
     */
    @GetMapping("/{serviceId}")
    public ResponseEntity<?> getServiceById(@PathVariable @Min(1) Long serviceId) {
        try {
            ServiceType service = serviceService.getServiceById(serviceId);
            if (service != null) {
                return ResponseEntity.ok(service);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy dịch vụ với ID: " + serviceId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lấy thông tin dịch vụ: " + e.getMessage());
        }
    }

    /**
     * Thêm dịch vụ mới vào bảng service
     * @param service Dịch vụ cần thêm
     * @return ResponseEntity với ServiceType đã được tạo
     */
    @PostMapping
    public ResponseEntity<?> addService(@Valid @RequestBody ServiceType service) {
        // Nếu có serviceId, kiểm tra đã tồn tại chưa
        if (service.getServiceId() != null) {
            if (serviceService.existsById(service.getServiceId())) {
                throw new com.example.VehicleServiceManagementService.exception.ConflictException("Service ID '" + service.getServiceId() + "' đã tồn tại");
            }
        }

        ServiceType savedService = serviceService.addService(service);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thêm dịch vụ thành công");
        response.put("data", savedService);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật dịch vụ trong bảng service
     * @param serviceId ID của dịch vụ cần cập nhật
     * @param service Dịch vụ với thông tin mới
     * @return ResponseEntity với ServiceType đã được cập nhật hoặc thông báo lỗi
     */
    @PutMapping("/{serviceId}")
    public ResponseEntity<?> updateService(@PathVariable Long serviceId, @Valid @RequestBody ServiceType service) {
        ServiceType updatedService = serviceService.updateService(serviceId, service);
        if (updatedService == null) {
            throw new com.example.VehicleServiceManagementService.exception.ResourceNotFoundException("Không tìm thấy dịch vụ với ID: " + serviceId);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật dịch vụ thành công");
        response.put("data", updatedService);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa dịch vụ khỏi bảng service
     * @param serviceId ID của dịch vụ cần xóa
     * @return ResponseEntity với thông báo kết quả
     */
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<?> deleteService(@PathVariable Long serviceId) {
        boolean deleted = serviceService.deleteService(serviceId);
        if (!deleted) {
            throw new com.example.VehicleServiceManagementService.exception.ResourceNotFoundException("Không tìm thấy dịch vụ với ID: " + serviceId);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dịch vụ đã được xóa thành công");
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra dịch vụ có tồn tại không
     * @param serviceId ID của dịch vụ
     * @return ResponseEntity với kết quả kiểm tra
     */
    @GetMapping("/{serviceId}/exists")
    public ResponseEntity<?> checkServiceExists(@PathVariable @Min(1) Long serviceId) {
        try {
            boolean exists = serviceService.existsById(serviceId);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("serviceId", serviceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi khi kiểm tra dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách các loại dịch vụ duy nhất từ bảng service
     * @return ResponseEntity với danh sách loại dịch vụ
     */
    @GetMapping("/types")
    public ResponseEntity<?> getServiceTypes(
            @RequestParam(required = false) @Size(min = 4, max = 10) String category) {
        try {
            List<String> serviceTypes = serviceService.getDistinctServiceTypes();
            return ResponseEntity.ok(serviceTypes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi khi lấy danh sách loại dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách dịch vụ theo loại
     * @param serviceType Loại dịch vụ
     * @return ResponseEntity với danh sách dịch vụ
     */
    @GetMapping({"/type/{serviceType}", "/type", "/type/"})
    public ResponseEntity<?> getServicesByType(@PathVariable(required = false) String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty() || serviceType.equals(":serviceType")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("serviceType không được để trống"));
        }
        if (serviceType.length() >= 50) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("serviceType không được quá 50 ký tự"));
        }
        try {
            List<ServiceType> services = serviceService.getServicesByType(serviceType);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi khi lấy danh sách dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Đếm tổng số dịch vụ
     * @return ResponseEntity với số lượng dịch vụ
     */
    @GetMapping("/count")
    public ResponseEntity<?> getServiceCount(
            @RequestParam(required = false) @Size(min = 3, max = 15) String status) {
        try {
            long count = serviceService.count();
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi khi đếm số lượng dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Lấy service_id tiếp theo sẽ được generate
     * @return ResponseEntity với service_id tiếp theo
     */
    @GetMapping("/next-id")
    public ResponseEntity<?> getNextServiceId(
            @RequestParam(required = false, defaultValue = "SRV") @Size(min = 2, max = 5) String prefix) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Service ID is now automatically generated (Auto-Increment)");
            response.put("prefix_ignored", prefix);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Đã xảy ra lỗi khi lấy service_id tiếp theo: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách service templates duy nhất từ bảng vehicleservice
     * @return ResponseEntity với danh sách Map từ vehicleservice
     */
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> getServiceTemplatesFromVehicleService(
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(50) Integer limit) {
        try {
            System.out.println("🔵 [ServiceAPI] Nhận request GET /api/services/templates");
            List<ServiceType> templates = serviceService.getDistinctServiceTemplatesFromVehicleService();
            System.out.println("✅ [ServiceAPI] Đã lấy " + templates.size() + " service templates từ bảng vehicleservice");
            
            // Convert ServiceType to Map for consistent response format
            List<Map<String, Object>> result = new ArrayList<>();
            for (ServiceType service : templates) {
                Map<String, Object> map = new HashMap<>();
                map.put("serviceId", service.getServiceId());
                map.put("serviceName", service.getServiceName());
                map.put("serviceType", service.getServiceType());
                result.add(map);
                System.out.println("  - serviceId: " + service.getServiceId() + ", serviceName: " + service.getServiceName());
            }
            
            System.out.println("✅ [ServiceAPI] Trả về " + result.size() + " service templates");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ [ServiceAPI] Lỗi khi lấy service templates từ vehicleservice: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * Tạo response lỗi
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
