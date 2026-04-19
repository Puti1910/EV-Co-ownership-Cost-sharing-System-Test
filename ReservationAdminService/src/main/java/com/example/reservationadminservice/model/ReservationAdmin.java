package com.example.reservationadminservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.sql.Timestamp;

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
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @jakarta.validation.constraints.Size(max = 1000, message = "Mục đích sử dụng không được quá 1000 ký tự")
    @Column(name = "purpose", length = 1000)
    private String purpose;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = true, updatable = false, insertable = false)
    private Timestamp createdAt;
}
