package com.example.VehicleServiceManagementService.integration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GroupManagementClient {

    private static final Logger log = LoggerFactory.getLogger(GroupManagementClient.class);

    private final RestTemplate restTemplate;

    public GroupManagementClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${group.management.service.url:http://localhost:8084}")
    private String primaryBaseUrl;

    @Value("${group.management.service.fallback-url:http://localhost:8082}")
    private String fallbackBaseUrl;

    public List<Map<String, Object>> getGroupsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String path = "/api/groups/user/" + userId;
        List<Map<String, Object>> result = exchangeForList(path);
        if (result == null) {
            log.warn("Không tìm thấy dữ liệu nhóm cho userId: {}. Trả về danh sách trống.", userId);
            return Collections.emptyList();
        }
        return result;
    }

    public List<Map<String, Object>> getMaintenanceOptions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String path = "/api/groups/user/" + userId + "/maintenance-options";
        List<Map<String, Object>> result = exchangeForList(path);
        if (result == null) {
            log.warn("Không tìm thấy tùy chọn bảo dưỡng cho userId: {}. Trả về danh sách trống.", userId);
            return Collections.emptyList();
        }
        return result;
    }

    public Optional<Map<String, Object>> getGroup(Long groupId) {
        if (groupId == null) {
            return Optional.empty();
        }
        String path = "/api/groups/" + groupId;
        return exchangeForMap(path);
    }

    public Optional<Map<String, Object>> getMembership(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return Optional.empty();
        }
        String path = "/api/groups/" + groupId + "/members/me/" + userId;
        try {
            return exchangeForMap(path);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound notFound) {
            log.warn("User {} is not a member of group {}", userId, groupId);
            return Optional.empty();
        }
    }

    private List<Map<String, Object>> exchangeForList(String path) {
        ParameterizedTypeReference<List<Map<String, Object>>> typeRef = new ParameterizedTypeReference<>() {};
        return executeWithFallback(path, typeRef, null);
    }

    private Optional<Map<String, Object>> exchangeForMap(String path) {
        ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};
        Map<String, Object> body = executeWithFallback(path, typeRef, null);
        return Optional.ofNullable(body);
    }

    private <T> T executeWithFallback(String path,
                                      ParameterizedTypeReference<T> typeRef,
                                      T defaultValue) {
        for (String base : new String[]{primaryBaseUrl, fallbackBaseUrl}) {
            if (base == null || base.isBlank()) {
                continue;
            }
            String url = base + path;
            try {
                log.debug("Calling GroupManagementService: {}", url);
                ResponseEntity<T> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        typeRef
                );
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
                log.warn("Group service {} responded with status {}", url, response.getStatusCode());
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound notFound) {
                log.warn("Resource not found at {}. Returning default value.", url);
                return defaultValue;
            } catch (Exception ex) {
                log.warn("Failed to call {}: {}", url, ex.getMessage());
            }
        }
        return defaultValue;
    }
}

