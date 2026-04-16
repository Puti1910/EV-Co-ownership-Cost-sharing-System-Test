package com.example.reservationservice.dto;

import java.time.LocalDateTime;

public class FairnessMemberDTO {
    private Integer userId;
    private String fullName;
    private String email;
    private Double ownershipPercentage;
    private Double usageHours;
    private Double usagePercentage;
    private Double difference;
    private Double fairnessScore;
    private String priority;
    private LocalDateTime lastUsageEnd;
    private LocalDateTime nextReservationStart;

    public FairnessMemberDTO() {}

    public FairnessMemberDTO(Integer userId, String fullName, String email, Double ownershipPercentage, 
                             Double usageHours, Double usagePercentage, Double difference, 
                             Double fairnessScore, String priority, LocalDateTime lastUsageEnd, 
                             LocalDateTime nextReservationStart) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.ownershipPercentage = ownershipPercentage;
        this.usageHours = usageHours;
        this.usagePercentage = usagePercentage;
        this.difference = difference;
        this.fairnessScore = fairnessScore;
        this.priority = priority;
        this.lastUsageEnd = lastUsageEnd;
        this.nextReservationStart = nextReservationStart;
    }

    // Static builder-like method to avoid breaking existing code using .builder()
    public static FairnessMemberDTOBuilder builder() {
        return new FairnessMemberDTOBuilder();
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer id) { this.userId = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String n) { this.fullName = n; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public Double getOwnershipPercentage() { return ownershipPercentage; }
    public void setOwnershipPercentage(Double v) { this.ownershipPercentage = v; }
    public Double getUsageHours() { return usageHours; }
    public void setUsageHours(Double v) { this.usageHours = v; }
    public Double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(Double v) { this.usagePercentage = v; }
    public Double getDifference() { return difference; }
    public void setDifference(Double v) { this.difference = v; }
    public Double getFairnessScore() { return fairnessScore; }
    public void setFairnessScore(Double v) { this.fairnessScore = v; }
    public String getPriority() { return priority; }
    public void setPriority(String p) { this.priority = p; }
    public LocalDateTime getLastUsageEnd() { return lastUsageEnd; }
    public void setLastUsageEnd(LocalDateTime dt) { this.lastUsageEnd = dt; }
    public LocalDateTime getNextReservationStart() { return nextReservationStart; }
    public void setNextReservationStart(LocalDateTime dt) { this.nextReservationStart = dt; }

    public static class FairnessMemberDTOBuilder {
        private FairnessMemberDTO instance = new FairnessMemberDTO();
        public FairnessMemberDTOBuilder userId(Integer v) { instance.setUserId(v); return this; }
        public FairnessMemberDTOBuilder fullName(String v) { instance.setFullName(v); return this; }
        public FairnessMemberDTOBuilder email(String v) { instance.setEmail(v); return this; }
        public FairnessMemberDTOBuilder ownershipPercentage(Double v) { instance.setOwnershipPercentage(v); return this; }
        public FairnessMemberDTOBuilder usageHours(Double v) { instance.setUsageHours(v); return this; }
        public FairnessMemberDTOBuilder usagePercentage(Double v) { instance.setUsagePercentage(v); return this; }
        public FairnessMemberDTOBuilder difference(Double v) { instance.setDifference(v); return this; }
        public FairnessMemberDTOBuilder fairnessScore(Double v) { instance.setFairnessScore(v); return this; }
        public FairnessMemberDTOBuilder priority(String v) { instance.setPriority(v); return this; }
        public FairnessMemberDTOBuilder lastUsageEnd(LocalDateTime v) { instance.setLastUsageEnd(v); return this; }
        public FairnessMemberDTOBuilder nextReservationStart(LocalDateTime v) { instance.setNextReservationStart(v); return this; }
        public FairnessMemberDTO build() { return instance; }
    }
}

