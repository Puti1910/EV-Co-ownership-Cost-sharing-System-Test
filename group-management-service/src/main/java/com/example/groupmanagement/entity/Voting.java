package com.example.groupmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Voting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"group", "votingResults"})
public class Voting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voteId")
    private Integer voteId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Group group;
    
    @Column(name = "topic", nullable = false, length = 255)
    private String topic;
    
    @Column(name = "optionA", length = 100)
    private String optionA;
    
    @Column(name = "optionB", length = 100)
    private String optionB;
    
    @Column(name = "finalResult", length = 100)
    private String finalResult;
    
    @Column(name = "totalVotes")
    private Integer totalVotes = 0;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "deadline")
    private LocalDateTime deadline; // Hạn chót bỏ phiếu
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VotingStatus status = VotingStatus.OPEN; // Trạng thái bỏ phiếu
    
    @Column(name = "closedAt")
    private LocalDateTime closedAt; // Thời điểm đóng bỏ phiếu
    
    @Column(name = "createdBy")
    private Integer createdBy; // User ID tạo bỏ phiếu
    
    @OneToMany(mappedBy = "voting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<VotingResult> votingResults;
    
    /**
     * Kiểm tra xem bỏ phiếu đã hết hạn chưa
     */
    public boolean isExpired() {
        if (deadline == null) {
            return false; // Không có deadline thì không hết hạn
        }
        return LocalDateTime.now().isAfter(deadline);
    }
    
    /**
     * Kiểm tra xem bỏ phiếu có đang mở không
     */
    public boolean isOpen() {
        return status == VotingStatus.OPEN && !isExpired();
    }
    
    /**
     * Đóng bỏ phiếu
     */
    public void close() {
        this.status = VotingStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
    
    public enum VotingStatus {
        OPEN("Đang mở"),
        CLOSED("Đã đóng"),
        CANCELLED("Đã hủy");
        
        private final String displayName;
        
        VotingStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
