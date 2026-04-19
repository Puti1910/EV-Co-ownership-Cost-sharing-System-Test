package com.example.reservationservice.dto;

<<<<<<< HEAD
import java.time.LocalDateTime;

public class FairnessReservationDTO {
    private Long reservationId;
    private Long vehicleId;
    private String vehicleName;
    private Long userId;
=======
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FairnessReservationDTO {
    private Long reservationId;
    private Integer vehicleId;
    private String vehicleName;
    private Integer userId;
>>>>>>> origin/main
    private String userName;
    private LocalDateTime start;
    private LocalDateTime end;
    private String status;
    private String purpose;
<<<<<<< HEAD

    public FairnessReservationDTO() {}

    public FairnessReservationDTO(Long reservationId, Long vehicleId, String vehicleName, 
                                  Long userId, String userName, LocalDateTime start, 
                                  LocalDateTime end, String status, String purpose) {
        this.reservationId = reservationId;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.userId = userId;
        this.userName = userName;
        this.start = start;
        this.end = end;
        this.status = status;
        this.purpose = purpose;
    }

    public static FairnessReservationDTOBuilder builder() {
        return new FairnessReservationDTOBuilder();
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long v) { this.reservationId = v; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long v) { this.vehicleId = v; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String v) { this.vehicleName = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime v) { this.start = v; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime v) { this.end = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }

    public static class FairnessReservationDTOBuilder {
        private FairnessReservationDTO instance = new FairnessReservationDTO();
        public FairnessReservationDTOBuilder reservationId(Long v) { instance.setReservationId(v); return this; }
        public FairnessReservationDTOBuilder vehicleId(Long v) { instance.setVehicleId(v); return this; }
        public FairnessReservationDTOBuilder vehicleName(String v) { instance.setVehicleName(v); return this; }
        public FairnessReservationDTOBuilder userId(Long v) { instance.setUserId(v); return this; }
        public FairnessReservationDTOBuilder userName(String v) { instance.setUserName(v); return this; }
        public FairnessReservationDTOBuilder start(LocalDateTime v) { instance.setStart(v); return this; }
        public FairnessReservationDTOBuilder end(LocalDateTime v) { instance.setEnd(v); return this; }
        public FairnessReservationDTOBuilder status(String v) { instance.setStatus(v); return this; }
        public FairnessReservationDTOBuilder purpose(String v) { instance.setPurpose(v); return this; }
        public FairnessReservationDTO build() { return instance; }
    }
=======
>>>>>>> origin/main
}

