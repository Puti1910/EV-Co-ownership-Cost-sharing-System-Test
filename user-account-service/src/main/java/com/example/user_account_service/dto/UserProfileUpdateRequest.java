package com.example.user_account_service.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class UserProfileUpdateRequest {
    // Thông tin cơ bản
    private String fullName;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    // Giấy tờ tùy thân
    private String idCardNumber;
    private LocalDate idCardIssueDate;
    private String idCardIssuePlace;

    // Giấy phép lái xe
    private String licenseNumber;
    private String licenseClass;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;

    // URL Hình ảnh
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String licenseImageUrl;
    private String portraitImageUrl;
}