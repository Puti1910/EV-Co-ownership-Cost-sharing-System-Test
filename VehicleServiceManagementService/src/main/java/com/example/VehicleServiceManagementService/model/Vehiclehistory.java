package com.example.VehicleServiceManagementService.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "vehiclehistory", schema = "vehicle_management")
public class Vehiclehistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Vehiclegroup group;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "usage_start")
    private Instant usageStart;

    @Column(name = "usage_end")
    private Instant usageEnd;

}