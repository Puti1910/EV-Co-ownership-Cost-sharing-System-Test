package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Giao dịch quỹ chung
 * Hỗ trợ Phương án C: Yêu cầu rút tiền + Voting + Phê duyệt
 */
@Entity
@Table(name = "fundtransaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;

    @Column(nullable = false)
    private Integer fundId;

    @Column(nullable = false)
    private Integer userId; // Người tạo giao dịch

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType = TransactionType.Deposit;

    @Column(nullable = false)
    private Double amount;

    @Column(length = 255)
    private String purpose; // Mục đích: "Mua bảo hiểm", "Sửa xe", "Đổ xăng"...

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    // ========================================
    // PHẦN MỚI: HỖ TRỢ PHÊ DUYỆT (Phương án C)
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.Completed;

    @Column
    private Integer approvedBy; // UserId của Admin phê duyệt

    @Column
    private LocalDateTime approvedAt; // Thời gian phê duyệt

    @Column
    private Integer voteId; // ID của vote (nếu có)

    @Column(length = 500)
    private String receiptUrl; // Link hóa đơn/chứng từ

    // ========================================
    // ENUMS
    // ========================================

    public enum TransactionType {
        Deposit,  // Nạp tiền
        Withdraw  // Rút tiền
    }

    public enum TransactionStatus {
        Pending,    // Chờ bỏ phiếu
        Approved,   // Đã được vote duyệt, chờ Admin xác nhận
        Rejected,   // Bị từ chối
        Completed   // Hoàn tất
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Tạo yêu cầu rút tiền (USER)
     */
    public static FundTransaction createWithdrawRequest(
        Integer fundId, Integer userId, Double amount, String purpose
    ) {
        FundTransaction tx = new FundTransaction();
        tx.setFundId(fundId);
        tx.setUserId(userId);
        tx.setTransactionType(TransactionType.Withdraw);
        tx.setAmount(amount);
        tx.setPurpose(purpose);
        tx.setStatus(TransactionStatus.Pending); // Chờ vote
        tx.setDate(LocalDateTime.now());
        return tx;
    }

    /**
     * Rút tiền trực tiếp (ADMIN)
     */
    public static FundTransaction createDirectWithdraw(
        Integer fundId, Integer adminId, Double amount, String purpose
    ) {
        FundTransaction tx = new FundTransaction();
        tx.setFundId(fundId);
        tx.setUserId(adminId);
        tx.setTransactionType(TransactionType.Withdraw);
        tx.setAmount(amount);
        tx.setPurpose(purpose);
        tx.setStatus(TransactionStatus.Completed); // Không cần vote
        tx.setApprovedBy(adminId);
        tx.setApprovedAt(LocalDateTime.now());
        tx.setDate(LocalDateTime.now());
        return tx;
    }

    /**
     * Nạp tiền (USER/ADMIN)
     */
    public static FundTransaction createDeposit(
        Integer fundId, Integer userId, Double amount, String purpose
    ) {
        FundTransaction tx = new FundTransaction();
        tx.setFundId(fundId);
        tx.setUserId(userId);
        tx.setTransactionType(TransactionType.Deposit);
        tx.setAmount(amount);
        tx.setPurpose(purpose);
        tx.setStatus(TransactionStatus.Completed); // Nạp tiền không cần duyệt
        tx.setDate(LocalDateTime.now());
        return tx;
    }

    /**
     * Phê duyệt yêu cầu
     */
    public void approve(Integer adminId) {
        if (this.status != TransactionStatus.Pending) {
            throw new IllegalStateException("Chỉ có thể duyệt giao dịch đang Pending");
        }
        this.status = TransactionStatus.Approved;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Từ chối yêu cầu
     */
    public void reject(Integer adminId) {
        if (this.status != TransactionStatus.Pending && this.status != TransactionStatus.Approved) {
            throw new IllegalStateException("Không thể từ chối giao dịch này");
        }
        this.status = TransactionStatus.Rejected;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Hoàn tất giao dịch
     */
    public void complete() {
        if (this.status != TransactionStatus.Approved && this.status != TransactionStatus.Pending) {
            throw new IllegalStateException("Không thể hoàn tất giao dịch này");
        }
        this.status = TransactionStatus.Completed;
    }

    /**
     * Kiểm tra có cần vote không
     */
    public boolean requiresVoting() {
        return this.transactionType == TransactionType.Withdraw 
            && this.status == TransactionStatus.Pending;
    }
}

