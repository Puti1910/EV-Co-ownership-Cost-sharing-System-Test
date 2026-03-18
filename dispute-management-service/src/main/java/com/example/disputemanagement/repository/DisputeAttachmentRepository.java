package com.example.disputemanagement.repository;

import com.example.disputemanagement.entity.DisputeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeAttachmentRepository extends JpaRepository<DisputeAttachment, Integer> {
    
    // Tìm file đính kèm của một tranh chấp (file chính, không phải của comment)
    List<DisputeAttachment> findByDispute_DisputeIdAndCommentIsNull(Integer disputeId);
    
    // Tìm file đính kèm của một bình luận
    List<DisputeAttachment> findByComment_CommentId(Integer commentId);
    
    // Tìm tất cả file đính kèm của một tranh chấp
    List<DisputeAttachment> findByDispute_DisputeId(Integer disputeId);
}

