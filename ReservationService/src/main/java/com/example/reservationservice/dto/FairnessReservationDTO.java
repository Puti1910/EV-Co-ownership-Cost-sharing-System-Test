package com.example.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FairnessReservationDTO {
    private Long reservationId;
    private Integer vehicleId;
    private String vehicleName;
    private Integer userId;
    private String userName;
    private LocalDateTime start;
    private LocalDateTime end;
    private String status;
    private String purpose;
}

