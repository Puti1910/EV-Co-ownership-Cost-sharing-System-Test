package com.example.ui_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteOptionDto {
    private Long id;
    private Long voteId;
    private String optionText;
    private Integer voteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
