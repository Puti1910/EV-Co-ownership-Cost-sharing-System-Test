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

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long id) { this.vehicleId = id; }
    public String getExternalVehicleId() { return externalVehicleId; }
    public void setExternalVehicleId(String id) { this.externalVehicleId = id; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String name) { this.vehicleName = name; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String lp) { this.licensePlate = lp; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vt) { this.vehicleType = vt; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String gid) { this.groupId = gid; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}

