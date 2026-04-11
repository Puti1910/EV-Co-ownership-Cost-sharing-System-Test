package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.GroupContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupContractRepository extends JpaRepository<GroupContract, Integer> {

    List<GroupContract> findByGroup_GroupId(Integer groupId);

    Optional<GroupContract> findTopByGroup_GroupIdOrderByCreationDateDesc(Integer groupId);

    Optional<GroupContract> findByContractCode(String contractCode);
}


