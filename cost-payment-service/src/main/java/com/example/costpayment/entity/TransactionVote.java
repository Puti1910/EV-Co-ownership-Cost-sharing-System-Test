package com.example.costpayment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity: Vote của user cho withdrawal request
 */
@Entity
@Table(name = "transactionvote", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"transactionId", "userId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer voteId;

    @Column(nullable = false)
    private Integer transactionId; // FK to FundTransaction

    @Column(nullable = false)
    private Integer userId; // User đang vote

    @Column(nullable = false)
    private Boolean approve; // true=Đồng ý, false=Từ chối

    @Column(length = 500)
    private String note; // Ghi chú (tùy chọn)

    @Column(nullable = false)
    private LocalDateTime votedAt = LocalDateTime.now();
}

