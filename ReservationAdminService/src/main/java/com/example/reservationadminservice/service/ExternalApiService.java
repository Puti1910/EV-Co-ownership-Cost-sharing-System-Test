package com.example.reservationadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Service để gọi API từ các service khác (user-account-service, group-management-service, vehicle-service)
 * Thay thế việc query trực tiếp từ database
 */
@Service
@RequiredArgsConstructor
public class ExternalApiService {
    
    private final RestTemplate restTemplate;
    
    @Value("${user-account.service.url:http://localhost:8083}")
    private String userAccountServiceUrl;
    
    @Value("${group-management.service.url:http://localhost:8082}")
    private String groupManagementServiceUrl;
    
    @Value("${vehicle.service.url:http://localhost:8085}")
    private String vehicleServiceUrl;
    
    /**
     * Lấy thông tin user từ user-account-service
     * Trả về "User#n" để tránh lỗi encoding tiếng Việt
     */
    public String getUserName(Long userId) {
        // Hiển thị User#n thay vì tên đầy đủ để tránh lỗi encoding tiếng Việt
        // Giống như cách ReservationService làm
        return "User#" + userId;
    }
    
    /**
     * Lấy thông tin user chi tiết từ user-account-service (nếu cần)
     */
    public Optional<Map<String, Object>> getUserById(Long userId) {
        try {
            String url = userAccountServiceUrl + "/api/users/" + userId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy thông tin user: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Lấy thông tin vehicle từ vehicle-service
     */
    public Optional<Map<String, Object>> getVehicleById(Long vehicleId) {
        try {
            String url = vehicleServiceUrl + "/api/vehicles/" + vehicleId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy thông tin vehicle: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Lấy tên vehicle từ vehicle-service hoặc trả về "Vehicle#n"
     */
    public String getVehicleName(Long vehicleId) {
        try {
            Optional<Map<String, Object>> vehicleOpt = getVehicleById(vehicleId);
            if (vehicleOpt.isPresent() && vehicleOpt.get().containsKey("vehicleName")) {
                return (String) vehicleOpt.get().get("vehicleName");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tên vehicle: " + e.getMessage());
        }
        // Fallback: hiển thị Vehicle#n
        return "Vehicle#" + vehicleId;
    }
    
    /**
     * Lấy thông tin nhóm theo vehicleId từ group-management-service
     */
    public Optional<Map<String, Object>> getGroupByVehicleId(Long vehicleId) {
        try {
            // Lấy tất cả groups và tìm group có vehicleId phù hợp
            String url = groupManagementServiceUrl + "/api/groups";
            ResponseEntity<java.util.List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {}
            );
            
            if (response.getBody() != null) {
                return response.getBody().stream()
                    .filter(group -> {
                        Object vehicleIdObj = group.get("vehicleId");
                        if (vehicleIdObj == null) return false;
                        Long groupVehicleId = vehicleIdObj instanceof Number 
                            ? ((Number) vehicleIdObj).longValue() 
                            : Long.parseLong(vehicleIdObj.toString());
                        return groupVehicleId.equals(vehicleId);
                    })
                    .findFirst();
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy group theo vehicleId: " + e.getMessage());
            return Optional.empty();
        }
    }
}


