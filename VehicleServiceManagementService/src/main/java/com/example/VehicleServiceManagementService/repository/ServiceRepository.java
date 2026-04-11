package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceType, String> {

    /**
     * Tìm dịch vụ theo tên
     * @param serviceName Tên dịch vụ
     * @return Optional ServiceType
     */
    Optional<ServiceType> findByServiceName(String serviceName);

    /**
     * Tìm dịch vụ theo loại
     * @param serviceType Loại dịch vụ
     * @return Danh sách dịch vụ
     */
    List<ServiceType> findByServiceType(String serviceType);

    /**
     * Lấy danh sách các loại dịch vụ duy nhất
     * @return Danh sách các loại dịch vụ
     */
    @Query("SELECT DISTINCT s.serviceType FROM ServiceType s ORDER BY s.serviceType")
    List<String> findDistinctServiceTypes();

    /**
     * Lấy service_id lớn nhất có prefix "SRV"
     * @return Service ID lớn nhất (ví dụ: "SRV003")
     */
    @Query("SELECT MAX(s.serviceId) FROM ServiceType s WHERE s.serviceId LIKE 'SRV%'")
    String findMaxServiceIdWithPrefix();
}
