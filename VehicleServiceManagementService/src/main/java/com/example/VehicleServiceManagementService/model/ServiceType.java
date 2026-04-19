package com.example.VehicleServiceManagementService.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service", schema = "vehicle_management", indexes = {
        @Index(name = "idx_service_name", columnList = "service_name"),
        @Index(name = "idx_service_type", columnList = "service_type")
})
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id", nullable = false, unique = true, updatable = false)
    private Long serviceId; 

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(min = 1, max = 255, message = "Tên dịch vụ phải từ 1 đến 255 ký tự")
    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @NotBlank(message = "Loại dịch vụ không được để trống")
    @Size(max = 50, message = "Loại dịch vụ không được vượt quá 50 ký tự")
    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

    public ServiceType(Long serviceId, String serviceName) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }

    public ServiceType(Long serviceId, String serviceName, String serviceType) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return "ServiceType{" +
                "serviceId=" + serviceId +
                ", serviceName='" + serviceName + '\'' +
                ", serviceType='" + serviceType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceType that = (ServiceType) o;
        return serviceId != null && serviceId.equals(that.serviceId);
    }

    @Override
    public int hashCode() {
        return serviceId != null ? serviceId.hashCode() : 0;
    }
}
