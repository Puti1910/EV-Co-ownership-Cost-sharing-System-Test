package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    // JpaRepository đã cung cấp các phương thức cơ bản như save(), findAll(), findById(), deleteById()...

    /**
     * Tìm tất cả các xe thuộc về một nhóm xe theo groupId
     * @param groupId ID của nhóm xe
     * @return Danh sách các xe thuộc nhóm
     */
    @Query("SELECT v FROM Vehicle v WHERE v.group.groupId = :groupId")
    List<Vehicle> findByGroupId(@Param("groupId") String groupId);

    /**
     * Đếm số lượng xe thuộc về một nhóm xe
     * @param groupId ID của nhóm xe
     * @return Số lượng xe
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.group.groupId = :groupId")
    long countByGroupId(@Param("groupId") String groupId);

    /**
     * Tìm xe theo biển số xe
     * @param vehicleNumber Biển số xe
     * @return Optional Vehicle
     */
    @Query("SELECT v FROM Vehicle v WHERE v.vehicleNumber = :vehicleNumber")
    java.util.Optional<Vehicle> findByVehicleNumber(@Param("vehicleNumber") String vehicleNumber);

    /**
     * Kiểm tra xem biển số xe đã tồn tại chưa (trừ xe hiện tại khi update)
     * @param vehicleNumber Biển số xe
     * @param vehicleId ID của xe (để loại trừ khi update)
     * @return true nếu biển số đã tồn tại
     */
    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE v.vehicleNumber = :vehicleNumber AND v.vehicleNumber IS NOT NULL AND v.vehicleNumber != '' AND v.vehicleId != :vehicleId")
    boolean existsByVehicleNumberAndVehicleIdNot(@Param("vehicleNumber") String vehicleNumber, @Param("vehicleId") String vehicleId);

    /**
     * Kiểm tra xem biển số xe đã tồn tại chưa (khi thêm mới)
     * @param vehicleNumber Biển số xe
     * @return true nếu biển số đã tồn tại
     */
    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE v.vehicleNumber = :vehicleNumber AND v.vehicleNumber IS NOT NULL AND v.vehicleNumber != ''")
    boolean existsByVehicleNumber(@Param("vehicleNumber") String vehicleNumber);
}
