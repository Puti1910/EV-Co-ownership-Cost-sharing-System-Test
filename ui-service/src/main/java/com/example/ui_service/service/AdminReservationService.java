package com.example.ui_service.service;

import com.example.ui_service.security.TokenRelayInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReservationService {

    @Value("${reservation.admin.service.url:http://localhost:8084}")
    private String adminReservationServiceUrl;

    private final TokenRelayInterceptor tokenRelayInterceptor;

    public AdminReservationService(TokenRelayInterceptor tokenRelayInterceptor) {
        this.tokenRelayInterceptor = tokenRelayInterceptor;
    }

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(tokenRelayInterceptor));
        return restTemplate;
    }

    /**
     * Lấy tất cả reservations từ Admin Reservation Service
     */
    public List<Map<String, Object>> getAllReservations() {
        try {
            String url = adminReservationServiceUrl + "/api/admin/reservations";
            RestTemplate restTemplate = createRestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");

            String token = resolveToken();
            if (token != null) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> result = response.getBody();
            return result != null ? result : List.of();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi lấy danh sách đặt lịch từ admin service: " + e.getMessage());
            return List.of();
        }
    }

    public boolean updateReservation(Long id, Long userId, Long vehicleId, String startDatetime, String endDatetime, String note, String status) {
        try {
            String url = adminReservationServiceUrl + "/api/admin/reservations/" + id;
            RestTemplate restTemplate = createRestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = resolveToken();
            if (token != null) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", id);
            payload.put("userId", userId);
            payload.put("vehicleId", vehicleId);
            payload.put("startDatetime", startDatetime);
            payload.put("endDatetime", endDatetime);
            if (note != null) {
                payload.put("purpose", note);
            }
            payload.put("status", status);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi cập nhật đặt lịch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateReservationStatus(Long id, String status) {
        try {
            String url = adminReservationServiceUrl + "/api/admin/reservations/" + id;
            RestTemplate restTemplate = createRestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = resolveToken();
            if (token != null) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            // Normalize status: uppercase và trim
            String normalizedStatus = status != null ? status.trim().toUpperCase() : null;
            System.out.println("🔄 Updating reservation " + id + " status: " + status + " → " + normalizedStatus);

            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", id);
            if (normalizedStatus != null) {
                payload.put("status", normalizedStatus);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                System.out.println("✅ Successfully updated reservation " + id + " status to: " + normalizedStatus);
            } else {
                System.err.println("⚠️ Failed to update reservation " + id + " status. Response: " + response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi cập nhật trạng thái đặt lịch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteReservation(Long id) {
        try {
            String url = adminReservationServiceUrl + "/api/admin/reservations/" + id;
            RestTemplate restTemplate = createRestTemplate();

            HttpHeaders headers = new HttpHeaders();
            String token = resolveToken();
            if (token != null) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi xóa đặt lịch: " + e.getMessage());
            return false;
        }
    }

    private String resolveToken() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header)) {
                return header.startsWith("Bearer ") ? header : "Bearer " + header;
            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                        String token = cookie.getValue();
                        return token.startsWith("Bearer ") ? token : "Bearer " + token;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

