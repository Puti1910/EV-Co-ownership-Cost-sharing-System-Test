package com.example.costpayment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: Admin phê duyệt/từ chối yêu cầu rút tiền
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRequestDto {

    @NotNull(message = "transactionId không được để trống")
    private Integer transactionId;

    @NotNull(message = "adminId không được để trống")
    private Integer adminId;

    @NotNull(message = "Quyết định không được để trống")
    private Boolean approved; // true=Duyệt, false=Từ chối

    private String note; // Ghi chú của Admin
}

