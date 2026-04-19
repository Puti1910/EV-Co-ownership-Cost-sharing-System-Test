package com.example.groupmanagement.util;

import com.example.groupmanagement.exception.ValidationException;

public class GroupValidationUtil {
    
    private static final int GROUP_NAME_MAX_LENGTH = 100;
    private static final int GROUP_NAME_MIN_LENGTH = 1;
    
    /**
     * Validate groupName
     * @param groupName Tên nhóm cần validate
     * @throws ValidationException nếu không hợp lệ
     */
    public static void validateGroupName(String groupName) {
        // Check if null or empty
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new ValidationException(
                "Tên nhóm không được để trống",
                "groupName",
                "GROUP_NAME_EMPTY"
            );
        }
        
        // Check max length
        if (groupName.length() > GROUP_NAME_MAX_LENGTH) {
            throw new ValidationException(
                String.format("Tên nhóm không được vượt quá %d ký tự (hiện tại: %d ký tự)", 
                    GROUP_NAME_MAX_LENGTH, groupName.length()),
                "groupName",
                "GROUP_NAME_TOO_LONG"
            );
        }
        
        // Check min length
        if (groupName.trim().length() < GROUP_NAME_MIN_LENGTH) {
            throw new ValidationException(
                "Tên nhóm phải chứa ít nhất 1 ký tự",
                "groupName",
                "GROUP_NAME_TOO_SHORT"
            );
        }
    }
    
    /**
     * Validate groupName khi update (optional field)
     * @param groupName Tên nhóm từ request (có thể null)
     * @return true nếu hợp lệ hoặc null, false nếu không hợp lệ
     * @throws ValidationException nếu không hợp lệ
     */
    public static void validateGroupNameForUpdate(String groupName) {
        // Nếu groupName được gửi lên, phải validate
        if (groupName != null) {
            validateGroupName(groupName);
        }
    }
}