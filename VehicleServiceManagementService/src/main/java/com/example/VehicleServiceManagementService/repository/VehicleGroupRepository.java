package com.example.VehicleServiceManagementService.repository;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleGroupRepository extends JpaRepository<Vehiclegroup, Long> {
    // Các phương thức truy vấn tùy chỉnh có thể thêm vào đây nếu cần

    Optional<Vehiclegroup> findById(Long groupId);

    void deleteById(Long groupId);}
