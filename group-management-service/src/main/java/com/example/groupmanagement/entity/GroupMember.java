package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "GroupMember")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"group", "votingResults"})
public class GroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberId")
    private Integer memberId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Group group;
    
    @Column(name = "userId", nullable = false)
    private Integer userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private MemberRole role = MemberRole.Member;
    
    @Column(name = "ownershipPercent")
    private Double ownershipPercent = 0.0;
    
    @Column(name = "joinedAt")
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "groupMember", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private java.util.List<VotingResult> votingResults;
    
    public enum MemberRole {
        Admin, Member
    }
}