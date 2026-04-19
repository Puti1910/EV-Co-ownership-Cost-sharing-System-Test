package com.example.VehicleServiceManagementService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    private ServiceType service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
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

    @Column(name = "requested_by_user_name", length = 255)
    private String requestedByUserName;

    @Column(name = "preferred_start_datetime")
    private LocalDateTime preferredStartDatetime;

    @Column(name = "preferred_end_datetime")
    private LocalDateTime preferredEndDatetime;

    @PostLoad
    public void ensureServiceType() {
        if ((serviceType == null || serviceType.trim().isEmpty()) && service != null) {
            String serviceTypeFromService = service.getServiceType();
            if (serviceTypeFromService != null && !serviceTypeFromService.trim().isEmpty()) {
                this.serviceType = serviceTypeFromService;
            }
        }
        if ((serviceName == null || serviceName.trim().isEmpty()) && service != null) {
            String serviceNameFromService = service.getServiceName();
            if (serviceNameFromService != null && !serviceNameFromService.trim().isEmpty()) {
                this.serviceName = serviceNameFromService;
            }
        }
    }
}