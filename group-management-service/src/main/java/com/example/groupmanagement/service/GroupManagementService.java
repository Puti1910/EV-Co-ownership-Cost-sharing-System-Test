package com.example.groupmanagement.service;

import com.example.groupmanagement.entity.Group;
import com.example.groupmanagement.entity.GroupMember;
import com.example.groupmanagement.repository.GroupRepository;
import com.example.groupmanagement.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupManagementService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Optional<Group> getGroupById(Integer id) {
        return groupRepository.findById(id);
    }

    public Optional<Group> updateGroup(Integer id, Group group) {
        return groupRepository.findById(id)
                .map(existingGroup -> {
                    existingGroup.setGroupName(group.getGroupName());
                    existingGroup.setAdminId(group.getAdminId());
                    existingGroup.setVehicleId(group.getVehicleId());
                    existingGroup.setStatus(group.getStatus());
                    return groupRepository.save(existingGroup);
                });
    }

    public void deleteGroup(Integer id) {
        groupRepository.deleteById(id);
    }

    public List<Group> getGroupsByAdmin(Integer adminId) {
        return groupRepository.findByAdminId(adminId);
    }

    public List<Group> getGroupsByStatus(Group.GroupStatus status) {
        return groupRepository.findByStatus(status);
    }

    public GroupMember addMember(Integer groupId, GroupMember member) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Check if user is already a member
        List<GroupMember> existingMembers = memberRepository.findByGroup_GroupId(groupId);
        boolean isAlreadyMember = existingMembers.stream()
                .anyMatch(m -> m.getUserId().equals(member.getUserId()));
        
        if (isAlreadyMember) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        member.setGroup(group);
        return memberRepository.save(member);
    }

    public List<GroupMember> getGroupMembers(Integer groupId) {
        return memberRepository.findByGroup_GroupId(groupId);
    }

    public Optional<GroupMember> updateMember(Integer groupId, Integer memberId, GroupMember member) {
        return memberRepository.findById(memberId)
                .filter(m -> m.getGroup().getGroupId().equals(groupId))
                .map(existingMember -> {
                    existingMember.setRole(member.getRole());
                    return memberRepository.save(existingMember);
                });
    }

    public void removeMember(Integer groupId, Integer memberId) {
        memberRepository.findById(memberId)
                .filter(m -> m.getGroup().getGroupId().equals(groupId))
                .ifPresent(memberRepository::delete);
    }

    public List<GroupMember> getMembersByUserId(Integer userId) {
        return memberRepository.findByUserId(userId);
    }
}
