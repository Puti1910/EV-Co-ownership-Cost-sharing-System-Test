package com.example.groupmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupContractDto {

    @NotBlank(message = "contractCode không được để trống")
    @Size(max = 100, message = "contractCode không được vượt quá 100 ký tự")
    private String contractCode;

    @NotBlank(message = "contractContent không được để trống")
    @Size(max = 65535, message = "Nội dung hợp đồng quá dài (max 65.535 ký tự)")
    private String contractContent;

    @Size(max = 50, message = "contractStatus không được vượt quá 50 ký tự")
    private String contractStatus; // Optional, default = "pending"

    @NotNull(message = "createdBy không được để trống")
    private Integer createdBy;
}
