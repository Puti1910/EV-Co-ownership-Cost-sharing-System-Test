package com.example.VehicleServiceManagementService.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;


@Entity
@Table(name = "vehiclegroup", schema = "vehicle_management")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehiclegroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name", length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    // Getter và Setter cho groupId
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    // Getter và Setter cho groupName
    public String getName() {
        return name;
    }

    public void setName(String groupName) {
        this.name = groupName;
    }

    // Getter và Setter cho description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
