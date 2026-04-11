package com.example.costpayment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: User vote cho withdrawal request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequestDto {

    @NotNull(message = "transactionId không được để trống")
    private Integer transactionId;

    @NotNull(message = "userId không được để trống")
    private Integer userId; // User đang vote

    @NotNull(message = "Quyết định không được để trống")
    private Boolean approve; // true=Đồng ý, false=Từ chối

    private String note; // Ghi chú (tùy chọn)
}

