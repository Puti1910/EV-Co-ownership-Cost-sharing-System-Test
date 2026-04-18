package com.example.groupmanagement.until;

import com.example.groupmanagement.exception.ValidationException;

public class MemberValidationUtil {
    
    private static final double MIN_OWNERSHIP = 0.01;
    private static final double MAX_OWNERSHIP = 100.0;
    
    /**
     * Validate userId (không null, > 0)
     */
    public static void validateUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(
                "User ID phải là số dương",
                "userId",
                "INVALID_USER_ID"
            );
        }
    }
    
    /**
     * Validate ownershipPercent (> 0.01 và <= 100)
     */
    public static void validateOwnershipPercent(Double ownershipPercent) {
        if (ownershipPercent == null) {
            throw new ValidationException(
                "Tỷ lệ sở hữu không được để trống",
                "ownershipPercent",
                "OWNERSHIP_PERCENT_REQUIRED"
            );
        }
        
        if (ownershipPercent < MIN_OWNERSHIP) {
            throw new ValidationException(
                String.format("Tỷ lệ sở hữu phải lớn hơn %.2f%%", MIN_OWNERSHIP),
                "ownershipPercent",
                "OWNERSHIP_PERCENT_TOO_LOW"
            );
        }
        
        if (ownershipPercent > MAX_OWNERSHIP) {
            throw new ValidationException(
                String.format("Tỷ lệ sở hữu không được vượt quá %.0f%%", MAX_OWNERSHIP),
                "ownershipPercent",
                "OWNERSHIP_PERCENT_TOO_HIGH"
            );
        }
    }
    
    /**
     * Validate total ownership (currentTotal + newOwnership <= 100)
     * FIX: Thay đổi logic để cho phép tổng = 100%
     */
    public static void validateTotalOwnership(double currentTotal, double newOwnership) {
        double newTotal = currentTotal + newOwnership;
        
        // FIX: Thay đổi từ > thành > (allow exactly 100%)
        if (newTotal > MAX_OWNERSHIP) {
            throw new ValidationException(
                String.format("Tổng tỷ lệ sở hữu không được vượt quá 100%%. Hiện tại: %.2f%%, yêu cầu thêm: %.2f%%, tổng sẽ là: %.2f%%", 
                    currentTotal, newOwnership, newTotal),
                "ownershipPercent",
                "TOTAL_OWNERSHIP_EXCEEDS_LIMIT"
            );
        }
    }
}
