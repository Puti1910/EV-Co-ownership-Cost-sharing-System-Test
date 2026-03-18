package com.example.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FairnessSummaryDTO {
    private Integer vehicleId;
    private String vehicleName;
    private Integer groupId;
    private String groupName;
    private Double fairnessIndex;
    private Double totalUsageHours;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDateTime generatedAt;
    @Builder.Default
    private List<FairnessMemberDTO> members = Collections.emptyList();
    @Builder.Default
    private List<FairnessReservationDTO> reservations = Collections.emptyList();
    @Builder.Default
    private List<FairnessAvailabilityDTO> availability = Collections.emptyList();
    @Builder.Default
    private List<Integer> priorityQueue = Collections.emptyList();
}