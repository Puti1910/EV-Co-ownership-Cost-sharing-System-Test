package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.ServiceType;
import com.example.VehicleServiceManagementService.repository.ServiceRepository;
import com.example.VehicleServiceManagementService.repository.VehicleServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private VehicleServiceRepository vehicleServiceRepository;

    /**
     * Lấy tất cả các dịch vụ từ bảng service
     * @return Danh sách tất cả dịch vụ
     */
    public List<ServiceType> getAllServices() {
        try {
            List<ServiceType> services = serviceRepository.findAll();
            if (!services.isEmpty()) {
                return services;
            }
            List<ServiceType> bootstrapped = bootstrapFromVehicleHistory();
            return bootstrapped.isEmpty() ? services : bootstrapped;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách dịch vụ: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách dịch vụ", e);
        }
    }

    /**
     * Lấy dịch vụ theo ID
     * @param serviceId ID của dịch vụ
     * @return ServiceType nếu tìm thấy, null nếu không
     */
    public ServiceType getServiceById(Long serviceId) {
        if (serviceId == null || serviceId <= 0) {
            throw new IllegalArgumentException("Service ID không hợp lệ");
        }
        Optional<ServiceType> service = serviceRepository.findById(serviceId);
        return service.orElse(null);
    }

    /**
     * Kiểm tra dịch vụ có tồn tại không
     */
    public boolean existsById(Long serviceId) {
        if (serviceId == null || serviceId <= 0) {
            return false;
        }
        return serviceRepository.existsById(serviceId);
    }

    /**
     * Tìm dịch vụ theo tên
     * @param serviceName Tên dịch vụ
     * @return ServiceType nếu tìm thấy, null nếu không
     */
    public ServiceType getServiceByName(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return null;
        }
        Optional<ServiceType> service = serviceRepository.findByServiceName(serviceName);
        return service.orElse(null);
    }

    /**
     * Thêm dịch vụ mới
     * @param service Dịch vụ cần thêm
     * @return ServiceType đã được lưu
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws DataIntegrityViolationException nếu serviceId đã tồn tại
     */
    public ServiceType addService(ServiceType service) {
        if (service == null) {
            throw new IllegalArgumentException("Service không được null");
        }
        
        if (service.getServiceName() == null || service.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống");
        }
        if (service.getServiceType() == null || service.getServiceType().trim().isEmpty()) {
            throw new IllegalArgumentException("Loại dịch vụ không được để trống");
        }

        try {
            return serviceRepository.save(service);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Không thể thêm dịch vụ: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật dịch vụ
     * @param serviceId ID của dịch vụ cần cập nhật
     * @param service Dịch vụ với thông tin mới
     * @return ServiceType đã được cập nhật, null nếu không tìm thấy
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     */
    public ServiceType updateService(Long serviceId, ServiceType service) {
        if (serviceId == null || serviceId <= 0) {
            throw new IllegalArgumentException("Service ID không hợp lệ");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service không được null");
        }
        if (service.getServiceName() == null || service.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống");
        }
        if (service.getServiceType() == null || service.getServiceType().trim().isEmpty()) {
            throw new IllegalArgumentException("Loại dịch vụ không được để trống");
        }

        Optional<ServiceType> existingServiceOpt = serviceRepository.findById(serviceId);
        if (existingServiceOpt.isPresent()) {
            ServiceType existingService = existingServiceOpt.get();
            existingService.setServiceName(service.getServiceName());
            existingService.setServiceType(service.getServiceType());
            return serviceRepository.save(existingService);
        }
        return null;
    }

    /**
     * Xóa dịch vụ
     * @param serviceId ID của dịch vụ cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy
     * @throws DataIntegrityViolationException nếu dịch vụ đang được sử dụng
     */
    public boolean deleteService(Long serviceId) {
        if (serviceId == null || serviceId <= 0) {
            throw new IllegalArgumentException("Service ID không hợp lệ");
        }

        if (!serviceRepository.existsById(serviceId)) {
            return false;
        }

        try {
            // Xóa tất cả các liên kết trong bảng vehicleservice trước để tránh lỗi khóa ngoại
            vehicleServiceRepository.deleteByServiceId(serviceId);
            serviceRepository.deleteById(serviceId);
            return true;
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException(
                "Không thể xóa dịch vụ '" + serviceId + "' vì vẫn còn ràng buộc dữ liệu không thể xóa tự động", e);
        }
    }

    /**
     * Đếm tổng số dịch vụ
     * @return Số lượng dịch vụ
     */
    public long count() {
        return serviceRepository.count();
    }

    /**
     * Lấy danh sách các loại dịch vụ duy nhất
     * @return Danh sách các loại dịch vụ
     */
    public List<String> getDistinctServiceTypes() {
        return serviceRepository.findDistinctServiceTypes();
    }

    /**
     * Lấy danh sách dịch vụ theo loại
     * @param serviceType Loại dịch vụ
     * @return Danh sách dịch vụ
     */
    public List<ServiceType> getServicesByType(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return getAllServices();
        }
        return serviceRepository.findByServiceType(serviceType);
    }

    /**
     * Lấy danh sách service templates duy nhất từ bảng vehicleservice
     * @return Danh sách ServiceType từ vehicleservice
     */
    public List<ServiceType> getDistinctServiceTemplatesFromVehicleService() {
        System.out.println("🔵 [ServiceService] Bắt đầu lấy service templates từ bảng vehicleservice...");
        List<Object[]> templates = vehicleServiceRepository.findDistinctServiceTemplates();
        
        if (CollectionUtils.isEmpty(templates)) {
            return new ArrayList<>();
        }

        List<ServiceType> result = new ArrayList<>();
        for (Object[] row : templates) {
            Long rawId = row[0] != null ? Long.valueOf(row[0].toString()) : null;
            String rawName = row[1] != null ? row[1].toString().trim() : null;
            String rawType = row[2] != null ? row[2].toString().trim() : null;

            if (rawId == null) {
                continue; 
            }
            
            if (!StringUtils.hasText(rawName)) {
                rawName = "Service " + rawId;
            }
            if (!StringUtils.hasText(rawType)) {
                rawType = "maintenance";
            }

            ServiceType service = new ServiceType();
            service.setServiceId(rawId);
            service.setServiceName(rawName);
            service.setServiceType(rawType.toLowerCase());
            result.add(service);
        }
        return result;
    }
    
    /**
     * Đồng bộ danh sách dịch vụ từ bảng vehicleservice khi bảng service trống
     */
    private List<ServiceType> bootstrapFromVehicleHistory() {
        List<Object[]> templates = vehicleServiceRepository.findDistinctServiceTemplates();
        if (CollectionUtils.isEmpty(templates)) {
            return new ArrayList<>();
        }

        List<ServiceType> created = new ArrayList<>();
        for (Object[] row : templates) {
            Long rawId = row[0] != null ? Long.valueOf(row[0].toString()) : null;
            String rawName = row[1] != null ? row[1].toString().trim() : null;
            String rawType = row[2] != null ? row[2].toString().trim() : null;

            if (rawId == null) {
                continue;
            }
            if (!StringUtils.hasText(rawName)) {
                rawName = "Service " + rawId;
            }
            if (!StringUtils.hasText(rawType)) {
                rawType = "maintenance";
            }

            if (serviceRepository.existsById(rawId)) {
                continue;
            }

            ServiceType service = new ServiceType();
            service.setServiceId(rawId);
            service.setServiceName(rawName);
            service.setServiceType(rawType.toLowerCase());

            ServiceType saved = serviceRepository.save(service);
            created.add(saved);
        }
        return created;
    }
}
