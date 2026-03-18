package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.repository.VehicleGroupRepository;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleDataSyncService {

    private final VehicleRepository vehicleRepository;
    private final VehicleGroupRepository vehicleGroupRepository;

    @Transactional
    public Vehiclegroup ensureGroupSynced(Integer groupId, Map<String, Object> groupPayload) {
        if (groupId == null) {
            return null;
        }
        String groupKey = String.valueOf(groupId);
        Optional<Vehiclegroup> existing = vehicleGroupRepository.findById(groupKey);
        if (existing.isPresent()) {
            Vehiclegroup group = existing.get();
            if (groupPayload != null) {
                Object name = groupPayload.get("groupName");
                if (name instanceof String && !((String) name).isBlank()) {
                    group.setName((String) name);
                }
                vehicleGroupRepository.save(group);
            }
            return group;
        }

        Vehiclegroup group = new Vehiclegroup();
        group.setGroupId(groupKey);
        if (groupPayload != null) {
            Object name = groupPayload.get("groupName");
            group.setName(name instanceof String && !((String) name).isBlank() ? (String) name : "Group #" + groupKey);
            Object description = groupPayload.get("description");
            if (description instanceof String) {
                group.setDescription((String) description);
            }
        } else {
            group.setName("Group #" + groupKey);
        }
        return vehicleGroupRepository.save(group);
    }

    @Transactional
    public Vehicle ensureVehicleSynced(String vehicleId,
                                       Vehiclegroup group,
                                       Map<String, Object> vehiclePayload,
                                       String fallbackName) {
        if (vehicleId == null || vehicleId.isBlank()) {
            throw new IllegalArgumentException("vehicleId is required");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseGet(() -> {
            Vehicle v = new Vehicle();
            v.setVehicleId(vehicleId);
            v.setStatus("ready");
            return v;
        });

        String name = extractString(vehiclePayload, "vehicleName");
        if (name == null) {
            name = extractString(vehiclePayload, "vehiclename");
        }
        if (name == null) {
            name = fallbackName;
        }
        if (name != null && !name.isBlank()) {
            vehicle.setVehicleName(name);
        }

        String number = extractString(vehiclePayload, "vehicleNumber");
        if (number != null && !number.isBlank()) {
            vehicle.setVehicleNumber(number);
        }

        String type = extractString(vehiclePayload, "vehicleType");
        if (type == null) {
            type = extractString(vehiclePayload, "type");
        }
        if (type != null && !type.isBlank()) {
            vehicle.setVehicleType(type);
        }

        if (group != null) {
            vehicle.setGroup(group);
        }

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        vehicleRepository.flush(); // Đảm bảo vehicle được flush vào database trước khi sử dụng
        return savedVehicle;
    }

    private String extractString(Map<String, Object> payload, String key) {
        if (payload == null || key == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return null;
    }
}

