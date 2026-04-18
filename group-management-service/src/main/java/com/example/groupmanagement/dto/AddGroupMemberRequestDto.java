package com.example.groupmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMemberRequestDto {
    
    @NotNull(message = "currentUserId không được để trống")
    @Min(value = 1, message = "currentUserId phải là số dương")
    private Integer currentUserId;
    
    @NotNull(message = "userId không được để trống")
    @Min(value = 1, message = "userId phải là số dương")
    private Integer userId;
    
    @NotNull(message = "ownershipPercent không được để trống")
    @DecimalMin(value = "0.01", message = "Tỷ lệ sở hữu phải lớn hơn 0%")
    @DecimalMax(value = "100.00", message = "Tỷ lệ sở hữu không được vượt quá 100%")
    private Double ownershipPercent;
    
    @Pattern(regexp = "^(Admin|Member)$", message = "Role phải là 'Admin' hoặc 'Member'")
    private String role = "Member";
}
