package com.example.reservationservice.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FairnessSummaryDTO {
    private Integer vehicleId;
    private String vehicleName;
    private Integer groupId;
    private String groupName;
    private Double fairnessIndex;
    private Double totalUsageHours;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDateTime generatedAt;
    private List<FairnessMemberDTO> members = new ArrayList<>();
    private List<FairnessReservationDTO> reservations = new ArrayList<>();
    private List<FairnessAvailabilityDTO> availability = new ArrayList<>();
    private List<Integer> priorityQueue = new ArrayList<>();

    public FairnessSummaryDTO() {}

    public FairnessSummaryDTO(Integer vehicleId, String vehicleName, Integer groupId, String groupName, 
                              Double fairnessIndex, Double totalUsageHours, LocalDateTime rangeStart, 
                              LocalDateTime rangeEnd, LocalDateTime generatedAt, List<FairnessMemberDTO> members, 
                              List<FairnessReservationDTO> reservations, List<FairnessAvailabilityDTO> availability, 
                              List<Integer> priorityQueue) {
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.fairnessIndex = fairnessIndex;
        this.totalUsageHours = totalUsageHours;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.generatedAt = generatedAt;
        this.members = members != null ? members : new ArrayList<>();
        this.reservations = reservations != null ? reservations : new ArrayList<>();
        this.availability = availability != null ? availability : new ArrayList<>();
        this.priorityQueue = priorityQueue != null ? priorityQueue : new ArrayList<>();
    }

    public static FairnessSummaryDTOBuilder builder() {
        return new FairnessSummaryDTOBuilder();
    }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer v) { this.vehicleId = v; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String v) { this.vehicleName = v; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer v) { this.groupId = v; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String v) { this.groupName = v; }
    public Double getFairnessIndex() { return fairnessIndex; }
    public void setFairnessIndex(Double v) { this.fairnessIndex = v; }
    public Double getTotalUsageHours() { return totalUsageHours; }
    public void setTotalUsageHours(Double v) { this.totalUsageHours = v; }
    public LocalDateTime getRangeStart() { return rangeStart; }
    public void setRangeStart(LocalDateTime v) { this.rangeStart = v; }
    public LocalDateTime getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(LocalDateTime v) { this.rangeEnd = v; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime v) { this.generatedAt = v; }
    public List<FairnessMemberDTO> getMembers() { return members; }
    public void setMembers(List<FairnessMemberDTO> v) { this.members = v; }
    public List<FairnessReservationDTO> getReservations() { return reservations; }
    public void setReservations(List<FairnessReservationDTO> v) { this.reservations = v; }
    public List<FairnessAvailabilityDTO> getAvailability() { return availability; }
    public void setAvailability(List<FairnessAvailabilityDTO> v) { this.availability = v; }
    public List<Integer> getPriorityQueue() { return priorityQueue; }
    public void setPriorityQueue(List<Integer> v) { this.priorityQueue = v; }

    public static class FairnessSummaryDTOBuilder {
        private FairnessSummaryDTO instance = new FairnessSummaryDTO();
        public FairnessSummaryDTOBuilder vehicleId(Integer v) { instance.setVehicleId(v); return this; }
        public FairnessSummaryDTOBuilder vehicleName(String v) { instance.setVehicleName(v); return this; }
        public FairnessSummaryDTOBuilder groupId(Integer v) { instance.setGroupId(v); return this; }
        public FairnessSummaryDTOBuilder groupName(String v) { instance.setGroupName(v); return this; }
        public FairnessSummaryDTOBuilder fairnessIndex(Double v) { instance.setFairnessIndex(v); return this; }
        public FairnessSummaryDTOBuilder totalUsageHours(Double v) { instance.setTotalUsageHours(v); return this; }
        public FairnessSummaryDTOBuilder rangeStart(LocalDateTime v) { instance.setRangeStart(v); return this; }
        public FairnessSummaryDTOBuilder rangeEnd(LocalDateTime v) { instance.setRangeEnd(v); return this; }
        public FairnessSummaryDTOBuilder generatedAt(LocalDateTime v) { instance.setGeneratedAt(v); return this; }
        public FairnessSummaryDTOBuilder members(List<FairnessMemberDTO> v) { instance.setMembers(v); return this; }
        public FairnessSummaryDTOBuilder reservations(List<FairnessReservationDTO> v) { instance.setReservations(v); return this; }
        public FairnessSummaryDTOBuilder availability(List<FairnessAvailabilityDTO> v) { instance.setAvailability(v); return this; }
        public FairnessSummaryDTOBuilder priorityQueue(List<Integer> v) { instance.setPriorityQueue(v); return this; }
        public FairnessSummaryDTO build() { return instance; }
    }
}