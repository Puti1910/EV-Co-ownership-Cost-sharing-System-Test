package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/main

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

<<<<<<< HEAD
    @Column(nullable = false)
    private Double totalContributed = 0.0; // Tổng tiền đã đóng góp

    @Column(nullable = false)
    private Double currentBalance = 0.0; // Số dư hiện tại
=======
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalContributed = BigDecimal.ZERO; // Tổng tiền đã đóng góp

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO; // Số dư hiện tại
>>>>>>> origin/main

    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú

    /**
     * Nạp tiền vào quỹ
     */
<<<<<<< HEAD
    public void deposit(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        }
        this.currentBalance += amount;
        this.totalContributed += amount;
=======
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        }
        this.currentBalance = this.currentBalance.add(amount);
        this.totalContributed = this.totalContributed.add(amount);
>>>>>>> origin/main
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Rút tiền từ quỹ
     */
<<<<<<< HEAD
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
=======
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
>>>>>>> origin/main
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra đủ số dư không
     */
<<<<<<< HEAD
    public boolean hasSufficientBalance(Double amount) {
        return this.currentBalance >= amount;
=======
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.currentBalance.compareTo(amount) >= 0;
>>>>>>> origin/main
    }
}

