package com.example.reservationservice.dto;

import lombok.Data;

@Data
public class CheckpointScanRequest {
    private String token;
    private Double latitude;
    private Double longitude;
}

