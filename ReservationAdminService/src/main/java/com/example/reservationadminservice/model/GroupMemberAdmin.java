package com.example.reservationadminservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_members")
@Getter @Setter @NoArgsConstructor
public class GroupMemberAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "group_id")
    private VehicleGroupAdmin group;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ownership_percentage")
    private Double ownershipPercentage;
}
