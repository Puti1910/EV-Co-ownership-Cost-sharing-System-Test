package com.example.ui_service.controller.admin;

import com.example.ui_service.external.model.VehicleDTO;
import com.example.ui_service.external.service.ServiceRestClient;
import com.example.ui_service.external.service.VehicleRestClient;
import com.example.ui_service.external.service.VehicleServiceRestClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/admin/vehicle-services")
public class AdminVehicleServiceController {

    private final VehicleServiceRestClient vehicleServiceRestClient;
    private final ServiceRestClient serviceRestClient;
    private final VehicleRestClient vehicleRestClient;

    public AdminVehicleServiceController(VehicleServiceRestClient vehicleServiceRestClient,
                                         ServiceRestClient serviceRestClient,
                                         VehicleRestClient vehicleRestClient) {
        this.vehicleServiceRestClient = vehicleServiceRestClient;
        this.serviceRestClient = serviceRestClient;
        this.vehicleRestClient = vehicleRestClient;
    }

    @GetMapping
    public String page(Model model,
                       @RequestParam(value = "searchQuery", required = false, defaultValue = "") String searchQuery,
                       @RequestParam(value = "serviceFilter", required = false, defaultValue = "all") String serviceFilter) {
        model.addAttribute("pageTitle", "Quản lý dịch vụ xe");
        model.addAttribute("pageSubtitle", "Quản lý và theo dõi các dịch vụ bảo dưỡng, kiểm tra và sửa chữa xe");
        model.addAttribute("activePage", "vehicle-services");
        model.addAttribute("contentFragment", "admin/vehicle-services :: content");
        model.addAttribute("pageCss", new String[]{"/ext/css/vehicle-manager.css"});
        model.addAttribute("pageJs", new String[]{"/ext/js/vehicle-manager.js"});

        model.addAttribute("services", serviceRestClient.getAllServices());
        model.addAttribute("serviceTypes", serviceRestClient.getServiceTypes());

        List<VehicleDTO> vehicles = vehicleRestClient.getAllVehicles();
        List<Map<String, Object>> vehicleServices = vehicleServiceRestClient.getAllVehicleServices();

        // Debug logging
        System.out.println("🔍 [AdminVehicleServiceController] Total vehicles: " + vehicles.size());
        System.out.println("🔍 [AdminVehicleServiceController] Total vehicle services: " + vehicleServices.size());
        // Log all services with their status and serviceType
        for (int i = 0; i < vehicleServices.size(); i++) {
            Map<String, Object> service = vehicleServices.get(i);
            String status = normalizeStatus(service.get("status"));
            String serviceType = normalizeServiceTypeValue(service.get("serviceType"));
            String vehicleId = extractVehicleId(service);
            System.out.println("🔍 [AdminVehicleServiceController] Service #" + i + " - vehicleId=" + vehicleId + 
                              ", status=" + status + ", serviceType=" + serviceType + 
                              ", rawStatus=" + service.get("status") + ", rawServiceType=" + service.get("serviceType"));
        }

        Map<String, List<Map<String, Object>>> servicesByVehicle = vehicleServices.stream()
                .filter(service -> extractVehicleId(service) != null && !extractVehicleId(service).isBlank())
                .collect(Collectors.groupingBy(this::extractVehicleId));

        System.out.println("🔍 [AdminVehicleServiceController] Services grouped by vehicle: " + servicesByVehicle.size() + " vehicles");

        List<Map<String, Object>> vehicleViewModels = vehicles.stream()
                .map(vehicle -> buildVehicleViewModel(vehicle,
                        servicesByVehicle.getOrDefault(vehicle.getVehicleId(), Collections.emptyList())))
                .collect(Collectors.toList());

        List<Map<String, Object>> filteredVehicles = vehicleViewModels.stream()
                .filter(vehicle -> matchesSearch(vehicle, searchQuery))
                .filter(vehicle -> matchesServiceFilter(vehicle, serviceFilter))
                .collect(Collectors.toList());

        long maintenanceCount = countVehiclesByServiceType(servicesByVehicle, "maintenance");
        long inspectionCount = countVehiclesByServiceType(servicesByVehicle, "inspection");
        long repairCount = countVehiclesByServiceType(servicesByVehicle, "repair");
        
        System.out.println("🔍 [AdminVehicleServiceController] Maintenance vehicles: " + maintenanceCount);
        System.out.println("🔍 [AdminVehicleServiceController] Inspection vehicles: " + inspectionCount);
        System.out.println("🔍 [AdminVehicleServiceController] Repair vehicles: " + repairCount);

        model.addAttribute("vehicles", filteredVehicles);
        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("maintenanceVehicles", maintenanceCount);
        model.addAttribute("inspectionVehicles", inspectionCount);
        model.addAttribute("brokenVehicles", repairCount);
        model.addAttribute("vehicleServices", vehicleServices);
        model.addAttribute("completedServices", buildCompletedServices(vehicleServices, vehicles));
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("serviceFilter", serviceFilter);

        return "admin-vehicle-services";
    }

    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> registerVehicleService(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Nếu request có vehicleId và serviceId thì đăng ký dịch vụ cho xe
            if (requestData.containsKey("vehicleId") && requestData.containsKey("serviceId")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("vehicleId", requestData.get("vehicleId"));
                payload.put("serviceId", requestData.get("serviceId"));
                if (requestData.containsKey("serviceDescription")) {
                    payload.put("serviceDescription", requestData.get("serviceDescription"));
                }
                vehicleServiceRestClient.registerVehicleService(payload);
                response.put("success", true);
                response.put("message", "Đăng ký dịch vụ thành công");
            } else if (requestData.containsKey("serviceId") && requestData.containsKey("serviceName") && requestData.containsKey("serviceType")) {
                // Nếu có serviceId, serviceName, serviceType (không có vehicleId) thì thêm dịch vụ mới vào bảng service
                Map<String, Object> serviceData = new HashMap<>();
                serviceData.put("serviceId", requestData.get("serviceId"));
                serviceData.put("serviceName", requestData.get("serviceName"));
                serviceData.put("serviceType", requestData.get("serviceType"));
                
                Map<String, Object> addServiceResponse = serviceRestClient.addService(serviceData);
                response.put("success", true);
                response.put("message", "Đã thêm dịch vụ mới vào hệ thống thành công");
                response.put("data", addServiceResponse);
            } else {
                response.put("success", false);
                response.put("message", "Dữ liệu không hợp lệ. Vui lòng cung cấp đầy đủ thông tin.");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/service/{serviceId}/vehicle/{vehicleId}/status")
    public String updateStatus(@PathVariable("serviceId") String serviceId,
                               @PathVariable("vehicleId") String vehicleId,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestBody(required = false) Map<String, Object> requestBody) {
        // Hỗ trợ cả RequestParam và RequestBody
        String statusToUpdate = status;
        if (statusToUpdate == null && requestBody != null && requestBody.containsKey("status")) {
            statusToUpdate = (String) requestBody.get("status");
        }
        if (statusToUpdate == null) {
            return "redirect:/admin/vehicle-services?error=status_required";
        }
        vehicleServiceRestClient.updateServiceStatus(serviceId, vehicleId, statusToUpdate);
        return "redirect:/admin/vehicle-services";
    }

    @PutMapping("/service/{serviceId}/vehicle/{vehicleId}/status")
    @ResponseBody
    public Map<String, Object> updateStatusAjax(@PathVariable("serviceId") String serviceId,
                                                @PathVariable("vehicleId") String vehicleId,
                                                @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        try {
            String status = (String) requestBody.get("status");
            if (status == null || status.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Trạng thái không được để trống");
                return response;
            }
            vehicleServiceRestClient.updateServiceStatus(serviceId, vehicleId, status);
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái thành công");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/api/vehicle/{vehicleId}/services")
    @ResponseBody
    public Map<String, Object> getVehicleServices(@PathVariable String vehicleId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> services = vehicleServiceRestClient.getVehicleServicesByVehicleId(vehicleId);
            response.put("success", true);
            response.put("services", services);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách dịch vụ: " + e.getMessage());
            response.put("services", Collections.emptyList());
        }
        return response;
    }

    @DeleteMapping("/service/{serviceId}/vehicle/{vehicleId}")
    @ResponseBody
    public Map<String, Object> deleteVehicleService(@PathVariable("serviceId") String serviceId,
                                                    @PathVariable("vehicleId") String vehicleId) {
        Map<String, Object> response = new HashMap<>();
        try {
            vehicleServiceRestClient.deleteVehicleService(serviceId, vehicleId);
            response.put("success", true);
            response.put("message", "Đã xóa dịch vụ thành công");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa dịch vụ: " + e.getMessage());
        }
        return response;
    }

    private Map<String, Object> buildVehicleViewModel(VehicleDTO vehicle, List<Map<String, Object>> services) {
        Map<String, Object> view = new HashMap<>();
        String vehicleId = vehicle.getVehicleId();
        String displayName = resolveVehicleName(vehicle, vehicleId);
        String plateNumber = defaultString(vehicle.getVehicleNumber(), "-");
        String vehicleType = defaultString(vehicle.getType(), "-");

        view.put("vehicleId", vehicleId);
        view.put("name", displayName);
        view.put("plateNumber", plateNumber);
        view.put("category", vehicleType);
        view.put("typeDetail", vehicleType);

        String iconClass = determineIconClass(vehicleType);
        view.put("iconClass", "vehicle-icon " + iconClass);

        // Lọc ra các dịch vụ completed - chỉ hiển thị pending và in_progress trong danh sách xe
        List<Map<String, Object>> activeServices = services.stream()
                .filter(service -> {
                    String status = normalizeStatus(service.get("status"));
                    return !"completed".equals(status);
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> serviceDisplays = activeServices.stream()
                .map(this::buildServiceDisplay)
                .collect(Collectors.toList());
        view.put("services", serviceDisplays);

        boolean hasService = !serviceDisplays.isEmpty();
        view.put("hasService", hasService);

        Map<String, Object> latestService = activeServices.stream()
                .sorted(Comparator.comparing(
                                (Map<String, Object> svc) -> parseInstant(svc.get("requestDate")),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .findFirst()
                .orElse(null);

        if (latestService != null) {
            String serviceId = extractServiceId(latestService);
            String serviceName = defaultString(latestService.get("serviceName"), "Dịch vụ không tên");
            String statusNormalized = normalizeStatus(latestService.get("status"));
            
            // Debug logging
            System.out.println("🔍 [buildVehicleViewModel] Vehicle: " + vehicleId);
            System.out.println("   - Latest service serviceId: " + serviceId);
            System.out.println("   - Latest service vehicleId: " + extractVehicleId(latestService));
            System.out.println("   - Latest service status: " + statusNormalized);
            System.out.println("   - Latest service full data: " + latestService);

            view.put("serviceId", serviceId);
            view.put("serviceName", serviceName);
            view.put("serviceStatus", statusNormalized);
            view.put("serviceStatusClass", mapStatusToClass(statusNormalized));
        } else {
            view.put("serviceId", null);
            view.put("serviceName", null);
            view.put("serviceStatus", "available");
            view.put("serviceStatusClass", "available");
        }

        String overallStatus = determineOverallStatus(serviceDisplays);
        view.put("overallStatus", overallStatus);

        Instant latestRequest = activeServices.stream()
                .map(service -> parseInstant(service.get("requestDate")))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        view.put("formattedRequestDate", formatInstant(latestRequest));

        return view;
    }

    private Map<String, Object> buildServiceDisplay(Map<String, Object> service) {
        Map<String, Object> display = new HashMap<>();
        String serviceName = defaultString(service.get("serviceName"), "Dịch vụ không tên");
        String statusRaw = defaultString(service.get("status"), "pending");
        String statusNormalized = normalizeStatus(statusRaw);
        String serviceTypeRaw = defaultString(service.get("serviceType"), "Khác");
        String serviceTypeNormalized = normalizeServiceTypeValue(serviceTypeRaw);

        display.put("serviceName", serviceName);
        display.put("status", statusRaw);
        display.put("statusClass", mapStatusToClass(statusNormalized));
        display.put("statusNormalized", statusNormalized);
        display.put("serviceType", prettyServiceType(serviceTypeRaw));
        display.put("serviceTypeNormalized", serviceTypeNormalized);
        display.put("requestDateRaw", service.get("requestDate"));

        return display;
    }

    private String determineOverallStatus(List<Map<String, Object>> services) {
        if (services == null || services.isEmpty()) {
            return "pending";
        }
        boolean hasPending = services.stream()
                .anyMatch(service -> "pending".equals(service.get("statusNormalized")));
        if (hasPending) {
            return "pending";
        }
        boolean hasInProgress = services.stream()
                .anyMatch(service -> "in_progress".equals(service.get("statusNormalized")));
        if (hasInProgress) {
            return "in_progress";
        }
        boolean hasCompleted = services.stream()
                .anyMatch(service -> "completed".equals(service.get("statusNormalized")));
        if (hasCompleted) {
            return "complete";
        }
        return "pending";
    }

    private String extractVehicleId(Map<String, Object> service) {
        if (service == null) {
            return null;
        }
        Object vehicleId = service.get("vehicleId");
        if (vehicleId != null) {
            return vehicleId.toString();
        }
        Object compoundId = service.get("id");
        if (compoundId instanceof Map<?, ?> compoundMap) {
            Object nestedVehicleId = compoundMap.get("vehicleId");
            if (nestedVehicleId != null) {
                return nestedVehicleId.toString();
            }
        }
        return null;
    }

    private String extractServiceId(Map<String, Object> service) {
        if (service == null) {
            return null;
        }
        
        // Thử lấy từ serviceId trực tiếp
        Object serviceId = service.get("serviceId");
        if (serviceId != null && !serviceId.toString().trim().isEmpty()) {
            return serviceId.toString().trim();
        }
        
        // Thử lấy từ compound id object
        Object compoundId = service.get("id");
        if (compoundId instanceof Map<?, ?> compoundMap) {
            Object nestedServiceId = compoundMap.get("serviceId");
            if (nestedServiceId != null && !nestedServiceId.toString().trim().isEmpty()) {
                return nestedServiceId.toString().trim();
            }
        }
        
        // Debug logging
        System.out.println("⚠️ [extractServiceId] Không tìm thấy serviceId trong service: " + service.keySet());
        
        return null;
    }

    private boolean matchesSearch(Map<String, Object> vehicle, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return true;
        }
        String keyword = normalizeText(searchQuery);
        return Stream.of("name", "plateNumber", "vehicleId")
                .map(key -> normalizeText(Objects.toString(vehicle.get(key), "")))
                .anyMatch(value -> value.contains(keyword));
    }

    @SuppressWarnings("unchecked")
    private boolean matchesServiceFilter(Map<String, Object> vehicle, String serviceFilter) {
        if (serviceFilter == null || serviceFilter.isBlank() || "all".equalsIgnoreCase(serviceFilter)) {
            return true;
        }
        String normalizedFilter = normalizeServiceTypeValue(serviceFilter);
        List<Map<String, Object>> services = (List<Map<String, Object>>) vehicle.get("services");
        if (services == null || services.isEmpty()) {
            return false;
        }
        return services.stream()
                .map(service -> Objects.toString(service.get("serviceTypeNormalized"), ""))
                .anyMatch(type -> type.equals(normalizedFilter));
    }

    private long countVehiclesByServiceType(Map<String, List<Map<String, Object>>> servicesByVehicle, String targetType) {
        if (servicesByVehicle == null || servicesByVehicle.isEmpty()) {
            System.out.println("⚠️ [countVehiclesByServiceType] servicesByVehicle is null or empty for type: " + targetType);
            return 0;
        }
        String normalizedTarget = normalizeServiceTypeValue(targetType);
        System.out.println("🔍 [countVehiclesByServiceType] Counting vehicles for type: " + targetType + " (normalized: " + normalizedTarget + ")");
        
        long count = servicesByVehicle.entrySet().stream()
                .filter(entry -> {
                    String vehicleId = entry.getKey();
                    List<Map<String, Object>> services = entry.getValue();
                    
                    boolean hasMatchingService = services.stream()
                            .filter(service -> {
                                // Chỉ đếm các dịch vụ có status là pending hoặc in_progress
                                String status = normalizeStatus(service.get("status"));
                                boolean statusMatch = "pending".equals(status) || "in_progress".equals(status);
                                
                                if (!statusMatch) {
                                    return false; // Bỏ qua nếu status không phải pending hoặc in_progress
                                }
                                
                                // Lấy serviceType từ serviceType field hoặc từ serviceName nếu serviceType null
                                String serviceType = normalizeServiceTypeValue(service.get("serviceType"));
                                if (serviceType == null || serviceType.isEmpty()) {
                                    // Nếu serviceType null, thử lấy từ serviceName
                                    String serviceName = service.get("serviceName") != null ? service.get("serviceName").toString() : "";
                                    serviceType = normalizeServiceTypeValue(serviceName);
                                }
                                
                                boolean typeMatch = serviceType != null && !serviceType.isEmpty() && serviceType.equals(normalizedTarget);
                                
                                // Log all services for this vehicle
                                System.out.println("🔍 [countVehiclesByServiceType] Vehicle " + vehicleId + 
                                                  " - status=" + status + " (raw: " + service.get("status") + 
                                                  "), serviceType=" + serviceType + " (raw serviceType: " + service.get("serviceType") + 
                                                  ", raw serviceName: " + service.get("serviceName") + 
                                                  "), statusMatch=" + statusMatch + ", typeMatch=" + typeMatch);
                                
                                if (statusMatch && typeMatch) {
                                    System.out.println("✅ [countVehiclesByServiceType] Found matching service for vehicle " + vehicleId + 
                                                      ": status=" + status + ", serviceType=" + serviceType);
                                }
                                
                                return typeMatch;
                            })
                            .findAny()
                            .isPresent();
                    
                    return hasMatchingService;
                })
                .count();
        
        System.out.println("🔍 [countVehiclesByServiceType] Count for " + targetType + ": " + count);
        return count;
    }

    private List<Map<String, Object>> buildCompletedServices(List<Map<String, Object>> vehicleServices,
                                                             List<VehicleDTO> vehicles) {
        if (vehicleServices == null || vehicleServices.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, VehicleDTO> vehicleIndex = vehicles.stream()
                .filter(vehicle -> vehicle.getVehicleId() != null && !vehicle.getVehicleId().isBlank())
                .collect(Collectors.toMap(VehicleDTO::getVehicleId, Function.identity(), (existing, replacement) -> existing));

        return vehicleServices.stream()
                .filter(service -> "completed".equals(normalizeStatus(service.get("status"))))
                .map(service -> {
                    Map<String, Object> view = new HashMap<>();
                    String vehicleId = extractVehicleId(service);
                    VehicleDTO vehicle = vehicleIndex.get(vehicleId);

                    view.put("vehicleName", resolveVehicleName(vehicle, vehicleId));
                    view.put("vehicleNumber", vehicle != null && vehicle.getVehicleNumber() != null
                            ? vehicle.getVehicleNumber() : "-");
                    view.put("vehicleType", vehicle != null && vehicle.getType() != null
                            ? vehicle.getType() : "-");
                    view.put("serviceName", defaultString(service.get("serviceName"), "-"));
                    view.put("serviceType", prettyServiceType(service.get("serviceType")));

                    Instant requestInstant = parseInstant(service.get("requestDate"));
                    Instant completionInstant = parseInstant(service.get("completionDate"));
                    view.put("formattedRequestDate", formatInstant(requestInstant));
                    view.put("formattedCompletionDate", formatInstant(completionInstant));
                    view.put("_completionInstant", completionInstant);
                    return view;
                })
                .sorted(Comparator.comparing(
                        (Map<String, Object> item) -> (Instant) item.get("_completionInstant"),
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(item -> {
                    item.remove("_completionInstant");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String determineIconClass(String vehicleType) {
        String normalized = normalizeText(vehicleType);
        if (normalized.contains("electric") || normalized.contains("ev")) {
            return "icon-electric";
        }
        if (normalized.contains("suv")) {
            return "icon-suv";
        }
        if (normalized.contains("truck")) {
            return "icon-truck";
        }
        if (normalized.contains("sedan")) {
            return "icon-sedan";
        }
        return "icon-default";
    }

    private String resolveVehicleName(VehicleDTO vehicle, String vehicleId) {
        if (vehicle == null) {
            return vehicleId != null && !vehicleId.isBlank() ? vehicleId : "-";
        }
        if (vehicle.getName() != null && !vehicle.getName().isBlank()) {
            return vehicle.getName();
        }
        if (vehicle.getVehicleNumber() != null && !vehicle.getVehicleNumber().isBlank()) {
            return vehicle.getVehicleNumber();
        }
        return vehicleId != null && !vehicleId.isBlank() ? vehicleId : "-";
    }

    private String defaultString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString().trim().toLowerCase(Locale.ENGLISH);
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String normalizeStatus(Object status) {
        if (status == null) {
            return "pending";
        }
        String normalized = normalizeText(status);
        if ("inprogress".equals(normalized)) {
            return "in_progress";
        }
        if ("completed".equals(normalized) || "complete".equals(normalized)) {
            return "completed";
        }
        if ("pending".equals(normalized)) {
            return "pending";
        }
        return normalized;
    }

    private String mapStatusToClass(String status) {
        if ("completed".equals(status)) {
            return "completed";
        }
        if ("in_progress".equals(status)) {
            return "in-progress";
        }
        if ("available".equals(status)) {
            return "available";
        }
        return "pending";
    }

    private String normalizeServiceTypeValue(Object type) {
        if (type == null) {
            return "";
        }
        String normalized = normalizeText(type);
        if (normalized.contains("maintenance") || normalized.contains("baoduong")) {
            return "maintenance";
        }
        if (normalized.contains("inspection") || normalized.contains("kiemtra")) {
            return "inspection";
        }
        if (normalized.contains("repair") || normalized.contains("suachua")) {
            return "repair";
        }
        return normalized.replaceAll("[^a-z]", "");
    }

    private String prettyServiceType(Object type) {
        if (type == null) {
            return "-";
        }
        return type.toString();
    }

    private Instant parseInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        if (raw instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (raw instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }
        String value = raw.toString();
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return java.time.LocalDateTime.parse(value)
                        .atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}

