package com.example.reservationadminservice.model.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity cho bảng vehicles trong booking database (read-only)
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class BookingVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;
    
    @Column(name = "vehicle_name")
    private String vehicleName;
    
    @Column(name = "vehicle_type")
    private String vehicleType;
    
    @Column(name = "license_plate")
    private String licensePlate;
    
    @Column(name = "group_id", length = 20)
    private String groupId;
    
    @Column(name = "status")
    private String status;
}

