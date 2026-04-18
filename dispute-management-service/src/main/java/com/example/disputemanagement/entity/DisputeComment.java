package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    
    @NotNull(message = "User ID không được để trống")
    @Positive(message = "User ID phải > 0")
    @Column(name = "userId", nullable = false)
    private Integer userId;
    
    @NotNull(message = "User role không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "userRole", nullable = false)
    private UserRole userRole;
    
    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @NotNull(message = "Cờ internal không được để trống")
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

