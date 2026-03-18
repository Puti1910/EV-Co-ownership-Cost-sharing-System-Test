package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ContractSignature")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"groupContract"})
public class ContractSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signature_id")
    private Integer signatureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private GroupContract groupContract;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "signed_at")
    private LocalDateTime signedAt = LocalDateTime.now();

    @Column(name = "signature_method")
    private String signatureMethod;

    @Column(name = "ip_address")
    private String ipAddress;
}


