package com.example.costpayment.repository;

import com.example.costpayment.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Integer> {

    /**
     * Tìm usage của nhóm trong tháng
     */
    List<UsageTracking> findByGroupIdAndMonthAndYear(Integer groupId, Integer month, Integer year);

    /**
     * Tìm usage của 1 user trong tháng
     */
    Optional<UsageTracking> findByGroupIdAndUserIdAndMonthAndYear(
            Integer groupId, Integer userId, Integer month, Integer year);

    /**
     * Lấy lịch sử usage của user
     */
    List<UsageTracking> findByUserIdOrderByYearDescMonthDesc(Integer userId);

    /**
     * Tìm theo userId
     */
    List<UsageTracking> findByUserId(Integer userId);
}


