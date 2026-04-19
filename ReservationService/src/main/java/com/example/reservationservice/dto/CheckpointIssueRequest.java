package com.example.reservationservice.dto;

import lombok.Data;

@Data
public class CheckpointIssueRequest {
    private String type;
    private String issuedBy;
    private String notes;
    private Integer validMinutes;
}

