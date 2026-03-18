package com.example.ui_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {
    private Integer groupId;
    private String groupName;
    private Integer adminId;
    private Integer vehicleId;
    private String status;
    private Date createdAt;
    private Integer memberCount;
    private Double totalCost;
    private Integer voteCount;
}