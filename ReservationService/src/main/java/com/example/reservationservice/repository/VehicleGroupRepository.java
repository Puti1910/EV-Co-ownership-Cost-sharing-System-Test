package com.example.reservationservice.repository;

import com.example.reservationservice.model.VehicleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleGroupRepository extends JpaRepository<VehicleGroup, String> {
    Optional<VehicleGroup> findByGroupId(String groupId);
    boolean existsByGroupId(String groupId);
}

