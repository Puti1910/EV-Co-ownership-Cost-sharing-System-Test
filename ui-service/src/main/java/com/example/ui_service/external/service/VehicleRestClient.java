package com.example.ui_service.external.service;

import com.example.ui_service.external.model.VehicleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VehicleRestClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public VehicleRestClient(RestTemplate restTemplate,
                             @Value("${external.vehicles.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<VehicleDTO> getAllVehicles() {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(baseUrl, Map[].class);
            Map[] vehicles = response.getBody();
            if (vehicles == null || vehicles.length == 0) {
                return Collections.emptyList();
            }
            List<VehicleDTO> result = new ArrayList<>();
            for (Map<String, Object> vehicle : vehicles) {
                result.add(parseVehicle(vehicle));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public VehicleDTO getVehicleById(String vehicleId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/" + vehicleId, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseVehicle(response.getBody());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Object addVehicle(Map<String, Object> requestData) {
        ResponseEntity<Object> response = restTemplate.postForEntity(baseUrl + "/batch", requestData, Object.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    public Map<String, Object> updateVehicle(String vehicleId, Map<String, Object> vehicleData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(vehicleData, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/" + vehicleId, HttpMethod.PUT, request, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    public boolean deleteVehicle(String vehicleId) {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + vehicleId, HttpMethod.DELETE, null, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    private VehicleDTO parseVehicle(Map<String, Object> vehicle) {
        VehicleDTO dto = new VehicleDTO();
        Object vehicleIdObj = vehicle.get("vehicleId");
        if (vehicleIdObj != null) dto.setVehicleId(vehicleIdObj.toString());

        Object vehicleNumberObj = vehicle.get("vehicleNumber");
        if (vehicleNumberObj != null) dto.setVehicleNumber(vehicleNumberObj.toString());

        Object vehicleNameObj = Optional.ofNullable(vehicle.get("vehicleName"))
                .orElse(vehicle.get("vehiclename"));
        if (vehicleNameObj != null && !vehicleNameObj.toString().trim().isEmpty()) {
            dto.setName(vehicleNameObj.toString().trim());
        }

        Object vehicleTypeObj = Optional.ofNullable(vehicle.get("vehicleType"))
                .orElse(vehicle.get("type"));
        if (vehicleTypeObj != null) dto.setType(vehicleTypeObj.toString());

        Object statusObj = vehicle.get("status");
        if (statusObj != null) dto.setStatus(statusObj.toString());

        Object groupObj = vehicle.get("group");
        if (groupObj instanceof Map<?, ?> groupMap) {
            Object groupIdObj = groupMap.get("groupId");
            if (groupIdObj != null) dto.setGroupId(groupIdObj.toString());
            Object groupNameObj = groupMap.get("name");
            if (groupNameObj != null && (dto.getName() == null || dto.getName().trim().isEmpty())) {
                dto.setName(groupNameObj.toString());
            }
        }

        Object lastServiceDateObj = vehicle.get("lastServiceDate");
        if (lastServiceDateObj instanceof String s) {
            try {
                dto.setLastServiceDate(LocalDateTime.parse(s.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception ignored) {
            }
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            if (dto.getVehicleNumber() != null && !dto.getVehicleNumber().trim().isEmpty()) {
                dto.setName(dto.getVehicleNumber());
            } else if (dto.getVehicleId() != null) {
                dto.setName(dto.getVehicleId());
            } else {
                dto.setName("Xe chưa có tên");
            }
        }
        return dto;
    }
}


