package com.example.costpayment.entity;

/**
 * Enum cho phương thức chia chi phí
 */
public enum SplitMethod {
    BY_OWNERSHIP,   // Chia theo tỉ lệ sở hữu
    BY_USAGE,       // Chia theo mức độ sử dụng (km)
    EQUAL,          // Chia đều
    CUSTOM          // Tùy chỉnh (nhập % thủ công)
}


