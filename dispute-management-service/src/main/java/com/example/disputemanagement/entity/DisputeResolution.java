package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DisputeResolution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"dispute"})
public class DisputeResolution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resolutionId")
    private Integer resolutionId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputeId", nullable = false, unique = true)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Dispute dispute;
    
    @Column(name = "resolvedBy", nullable = false)
    private Integer resolvedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resolutionType", nullable = false)
    private ResolutionType resolutionType;
    
    @Column(name = "resolutionDetails", nullable = false, columnDefinition = "TEXT")
    private String resolutionDetails;
    
    @Column(name = "actionTaken", columnDefinition = "TEXT")
    private String actionTaken;
    
    @Column(name = "compensationAmount")
    private Double compensationAmount;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    public enum ResolutionType {
        ACCEPTED,    // Chấp nhận yêu cầu
        REJECTED,    // Từ chối yêu cầu
        COMPROMISE,  // Thỏa hiệp
        REFUND,      // Hoàn tiền
        PENALTY,     // Phạt
        WARNING,     // Cảnh báo
        OTHER        // Khác
    }
}

