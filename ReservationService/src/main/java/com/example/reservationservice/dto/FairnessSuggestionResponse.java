package com.example.reservationservice.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FairnessSuggestionResponse {
    private Long vehicleId;
    private Long userId;
    private boolean approved;
    private String priority;
    private String reason;
    private LocalDateTime requestedStart;
    private LocalDateTime requestedEnd;
    private FairnessMemberDTO applicant;
    private List<FairnessReservationDTO> conflicts = new ArrayList<>();
    private List<FairnessAvailabilityDTO> recommendations = new ArrayList<>();

    public FairnessSuggestionResponse() {}

    public FairnessSuggestionResponse(Long vehicleId, Long userId, boolean approved, String priority, 
                                      String reason, LocalDateTime requestedStart, LocalDateTime requestedEnd, 
                                      FairnessMemberDTO applicant, List<FairnessReservationDTO> conflicts, 
                                      List<FairnessAvailabilityDTO> recommendations) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.approved = approved;
        this.priority = priority;
        this.reason = reason;
        this.requestedStart = requestedStart;
        this.requestedEnd = requestedEnd;
        this.applicant = applicant;
        this.conflicts = conflicts != null ? conflicts : new ArrayList<>();
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public static FairnessSuggestionResponseBuilder builder() {
        return new FairnessSuggestionResponseBuilder();
    }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean v) { this.approved = v; }
    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public LocalDateTime getRequestedStart() { return requestedStart; }
    public void setRequestedStart(LocalDateTime v) { this.requestedStart = v; }
    public LocalDateTime getRequestedEnd() { return requestedEnd; }
    public void setRequestedEnd(LocalDateTime v) { this.requestedEnd = v; }
    public FairnessMemberDTO getApplicant() { return applicant; }
    public void setApplicant(FairnessMemberDTO v) { this.applicant = v; }
    public List<FairnessReservationDTO> getConflicts() { return conflicts; }
    public void setConflicts(List<FairnessReservationDTO> v) { this.conflicts = v; }
    public List<FairnessAvailabilityDTO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<FairnessAvailabilityDTO> v) { this.recommendations = v; }

    public static class FairnessSuggestionResponseBuilder {
        private FairnessSuggestionResponse instance = new FairnessSuggestionResponse();
        public FairnessSuggestionResponseBuilder vehicleId(Long v) { instance.setVehicleId(v); return this; }
        public FairnessSuggestionResponseBuilder userId(Long v) { instance.setUserId(v); return this; }
        public FairnessSuggestionResponseBuilder approved(boolean v) { instance.setApproved(v); return this; }
        public FairnessSuggestionResponseBuilder priority(String v) { instance.setPriority(v); return this; }
        public FairnessSuggestionResponseBuilder reason(String v) { instance.setReason(v); return this; }
        public FairnessSuggestionResponseBuilder requestedStart(LocalDateTime v) { instance.setRequestedStart(v); return this; }
        public FairnessSuggestionResponseBuilder requestedEnd(LocalDateTime v) { instance.setRequestedEnd(v); return this; }
        public FairnessSuggestionResponseBuilder applicant(FairnessMemberDTO v) { instance.setApplicant(v); return this; }
        public FairnessSuggestionResponseBuilder conflicts(List<FairnessReservationDTO> v) { instance.setConflicts(v); return this; }
        public FairnessSuggestionResponseBuilder recommendations(List<FairnessAvailabilityDTO> v) { instance.setRecommendations(v); return this; }
        public FairnessSuggestionResponse build() { return instance; }
    }
}
