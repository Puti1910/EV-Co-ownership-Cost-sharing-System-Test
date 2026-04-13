package com.example.VehicleServiceManagementService.dto;

import lombok.Getter;
import lombok.Setter;

public class MaintenanceBookingRequest {
    private Long userId;
    private Long groupId;
    /**
     * Vehicle identifier (numeric).
     */
    private Long vehicleId;
    private String vehicleName;
    private Long serviceId;
    private String serviceName;
    private String serviceDescription;
    private String preferredStartDatetime;
    private String preferredEndDatetime;
    private String requestedByName;
    private String contactPhone;
    private String note;

    // Manual Getters/Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    public String getPreferredStartDatetime() { return preferredStartDatetime; }
    public void setPreferredStartDatetime(String preferredStartDatetime) { this.preferredStartDatetime = preferredStartDatetime; }
    public String getPreferredEndDatetime() { return preferredEndDatetime; }
    public void setPreferredEndDatetime(String preferredEndDatetime) { this.preferredEndDatetime = preferredEndDatetime; }
    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

