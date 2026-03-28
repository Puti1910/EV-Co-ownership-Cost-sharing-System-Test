package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalContributed = BigDecimal.ZERO; // Tổng tiền đã đóng góp

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO; // Số dư hiện tại

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú

    /**
     * Nạp tiền vào quỹ
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        }
        this.currentBalance = this.currentBalance.add(amount);
        this.totalContributed = this.totalContributed.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rút tiền từ quỹ
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        }
        if (this.currentBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                String.format("Số dư không đủ. Hiện có: %.2f VND, yêu cầu: %.2f VND", 
                    this.currentBalance.doubleValue(), amount.doubleValue())
            );
        }
        this.currentBalance = this.currentBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra đủ số dư không
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.currentBalance.compareTo(amount) >= 0;
    }
}

