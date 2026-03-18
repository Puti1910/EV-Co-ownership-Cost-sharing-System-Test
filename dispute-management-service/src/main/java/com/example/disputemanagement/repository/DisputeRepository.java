package com.example.disputemanagement.repository;

import com.example.disputemanagement.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Integer> {
    
    // Tìm theo nhóm
    List<Dispute> findByGroupId(Integer groupId);
    
    // Tìm theo trạng thái
    List<Dispute> findByStatus(Dispute.DisputeStatus status);
    
    // Tìm theo độ ưu tiên
    List<Dispute> findByPriority(Dispute.DisputePriority priority);
    
    // Tìm theo người tạo
    List<Dispute> findByCreatedBy(Integer createdBy);
    
    // Tìm theo người được giao
    List<Dispute> findByAssignedTo(Integer assignedTo);
    
    // Tìm theo loại tranh chấp
    List<Dispute> findByCategory(Dispute.DisputeCategory category);
    
    // Tìm theo nhóm và trạng thái
    List<Dispute> findByGroupIdAndStatus(Integer groupId, Dispute.DisputeStatus status);
    
    // Tìm theo người được giao và trạng thái
    List<Dispute> findByAssignedToAndStatus(Integer assignedTo, Dispute.DisputeStatus status);
    
    // Đếm theo trạng thái
    long countByStatus(Dispute.DisputeStatus status);
    
    // Đếm theo độ ưu tiên
    long countByPriority(Dispute.DisputePriority priority);
    
    // Tìm tranh chấp chưa được giao
    @Query("SELECT d FROM Dispute d WHERE d.assignedTo IS NULL AND (d.status = 'PENDING' OR d.status = 'IN_REVIEW')")
    List<Dispute> findUnassignedDisputes();
    
    // Tìm tranh chấp cần xử lý (theo độ ưu tiên)
    @Query("SELECT d FROM Dispute d WHERE d.status = 'PENDING' OR d.status = 'IN_REVIEW' ORDER BY " +
           "CASE d.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END, " +
           "d.createdAt ASC")
    List<Dispute> findPendingDisputesOrderedByPriority();
}

