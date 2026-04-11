package com.example.VehicleServiceManagementService.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicle", schema = "vehicle_management")
public class Vehicle {

    @Id
    @Column(name = "vehicle_id", length = 20, nullable = false)
    private String vehicleId; // 🔹 Dạng "VEH001", dùng String thay vì Integer/Long

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "group_id", nullable = true)
    private Vehiclegroup group;


    @Size(max = 20)
    @Column(name = "vehicle_number", length = 20, unique = true)
    private String vehicleNumber; // 🔹 Biển số xe, ví dụ: "30A-12345" (unique)

    @Transient
    private String vehicleName; // 🔹 Tên xe (tạm thời, không lưu trong DB)

    @Size(max = 50)
    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @Size(max = 50)
    @Column(name = "status", length = 50)
    private String status;

    // 🔹 Constructors
    public Vehicle() {}

    public Vehicle(String vehicleId, String vehicleNumber,
                   String vehicleType, String status) {
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.status = status;
    }

    public Vehicle(String vehicleId, String vehicleNumber, String vehicleName,
                   String vehicleType, String status) {
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.vehicleName = vehicleName;
        this.vehicleType = vehicleType;
        this.status = status;
    }

    // 🔹 Convenience getters cho hiển thị
    public String getDisplayName() {
        if (vehicleName != null && !vehicleName.trim().isEmpty()) {
            return vehicleName;
        }
        return vehicleNumber != null ? vehicleNumber : vehicleId;
    }
}
