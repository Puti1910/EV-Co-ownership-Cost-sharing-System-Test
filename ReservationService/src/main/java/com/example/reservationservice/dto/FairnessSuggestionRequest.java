package com.example.reservationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class FairnessSuggestionRequest {
    private Integer userId;
    private LocalDateTime desiredStart;
    private Double durationHours = 2.0;

    // Custom setter to handle multiple date formats from Postman
    public void setDesiredStart(Object value) {
        if (value == null || value.toString().isEmpty() || value.toString().contains("{{")) {
            this.desiredStart = null;
            return;
        }
        
        String dateStr = value.toString();
        try {
            // Thử format ISO (2026-12-01T10:00:00)
            this.desiredStart = LocalDateTime.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            try {
                // Thử format dd-MM-yyyy HH:mm
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                this.desiredStart = LocalDateTime.parse(dateStr, dtf);
            } catch (Exception e2) {
                // Nếu không được nữa thì để null, service sẽ handle mặc định
                this.desiredStart = null;
            }
        }
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public LocalDateTime getDesiredStart() { return desiredStart; }
    public Double getDurationHours() { return durationHours; }
    public void setDurationHours(Double durationHours) { this.durationHours = durationHours; }
}

