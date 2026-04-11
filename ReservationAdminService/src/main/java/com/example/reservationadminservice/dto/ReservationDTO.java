package com.example.reservationadminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private Long reservationId;
    private Long vehicleId;
    private String vehicleName;
    private Long userId;
    private String userName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
}

