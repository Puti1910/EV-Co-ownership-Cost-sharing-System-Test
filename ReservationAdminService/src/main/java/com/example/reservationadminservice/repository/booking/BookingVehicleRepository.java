package com.example.reservationadminservice.repository.booking;

import com.example.reservationadminservice.model.booking.BookingVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository để đọc dữ liệu vehicle từ booking database (read-only)
 */
@Repository
public interface BookingVehicleRepository extends JpaRepository<BookingVehicle, Long> {
}


