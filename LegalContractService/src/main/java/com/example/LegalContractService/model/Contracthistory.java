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
@Table(name = "ContractHistory")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "contract"})
public class Contracthistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Legalcontract contract;

    @Size(max = 255)
    @Column(name = "action")
    private String action;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "action_date")
    private Instant actionDate;

}

