package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.ContractSignature;
import com.example.groupmanagement.entity.GroupContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Integer> {

    boolean existsByGroupContractAndUserId(GroupContract groupContract, Integer userId);

    long countByGroupContract(GroupContract groupContract);

    List<ContractSignature> findByGroupContract(GroupContract groupContract);

    java.util.Optional<ContractSignature> findTopByGroupContract_Group_GroupIdAndUserIdOrderBySignedAtDesc(
            Integer groupId,
            Integer userId
    );
}


