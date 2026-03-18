package com.example.reservationadminservice.repository.admin;

import com.example.reservationadminservice.model.ReservationAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminReservationRepository extends JpaRepository<ReservationAdmin, Long> {
    List<ReservationAdmin> findByStatus(String status);
    List<ReservationAdmin> findByUserId(Long userId);
    List<ReservationAdmin> findByVehicleId(Long vehicleId);
    List<ReservationAdmin> findByStartDatetimeBetween(LocalDateTime start, LocalDateTime end);
}

