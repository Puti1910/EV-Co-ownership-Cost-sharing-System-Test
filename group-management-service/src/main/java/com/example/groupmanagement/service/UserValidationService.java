package com.example.groupmanagement.service;

import com.example.groupmanagement.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserValidationService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${microservices.user-account.url:http://user-account-service:8081}")
    private String userAccountServiceUrl;
    
    /**
     * Check if user exists in User Account Service
     * @param userId user ID to validate
     * @return true nếu user tồn tại, false nếu không
     * @throws ValidationException nếu userId không hợp lệ
     */
    public boolean isUserExists(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(
                "User ID phải là số dương",
                "userId",
                "INVALID_USER_ID"
            );
        }
        
        try {
            String url = userAccountServiceUrl + "/api/auth/users/check/" + userId;
            logger.info("🔵 [UserValidationService] Checking if user exists: {}", url);
            
            Boolean exists = restTemplate.getForObject(url, Boolean.class);
            
            if (exists == null || !exists) {
                logger.warn("❌ [UserValidationService] User ID {} không tồn tại", userId);
                return false;
            }
            
            logger.info("✅ [UserValidationService] User ID {} tồn tại", userId);
            return true;
            
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            logger.warn("❌ [UserValidationService] User ID {} không tìm thấy (404)", userId);
            return false;
        } catch (Exception e) {
            logger.warn("⚠️ [UserValidationService] Lỗi khi check user: {}", e.getMessage());
            // Fallback: cho phép thêm (để tránh lỗi network block action)
            return true;
        }
    }
}
