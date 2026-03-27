package com.example.reservationservice.repository;

import com.example.reservationservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVehicleId(Long vehicleId);
    boolean existsByVehicleId(Long vehicleId);
    
    // Tìm vehicle theo external_vehicle_id (ID từ vehicle_management)
    Optional<Vehicle> findByExternalVehicleId(String externalVehicleId);
    boolean existsByExternalVehicleId(String externalVehicleId);
    
    // Insert vehicle với vehicle_id cụ thể (bỏ qua AUTO_INCREMENT)
    @Modifying
    @Query(value = "INSERT INTO vehicles (vehicle_id, external_vehicle_id, vehicle_name, license_plate, vehicle_type, group_id, status) " +
                   "VALUES (:vehicleId, :externalVehicleId, :vehicleName, :licensePlate, :vehicleType, :groupId, :status)", 
           nativeQuery = true)
    void insertWithVehicleId(@Param("vehicleId") Long vehicleId,
                             @Param("externalVehicleId") String externalVehicleId,
                             @Param("vehicleName") String vehicleName,
                             @Param("licensePlate") String licensePlate,
                             @Param("vehicleType") String vehicleType,
                             @Param("groupId") String groupId,
                             @Param("status") String status);
}

