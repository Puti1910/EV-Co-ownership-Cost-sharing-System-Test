package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {
    List<GroupMember> findByGroup_GroupId(Integer groupId);
    List<GroupMember> findByUserId(Integer userId);
    List<GroupMember> findByRole(GroupMember.MemberRole role);
    Integer countByGroup_GroupId(Integer groupId);
}