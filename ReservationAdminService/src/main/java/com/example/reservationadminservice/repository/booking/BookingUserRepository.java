package com.example.reservationadminservice.repository.booking;

import com.example.reservationadminservice.model.booking.BookingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository để đọc dữ liệu user từ booking database (read-only)
 */
@Repository
public interface BookingUserRepository extends JpaRepository<BookingUser, Long> {
}

