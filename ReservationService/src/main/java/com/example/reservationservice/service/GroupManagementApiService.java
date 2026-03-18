package com.example.reservationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service để gọi API từ group-management-service
 * Thay thế việc query trực tiếp từ database
 */
@Service
@RequiredArgsConstructor
public class GroupManagementApiService {
    
    private final RestTemplate restTemplate;
    
    @Value("${group-management.service.url:http://localhost:8082}")
    private String groupManagementServiceUrl;
    
    /**
     * Tạo HttpEntity với JWT token nếu có
     */
    private HttpEntity<?> createHttpEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isEmpty()) {
            headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
        }
        return new HttpEntity<>(headers);
    }
    
    /**
     * Lấy thông tin nhóm theo vehicleId
     * Từ group-management-service, Group có vehicleId
     */
    public Optional<Map<String, Object>> getGroupByVehicleId(Integer vehicleId) {
        return getGroupByVehicleId(vehicleId, null);
    }
    
    public Optional<Map<String, Object>> getGroupByVehicleId(Integer vehicleId, String token) {
        try {
            // Lấy tất cả groups và tìm group có vehicleId phù hợp
            String url = groupManagementServiceUrl + "/api/groups";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(token),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getBody() != null) {
                return response.getBody().stream()
                    .filter(group -> {
                        Object vehicleIdObj = group.get("vehicleId");
                        if (vehicleIdObj == null) return false;
                        Integer groupVehicleId = vehicleIdObj instanceof Number 
                            ? ((Number) vehicleIdObj).intValue() 
                            : Integer.parseInt(vehicleIdObj.toString());
                        return groupVehicleId.equals(vehicleId);
                    })
                    .findFirst();
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy group theo vehicleId: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * Lấy danh sách thành viên trong nhóm
     */
    public List<Map<String, Object>> getGroupMembers(Integer groupId) {
        return getGroupMembers(groupId, null);
    }
    
    public List<Map<String, Object>> getGroupMembers(Integer groupId, String token) {
        try {
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members/view";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(token),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null && response.getBody().containsKey("members")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> members = (List<Map<String, Object>>) response.getBody().get("members");
                return members != null ? members : List.of();
            }
            return List.of();
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy group members: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Lấy thông tin nhóm theo groupId
     */
    public Optional<Map<String, Object>> getGroupById(Integer groupId) {
        return getGroupById(groupId, null);
    }
    
    public Optional<Map<String, Object>> getGroupById(Integer groupId, String token) {
        try {
            String url = groupManagementServiceUrl + "/api/groups/" + groupId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(token),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API lấy group: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Lấy danh sách nhóm mà user tham gia
     */
    public List<Map<String, Object>> getGroupsByUserId(Integer userId) {
        return getGroupsByUserId(userId, null);
    }
    
    public List<Map<String, Object>> getGroupsByUserId(Integer userId, String token) {
        try {
            String url = groupManagementServiceUrl + "/api/groups/user/" + userId;
            System.out.println("🔵 [GroupManagementApiService] Calling URL: " + url);
            System.out.println("🔵 [GroupManagementApiService] Token: " + (token != null ? "Present" : "Missing"));
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(token),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            System.out.println("✅ [GroupManagementApiService] Response status: " + response.getStatusCode());
            System.out.println("✅ [GroupManagementApiService] Response body size: " + (response.getBody() != null ? response.getBody().size() : 0));
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Connection refused hoặc timeout
            System.err.println("❌ [GroupManagementApiService] Lỗi kết nối khi gọi API lấy group theo userId: " + e.getMessage());
            e.printStackTrace();
            // Retry logic hoặc return empty list để không crash
            return List.of();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx errors
            System.err.println("❌ [GroupManagementApiService] HTTP error khi gọi API lấy group theo userId: " + e.getStatusCode() + " - " + e.getMessage());
            e.printStackTrace();
            return List.of();
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 5xx errors
            System.err.println("❌ [GroupManagementApiService] Server error khi gọi API lấy group theo userId: " + e.getStatusCode() + " - " + e.getMessage());
            e.printStackTrace();
            return List.of();
        } catch (Exception e) {
            System.err.println("❌ [GroupManagementApiService] Lỗi khi gọi API lấy group theo userId: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * Lấy thông tin membership của user trong nhóm
     */
    public Optional<Map<String, Object>> getMembershipInfo(Integer groupId, Integer userId) {
        return getMembershipInfo(groupId, userId, null);
    }
    
    public Optional<Map<String, Object>> getMembershipInfo(Integer groupId, Integer userId, String token) {
        try {
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members/me/" + userId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createHttpEntity(token),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API membership info: " + e.getMessage());
            return Optional.empty();
        }
    }
}

