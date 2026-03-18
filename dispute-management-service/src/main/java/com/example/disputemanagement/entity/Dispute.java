package com.example.disputemanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Dispute")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"comments", "resolution", "history", "attachments"})
public class Dispute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disputeId")
    private Integer disputeId;
    
    @Column(name = "groupId", nullable = false)
    private Integer groupId;
    
    @Column(name = "vehicleId")
    private Integer vehicleId;
    
    @Column(name = "reservationId")
    private Integer reservationId;
    
    @Column(name = "costId")
    private Integer costId;
    
    @Column(name = "paymentId")
    private Integer paymentId;
    
    @Column(name = "createdBy", nullable = false)
    private Integer createdBy;
    
    @Column(name = "reportedUserId")
    private Integer reportedUserId;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private DisputeCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private DisputePriority priority;
    
    @Column(name = "assignedTo")
    private Integer assignedTo;
    
    @Column(name = "resolvedBy")
    private Integer resolvedBy;
    
    @Column(name = "resolutionNote", columnDefinition = "TEXT")
    private String resolutionNote;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    @Column(name = "resolvedAt")
    private LocalDateTime resolvedAt;
    
    @Column(name = "closedAt")
    private LocalDateTime closedAt;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<DisputeComment> comments;
    
    @OneToOne(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private DisputeResolution resolution;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<DisputeHistory> history;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<DisputeAttachment> attachments;
    
    public enum DisputeCategory {
        RESERVATION,      // Tranh chấp về đặt lịch
        COST_SHARING,     // Tranh chấp về chia chi phí
        VEHICLE_DAMAGE,   // Hư hỏng xe
        USAGE_VIOLATION,  // Vi phạm quy định sử dụng
        PAYMENT,          // Thanh toán
        OWNERSHIP,        // Quyền sở hữu
        OTHER             // Khác
    }
    
    public enum DisputeStatus {
        PENDING,      // Chờ xử lý
        IN_REVIEW,    // Đang xem xét
        RESOLVED,     // Đã giải quyết
        CLOSED,       // Đã đóng
        ESCALATED     // Đã chuyển lên cấp cao
    }
    
    public enum DisputePriority {
        LOW,      // Thấp
        MEDIUM,   // Trung bình
        HIGH,     // Cao
        URGENT    // Khẩn cấp
    }
}

