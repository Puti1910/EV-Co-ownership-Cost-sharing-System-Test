package com.example.VehicleServiceManagementService.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class VehicleServiceRequest {
    @NotNull(message = "serviceId là bắt buộc")
    @Min(value = 1, message = "serviceId phải lớn hơn hoặc bằng 1")
    private Long serviceId;

    @NotNull(message = "vehicleId là bắt buộc")
    @Min(value = 1, message = "vehicleId phải lớn hơn hoặc bằng 1")
    private Long vehicleId;

    @NotNull(message = "userId là bắt buộc")
    @Min(value = 1, message = "userId phải lớn hơn hoặc bằng 1")
    @JsonProperty("userId") // Tên khi trả về JSON
    @JsonAlias({"userId", "requestedByUserId"}) // Chấp nhận cả 2 khi nhận JSON
    private Long requestedByUserId;

    @Size(max = 50, message = "status không được vượt quá 50 ký tự")
    private String status;

    @Size(min = 1, max = 255, message = "userName không được để trống và không được vượt quá 255 ký tự")
    @JsonProperty("userName")
    @JsonAlias({"userName", "requestedByName", "requestedByUserName"})
    private String requestedByUserName;

    @Size(max = 65535, message = "Mô tả dịch vụ không được vượt quá 65535 ký tự")
    private String serviceDescription;

    private Long groupRefId;
    private String preferredStartDatetime;
    private String preferredEndDatetime;
}
