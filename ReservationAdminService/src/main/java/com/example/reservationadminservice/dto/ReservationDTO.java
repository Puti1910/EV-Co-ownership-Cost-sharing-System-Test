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
    @jakarta.validation.constraints.NotBlank(message = "purpose không được để trống")
    @jakarta.validation.constraints.Size(max = 255, message = "Mục đích sử dụng không được quá 255 ký tự")
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
}

