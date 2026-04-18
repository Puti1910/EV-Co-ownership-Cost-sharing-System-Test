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
    @Max(value = Long.MAX_VALUE, message = "userId không được vượt quá Max Long")
    private Long userId;
    
    @Min(value = 1, message = "vehicleId phải bắt đầu từ 1")
    @Max(value = 2147483647L, message = "vehicleId không được vượt quá 2147483647")
    private Long vehicleId;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDatetime;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDatetime;

    @Size(min = 1, max = 255, message = "Mục đích sử dụng phải từ 1 đến 255 ký tự")
    private String purpose;

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    // Cho phép null hoặc chuỗi rỗng (sẽ dùng default BOOKED ở service)
    // Nếu có giá trị thì phải là một trong 4 giá trị hợp lệ (case-sensitive)
    @Pattern(
        regexp = "(?i)^(BOOKED|IN_USE|COMPLETED|CANCELLED)?$",
        message = "status must be one of: BOOKED, IN_USE, COMPLETED, CANCELLED"
    )
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public LocalDateTime getStartDatetime() { return startDatetime; }
    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }
    public LocalDateTime getEndDatetime() { return endDatetime; }
    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
