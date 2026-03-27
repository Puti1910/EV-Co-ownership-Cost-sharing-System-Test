package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "DisputeComment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"dispute", "attachments"})
public class DisputeComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentId")
    private Integer commentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputeId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Dispute dispute;
    
    @Column(name = "userId", nullable = false)
    private Integer userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "userRole", nullable = false)
    private UserRole userRole;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "isInternal", nullable = false)
    private Boolean isInternal;
    
    @Column(name = "attachments", columnDefinition = "JSON")
    private String attachments; // JSON string
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<DisputeAttachment> attachmentsList;
    
    public enum UserRole {
        CO_OWNER,  // Chủ xe
        STAFF,     // Nhân viên
        ADMIN      // Quản trị viên
    }
}

