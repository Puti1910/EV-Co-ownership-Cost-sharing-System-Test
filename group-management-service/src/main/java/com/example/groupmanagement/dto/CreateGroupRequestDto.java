package com.example.groupmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDto {
    @NotBlank(message = "groupName is required")
    @Size(max = 100, message = "groupName must not exceed 100 characters")
    private String groupName;

    private Long adminId;
    private Double ownershipPercent;
    private String status;
}
