package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DisputeHistory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"dispute"})
public class DisputeHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyId")
    private Integer historyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputeId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Dispute dispute;
    
    @Column(name = "changedBy", nullable = false)
    private Integer changedBy;
    
    @Column(name = "oldStatus", length = 50)
    private String oldStatus;
    
    @Column(name = "newStatus", length = 50)
    private String newStatus;
    
    @Column(name = "changeNote", columnDefinition = "TEXT")
    private String changeNote;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
}

