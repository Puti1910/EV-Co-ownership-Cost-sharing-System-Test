package com.example.LegalContractService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "LegalContract", catalog = "legal_contract")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Legalcontract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id", nullable = false)
    private Integer contractId;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @Size(max = 100)
    @Column(name = "contract_code", nullable = false, length = 100)
    private String contractCode;

    @Size(max = 50)
    @Column(name = "contract_status", length = 50)
    private String contractStatus;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "creation_date")
    private Instant creationDate;

    @Column(name = "signed_date")
    private Instant signedDate;

}