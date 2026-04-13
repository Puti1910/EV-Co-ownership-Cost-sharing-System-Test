package com.example.VehicleServiceManagementService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

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
    private Long groupRefId;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "requested_by_user_name", length = 150)
    private String requestedByUserName;

    @Column(name = "preferred_start_datetime")
    private LocalDateTime preferredStartDatetime;

    @Column(name = "preferred_end_datetime")
    private LocalDateTime preferredEndDatetime;

    // Manual Getters/Setters for all fields (Robustness against Lombok failure)
    public VehicleserviceId getId() { return id; }
    public void setId(VehicleserviceId id) { this.id = id; }

    public ServiceType getService() { return service; }
    public void setService(ServiceType service) { this.service = service; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public Instant getRequestDate() { return requestDate; }
    public void setRequestDate(Instant requestDate) { this.requestDate = requestDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCompletionDate() { return completionDate; }
    public void setCompletionDate(Instant completionDate) { this.completionDate = completionDate; }

    public Long getGroupRefId() { return groupRefId; }
    public void setGroupRefId(Long groupRefId) { this.groupRefId = groupRefId; }

    public Long getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(Long requestedByUserId) { this.requestedByUserId = requestedByUserId; }

    public String getRequestedByUserName() { return requestedByUserName; }
    public void setRequestedByUserName(String requestedByUserName) { this.requestedByUserName = requestedByUserName; }

    public LocalDateTime getPreferredStartDatetime() { return preferredStartDatetime; }
    public void setPreferredStartDatetime(LocalDateTime preferredStartDatetime) { this.preferredStartDatetime = preferredStartDatetime; }

    public LocalDateTime getPreferredEndDatetime() { return preferredEndDatetime; }
    public void setPreferredEndDatetime(LocalDateTime preferredEndDatetime) { this.preferredEndDatetime = preferredEndDatetime; }

    
    // Helper methods để dễ dàng truy cập serviceId và vehicleId
    public Long getServiceId() {
        if (id != null && id.getServiceId() != null) {
            return id.getServiceId();
        }
        return service != null ? service.getServiceId() : null;
    }

    public void setServiceId(Long serviceId) {
        if (id == null) {
            id = new VehicleserviceId();
        }
        id.setServiceId(serviceId);
    }
    
    public Long getVehicleId() {
        if (id != null && id.getVehicleId() != null) {
            return id.getVehicleId();
        }
        return vehicle != null ? vehicle.getVehicleId() : null;
    }

    public void setVehicleId(Long vehicleId) {
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