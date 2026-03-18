package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "LeaveRequest")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"group", "groupMember"})
public class LeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requestId")
    private Integer requestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private GroupMember groupMember;
    
    @Column(name = "userId", nullable = false)
    private Integer userId;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LeaveStatus status = LeaveStatus.Pending;
    
    @Column(name = "requestedAt")
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    @Column(name = "processedAt")
    private LocalDateTime processedAt;
    
    @Column(name = "processedBy")
    private Integer processedBy; // Admin userId who processed the request
    
    @Column(name = "adminNote", columnDefinition = "TEXT")
    private String adminNote;
    
    public enum LeaveStatus {
        Pending,    // Đang chờ phê duyệt
        Approved,   // Đã được phê duyệt
        Rejected    // Bị từ chối
    }
}

