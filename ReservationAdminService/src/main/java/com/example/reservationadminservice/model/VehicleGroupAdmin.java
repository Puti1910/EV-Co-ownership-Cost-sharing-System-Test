package com.example.reservationadminservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_groups")
@Getter @Setter @NoArgsConstructor
public class VehicleGroupAdmin {
    @Id
    @Column(name = "group_id", length = 20)
    private String groupId;

    @Column(name = "group_name")
    private String groupName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "creation_date")
    private LocalDateTime creationDate;
    
    @Column(name = "active")
    private String active;
}
