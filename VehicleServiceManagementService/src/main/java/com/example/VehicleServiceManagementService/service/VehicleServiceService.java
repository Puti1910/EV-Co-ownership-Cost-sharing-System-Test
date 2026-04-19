package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import com.example.VehicleServiceManagementService.repository.ServiceRepository;
import com.example.VehicleServiceManagementService.integration.UserAccountClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VehicleServiceService {

    @Autowired
    private VehicleServiceRepository vehicleServiceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserAccountClient userAccountClient;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lưu Vehicleservice vào database với transaction
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public Vehicleservice saveVehicleService(Vehicleservice vehicleService) {
        log.info("🔒 [SAVE] Bắt đầu save Vehicleservice vào database...");
        
        try {
            Long serviceId = vehicleService.getServiceId();
            Long vehicleId = vehicleService.getVehicleId();
            
            // Đảm bảo requestDate được set
            if (vehicleService.getRequestDate() == null) {
                vehicleService.setRequestDate(Instant.now());
            }
            
            // Đảm bảo status được set
            if (vehicleService.getStatus() == null || vehicleService.getStatus().trim().isEmpty()) {
                vehicleService.setStatus("pending");
            }
            
            // Tìm bản ghi mới nhất để cập nhật nếu có (dành cho update flow)
            // Lưu ý: serviceId và vehicleId không còn là composite key chính thức (dùng Long id thay thế)
            // Nhưng logic nghiệp vụ có thể vẫn muốn update bản ghi pending cũ
            Optional<Vehicleservice> existingOpt = vehicleServiceRepository.findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(serviceId, vehicleId);
            
            Vehicleservice entityToSave;
            if (existingOpt.isPresent() && !"completed".equalsIgnoreCase(existingOpt.get().getStatus())) {
                Vehicleservice existing = existingOpt.get();
                log.info("ℹ️ [SAVE] Tìm thấy bản ghi hiện tại (ID: {}) → tiến hành cập nhật", existing.getId());
                existing.setServiceName(vehicleService.getServiceName());
                existing.setServiceDescription(vehicleService.getServiceDescription());
                existing.setServiceType(vehicleService.getServiceType());
                existing.setStatus(vehicleService.getStatus());
                existing.setCompletionDate(vehicleService.getCompletionDate());
                existing.setGroupRefId(vehicleService.getGroupRefId());
                existing.setRequestedByUserId(vehicleService.getRequestedByUserId());
                existing.setRequestedByUserName(vehicleService.getRequestedByUserName());
                existing.setPreferredStartDatetime(vehicleService.getPreferredStartDatetime());
                existing.setPreferredEndDatetime(vehicleService.getPreferredEndDatetime());
                entityToSave = existing;
            } else {
                log.info("ℹ️ [SAVE] Không tìm thấy bản ghi cũ hoặc bản ghi cũ đã hoàn thành → tạo mới hoàn toàn");
                entityToSave = vehicleService;
            }
            
            Vehicleservice savedService = vehicleServiceRepository.save(entityToSave);
            vehicleServiceRepository.flush();
            
            log.info("✅ [SAVE] Thành công! ID mới: {}", savedService.getId());
            
            // Đồng bộ trạng thái vehicle
            try {
                syncVehicleStatus(vehicleId);
            } catch (Exception ignored) {}
            
            return savedService;
            
        } catch (Exception e) {
            log.error("❌ [SAVE] Lỗi: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Kiểm tra service tồn tại
     */
    public ServiceType validateAndGetService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ với ID: " + serviceId));
    }

    /**
     * Kiểm tra vehicle tồn tại
     */
    public Vehicle validateAndGetVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + vehicleId));
    }

    /**
     * Kiểm tra User tồn tại qua User Account Service
     */
    public void validateAndGetUser(Long userId) {
        if (userId != null && !userAccountClient.existsById(userId)) {
            throw new RuntimeException("Không tìm thấy người dùng với ID: " + userId);
        }
    }

    /**
     * Kiểm tra xe có tồn tại không
     */
    public boolean existsVehicleById(Long vehicleId) {
        return vehicleId != null && vehicleId > 0 && vehicleRepository.existsById(vehicleId);
    }

    /**
     * Tạo Vehicleservice entity từ request data
     */
    public Vehicleservice createVehicleService(
            ServiceType service,
            Vehicle vehicle,
            String serviceDescription,
            String status,
            Long groupRefId,
            Long requestedByUserId,
            String requestedByName,
            LocalDateTime preferredStart,
            LocalDateTime preferredEnd) {
        
        Vehicleservice vehicleService = new Vehicleservice();
        vehicleService.setServiceId(service.getServiceId());
        vehicleService.setVehicleId(vehicle.getVehicleId());
        vehicleService.setService(service);
        vehicleService.setVehicle(vehicle);
        vehicleService.setServiceName(service.getServiceName());
        vehicleService.setServiceType(service.getServiceType());
        
        if (serviceDescription != null && !serviceDescription.trim().isEmpty()) {
            vehicleService.setServiceDescription(serviceDescription.trim());
        }
        
        vehicleService.setStatus(status == null ? "pending" : status);
        vehicleService.setRequestDate(Instant.now());
        vehicleService.setGroupRefId(groupRefId);
        vehicleService.setRequestedByUserId(requestedByUserId);
        vehicleService.setRequestedByUserName(requestedByName);
        vehicleService.setPreferredStartDatetime(preferredStart);
        vehicleService.setPreferredEndDatetime(preferredEnd);
        
        return vehicleService;
    }
    
    /**
     * Đồng bộ trạng thái xe (vehicle.status) dựa trên dịch vụ đang chờ (vehicleservice)
     */
    @Transactional
    public void syncVehicleStatus(Long vehicleId) {
        try {
            log.info("🔄 [SYNC VEHICLE STATUS] Bắt đầu đồng bộ trạng thái cho vehicle: {}", vehicleId);
            
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                log.warn("   ⚠️ Vehicle không tồn tại: {}", vehicleId);
                return;
            }
            
            Vehicle vehicle = vehicleOpt.get();
            String currentStatus = vehicle.getStatus();
            
            List<Vehicleservice> activeServices = vehicleServiceRepository.findByVehicleId(vehicleId).stream()
                    .filter(vs -> {
                        String status = vs.getStatus();
                        if (status == null) return false;
                        String statusLower = status.toLowerCase().trim();
                        return statusLower.equals("pending") || 
                               statusLower.equals("in_progress") || 
                               statusLower.equals("in progress");
                    })
                    .collect(Collectors.toList());
            
            log.info("   - Số dịch vụ đang chờ: {}", activeServices.size());
            
            String newStatus = null;
            
            if (!activeServices.isEmpty()) {
                boolean hasMaintenance = false;
                boolean hasRepair = false;
                boolean hasChecking = false;
                
                for (Vehicleservice vs : activeServices) {
                    String serviceType = vs.getServiceType();
                    if (serviceType != null) {
                        String serviceTypeLower = serviceType.toLowerCase().trim();
                        if (serviceTypeLower.contains("maintenance") || serviceTypeLower.contains("bảo dưỡng")) {
                            hasMaintenance = true;
                        } else if (serviceTypeLower.contains("repair") || serviceTypeLower.contains("sửa chữa")) {
                            hasRepair = true;
                        } else if (serviceTypeLower.contains("checking") || serviceTypeLower.contains("kiểm tra")) {
                            hasChecking = true;
                        }
                    }
                }
                
                if (hasMaintenance) {
                    newStatus = "maintenance";
                } else if (hasRepair) {
                    newStatus = "repair";
                } else if (hasChecking) {
                    newStatus = "checking";
                } else {
                    String firstServiceType = activeServices.get(0).getServiceType();
                    if (firstServiceType != null && !firstServiceType.trim().isEmpty()) {
                        newStatus = firstServiceType.toLowerCase().trim();
                    } else {
                        newStatus = "maintenance";
                    }
                }
                
                log.info("   - Có dịch vụ đang chờ → Cập nhật vehicle status = {}", newStatus);
            } else {
                if (currentStatus != null && 
                    (currentStatus.equalsIgnoreCase("in_use") || 
                     currentStatus.equalsIgnoreCase("in-use") ||
                     currentStatus.equalsIgnoreCase("in use"))) {
                    newStatus = "in_use";
                } else {
                    newStatus = "ready";
                }
                log.info("   - Không có dịch vụ đang chờ → Cập nhật vehicle status = {}", newStatus);
            }
            
            if (newStatus != null && !newStatus.equals(currentStatus)) {
                vehicle.setStatus(newStatus);
                vehicleRepository.save(vehicle);
                vehicleRepository.flush();
                log.info("   ✅ Đã cập nhật vehicle status từ \"{}\" thành \"{}\"", currentStatus, newStatus);
            } else {
                log.info("   ℹ️ Vehicle status không thay đổi: {}", currentStatus);
            }
            
        } catch (Exception e) {
            log.error("   ❌ [SYNC ERROR] Lỗi khi đồng bộ trạng thái vehicle: {}", e.getMessage());
        }
    }
    
    /**
     * Đồng bộ trạng thái cho tất cả vehicles
     */
    @Transactional
    public void syncAllVehicleStatuses() {
        try {
            log.info("🔄 [SYNC ALL VEHICLES] Bắt đầu đồng bộ trạng thái cho tất cả vehicles...");
            List<Vehicle> allVehicles = vehicleRepository.findAll();
            for (Vehicle vehicle : allVehicles) {
                syncVehicleStatus(vehicle.getVehicleId());
            }
            log.info("✅ [SYNC ALL VEHICLES] Đã đồng bộ hoàn tất.");
        } catch (Exception e) {
            log.error("❌ [SYNC ALL ERROR] Lỗi khi đồng bộ tất cả vehicles: {}", e.getMessage());
        }
    }
}
