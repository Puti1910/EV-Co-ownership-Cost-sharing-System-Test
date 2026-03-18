package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.model.VehicleserviceId;
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
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VehicleServiceService {

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
     * Sử dụng id AUTO_INCREMENT làm primary key
     * Cho phép đăng ký cùng một dịch vụ (service_id) cho cùng một xe (vehicle_id) nhiều lần
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public Vehicleservice saveVehicleService(Vehicleservice vehicleService) {
        log.info("🔒 [SAVE] Bắt đầu save Vehicleservice vào database trong transaction...");
        
        try {
            String serviceId = vehicleService.getServiceId();
            String vehicleId = vehicleService.getVehicleId();
            
            log.info("🔒 [SAVE] Saving entity - serviceId: {}, vehicleId: {}", serviceId, vehicleId);
            
            VehicleserviceId id = new VehicleserviceId(serviceId, vehicleId);
            vehicleService.setId(id);
            
            // Kiểm tra duplicate đã được xử lý ở controller layer
            // Ở đây chỉ cần đảm bảo không có conflict khi save
            log.debug("🔒 [SAVE] Kiểm tra lại trước khi save...");
            
            // Kiểm tra xem có dịch vụ đang chờ không (double check)
            long activeCount = vehicleServiceRepository.countActiveByServiceIdAndVehicleId(serviceId, vehicleId);
            if (activeCount > 0) {
                log.warn("⚠️ [SAVE] Vẫn còn {} dịch vụ đang chờ, không thể save", activeCount);
                throw new IllegalArgumentException("Dịch vụ này đã được đăng ký cho xe này và đang trong trạng thái chờ xử lý.");
            }
            
            log.info("🔒 [SAVE] Đăng ký dịch vụ mới hoặc cập nhật bản ghi hiện có - serviceId: {}, vehicleId: {}", serviceId, vehicleId);
            
            // Đảm bảo service và vehicle được set
            if (vehicleService.getService() == null && serviceId != null) {
                ServiceType serviceEntity = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
                vehicleService.setService(serviceEntity);
            }
            
            if (vehicleService.getVehicle() == null && vehicleId != null) {
                Vehicle vehicleEntity = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
                vehicleService.setVehicle(vehicleEntity);
            }
            
            // Đảm bảo requestDate được set
            if (vehicleService.getRequestDate() == null) {
                vehicleService.setRequestDate(Instant.now());
            }
            
            // Đảm bảo status được set
            if (vehicleService.getStatus() == null || vehicleService.getStatus().trim().isEmpty()) {
                vehicleService.setStatus("pending");
            }
            
            Vehicleservice entityToSave = vehicleService;
            
            Optional<Vehicleservice> existingOpt = vehicleServiceRepository.findById(id);
            if (existingOpt.isPresent()) {
                Vehicleservice existing = existingOpt.get();
                log.info("ℹ️ [SAVE] Bản ghi đã tồn tại → cập nhật thông tin");
                existing.setService(vehicleService.getService());
                existing.setVehicle(vehicleService.getVehicle());
                existing.setServiceName(vehicleService.getServiceName());
                existing.setServiceDescription(vehicleService.getServiceDescription());
                existing.setServiceType(vehicleService.getServiceType());
                existing.setRequestDate(vehicleService.getRequestDate());
                existing.setStatus(vehicleService.getStatus());
                existing.setCompletionDate(vehicleService.getCompletionDate());
                existing.setGroupRefId(vehicleService.getGroupRefId());
                existing.setRequestedByUserId(vehicleService.getRequestedByUserId());
                existing.setRequestedByUserName(vehicleService.getRequestedByUserName());
                existing.setPreferredStartDatetime(vehicleService.getPreferredStartDatetime());
                existing.setPreferredEndDatetime(vehicleService.getPreferredEndDatetime());
                entityToSave = existing;
            }
            
            // Lưu entity (id là composite key serviceId + vehicleId)
            System.out.println("🔒 [SAVE] Đang lưu Vehicleservice vào database...");
            System.out.println("🔒 [SAVE] Entity trước khi save - serviceId: " + entityToSave.getServiceId() + 
                             ", vehicleId: " + entityToSave.getVehicleId() + ", status: " + entityToSave.getStatus() + 
                             ", requestDate: " + entityToSave.getRequestDate());
            System.out.println("🔒 [SAVE] Service entity: " + (entityToSave.getService() != null ? entityToSave.getService().getServiceId() : "NULL") +
                             ", Vehicle entity: " + (entityToSave.getVehicle() != null ? entityToSave.getVehicle().getVehicleId() : "NULL"));
            log.info("🔒 [SAVE] Đang lưu Vehicleservice vào database...");
            log.info("🔒 [SAVE] Entity trước khi save - serviceId: {}, vehicleId: {}, status: {}, requestDate: {}", 
                    entityToSave.getServiceId(), entityToSave.getVehicleId(), 
                    entityToSave.getStatus(), entityToSave.getRequestDate());
            
            Vehicleservice savedService = vehicleServiceRepository.save(entityToSave);
            System.out.println("🔒 [SAVE] Sau khi gọi save(), đang flush...");
            vehicleServiceRepository.flush();
            System.out.println("🔒 [SAVE] Flush hoàn tất!");
            log.info("🔒 [SAVE] Sau khi gọi save(), đang flush...");
            log.info("🔒 [SAVE] Flush hoàn tất!");
            
            System.out.println("✅ [SAVE] Entity đã được lưu thành công vào database!");
            System.out.println("✅ [SAVE] Key: serviceId=" + savedService.getServiceId() + ", vehicleId=" + savedService.getVehicleId() + 
                             ", status=" + savedService.getStatus() + ", id=" + savedService.getId());
            log.info("✅ [SAVE] Entity đã được lưu thành công vào database!");
            log.info("✅ [SAVE] Key: serviceId={}, vehicleId={}, status={}, id={}", 
                    savedService.getServiceId(), savedService.getVehicleId(), 
                    savedService.getStatus(), savedService.getId());
            
            // Đồng bộ trạng thái vehicle sau khi lưu vehicleservice
            try {
                syncVehicleStatus(vehicleId);
            } catch (Exception e) {
                log.warn("⚠️ [SAVE] Lỗi khi đồng bộ vehicle status (không ảnh hưởng đến việc lưu): {}", e.getMessage());
                // Không throw exception để không ảnh hưởng đến việc lưu vehicleservice
            }
            
            return savedService;
            
        } catch (Exception e) {
            log.error("❌ [SAVE] Lỗi khi save Vehicleservice vào database", e);
            log.error("❌ [SAVE] Error type: {}, message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ [SAVE] Cause: {}", e.getCause().getMessage());
            }
            // Re-throw exception để transaction rollback
            throw e;
        }
    }

    /**
     * Kiểm tra service và vehicle tồn tại
     */
    public ServiceType validateAndGetService(String serviceId) {
        Optional<ServiceType> serviceOpt = serviceRepository.findById(serviceId);
        if (serviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy dịch vụ với ID: " + serviceId);
        }
        return serviceOpt.get();
    }

    /**
     * Kiểm tra vehicle tồn tại
     */
    public Vehicle validateAndGetVehicle(String vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy xe với ID: " + vehicleId);
        }
        return vehicleOpt.get();
    }

    /**
     * Tạo Vehicleservice entity từ request data
     */
    public Vehicleservice createVehicleService(
            ServiceType service,
            Vehicle vehicle,
            String serviceDescription,
            String status,
            Integer groupRefId,
            Integer requestedByUserId,
            String requestedByName,
            LocalDateTime preferredStart,
            LocalDateTime preferredEnd) {
        
        Vehicleservice vehicleService = new Vehicleservice();
        
        // id sẽ được tự động generate bởi database (AUTO_INCREMENT)
        // Không cần set id
        
        vehicleService.setService(service);
        vehicleService.setVehicle(vehicle);
        vehicleService.setId(new VehicleserviceId(service.getServiceId(), vehicle.getVehicleId()));
        vehicleService.setServiceName(service.getServiceName());
        vehicleService.setServiceType(service.getServiceType());
        
        if (serviceDescription != null && !serviceDescription.trim().isEmpty()) {
            vehicleService.setServiceDescription(serviceDescription.trim());
        }
        
        if (status == null || status.trim().isEmpty()) {
            status = "pending";
        }
        vehicleService.setStatus(status);
        vehicleService.setRequestDate(Instant.now());
        vehicleService.setCompletionDate(null);
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
    public void syncVehicleStatus(String vehicleId) {
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
            List<Vehicleservice> activeServices = vehicleServiceRepository.findByVehicle_VehicleId(vehicleId).stream()
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

