package com.example.groupmanagement.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVotingRequestDto {

    @NotBlank(message = "topic không được để trống")
    @Size(max = 255, message = "topic không được vượt quá 255 ký tự")
    private String topic;

    @Size(max = 100, message = "optionA không được vượt quá 100 ký tự")
    private String optionA;

    @Size(max = 100, message = "optionB không được vượt quá 100 ký tự")
    private String optionB;

    @NotNull(message = "deadline là bắt buộc")
    @Future(message = "deadline phải ở tương lai")
    private LocalDateTime deadline;
}
