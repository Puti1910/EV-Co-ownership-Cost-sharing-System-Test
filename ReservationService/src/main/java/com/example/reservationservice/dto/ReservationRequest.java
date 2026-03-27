package com.example.reservationservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private Integer userId;
    private Integer vehicleId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String purpose;
    private String status;
}

