package com.example.disputemanagement.repository;

import com.example.disputemanagement.entity.DisputeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeCommentRepository extends JpaRepository<DisputeComment, Integer> {
    
    // Tìm tất cả bình luận của một tranh chấp
    List<DisputeComment> findByDispute_DisputeIdOrderByCreatedAtAsc(Integer disputeId);
    
    // Tìm bình luận công khai (không phải internal)
    List<DisputeComment> findByDispute_DisputeIdAndIsInternalFalseOrderByCreatedAtAsc(Integer disputeId);
    
    // Tìm bình luận internal (chỉ admin/staff)
    List<DisputeComment> findByDispute_DisputeIdAndIsInternalTrueOrderByCreatedAtAsc(Integer disputeId);
    
    // Đếm số bình luận của một tranh chấp
    long countByDispute_DisputeId(Integer disputeId);
}

