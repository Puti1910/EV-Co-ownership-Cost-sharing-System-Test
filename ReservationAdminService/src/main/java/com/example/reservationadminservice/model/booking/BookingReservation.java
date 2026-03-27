package com.example.reservationadminservice.model.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity cho bảng reservations trong booking database (read-only)
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
public class BookingReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;
    
    @Column(name = "vehicle_id")
    private Long vehicleId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;
    
    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;
    
    @Column(name = "purpose")
    private String purpose;
    
    @Column(name = "status")
    private String status;
    
    // Note: created_at column không tồn tại trong bảng reservations
    // Đã xóa field này để tránh lỗi SQL
}

