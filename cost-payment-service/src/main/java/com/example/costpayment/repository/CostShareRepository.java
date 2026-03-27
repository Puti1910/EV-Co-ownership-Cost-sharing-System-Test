package com.example.costpayment.repository;

import com.example.costpayment.entity.CostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CostShareRepository extends JpaRepository<CostShare, Integer> {
    List<CostShare> findByCostId(Integer costId);
    List<CostShare> findByUserId(Integer userId);
}
