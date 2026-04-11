package com.example.reservationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_checkpoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint_type")
    private CheckpointType checkpointType;

    @Enumerated(EnumType.STRING)
    private CheckpointStatus status = CheckpointStatus.PENDING;

    @Column(name = "qr_token", unique = true, nullable = false, length = 128)
    private String qrToken;

    @Column(name = "issued_by")
    private String issuedBy;

    @CreationTimestamp
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signer_name")
    private String signerName;

    @Column(name = "signer_id_number")
    private String signerIdNumber;

    @Column(name = "signature_data", columnDefinition = "LONGTEXT")
    private String signatureData;

    private Double latitude;
    private Double longitude;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum CheckpointType {
        CHECK_IN,
        CHECK_OUT
    }

    public enum CheckpointStatus {
        PENDING,
        SCANNED,
        SIGNED,
        COMPLETED,
        EXPIRED
    }
}

