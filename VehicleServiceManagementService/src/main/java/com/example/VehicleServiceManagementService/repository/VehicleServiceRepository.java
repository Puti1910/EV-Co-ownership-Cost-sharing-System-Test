package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.Vehicleservice;
import com.example.VehicleServiceManagementService.model.VehicleserviceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleServiceRepository extends JpaRepository<Vehicleservice, VehicleserviceId> {
    
    /**
     * Tìm tất cả các dịch vụ của một xe
     * @param vehicleId ID của xe
     * @return Danh sách các dịch vụ
     */
    List<Vehicleservice> findByVehicle_VehicleId(String vehicleId);
    
    /**
     * Tìm tất cả các dịch vụ theo service_id
     * @param serviceId ID của dịch vụ
     * @return Danh sách các đăng ký dịch vụ
     */
    List<Vehicleservice> findByService_ServiceId(String serviceId);
    
    /**
     * Tìm đăng ký dịch vụ theo service_id và vehicle_id (lấy bản ghi mới nhất)
     * @param serviceId ID của dịch vụ
     * @param vehicleId ID của xe
     * @return Optional Vehicleservice
     */
    Optional<Vehicleservice> findByIdServiceIdAndIdVehicleId(String serviceId, String vehicleId);
    
    /**
     * Kiểm tra đăng ký dịch vụ có tồn tại không
     * @param serviceId ID của dịch vụ
     * @param vehicleId ID của xe
     * @return true nếu tồn tại
     */
    boolean existsByService_ServiceIdAndVehicle_VehicleId(String serviceId, String vehicleId);
    
    /**
     * Xóa đăng ký dịch vụ theo service_id và vehicle_id (xóa tất cả)
     * @param serviceId ID của dịch vụ
     * @param vehicleId ID của xe
     */
    @Modifying
    @Transactional
    void deleteByIdServiceIdAndIdVehicleId(String serviceId, String vehicleId);
    
    /**
     * Xóa tất cả các dịch vụ của một xe
     * @param vehicleId ID của xe
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicleservice v WHERE v.vehicle.vehicleId = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") String vehicleId);
    
    /**
     * Kiểm tra duplicate bằng native query
     */
    long countByIdServiceIdAndIdVehicleId(String serviceId, String vehicleId);
    
    /**
     * Kiểm tra xem có dịch vụ đang chờ (pending/in_progress) không
     * Chỉ chặn duplicate nếu dịch vụ trước đó chưa completed
     */
    @Query("SELECT COUNT(v) FROM Vehicleservice v " +
           "WHERE v.id.serviceId = :serviceId AND v.id.vehicleId = :vehicleId " +
           "AND LOWER(v.status) IN ('pending', 'in_progress', 'in progress')")
    long countActiveByServiceIdAndVehicleId(@Param("serviceId") String serviceId, @Param("vehicleId") String vehicleId);

    /**
     * Lấy danh sách service template độc nhất từ bảng vehicleservice
     * Lấy tất cả các service templates, kể cả những cái không có service_id
     */
    @Query(value = "SELECT DISTINCT v.service_id, v.service_name, v.service_type " +
                   "FROM vehicle_management.vehicleservice v " +
                   "WHERE (v.service_name IS NOT NULL AND v.service_name != '') " +
                   "ORDER BY v.service_name", nativeQuery = true)
    List<Object[]> findDistinctServiceTemplates();
    
}

