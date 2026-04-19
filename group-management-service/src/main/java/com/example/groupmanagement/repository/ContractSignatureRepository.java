package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.ContractSignature;
import com.example.groupmanagement.entity.GroupContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    boolean existsByGroupContractAndUserId(GroupContract groupContract, Long userId);

    long countByGroupContract(GroupContract groupContract);

    List<ContractSignature> findByGroupContract(GroupContract groupContract);

    Optional<ContractSignature> findByGroupContractAndUserId(GroupContract groupContract, Long userId);

    Optional<ContractSignature> findTopByGroupContract_Group_GroupIdAndUserIdOrderBySignedAtDesc(
            Long groupId,
            Long userId
    );
}
