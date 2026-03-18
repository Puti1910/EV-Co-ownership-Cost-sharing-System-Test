package com.example.ui_service.external.service;

import com.example.ui_service.external.model.VehiclegroupDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class VehicleGroupRestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public VehicleGroupRestClient(RestTemplate restTemplate,
                                  @Value("${external.vehicles.base-url}") String vehiclesBaseUrl) {
        this.restTemplate = restTemplate;
        // Vehicle groups endpoint in backend is /api/vehicle-groups
        this.baseUrl = vehiclesBaseUrl.replace("/api/vehicles", "/api/vehicle-groups");
    }

    public List<VehiclegroupDTO> getAllVehicleGroups() {
        try {
            ResponseEntity<List<VehiclegroupDTO>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<VehiclegroupDTO>>() {
                    }
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<VehiclegroupDTO> getAvailableVehicleGroups(String includeGroupId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/available");
            if (includeGroupId != null && !includeGroupId.isBlank()) {
                builder.queryParam("currentGroupId", includeGroupId);
            }
            ResponseEntity<Map[]> response = restTemplate.getForEntity(builder.build(true).toUri(), Map[].class);
            Map[] items = response.getBody();
            List<VehiclegroupDTO> result = new ArrayList<>();
            if (items != null) {
                for (Map<String, Object> it : items) {
                    VehiclegroupDTO dto = new VehiclegroupDTO();
                    Object id = it.get("groupId");
                    Object name = it.get("name");
                    if (id != null) dto.setGroupId(id.toString());
                    if (name != null) dto.setName(name.toString());
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    public VehiclegroupDTO getVehicleGroupById(String groupId) {
        try {
            return restTemplate.getForObject(baseUrl + "/" + groupId, VehiclegroupDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getVehiclesByGroupId(String groupId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl + "/" + groupId + "/vehicles",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public DeleteResult deleteVehicleGroup(String groupId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + groupId).build(true).toUri(),
                    HttpMethod.DELETE,
                    null,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                return DeleteResult.success(body != null && !body.isBlank()
                        ? body
                        : "Nhóm xe đã được xóa thành công.");
            }
            return DeleteResult.failure("Không thể xóa nhóm xe. Mã lỗi: " + response.getStatusCode().value());
        } catch (HttpClientErrorException.NotFound e) {
            return DeleteResult.failure("Không tìm thấy nhóm xe với ID: " + groupId);
        } catch (Exception e) {
            return DeleteResult.failure("Đã xảy ra lỗi khi xóa nhóm xe: " + e.getMessage());
        }
    }

    public record DeleteResult(boolean success, String message) {
        public static DeleteResult success(String message) {
            return new DeleteResult(true, message);
        }

        public static DeleteResult failure(String message) {
            return new DeleteResult(false, message);
        }
    }
}


