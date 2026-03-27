package com.example.VehicleServiceManagementService.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.VehicleServiceManagementService.dto.MaintenanceBookingRequest;
import com.example.VehicleServiceManagementService.integration.GroupManagementClient;
import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.repository.ServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceBookingService {

    private final GroupManagementClient groupClient;
    private final VehicleServiceService vehicleServiceService;
    private final ServiceRepository serviceRepository;
    private final VehicleDataSyncService vehicleDataSyncService;
    private final ServiceService serviceService;
    private final com.example.VehicleServiceManagementService.repository.VehicleServiceRepository vehicleServiceRepository;

    /**
     * Get list of groups/vehicles a user can book maintenance for.
     */
    public List<Map<String, Object>> getUserMaintenanceOptions(Integer userId) {
        List<Map<String, Object>> options = groupClient.getMaintenanceOptions(userId);
        if (options.isEmpty()) {
            log.warn("Không lấy được maintenance-options qua endpoint mới, fallback sang danh sách nhóm cơ bản");
            options = groupClient.getGroupsByUserId(userId);
        }
        if (options.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> option : options) {
            Map<String, Object> view = new HashMap<>();
            Integer groupId = toInteger(option.get("groupId"));
            view.put("groupId", groupId);
            view.put("groupName", option.getOrDefault("groupName", "Group #" + groupId));
            view.put("role", option.getOrDefault("memberRole", option.get("role")));
            Object vehicleIdObj = option.get("vehicleId");
            if (vehicleIdObj != null) {
                String vehicleId = String.valueOf(vehicleIdObj);
                view.put("vehicleId", vehicleId);
                Object label = option.get("vehicleLabel");
                if (label != null) {
                    view.put("vehicleName", label);
                } else {
                    view.put("vehicleName", "Xe #" + vehicleId);
                }
            } else {
                view.put("vehicleId", null);
                view.put("vehicleName", null);
            }
            results.add(view);
        }
        return results;
    }

    /**
     * Book maintenance service.
     */
    @Transactional
    public Map<String, Object> bookMaintenance(MaintenanceBookingRequest request) {
        log.info("📝 [BOOKING] Bắt đầu đặt dịch vụ bảo dưỡng - userId: {}, groupId: {}, vehicleId: {}, serviceId: {}", 
                request.getUserId(), request.getGroupId(), request.getVehicleId(), request.getServiceId());
        
        try {
            validateRequest(request);

            Map<String, Object> groupPayload = groupClient.getGroup(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm xe #" + request.getGroupId()));

            groupClient.getMembership(request.getGroupId(), request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không thuộc nhóm này"));

            String resolvedVehicleId = resolveVehicleId(request, groupPayload);
            log.info("📝 [BOOKING] Vehicle ID đã được resolve: {}", resolvedVehicleId);
            
            Vehiclegroup groupEntity = vehicleDataSyncService.ensureGroupSynced(request.getGroupId(), groupPayload);
            Map<String, Object> vehicleSnapshot = new HashMap<>();
            if (request.getVehicleName() != null) {
                vehicleSnapshot.put("vehicleName", request.getVehicleName());
            }
            Vehicle vehicleEntity = vehicleDataSyncService.ensureVehicleSynced(
                    resolvedVehicleId,
                    groupEntity,
                    vehicleSnapshot,
                    request.getVehicleName()
            );
            log.info("📝 [BOOKING] Vehicle entity đã được sync: {}", vehicleEntity.getVehicleId());

            ServiceType serviceType = resolveServiceType(request);
            System.out.println("📝 [BOOKING] Service type đã được resolve: " + serviceType.getServiceId() + " - " + serviceType.getServiceName());
            log.info("📝 [BOOKING] Service type đã được resolve: {} - {}", serviceType.getServiceId(), serviceType.getServiceName());

            LocalDateTime preferredStart = parseDateTime(request.getPreferredStartDatetime());
            LocalDateTime preferredEnd = parseDateTime(request.getPreferredEndDatetime());
            
            System.out.println("📝 [BOOKING] Đang tạo Vehicleservice entity...");

            Vehicleservice vehicleService = vehicleServiceService.createVehicleService(
                    serviceType,
                    vehicleEntity,
                    request.getServiceDescription(),
                    null,
                    request.getGroupId(),
                    request.getUserId(),
                    request.getRequestedByName(),
                    preferredStart,
                    preferredEnd
            );
            vehicleService.setServiceDescription(buildDescription(request));
            
            System.out.println("📝 [BOOKING] Vehicleservice entity đã được tạo, bắt đầu lưu vào database...");
            System.out.println("📝 [BOOKING] ServiceId: " + vehicleService.getServiceId() + ", VehicleId: " + vehicleService.getVehicleId() + 
                             ", Status: " + vehicleService.getStatus() + ", GroupId: " + vehicleService.getGroupRefId() + 
                             ", UserId: " + vehicleService.getRequestedByUserId());
            log.info("📝 [BOOKING] Vehicleservice entity đã được tạo, bắt đầu lưu vào database...");
            log.info("📝 [BOOKING] ServiceId: {}, VehicleId: {}, Status: {}, GroupId: {}, UserId: {}", 
                    vehicleService.getServiceId(), vehicleService.getVehicleId(), 
                    vehicleService.getStatus(), vehicleService.getGroupRefId(), 
                    vehicleService.getRequestedByUserId());
            log.info("📝 [BOOKING] Service entity: {}, Vehicle entity: {}", 
                    vehicleService.getService() != null ? vehicleService.getService().getServiceId() : "NULL",
                    vehicleService.getVehicle() != null ? vehicleService.getVehicle().getVehicleId() : "NULL");

            System.out.println("🔒 [BOOKING] Đang gọi saveVehicleService...");
            Vehicleservice saved = vehicleServiceService.saveVehicleService(vehicleService);
            
            System.out.println("✅ [BOOKING] Vehicleservice đã được lưu thành công vào database!");
            System.out.println("✅ [BOOKING] Saved entity - ServiceId: " + saved.getServiceId() + ", VehicleId: " + saved.getVehicleId() + 
                             ", Status: " + saved.getStatus());
            log.info("✅ [BOOKING] Vehicleservice đã được lưu thành công vào database!");
            log.info("✅ [BOOKING] Saved entity - ServiceId: {}, VehicleId: {}, Status: {}, Id: {}", 
                    saved.getServiceId(), saved.getVehicleId(), saved.getStatus(), saved.getId());
            
            // Kiểm tra lại xem đã thực sự lưu vào database chưa
            com.example.VehicleServiceManagementService.model.VehicleserviceId verifyId = 
                    new com.example.VehicleServiceManagementService.model.VehicleserviceId(
                            saved.getServiceId(), saved.getVehicleId());
            Optional<Vehicleservice> verify = vehicleServiceRepository.findById(verifyId);
            if (verify.isPresent()) {
                log.info("✅ [BOOKING] Xác nhận: Dữ liệu đã tồn tại trong database!");
            } else {
                log.error("❌ [BOOKING] CẢNH BÁO: Dữ liệu không tồn tại trong database sau khi save!");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("reservation", convertToResponse(saved, groupPayload));
            return response;
        } catch (Exception e) {
            log.error("❌ [BOOKING] Lỗi khi đặt dịch vụ bảo dưỡng", e);
            throw e;
        }
    }

    private ServiceType resolveServiceType(MaintenanceBookingRequest request) {
        // Ưu tiên tìm theo serviceId
        if (request.getServiceId() != null && !request.getServiceId().isBlank()) {
            // Tìm lại một lần nữa để tránh race condition
            Optional<ServiceType> serviceOpt = serviceRepository.findById(request.getServiceId());
            if (serviceOpt.isPresent()) {
                return serviceOpt.get();
            }
            // Nếu không tìm thấy theo ID, tự động tạo mới
            return createServiceIfNotExists(request.getServiceId(), request.getServiceName());
        }
        
        // Tìm theo serviceName
        if (request.getServiceName() != null && !request.getServiceName().isBlank()) {
            Optional<ServiceType> serviceOpt = serviceRepository.findByServiceName(request.getServiceName());
            if (serviceOpt.isPresent()) {
                return serviceOpt.get();
            }
            // Nếu không tìm thấy theo tên, tự động tạo mới
            log.info("Không tìm thấy dịch vụ với tên: {}, tự động tạo mới", request.getServiceName());
            ServiceType newService = new ServiceType();
            newService.setServiceName(request.getServiceName());
            newService.setServiceType("maintenance");
            // Sử dụng ServiceService để tự động generate serviceId
            return serviceService.addService(newService);
        }
        
        // Fallback: tìm bất kỳ dịch vụ maintenance nào
        return serviceRepository.findAll().stream()
                .filter(s -> s.getServiceType() != null && s.getServiceType().equalsIgnoreCase("maintenance"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình dịch vụ bảo dưỡng nào (serviceType=maintenance)"));
    }

    /**
     * Tạo service mới nếu chưa tồn tại, với transaction riêng để tránh rollback toàn bộ request
     * Sử dụng REQUIRES_NEW để tạo transaction mới, độc lập với transaction chính
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = DataIntegrityViolationException.class)
    private ServiceType createServiceIfNotExists(String serviceId, String serviceName) {
        // Kiểm tra lại một lần nữa (double-check locking pattern)
        Optional<ServiceType> existingOpt = serviceRepository.findById(serviceId);
        if (existingOpt.isPresent()) {
            log.info("Service với ID {} đã tồn tại (có thể do request khác đã tạo)", serviceId);
            return existingOpt.get();
        }
        
        // Nếu không có serviceName, tạo tên mặc định dựa trên serviceId
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = "Dịch vụ bảo dưỡng " + serviceId;
        }
        
        log.info("Không tìm thấy dịch vụ với ID: {}, tự động tạo mới với tên: {}", serviceId, serviceName);
        ServiceType newService = new ServiceType(serviceId, serviceName, "maintenance");
        
        try {
            return serviceRepository.save(newService);
        } catch (DataIntegrityViolationException e) {
            // Nếu lỗi duplicate key (có thể do request khác đã tạo trong transaction khác), thử tìm lại
            log.warn("Service với ID {} đã tồn tại (có thể do request khác đã tạo), đang tìm lại...", serviceId);
            // Retry với delay nhỏ để đảm bảo transaction khác đã commit
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(50 * (i + 1)); // Tăng dần delay: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Optional<ServiceType> retryOpt = serviceRepository.findById(serviceId);
                if (retryOpt.isPresent()) {
                    log.info("Tìm thấy service sau {} lần retry", i + 1);
                    return retryOpt.get();
                }
            }
            // Nếu vẫn không tìm thấy sau retry, throw exception
            throw new IllegalArgumentException("Không thể tạo hoặc tìm thấy dịch vụ với ID: " + serviceId);
        } catch (Exception e) {
            log.error("Lỗi khi tạo dịch vụ mới với ID: {}", serviceId, e);
            // Nếu lưu thất bại, thử tìm lại
            Optional<ServiceType> retryOpt = serviceRepository.findById(serviceId);
            if (retryOpt.isPresent()) {
                return retryOpt.get();
            }
            throw new IllegalArgumentException("Không thể tạo dịch vụ với ID: " + serviceId + ". " + e.getMessage());
        }
    }

    private Map<String, Object> convertToResponse(Vehicleservice saved, Map<String, Object> groupPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceId", saved.getServiceId());
        result.put("vehicleId", saved.getVehicleId());
        result.put("status", saved.getStatus());
        result.put("requestDate", saved.getRequestDate());
        result.put("preferredStart", saved.getPreferredStartDatetime());
        result.put("preferredEnd", saved.getPreferredEndDatetime());
        result.put("groupId", saved.getGroupRefId());
        result.put("requestedBy", saved.getRequestedByUserId());
        result.put("groupName", groupPayload.getOrDefault("groupName", "Group #" + saved.getGroupRefId()));
        return result;
    }

    private void validateRequest(MaintenanceBookingRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId là bắt buộc");
        }
        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("groupId là bắt buộc");
        }
        if (request.getVehicleId() == null || request.getVehicleId().isBlank()) {
            throw new IllegalArgumentException("vehicleId là bắt buộc");
        }
    }

    private String resolveVehicleId(MaintenanceBookingRequest request, Map<String, Object> groupPayload) {
        Object groupVehicleId = groupPayload.get("vehicleId");
        String targetVehicleId = request.getVehicleId();
        if (groupVehicleId != null) {
            String normalized = String.valueOf(groupVehicleId);
            if (!Objects.equals(normalized, targetVehicleId)) {
                log.warn("VehicleId {} từ yêu cầu khác với vehicleId {} trong nhóm, sẽ ưu tiên group", targetVehicleId, normalized);
            }
            targetVehicleId = normalized;
        }
        return targetVehicleId;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Không thể parse thời gian: " + value);
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildDescription(MaintenanceBookingRequest request) {
        List<String> parts = new ArrayList<>();
        if (request.getServiceDescription() != null && !request.getServiceDescription().isBlank()) {
            parts.add(request.getServiceDescription().trim());
        }
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            parts.add("SĐT: " + request.getContactPhone());
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            parts.add("Ghi chú: " + request.getNote().trim());
        }
        return String.join(" | ", parts);
    }
}

