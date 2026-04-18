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

    @jakarta.validation.constraints.Min(value = 1, message = "vehicleId must be >= 1")
    @jakarta.validation.constraints.Max(value = 2147483647L, message = "vehicleId exceeds Integer range")
    private Long vehicleId;
    
    private String vehicleName;

    @jakarta.validation.constraints.Min(value = 1, message = "userId must be >= 1")
    @jakarta.validation.constraints.Max(value = Long.MAX_VALUE, message = "userId exceeds Long range")
    private Long userId;
    private String userName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    @jakarta.validation.constraints.Size(min = 1, max = 255, message = "Mục đích sử dụng phải từ 1 đến 255 ký tự")
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
}

