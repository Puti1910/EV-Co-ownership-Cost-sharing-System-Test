package com.example.disputemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: Yêu cầu tạo tệp đính kèm tranh chấp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeAttachmentRequest {
    
    @NotNull(message = "Dispute ID không được để trống")
    private Integer disputeId;
    
    @NotBlank(message = "Tên tệp không được để trống")
    @Size(max = 255, message = "Tên tệp không quá 255 ký tự")
    private String fileName;
    
    @NotBlank(message = "Đường dẫn tệp không được để trống")
    @Size(max = 500, message = "Đường dẫn tệp không quá 500 ký tự")
    private String fileUrl;
    
    @Size(max = 50, message = "Loại tệp không quá 50 ký tự")
    private String fileType;
    
    @Positive(message = "Kích thước tệp phải > 0")
    private Long fileSize;
    
    @NotNull(message = "ID người tải lên không được để trống")
    @Positive(message = "ID người tải lên phải > 0")
    private Integer uploadedBy;
    
    private Integer commentId; // Optional: ID của bình luận nếu tệp được đính kèm vào bình luận
}
