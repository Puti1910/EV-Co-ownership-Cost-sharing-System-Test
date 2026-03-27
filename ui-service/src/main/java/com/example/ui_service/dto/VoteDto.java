package com.example.ui_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteDto {
    private Integer voteId;
    private Integer groupId;
    private String topic;
    private String optionA;
    private String optionB;
    private String finalResult;
    private Integer totalVotes;
}