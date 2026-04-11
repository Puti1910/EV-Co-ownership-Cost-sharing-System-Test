package com.example.reservationservice.dto;

import com.example.reservationservice.model.ReservationCheckpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointDTO {
    private Long checkpointId;
    private Long reservationId;
    private ReservationCheckpoint.CheckpointType checkpointType;
    private ReservationCheckpoint.CheckpointStatus status;
    private String qrToken;
    private String qrPayload;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime scannedAt;
    private LocalDateTime signedAt;
    private String signerName;
    private String signerIdNumber;
    private String signatureData;
    private Double latitude;
    private Double longitude;
    private String notes;
}

