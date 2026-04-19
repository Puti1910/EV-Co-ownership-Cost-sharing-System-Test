package com.example.VehicleServiceManagementService.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceBookingRequest {
    @NotNull(message = "userId là bắt buộc")
    @Min(value = 1, message = "userId phải lớn hơn hoặc bằng 1")
    @JsonAlias({"userId", "requestedByUserId"})
    private Long userId;

    @NotNull(message = "groupId là bắt buộc")
    @Min(value = 1, message = "groupId phải lớn hơn hoặc bằng 1")
    private Long groupId;

    @NotNull(message = "vehicleId là bắt buộc")
    @Min(value = 1, message = "vehicleId phải lớn hơn hoặc bằng 1")
    private Long vehicleId;

    @Size(max = 100, message = "Tên xe không được vượt quá 100 ký tự")
    private String vehicleName;

    @NotNull(message = "serviceId là bắt buộc")
    @Min(value = 1, message = "serviceId phải lớn hơn hoặc bằng 1")
    private Long serviceId;

    @Size(max = 255, message = "Tên dịch vụ không được vượt quá 255 ký tự")
    private String serviceName;

    @Size(max = 65535, message = "Mô tả dịch vụ không được vượt quá 65535 ký tự")
    private String serviceDescription;

    @NotBlank(message = "Thời gian bắt đầu không được để trống")
    private String preferredStartDatetime;

    @NotBlank(message = "Thời gian kết thúc không được để trống")
    private String preferredEndDatetime;

    @Size(min = 1, max = 255, message = "Tên người yêu cầu không được để trống và không được vượt quá 255 ký tự")
    @JsonProperty("userName")
    @JsonAlias({"userName", "requestedByName", "requestedByUserName"})
    private String requestedByName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải bao gồm 10 chữ số")
    private String contactPhone;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;
}
