package com.example.ui_service.external.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleServiceRestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public VehicleServiceRestClient(RestTemplate restTemplate,
                                    @Value("${external.vehicleservices.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> registerVehicleService(Map<String, Object> serviceData) {
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(serviceData);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Đăng ký dịch vụ thất bại: " + response.getStatusCode());
        } catch (RestClientException e) {
            throw new RuntimeException("Không thể đăng ký dịch vụ: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllVehicleServices() {
        try {
            ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {};
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    null,
                    typeRef
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getVehicleServicesByVehicleId(String vehicleId) {
        try {
            ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {};
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl + "/vehicle/" + vehicleId,
                    HttpMethod.GET,
                    null,
                    typeRef
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> updateServiceStatus(String serviceId, String vehicleId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/service/" + serviceId + "/vehicle/" + vehicleId,
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Cập nhật trạng thái thất bại: " + response.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMaintenanceOptions(Integer userId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/maintenance/options?userId=" + userId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object optionsObj = response.getBody().get("options");
                if (optionsObj instanceof List<?> list) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            results.add(new HashMap<>((Map<String, Object>) map));
                        }
                    }
                    return results;
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Map<String, Object> bookMaintenance(Map<String, Object> payload) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/maintenance/book",
                HttpMethod.POST,
                new HttpEntity<>(payload),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Không thể đặt lịch bảo dưỡng: " + response.getStatusCode());
    }

    public void deleteVehicleService(String serviceId, String vehicleId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/service/" + serviceId + "/vehicle/" + vehicleId,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // Kiểm tra response status
            if (response.getStatusCode().is2xxSuccessful()) {
                // Thành công - kiểm tra response body
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("success") && !Boolean.TRUE.equals(body.get("success"))) {
                    String errorMessage = body.containsKey("message") ? (String) body.get("message") : "Không thể xóa dịch vụ";
                    throw new RuntimeException(errorMessage);
                }
                // Nếu success = true hoặc không có field success, coi như thành công
                return;
            } else {
                // Status không phải 2xx
                String errorMessage = "Không thể xóa dịch vụ: " + response.getStatusCode();
                if (response.getBody() != null && response.getBody().containsKey("message")) {
                    errorMessage = (String) response.getBody().get("message");
                }
                throw new RuntimeException(errorMessage);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Xử lý lỗi HTTP (404, 400, etc.)
            String errorMessage = e.getResponseBodyAsString();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                try {
                    // Thử parse JSON error message
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> errorMap = mapper.readValue(errorMessage, Map.class);
                    if (errorMap.containsKey("message")) {
                        errorMessage = (String) errorMap.get("message");
                    }
                } catch (Exception ignored) {
                    // Nếu không parse được JSON, có thể là plain text, dùng message gốc
                }
            } else {
                errorMessage = "Không tìm thấy đăng ký dịch vụ với serviceId: " + serviceId + " và vehicleId: " + vehicleId;
            }
            throw new RuntimeException("Không thể xóa dịch vụ: " + e.getStatusCode() + " " + errorMessage, e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Lỗi kết nối
            throw new RuntimeException("Không thể kết nối đến service: " + e.getMessage(), e);
        } catch (org.springframework.web.client.RestClientException e) {
            // Lỗi khác từ RestTemplate
            String errorMessage = e.getMessage();
            // Kiểm tra nếu là lỗi parse JSON
            if (errorMessage != null && errorMessage.contains("extracting response")) {
                // Có thể response là String thay vì JSON, nhưng xóa đã thành công
                // Trong trường hợp này, không throw exception vì xóa đã thành công
                System.out.println("⚠️ [DELETE] Response không phải JSON nhưng có thể xóa đã thành công");
                return;
            }
            throw new RuntimeException("Không thể xóa dịch vụ: " + errorMessage, e);
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa dịch vụ: " + e.getMessage(), e);
        }
    }
}


