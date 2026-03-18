package com.example.reservationadminservice.repository.booking;

import com.example.reservationadminservice.model.booking.BookingVehicleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingVehicleGroupRepository extends JpaRepository<BookingVehicleGroup, Long> {
}

