package com.example.costpayment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: Yêu cầu rút tiền từ quỹ (USER)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequestDto {

    @NotNull(message = "fundId không được để trống")
    private Integer fundId;

    @NotNull(message = "userId không được để trống")
    private Integer userId;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải > 0")
    private Double amount;

    @NotBlank(message = "Mục đích không được để trống")
    @Size(max = 255, message = "Mục đích không quá 255 ký tự")
    private String purpose;

    private String receiptUrl; // Optional: Link hóa đơn
}

