package com.example.costpayment.repository;

import com.example.costpayment.entity.GroupFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository: Quản lý Quỹ chung
 */
@Repository
public interface GroupFundRepository extends JpaRepository<GroupFund, Integer> {

    /**
     * Tìm quỹ theo groupId
     */
    Optional<GroupFund> findByGroupId(Integer groupId);

    /**
     * Kiểm tra quỹ có tồn tại không
     */
    boolean existsByGroupId(Integer groupId);
}

