package com.example.groupmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequestDto {

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @Size(max = 255, message = "reason không được vượt quá 255 ký tự")
    private String reason;
}