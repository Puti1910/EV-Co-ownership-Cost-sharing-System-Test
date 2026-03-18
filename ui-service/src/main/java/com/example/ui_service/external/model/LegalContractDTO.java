package com.example.ui_service.external.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class LegalContractDTO {
    private Integer contractId;
    private Integer groupId;
    private String contractCode;
    private String contractStatus;
    private Instant creationDate;
    private Instant signedDate;
}

