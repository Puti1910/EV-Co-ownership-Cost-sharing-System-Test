package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByGroup_GroupId(Long groupId);
    List<LeaveRequest> findByGroup_GroupIdAndStatus(Long groupId, LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByUserIdAndStatus(Long userId, LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByGroup_GroupIdAndUserId(Long groupId, Long userId);
    Optional<LeaveRequest> findByGroup_GroupIdAndUserIdAndStatus(Long groupId, Long userId, LeaveRequest.LeaveStatus status);
}
