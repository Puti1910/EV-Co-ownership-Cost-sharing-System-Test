package com.example.VehicleServiceManagementService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vehicleservice", schema = "vehicle_management", 
       indexes = {
           @Index(name = "idx_vehicle_id", columnList = "vehicle_id"),
           @Index(name = "idx_service_id", columnList = "service_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_service_vehicle", columnList = "service_id, vehicle_id")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "service", "vehicle"})
public class Vehicleservice {

    @EmbeddedId
    private VehicleserviceId id;

    // Thay đổi optional = true để có thể load được ngay cả khi foreign key không tồn tại
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceType service;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @MapsId("vehicleId")
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Size(max = 255)
    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Lob
    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;

    @Size(max = 50)
    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Column(name = "request_date", nullable = false, updatable = false)
    private Instant requestDate;

    @Size(max = 50)
    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "completion_date", nullable = true)
    private Instant completionDate;

    @Column(name = "group_ref_id")
    private Integer groupRefId;

    @Column(name = "requested_by_user_id")
    private Integer requestedByUserId;

    @Column(name = "requested_by_user_name", length = 150)
    private String requestedByUserName;

    @Column(name = "preferred_start_datetime")
    private LocalDateTime preferredStartDatetime;

    @Column(name = "preferred_end_datetime")
    private LocalDateTime preferredEndDatetime;

    
    // Helper methods để dễ dàng truy cập serviceId và vehicleId
    public String getServiceId() {
        if (id != null && id.getServiceId() != null) {
            return id.getServiceId();
        }
        return service != null ? service.getServiceId() : null;
    }

    public void setServiceId(String serviceId) {
        if (id == null) {
            id = new VehicleserviceId();
        }
        id.setServiceId(serviceId);
    }
    
    public String getVehicleId() {
        if (id != null && id.getVehicleId() != null) {
            return id.getVehicleId();
        }
        return vehicle != null ? vehicle.getVehicleId() : null;
    }

    public void setVehicleId(String vehicleId) {
        if (id == null) {
            id = new VehicleserviceId();
        }
        id.setVehicleId(vehicleId);
    }
    
    // Method để đảm bảo serviceType luôn được set từ ServiceType nếu null
    // Được gọi sau khi entity được load từ database
    @PostLoad
    public void ensureServiceType() {
        // Nếu serviceType null hoặc rỗng, lấy từ ServiceType entity
        if ((serviceType == null || serviceType.trim().isEmpty()) && service != null) {
            String serviceTypeFromService = service.getServiceType();
            if (serviceTypeFromService != null && !serviceTypeFromService.trim().isEmpty()) {
                this.serviceType = serviceTypeFromService;
            }
        }
        // Đảm bảo serviceName cũng được set nếu null
        if ((serviceName == null || serviceName.trim().isEmpty()) && service != null) {
            String serviceNameFromService = service.getServiceName();
            if (serviceNameFromService != null && !serviceNameFromService.trim().isEmpty()) {
                this.serviceName = serviceNameFromService;
            }
        }
    }
}