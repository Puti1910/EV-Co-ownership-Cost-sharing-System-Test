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
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import com.example.VehicleServiceManagementService.integration.UserAccountClient;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;

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
    private final VehicleServiceRepository vehicleServiceRepository;
    private final VehicleRepository vehicleRepository;
    private final UserAccountClient userAccountClient;

    /**
     * Get list of groups/vehicles a user can book maintenance for.
     */
    public List<Map<String, Object>> getUserMaintenanceOptions(Long userId) {
        log.info("🔍 Fetching maintenance options for userId: {}", userId);
        
        List<Map<String, Object>> userGroups = groupClient.getGroupsByUserId(userId);
        if (userGroups == null || userGroups.isEmpty()) {
            log.warn("No groups found for userId: {}", userId);
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, Object> groupInfo : userGroups) {
            Long groupId = toLong(groupInfo.get("groupId"));
            String groupName = (String) groupInfo.getOrDefault("groupName", "Nhóm #" + groupId);
            String role = (String) groupInfo.getOrDefault("memberRole", groupInfo.get("role"));
            
            List<Vehicle> groupVehicles = vehicleRepository.findByGroupId(groupId);
            
            if (groupVehicles != null && !groupVehicles.isEmpty()) {
                for (Vehicle vehicle : groupVehicles) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("groupId", groupId);
                    option.put("groupName", groupName);
                    option.put("role", role);
                    option.put("vehicleId", vehicle.getVehicleId());
                    option.put("vehicleName", vehicle.getDisplayName());
                    option.put("vehicleNumber", vehicle.getVehicleNumber());
                    option.put("vehicleType", vehicle.getVehicleType());
                    results.add(option);
                }
            } else {
                Map<String, Object> option = new HashMap<>();
                option.put("groupId", groupId);
                option.put("groupName", groupName);
                option.put("role", role);
                option.put("vehicleId", null);
                option.put("vehicleName", "Chưa có xe");
                results.add(option);
            }
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

            if (!userAccountClient.existsById(request.getUserId())) {
                throw new RuntimeException("Không tìm thấy người dùng với ID: " + request.getUserId());
            }

            Map<String, Object> groupPayload = groupClient.getGroup(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm xe #" + request.getGroupId()));

            groupClient.getMembership(request.getGroupId(), request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Người dùng không thuộc nhóm này"));

            Long resolvedVehicleId = request.getVehicleId();
            log.info("📝 [BOOKING] Vehicle ID: {}", resolvedVehicleId);
            
            if (!vehicleRepository.existsById(resolvedVehicleId)) {
                throw new RuntimeException("Không tìm thấy xe với ID: " + resolvedVehicleId);
            }

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

            ServiceType serviceType = resolveServiceType(request);
            log.info("📝 [BOOKING] Service type: {} - {}", serviceType.getServiceId(), serviceType.getServiceName());

            LocalDateTime preferredStart = parseDateTime(request.getPreferredStartDatetime());
            LocalDateTime preferredEnd = parseDateTime(request.getPreferredEndDatetime());
            
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
            
            Vehicleservice saved = vehicleServiceService.saveVehicleService(vehicleService);
            
            // Verify save
            Optional<Vehicleservice> verify = vehicleServiceRepository.findById(saved.getId());
            if (verify.isPresent()) {
                log.info("✅ [BOOKING] Xác nhận: Dữ liệu đã được lưu! ID: {}", verify.get().getId());
            } else {
                log.error("❌ [BOOKING] CẢNH BÁO: Không tìm thấy dữ liệu sau khi save!");
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
        if (request.getServiceId() != null) {
            Long serviceId = request.getServiceId();
            return serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ với ID: " + serviceId));
        }
        
        if (request.getServiceName() != null && !request.getServiceName().isBlank()) {
            Optional<ServiceType> serviceOpt = serviceRepository.findByServiceName(request.getServiceName());
            if (serviceOpt.isPresent()) {
                return serviceOpt.get();
            }
            log.info("Không tìm thấy dịch vụ với tên: {}, tự động tạo mới", request.getServiceName());
            ServiceType newService = new ServiceType();
            newService.setServiceName(request.getServiceName());
            newService.setServiceType("maintenance");
            return serviceService.addService(newService);
        }
        
        return serviceRepository.findAll().stream()
                .filter(s -> s.getServiceType() != null && s.getServiceType().equalsIgnoreCase("maintenance"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình dịch vụ bảo dưỡng nào (serviceType=maintenance)"));
    }

    private Map<String, Object> convertToResponse(Vehicleservice saved, Map<String, Object> groupPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("serviceId", saved.getServiceId());
        result.put("vehicleId", saved.getVehicleId());
        result.put("status", saved.getStatus());
        result.put("requestDate", saved.getRequestDate());
        result.put("preferredStart", saved.getPreferredStartDatetime());
        result.put("preferredEnd", saved.getPreferredEndDatetime());
        result.put("groupId", saved.getGroupRefId());
        result.put("requestedBy", saved.getRequestedByUserId());
        result.put("groupName", groupPayload.getOrDefault("groupName", "Nhóm #" + saved.getGroupRefId()));
        return result;
    }

    private void validateRequest(MaintenanceBookingRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId là bắt buộc");
        }
        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("groupId là bắt buộc");
        }
        if (request.getVehicleId() == null) {
            throw new IllegalArgumentException("vehicleId là bắt buộc");
        }
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

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
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
