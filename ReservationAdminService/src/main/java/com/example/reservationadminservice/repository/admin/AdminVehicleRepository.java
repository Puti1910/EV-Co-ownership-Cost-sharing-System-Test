package com.example.reservationadminservice.repository.admin;

import com.example.reservationadminservice.model.VehicleAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminVehicleRepository extends JpaRepository<VehicleAdmin, Long> {
    List<VehicleAdmin> findByGroupId(Long groupId);
}


