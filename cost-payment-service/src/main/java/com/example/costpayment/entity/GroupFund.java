package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Quỹ chung của nhóm
 * Quản lý số dư và lịch sử đóng góp
 */
@Entity
@Table(name = "groupfund")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fundId;

    @Column(nullable = false)
    private Integer groupId;

    @Column(nullable = false)
    private Double totalContributed = 0.0; // Tổng tiền đã đóng góp

    @Column(nullable = false)
    private Double currentBalance = 0.0; // Số dư hiện tại

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú

    /**
     * Nạp tiền vào quỹ
     */
    public void deposit(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        }
        this.currentBalance += amount;
        this.totalContributed += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rút tiền từ quỹ
     */
    public void withdraw(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        }
        if (this.currentBalance < amount) {
            throw new IllegalStateException(
                String.format("Số dư không đủ. Hiện có: %.2f VND, yêu cầu: %.2f VND", 
                    this.currentBalance, amount)
            );
        }
        this.currentBalance -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra đủ số dư không
     */
    public boolean hasSufficientBalance(Double amount) {
        return this.currentBalance >= amount;
    }
}

