package com.example.VehicleServiceManagementService.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class VehicleserviceId implements Serializable {

    @Column(name = "service_id", length = 20, nullable = false)
    private String serviceId;

    @Column(name = "vehicle_id", length = 20, nullable = false)
    private String vehicleId;

    public VehicleserviceId(String serviceId, String vehicleId) {
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

