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
public class FairnessSuggestionResponse {
    private Integer vehicleId;
    private Integer userId;
    private boolean approved;
    private String priority;
    private String reason;
    private LocalDateTime requestedStart;
    private LocalDateTime requestedEnd;
    private FairnessMemberDTO applicant;
    @Builder.Default
    private List<FairnessReservationDTO> conflicts = Collections.emptyList();
    @Builder.Default
    private List<FairnessAvailabilityDTO> recommendations = Collections.emptyList();
}

