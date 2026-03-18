package com.example.LegalContractService.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ContractSignatures", schema = "legal_contract")
public class Contractsignature {
    @Id
    @Column(name = "signature_id", nullable = false, length = 20)
    private String signatureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Legalcontract contract;

    @Column(name = "signer_id", length = 20)
    private String signerId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "signature_date")
    private Instant signatureDate;

}