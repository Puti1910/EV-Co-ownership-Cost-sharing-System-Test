package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    List<LeaveRequest> findByGroup_GroupId(Integer groupId);
    List<LeaveRequest> findByGroup_GroupIdAndStatus(Integer groupId, LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByUserId(Integer userId);
    List<LeaveRequest> findByUserIdAndStatus(Integer userId, LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByGroup_GroupIdAndUserId(Integer groupId, Integer userId);
    Optional<LeaveRequest> findByGroup_GroupIdAndUserIdAndStatus(Integer groupId, Integer userId, LeaveRequest.LeaveStatus status);
}

