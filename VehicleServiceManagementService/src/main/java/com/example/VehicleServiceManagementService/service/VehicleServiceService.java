package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import com.example.VehicleServiceManagementService.repository.ServiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleServiceService {
    private static final Logger log = LoggerFactory.getLogger(VehicleServiceService.class);

    @Autowired
    private VehicleServiceRepository vehicleServiceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

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
            
            // Tìm bản ghi mới nhất để cập nhật nếu có
            Optional<Vehicleservice> existingOpt = vehicleServiceRepository.findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(serviceId, vehicleId);
            
            Vehicleservice entityToSave;
            if (existingOpt.isPresent()) {
                Vehicleservice existing = existingOpt.get();
                // Chỉ cập nhật nếu bản ghi đó chưa hoàn thành (thường dùng cho các flow update từ controller)
                // Hoặc nếu đây là request cập nhật thông tin chung
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
                log.info("ℹ️ [SAVE] Không tìm thấy bản ghi cũ → tạo mới hoàn toàn");
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
     * Kiểm tra service và vehicle tồn tại
     */
    public ServiceType validateAndGetService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ với ID: " + serviceId));
    }

    /**
     * Kiểm tra vehicle tồn tại
     */
    public Vehicle validateAndGetVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe với ID: " + vehicleId));
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
     * Logic:
     * - Nếu có dịch vụ đang chờ (pending/in_progress), cập nhật vehicle status theo serviceType
     * - Nếu không có dịch vụ nào đang chờ, set vehicle status = "ready" (hoặc giữ "in_use" nếu đang là "in_use")
     * 
     * Ưu tiên status:
     * 1. maintenance (bảo dưỡng)
     * 2. repair (sửa chữa)
     * 3. checking (kiểm tra)
     * 4. in_use (đang sử dụng) - chỉ khi không có dịch vụ đang chờ
     * 5. ready (sẵn sàng) - mặc định
     */
    @Transactional
    public void syncVehicleStatus(Long vehicleId) {
        try {
            System.out.println("🔄 [SYNC VEHICLE STATUS] Bắt đầu đồng bộ trạng thái cho vehicle: " + vehicleId);
            
            // Lấy vehicle
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                System.out.println("   ⚠️ Vehicle không tồn tại: " + vehicleId);
                return;
            }
            
            Vehicle vehicle = vehicleOpt.get();
            String currentStatus = vehicle.getStatus();
            
            // Lấy tất cả dịch vụ đang chờ (pending/in_progress) của vehicle này
            List<Vehicleservice> activeServices = vehicleServiceRepository.findByVehicleId(vehicleId).stream()
                    .filter(vs -> {
                        String status = vs.getStatus();
                        if (status == null) return false;
                        String statusLower = status.toLowerCase().trim();
                        return statusLower.equals("pending") || 
                               statusLower.equals("in_progress") || 
                               statusLower.equals("in progress");
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("   - Số dịch vụ đang chờ: " + activeServices.size());
            
            String newStatus = null;
            
            if (!activeServices.isEmpty()) {
                // Có dịch vụ đang chờ - xác định status dựa trên serviceType
                // Ưu tiên: maintenance > repair > checking
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
                
                // Xác định status ưu tiên
                if (hasMaintenance) {
                    newStatus = "maintenance";
                } else if (hasRepair) {
                    newStatus = "repair";
                } else if (hasChecking) {
                    newStatus = "checking";
                } else {
                    // Nếu có dịch vụ khác nhưng không xác định được loại, dùng status đầu tiên
                    String firstServiceType = activeServices.get(0).getServiceType();
                    if (firstServiceType != null && !firstServiceType.trim().isEmpty()) {
                        newStatus = firstServiceType.toLowerCase().trim();
                    } else {
                        newStatus = "maintenance"; // Mặc định
                    }
                }
                
                System.out.println("   - Có dịch vụ đang chờ → Cập nhật vehicle status = " + newStatus);
            } else {
                // Không có dịch vụ nào đang chờ
                // Nếu vehicle đang là "in_use" hoặc "in-use", giữ nguyên
                // Nếu không, set về "ready"
                if (currentStatus != null && 
                    (currentStatus.equalsIgnoreCase("in_use") || 
                     currentStatus.equalsIgnoreCase("in-use") ||
                     currentStatus.equalsIgnoreCase("in use"))) {
                    newStatus = "in_use";
                    System.out.println("   - Không có dịch vụ đang chờ, giữ nguyên status = " + newStatus);
                } else {
                    newStatus = "ready";
                    System.out.println("   - Không có dịch vụ đang chờ → Cập nhật vehicle status = " + newStatus);
                }
            }
            
            // Chỉ cập nhật nếu status thay đổi
            if (newStatus != null && !newStatus.equals(currentStatus)) {
                vehicle.setStatus(newStatus);
                vehicleRepository.save(vehicle);
                vehicleRepository.flush();
                System.out.println("   ✅ Đã cập nhật vehicle status từ \"" + currentStatus + "\" thành \"" + newStatus + "\"");
            } else {
                System.out.println("   ℹ️ Vehicle status không thay đổi: " + currentStatus);
            }
            
        } catch (Exception e) {
            System.err.println("   ❌ [SYNC ERROR] Lỗi khi đồng bộ trạng thái vehicle: " + e.getMessage());
            e.printStackTrace();
            // Không throw exception để không ảnh hưởng đến luồng chính
        }
    }
    
    /**
     * Đồng bộ trạng thái cho tất cả vehicles
     */
    @Transactional
    public void syncAllVehicleStatuses() {
        try {
            System.out.println("🔄 [SYNC ALL VEHICLES] Bắt đầu đồng bộ trạng thái cho tất cả vehicles...");
            List<Vehicle> allVehicles = vehicleRepository.findAll();
            int count = 0;
            for (Vehicle vehicle : allVehicles) {
                syncVehicleStatus(vehicle.getVehicleId());
                count++;
            }
            System.out.println("✅ [SYNC ALL VEHICLES] Đã đồng bộ " + count + " vehicles");
        } catch (Exception e) {
            System.err.println("❌ [SYNC ALL ERROR] Lỗi khi đồng bộ tất cả vehicles: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

