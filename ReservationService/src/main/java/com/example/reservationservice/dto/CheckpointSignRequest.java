package com.example.reservationservice.dto;

import lombok.Data;

@Data
public class CheckpointSignRequest {
    private String signerName;
    private String signerIdNumber;
    private String signatureData;
}

