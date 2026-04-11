package com.example.reservationadminservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class ReservationAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_datetime", nullable = false)
    private java.time.LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private java.time.LocalDateTime endDatetime;

    @jakarta.validation.constraints.Size(max = 255, message = "Mục đích sử dụng không được quá 255 ký tự")
    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = true, updatable = false, insertable = false)
    private java.sql.Timestamp createdAt;
}
