package com.example.LegalContractService.repository;

import com.example.LegalContractService.model.Contracthistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for ContractHistory entity
 */
@Repository
public interface ContractHistoryRepository extends JpaRepository<Contracthistory, Integer> {
    
    /**
     * Find all history records for a specific contract
     */
    List<Contracthistory> findByContract_ContractId(Integer contractId);
    
    /**
     * Find history records by contract ID ordered by date descending
     */
    @Query("SELECT h FROM Contracthistory h WHERE h.contract.contractId = :contractId ORDER BY h.actionDate DESC")
    List<Contracthistory> findByContractIdOrderByDateDesc(@Param("contractId") Integer contractId);
    
    /**
     * Delete all history records for a specific contract (using native query for simplicity)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ContractHistory WHERE contract_id = :contractId", nativeQuery = true)
    void deleteByContractId(@Param("contractId") Integer contractId);
}

