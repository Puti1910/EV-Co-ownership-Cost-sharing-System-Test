package com.example.reservationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleGroup {
    @Id
    @Column(name = "group_id", length = 20)
    private String groupId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "active", length = 50)
    private String active;
}

