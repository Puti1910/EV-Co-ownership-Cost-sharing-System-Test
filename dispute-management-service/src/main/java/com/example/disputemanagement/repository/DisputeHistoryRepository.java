package com.example.disputemanagement.repository;

import com.example.disputemanagement.entity.DisputeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeHistoryRepository extends JpaRepository<DisputeHistory, Integer> {
    
    // Tìm lịch sử của một tranh chấp
    List<DisputeHistory> findByDispute_DisputeIdOrderByCreatedAtAsc(Integer disputeId);
}

