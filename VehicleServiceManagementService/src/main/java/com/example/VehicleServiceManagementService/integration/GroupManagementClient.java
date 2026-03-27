package com.example.VehicleServiceManagementService.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class GroupManagementClient {

    private final RestTemplate restTemplate;

    @Value("${group.management.service.url:http://localhost:8084}")
    private String primaryBaseUrl;

    @Value("${group.management.service.fallback-url:http://localhost:8082}")
    private String fallbackBaseUrl;

    public List<Map<String, Object>> getGroupsByUserId(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String path = "/api/groups/user/" + userId;
        return exchangeForList(path);
    }

    public List<Map<String, Object>> getMaintenanceOptions(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        String path = "/api/groups/user/" + userId + "/maintenance-options";
        return exchangeForList(path);
    }

    public Optional<Map<String, Object>> getGroup(Integer groupId) {
        if (groupId == null) {
            return Optional.empty();
        }
        String path = "/api/groups/" + groupId;
        return exchangeForMap(path);
    }

    public Optional<Map<String, Object>> getMembership(Integer groupId, Integer userId) {
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
        return executeWithFallback(path, typeRef, Collections.emptyList());
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
                throw notFound;
            } catch (Exception ex) {
                log.warn("Failed to call {}: {}", url, ex.getMessage());
            }
        }
        return defaultValue;
    }
}

