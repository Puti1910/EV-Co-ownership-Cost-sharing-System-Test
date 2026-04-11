package com.example.reservationadminservice.repository.booking;

import com.example.reservationadminservice.model.booking.BookingReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository để đọc dữ liệu từ booking database (read-only)
 */
@Repository
public interface BookingReservationRepository extends JpaRepository<BookingReservation, Long> {
}


