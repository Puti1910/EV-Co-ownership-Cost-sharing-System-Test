package com.example.reservationservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "external_vehicle_id", length = 50)
    private String externalVehicleId; // Lưu vehicle_id từ vehicle_management database

    @Column(name = "vehicle_name")
    private String vehicleName;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "status")
    private String status = "AVAILABLE";
}

