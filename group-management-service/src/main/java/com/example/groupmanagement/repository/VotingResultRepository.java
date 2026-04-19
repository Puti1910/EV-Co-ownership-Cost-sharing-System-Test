package com.example.groupmanagement.repository;

import com.example.groupmanagement.entity.VotingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingResultRepository extends JpaRepository<VotingResult, Long> {
    List<VotingResult> findByVoting_VoteId(Long voteId);
    List<VotingResult> findByGroupMember_MemberId(Long memberId);
}
