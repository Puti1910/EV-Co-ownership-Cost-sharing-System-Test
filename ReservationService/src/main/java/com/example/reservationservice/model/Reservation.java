package com.example.reservationservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;
    
    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;
    
    private String purpose;

    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;

    public enum Status {
        BOOKED,
        IN_USE,
        COMPLETED,
        CANCELLED
    }
}
