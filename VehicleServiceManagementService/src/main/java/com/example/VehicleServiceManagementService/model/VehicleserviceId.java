package com.example.VehicleServiceManagementService.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VehicleserviceId implements Serializable {
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public VehicleserviceId() {}

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    public VehicleserviceId(Long serviceId, Long vehicleId) {
        this.serviceId = serviceId;
        this.vehicleId = vehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VehicleserviceId that)) {
            return false;
        }
        return Objects.equals(serviceId, that.serviceId)
                && Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, vehicleId);
    }
}

