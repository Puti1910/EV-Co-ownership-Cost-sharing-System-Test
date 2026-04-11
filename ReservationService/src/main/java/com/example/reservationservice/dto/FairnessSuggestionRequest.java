package com.example.reservationservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FairnessSuggestionRequest {
    private Integer userId;
    private LocalDateTime desiredStart;
    private Double durationHours = 2.0;
}

