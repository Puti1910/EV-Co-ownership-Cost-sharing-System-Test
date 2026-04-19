package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.GroupContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupContractRepository extends JpaRepository<GroupContract, Long> {

    List<GroupContract> findByGroup_GroupId(Long groupId);

    Optional<GroupContract> findTopByGroup_GroupIdOrderByCreationDateDesc(Long groupId);

    Optional<GroupContract> findByContractCode(String contractCode);
}
