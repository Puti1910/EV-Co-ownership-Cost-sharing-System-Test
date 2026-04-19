package com.example.costpayment.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

/**
 * DTO: Nạp tiền vào quỹ (USER/ADMIN)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequestDto {

    @NotNull(message = "fundId không được để trống")
    private Integer fundId;

    @NotNull(message = "userId không được để trống")
    private Integer userId;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải > 0")
<<<<<<< HEAD
    private Double amount;
=======
    private BigDecimal amount;
>>>>>>> origin/main

    @Size(max = 255, message = "Mục đích không quá 255 ký tự")
    private String purpose; // Optional: "Nạp quỹ tháng 11", "Đóng góp thêm"...
}

