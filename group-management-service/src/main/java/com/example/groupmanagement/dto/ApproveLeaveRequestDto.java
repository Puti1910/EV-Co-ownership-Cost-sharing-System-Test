package com.example.groupmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveLeaveRequestDto {

    @NotNull(message = "currentUserId không được để trống")
    private Long currentUserId;

    @Size(max = 255, message = "adminNote không được vượt quá 255 ký tự")
    private String adminNote; // Optional, có thể null
}