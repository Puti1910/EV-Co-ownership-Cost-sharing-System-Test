package com.example.disputemanagement.dto;

import com.example.disputemanagement.entity.Dispute;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDisputeRequest {

    @NotNull(message = "Group ID không được để trống")
    private Integer groupId;

    private Integer vehicleId;

    private Integer reservationId;

    private Integer costId;

    private Integer paymentId;

    @NotNull(message = "CreatedBy không được để trống")
    private Integer createdBy;

    private Integer reportedUserId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Category không được để trống")
    private Dispute.DisputeCategory category;

    @NotNull(message = "Priority không được để trống")
    private Dispute.DisputePriority priority;

    private String resolutionNote;
}
