package com.example.costpayment.repository;

import com.example.costpayment.entity.FundTransaction;
import com.example.costpayment.entity.FundTransaction.TransactionStatus;
import com.example.costpayment.entity.FundTransaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository: Quản lý giao dịch quỹ
 */
@Repository
public interface FundTransactionRepository extends JpaRepository<FundTransaction, Integer> {

    /**
     * Tìm tất cả giao dịch của một quỹ
     */
    List<FundTransaction> findByFundIdOrderByDateDesc(Integer fundId);

    /**
     * Tìm giao dịch theo userId
     */
    List<FundTransaction> findByUserIdOrderByDateDesc(Integer userId);

    /**
     * Tìm giao dịch theo loại
     */
    List<FundTransaction> findByFundIdAndTransactionTypeOrderByDateDesc(
        Integer fundId, TransactionType transactionType
    );

    /**
     * Tìm giao dịch theo trạng thái
     */
    List<FundTransaction> findByFundIdAndStatusOrderByDateDesc(
        Integer fundId, TransactionStatus status
    );

    /**
     * Tìm các yêu cầu rút tiền đang chờ duyệt (Pending hoặc Approved - chờ admin duyệt)
     */
    @Query("SELECT ft FROM FundTransaction ft " +
           "WHERE (:fundId IS NULL OR ft.fundId = :fundId) " +
           "AND ft.transactionType = 'Withdraw' " +
           "AND (ft.status = 'Pending' OR ft.status = 'Approved') " +
           "ORDER BY ft.date DESC")
    List<FundTransaction> findPendingWithdrawRequests(@Param("fundId") Integer fundId);

    /**
     * Tìm giao dịch theo voteId
     */
    List<FundTransaction> findByVoteId(Integer voteId);

    /**
     * Tổng tiền nạp vào quỹ
     */
    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FundTransaction ft " +
           "WHERE ft.fundId = :fundId " +
           "AND ft.transactionType = 'Deposit' " +
           "AND ft.status = 'Completed'")
    Double getTotalDeposit(@Param("fundId") Integer fundId);

    /**
     * Tổng tiền rút từ quỹ
     */
    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FundTransaction ft " +
           "WHERE ft.fundId = :fundId " +
           "AND ft.transactionType = 'Withdraw' " +
           "AND ft.status = 'Completed'")
    Double getTotalWithdraw(@Param("fundId") Integer fundId);

    /**
     * Lịch sử giao dịch theo khoảng thời gian
     */
    @Query("SELECT ft FROM FundTransaction ft " +
           "WHERE ft.fundId = :fundId " +
           "AND ft.date BETWEEN :startDate AND :endDate " +
           "ORDER BY ft.date DESC")
    List<FundTransaction> findByDateRange(
        @Param("fundId") Integer fundId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Đếm số giao dịch đang chờ duyệt
     */
    @Query("SELECT COUNT(ft) FROM FundTransaction ft " +
           "WHERE ft.fundId = :fundId AND ft.status = 'Pending'")
    Long countPendingTransactions(@Param("fundId") Integer fundId);
}

