package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.groupmanagement.entity.converter.ContractStatusConverter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GroupContract")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"group", "signatures"})
public class GroupContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Integer contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Group group;

    @Column(name = "contract_code", nullable = false, unique = true, length = 100)
    private String contractCode;

    @Lob
    @Column(name = "contract_content")
    private String contractContent;

    @Convert(converter = ContractStatusConverter.class)
    @Column(name = "contract_status")
    private ContractStatus contractStatus = ContractStatus.PENDING;

    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();

    @Column(name = "signed_date")
    private LocalDateTime signedDate;

    @Column(name = "created_by")
    private Integer createdBy;

    @OneToMany(mappedBy = "groupContract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private List<ContractSignature> signatures;

    public enum ContractStatus {
        PENDING,
        SIGNED,
        ARCHIVED
    }
}

