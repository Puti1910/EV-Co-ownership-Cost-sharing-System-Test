package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity lưu mức độ sử dụng xe (km đã chạy)
 */
@Entity
@Table(name = "usagetracking")
@Data
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usageId")
    private Integer usageId;

    @Column(name = "groupId", nullable = false)
    private Integer groupId;

    @Column(name = "userId", nullable = false)
    private Integer userId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "kmDriven")
    private Double kmDriven;

    @Column(name = "recordedAt")
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}


