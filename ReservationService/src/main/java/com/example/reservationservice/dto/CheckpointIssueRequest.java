package com.example.reservationservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckpointIssueRequest {
    private String type;

    @Size(max = 50, message = "issuedBy must not exceed 50 characters")
    private String issuedBy;

    private String notes;
    private Integer validMinutes;
}
