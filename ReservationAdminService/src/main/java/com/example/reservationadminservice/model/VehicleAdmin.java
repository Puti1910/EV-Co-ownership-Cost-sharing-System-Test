package com.example.reservationadminservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class VehicleAdmin {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "vehicle_name", nullable = false)
    private String vehicleName;

    @Column(name = "vehicle_type")
    private String vehicleType;
    
    @Column(name = "license_plate")
    private String licensePlate;
    
    @Column(name = "group_id")
    private Long groupId;
    
    @Column(name = "status")
    private String status;
}
