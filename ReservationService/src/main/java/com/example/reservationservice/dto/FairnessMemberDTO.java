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
public class FairnessMemberDTO {
    private Integer userId;
    private String fullName;
    private String email;
    private Double ownershipPercentage;
    private Double usageHours;
    private Double usagePercentage;
    private Double difference;
    private Double fairnessScore;
    private String priority;
    private LocalDateTime lastUsageEnd;
    private LocalDateTime nextReservationStart;
}

