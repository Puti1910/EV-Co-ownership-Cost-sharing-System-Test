package com.example.reservationservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @Min(value = 1, message = "userId must be at least 1")
    private Integer userId;

    @Min(value = 1, message = "vehicleId phải bắt đầu từ 1")
    @Max(value = Integer.MAX_VALUE, message = "vehicleId không được vượt quá Max Integer")
    private Integer vehicleId;

    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;

    @jakarta.validation.constraints.NotBlank(message = "purpose không được để trống")
    @Size(max = 255, message = "Mục đích sử dụng không được quá 255 ký tự")
    private String purpose;

    // Cho phép null hoặc chuỗi rỗng (sẽ dùng default BOOKED ở service)
    // Nếu có giá trị thì phải là một trong 4 giá trị hợp lệ (case-sensitive)
    @Pattern(
        regexp = "^(BOOKED|IN_USE|COMPLETED|CANCELLED)?$",
        message = "status must be one of: BOOKED, IN_USE, COMPLETED, CANCELLED"
    )
    private String status;
}
