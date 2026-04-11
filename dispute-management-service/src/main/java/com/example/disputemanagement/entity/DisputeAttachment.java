package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DisputeAttachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"dispute", "comment"})
public class DisputeAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachmentId")
    private Integer attachmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputeId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Dispute dispute;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commentId")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private DisputeComment comment;
    
    @Column(name = "fileName", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "fileUrl", nullable = false, length = 500)
    private String fileUrl;
    
    @Column(name = "fileType", length = 50)
    private String fileType;
    
    @Column(name = "fileSize")
    private Long fileSize;
    
    @Column(name = "uploadedBy", nullable = false)
    private Integer uploadedBy;
    
    @Column(name = "uploadedAt")
    private LocalDateTime uploadedAt;
}

