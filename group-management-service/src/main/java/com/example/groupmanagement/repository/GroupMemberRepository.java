package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroup_GroupId(Long groupId);
    List<GroupMember> findByUserId(Long userId);
    List<GroupMember> findByRole(GroupMember.MemberRole role);
    Integer countByGroup_GroupId(Long groupId);
    Optional<GroupMember> findByGroup_GroupIdAndUserId(Long groupId, Long userId);
}