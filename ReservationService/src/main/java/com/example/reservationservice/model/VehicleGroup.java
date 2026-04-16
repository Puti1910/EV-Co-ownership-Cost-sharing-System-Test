package com.example.reservationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_groups")
public class VehicleGroup {
    @Id
    @Column(name = "group_id", length = 20)
    private String groupId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "active", length = 50)
    private String active;

    public VehicleGroup() {}

    public VehicleGroup(String groupId, String groupName, String description, LocalDateTime creationDate, String active) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.creationDate = creationDate;
        this.active = active;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String v) { this.groupId = v; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String v) { this.groupName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime v) { this.creationDate = v; }
    public String getActive() { return active; }
    public void setActive(String v) { this.active = v; }
}

