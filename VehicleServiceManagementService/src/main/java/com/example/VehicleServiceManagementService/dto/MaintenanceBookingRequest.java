package com.example.VehicleServiceManagementService.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceBookingRequest {
    private Integer userId;
    private Integer groupId;
    /**
     * Vehicle identifier as seen by other services (can be numeric or string).
     */
    private String vehicleId;
    private String vehicleName;
    private String serviceId;
    private String serviceName;
    private String serviceDescription;
    private String preferredStartDatetime;
    private String preferredEndDatetime;
    private String requestedByName;
    private String contactPhone;
    private String note;
}

