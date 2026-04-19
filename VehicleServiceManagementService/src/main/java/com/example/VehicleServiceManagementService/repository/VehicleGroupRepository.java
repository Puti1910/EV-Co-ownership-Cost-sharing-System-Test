package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleGroupRepository extends JpaRepository<Vehiclegroup, Long> {
    // JpaRepository đã cung cấp findById(Long) và deleteById(Long) mặc định
}
