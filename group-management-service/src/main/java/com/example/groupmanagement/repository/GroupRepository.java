package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByAdminId(Long adminId);
    List<Group> findByStatus(Group.GroupStatus status);
}
