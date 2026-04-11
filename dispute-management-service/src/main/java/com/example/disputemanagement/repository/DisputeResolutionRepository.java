package com.example.disputemanagement.repository;

import com.example.disputemanagement.entity.DisputeResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeResolutionRepository extends JpaRepository<DisputeResolution, Integer> {
    
    // Tìm giải pháp theo dispute ID
    Optional<DisputeResolution> findByDispute_DisputeId(Integer disputeId);
    
    // Tìm giải pháp theo người giải quyết
    java.util.List<DisputeResolution> findByResolvedBy(Integer resolvedBy);
}

