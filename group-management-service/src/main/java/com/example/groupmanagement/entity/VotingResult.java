package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "VotingResult")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"voting", "groupMember"})
public class VotingResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resultId")
    private Integer resultId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voteId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Voting voting;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private GroupMember groupMember;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false)
    private VoteChoice choice;
    
    @Column(name = "votedAt")
    private LocalDateTime votedAt = LocalDateTime.now();
    
    public enum VoteChoice {
        A, B
    }
}
