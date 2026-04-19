package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.Vehicleservice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleServiceRepository extends JpaRepository<Vehicleservice, Long> {
    
    /**
     * Tìm tất cả các dịch vụ của một xe
     * @param vehicleId ID của xe
     * @return Danh sách các dịch vụ
     */
    List<Vehicleservice> findByVehicleId(Long vehicleId);
    
    /**
     * Tìm tất cả các dịch vụ theo service_id
     * @param serviceId ID của dịch vụ
     * @return Danh sách các đăng ký dịch vụ
     */
    List<Vehicleservice> findByServiceId(Long serviceId);
    
    /**
     * Tìm đăng ký dịch vụ theo service_id và vehicle_id (lấy bản ghi mới nhất)
     * @param serviceId ID của dịch vụ
     * @param vehicleId ID của xe
     * @return Optional Vehicleservice
     */
    default Optional<Vehicleservice> findByIdServiceIdAndIdVehicleId(Long serviceId, Long vehicleId) {
        return findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(serviceId, vehicleId);
    }
    
    Optional<Vehicleservice> findTopByServiceIdAndVehicleIdOrderByRequestDateDesc(Long serviceId, Long vehicleId);
    
    /**
     * Kiểm tra đăng ký dịch vụ có tồn tại không
     */
    boolean existsByServiceIdAndVehicleId(Long serviceId, Long vehicleId);
    
    /**
     * Xóa đăng ký dịch vụ theo service_id và vehicle_id (xóa tất cả)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicleservice v WHERE v.serviceId = :serviceId AND v.vehicleId = :vehicleId")
    void deleteByServiceIdAndVehicleId(@Param("serviceId") Long serviceId, @Param("vehicleId") Long vehicleId);
    
    /**
     * Xóa tất cả các dịch vụ của một xe
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicleservice v WHERE v.vehicleId = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Xóa tất cả đăng ký dịch vụ theo service_id
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicleservice v WHERE v.serviceId = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
    
    /**
     * Đếm số lượng theo cặp ID
     */
    long countByServiceIdAndVehicleId(Long serviceId, Long vehicleId);
    
    /**
     * Kiểm tra xem có dịch vụ đang chờ (pending/in_progress) không
     */
    @Query("SELECT COUNT(v) FROM Vehicleservice v " +
           "WHERE v.serviceId = :serviceId AND v.vehicleId = :vehicleId " +
           "AND LOWER(v.status) IN ('pending', 'in_progress', 'in progress')")
    long countActiveByServiceIdAndVehicleId(@Param("serviceId") Long serviceId, @Param("vehicleId") Long vehicleId);

    /**
     * Lấy danh sách service template độc nhất
     */
    @Query(value = "SELECT DISTINCT v.service_id, v.service_name, v.service_type " +
                   "FROM vehicle_management.vehicleservice v " +
                   "WHERE (v.service_name IS NOT NULL AND v.service_name != '') " +
                   "ORDER BY v.service_name", nativeQuery = true)
    List<Object[]> findDistinctServiceTemplates();
    
}

