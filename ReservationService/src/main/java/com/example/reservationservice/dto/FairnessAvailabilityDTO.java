package com.example.reservationservice.dto;

import java.time.LocalDateTime;

public class FairnessAvailabilityDTO {
    private LocalDateTime start;
    private LocalDateTime end;
    private Double durationHours;
    private String label;

    public FairnessAvailabilityDTO() {}

    public FairnessAvailabilityDTO(LocalDateTime start, LocalDateTime end, Double durationHours, String label) {
        this.start = start;
        this.end = end;
        this.durationHours = durationHours;
        this.label = label;
    }

    public static FairnessAvailabilityDTOBuilder builder() {
        return new FairnessAvailabilityDTOBuilder();
    }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime v) { this.start = v; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime v) { this.end = v; }
    public Double getDurationHours() { return durationHours; }
    public void setDurationHours(Double v) { this.durationHours = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }

    public static class FairnessAvailabilityDTOBuilder {
        private FairnessAvailabilityDTO instance = new FairnessAvailabilityDTO();
        public FairnessAvailabilityDTOBuilder start(LocalDateTime v) { instance.setStart(v); return this; }
        public FairnessAvailabilityDTOBuilder end(LocalDateTime v) { instance.setEnd(v); return this; }
        public FairnessAvailabilityDTOBuilder durationHours(Double v) { instance.setDurationHours(v); return this; }
        public FairnessAvailabilityDTOBuilder label(String v) { instance.setLabel(v); return this; }
        public FairnessAvailabilityDTO build() { return instance; }
    }
}

