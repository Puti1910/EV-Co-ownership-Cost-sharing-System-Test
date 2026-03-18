package com.example.groupmanagement.dto;

import com.example.groupmanagement.entity.Group;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {
    private Integer groupId;
    private String groupName;
    private Integer adminId;
    private Integer vehicleId;
    private LocalDateTime createdAt;
    private String status;
    private Integer memberCount;
    private Integer voteCount;

    // User-specific membership metadata (optional)
    private Integer memberId;
    private String memberRole;
    private Double ownershipPercent;
    private Boolean hasOwnership;
    
    public static GroupResponseDto fromEntity(Group group, Integer memberCount, Integer voteCount) {
        GroupResponseDto dto = new GroupResponseDto();
        dto.setGroupId(group.getGroupId());
        dto.setGroupName(group.getGroupName());
        dto.setAdminId(group.getAdminId());
        dto.setVehicleId(group.getVehicleId());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setStatus(group.getStatus() != null ? group.getStatus().name() : null);
        dto.setMemberCount(memberCount);
        dto.setVoteCount(voteCount);
        return dto;
    }
}

