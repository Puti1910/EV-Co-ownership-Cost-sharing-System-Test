package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingRepository extends JpaRepository<Voting, Long> {
    List<Voting> findByGroup_GroupId(Long groupId);
    Integer countByGroup_GroupId(Long groupId);
}
