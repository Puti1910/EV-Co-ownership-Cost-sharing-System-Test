package com.example.user_account_service.dto;

<<<<<<< HEAD
=======
import jakarta.validation.constraints.*;
>>>>>>> origin/main
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class UserProfileUpdateRequest {
    // Thông tin cơ bản
<<<<<<< HEAD
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
=======
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 50, message = "Họ tên phải từ 2 đến 50 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 số và bắt đầu bằng số 0")
    private String phoneNumber;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    // Giấy tờ tùy thân
    @NotBlank(message = "Số CCCD không được để trống")
    @Size(min = 12, max = 12, message = "Số CCCD phải có đúng 12 chữ số")
    @Pattern(regexp = "^[0-9]+$", message = "Số CCCD chỉ được chứa các chữ số")
    private String idCardNumber;

    @NotNull(message = "Ngày cấp CCCD không được để trống")
    @PastOrPresent(message = "Ngày cấp CCCD không được ở tương lai")
    private LocalDate idCardIssueDate;

    @NotBlank(message = "Nơi cấp CCCD không được để trống")
    private String idCardIssuePlace;

    // Giấy phép lái xe
    @NotBlank(message = "Số GPLX không được để trống")
    @Size(min = 12, max = 12, message = "Số GPLX phải có đúng 12 chữ số")
    @Pattern(regexp = "^[0-9]+$", message = "Số GPLX chỉ được chứa các chữ số")
    private String licenseNumber;

    @NotBlank(message = "Hạng GPLX không được để trống")
    private String licenseClass;

    @NotNull(message = "Ngày cấp GPLX không được để trống")
    @PastOrPresent(message = "Ngày cấp GPLX không được ở tương lai")
    private LocalDate licenseIssueDate;

    @NotNull(message = "Ngày hết hạn GPLX không được để trống")
    @Future(message = "Ngày hết hạn GPLX phải ở tương lai")
>>>>>>> origin/main
    private LocalDate licenseExpiryDate;

    // URL Hình ảnh
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String licenseImageUrl;
    private String portraitImageUrl;
}