package com.example.reservationadminservice.model.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity cho bảng vehicle_groups trong booking database (read-only)
 */
@Entity
@Table(name = "vehicle_groups")
@Getter
@Setter
public class BookingVehicleGroup {
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

