package com.example.VehicleServiceManagementService.integration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserAccountClient {

    private static final Logger log = LoggerFactory.getLogger(UserAccountClient.class);

    private final RestTemplate restTemplate;

    public UserAccountClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${user.account.service.url:http://user-account-service:8083}")
    private String baseUrl;

    /**
     * Kiểm tra sự tồn tại của người dùng qua User Account Service.
     */
    public boolean existsById(Long userId) {
        if (userId == null) {
            return false;
        }
        
        String url = baseUrl + "/api/auth/users/" + userId + "/exists";
        try {
            log.info("Calling UserAccountService: {}", url);
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception ex) {
            log.error("Failed to call UserAccountService at {}: {}", url, ex.getMessage());
        }
        return false;
    }
}
