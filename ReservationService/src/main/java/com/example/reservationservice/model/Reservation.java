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
    private Long vehicleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;
    
    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;
    
    @Column(name = "purpose", length = 1000)
    private String purpose;

    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long id) { this.reservationId = id; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long id) { this.vehicleId = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long id) { this.userId = id; }
    public LocalDateTime getStartDatetime() { return startDatetime; }
    public void setStartDatetime(LocalDateTime dt) { this.startDatetime = dt; }
    public LocalDateTime getEndDatetime() { return endDatetime; }
    public void setEndDatetime(LocalDateTime dt) { this.endDatetime = dt; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String p) { this.purpose = p; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }

    public enum Status {
        BOOKED,
        IN_USE,
        COMPLETED,
        CANCELLED
    }
}
