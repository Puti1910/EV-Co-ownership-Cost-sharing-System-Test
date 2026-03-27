package com.example.costpayment.repository;

import com.example.costpayment.entity.TransactionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionVoteRepository extends JpaRepository<TransactionVote, Integer> {

    /**
     * Tìm vote của user cho transaction
     */
    Optional<TransactionVote> findByTransactionIdAndUserId(Integer transactionId, Integer userId);

    /**
     * Lấy tất cả votes của transaction
     */
    List<TransactionVote> findByTransactionId(Integer transactionId);

    /**
     * Đếm số phiếu đồng ý
     */
    @Query("SELECT COUNT(v) FROM TransactionVote v WHERE v.transactionId = :transactionId AND v.approve = true")
    long countApprovesByTransactionId(@Param("transactionId") Integer transactionId);

    /**
     * Đếm số phiếu từ chối
     */
    @Query("SELECT COUNT(v) FROM TransactionVote v WHERE v.transactionId = :transactionId AND v.approve = false")
    long countRejectsByTransactionId(@Param("transactionId") Integer transactionId);

    /**
     * Đếm tổng số phiếu
     */
    @Query("SELECT COUNT(v) FROM TransactionVote v WHERE v.transactionId = :transactionId")
    long countByTransactionId(@Param("transactionId") Integer transactionId);
}

