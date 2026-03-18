package com.example.ui_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    @Value("${reservation.service.url}")
    private String reservationServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    private String getApiBaseUrl() {
        return reservationServiceUrl + "/api";
    }

    public List<Map<String, Object>> getReservationsByVehicleId(int vehicleId) {
        try {
            // Thêm timestamp để tránh cache
            String url = getApiBaseUrl() + "/vehicles/" + vehicleId + "/reservations?t=" + System.currentTimeMillis();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> result = response.getBody();
            System.out.println("📦 Fetched " + (result != null ? result.size() : 0) + " reservations for vehicle " + vehicleId);
            return result != null ? result : List.of();
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi lấy danh sách đặt xe: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public Map<String, Object> createReservation(Map<String, Object> data) {
        try {
            String url = getApiBaseUrl() + "/reservations";

            // ✅ Tạo JSON request body theo đúng format backend yêu cầu
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vehicleId", Long.parseLong(data.get("vehicleId").toString()));
            requestBody.put("userId", Long.parseLong(data.get("userId").toString()));
            requestBody.put("startDatetime", data.get("startDate").toString());
            requestBody.put("endDatetime", data.get("endDate").toString());
            
            // Gửi "purpose" như backend yêu cầu
            String purposeValue = data.get("note") != null ? data.get("note").toString() : "";
            if (!purposeValue.trim().isEmpty()) {
                requestBody.put("purpose", purposeValue);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return res.getBody();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Xử lý lỗi HTTP cụ thể
            String errorMessage = "Không thể đặt lịch";
            String errorType = "general"; // general, overlap, validation, server
            
            if (e.getStatusCode().value() == 415) {
                errorMessage = "Lỗi định dạng dữ liệu. Vui lòng thử lại.";
            } else if (e.getStatusCode().value() == 400) {
                // Kiểm tra xem có phải lỗi overlap không
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && (responseBody.contains("overlap") || responseBody.contains("overlaps") || responseBody.contains("trùng"))) {
                    errorMessage = "Thời gian đặt lịch bị trùng với lịch đã có. Vui lòng chọn thời gian khác.";
                    errorType = "overlap";
                } else {
                    errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
                    errorType = "validation";
                }
            } else if (e.getStatusCode().value() == 500) {
                String responseBody = e.getResponseBodyAsString();
                System.out.println("🔍 Response body (500): " + responseBody);
                
                // Kiểm tra lỗi overlap trong response body với thông tin chi tiết
                if (responseBody != null && (responseBody.contains("overlap") || responseBody.contains("overlaps") || 
                    responseBody.contains("trùng") || responseBody.contains("Time range overlaps") ||
                    responseBody.contains("IllegalStateException"))) {
                    errorType = "overlap";
                    // Parse thông tin chi tiết từ error message
                    // Có thể trong JSON format hoặc plain text
                    String overlapInfo = null;
                    
                    // Thử parse JSON trước
                    try {
                        if (responseBody.trim().startsWith("{")) {
                            // Parse JSON
                            ObjectMapper mapper = new ObjectMapper();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> jsonMap = mapper.readValue(responseBody, Map.class);
                            String message = (String) jsonMap.get("message");
                            System.out.println("📝 Parsed message from JSON: " + message);
                            if (message != null && message.contains("OVERLAP:")) {
                                overlapInfo = message.substring(message.indexOf("OVERLAP:") + 8);
                                System.out.println("✅ Found OVERLAP info: " + overlapInfo);
                            } else if (message != null) {
                                overlapInfo = message;
                            }
                        }
                    } catch (Exception jsonEx) {
                        System.out.println("⚠️ JSON parse failed, trying plain text: " + jsonEx.getMessage());
                        // Không phải JSON, parse plain text
                        if (responseBody.contains("OVERLAP:")) {
                            overlapInfo = responseBody.substring(responseBody.indexOf("OVERLAP:") + 8);
                            // Có thể có thêm text sau, lấy đến dấu xuống dòng hoặc ký tự đặc biệt
                            int endIndex = overlapInfo.length();
                            if (overlapInfo.contains("\n")) {
                                endIndex = overlapInfo.indexOf("\n");
                            }
                            if (overlapInfo.contains("\"")) {
                                int quoteIndex = overlapInfo.indexOf("\"");
                                if (quoteIndex < endIndex) endIndex = quoteIndex;
                            }
                            overlapInfo = overlapInfo.substring(0, endIndex).trim();
                        }
                    }
                    
                    if (overlapInfo != null && !overlapInfo.isEmpty()) {
                        errorMessage = "overlap:" + overlapInfo;
                    } else {
                        errorMessage = "overlap:Thời gian đặt lịch bị trùng với lịch đã có. Vui lòng chọn thời gian khác.";
                    }
                } else {
                    // Tất cả lỗi 500 khác cũng hiển thị modal thất bại
                    errorMessage = "server:Không thể đặt lịch. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.";
                    errorType = "server";
                }
            }
            
            // Lưu errorType vào exception message để controller có thể đọc
            throw new RuntimeException(errorType + ":" + errorMessage, e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // Xử lý lỗi 500 từ server
            String errorMessage = "Không thể đặt lịch. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.";
            String errorType = "server";
            
            String responseBody = e.getResponseBodyAsString();
            System.out.println("🔍 HttpServerErrorException response body: " + responseBody);
            
            if (responseBody != null && (responseBody.contains("overlap") || responseBody.contains("overlaps") || 
                responseBody.contains("trùng") || responseBody.contains("Time range overlaps") ||
                responseBody.contains("IllegalStateException"))) {
                errorType = "overlap";
                // Parse thông tin chi tiết từ error message
                String overlapInfo = null;
                
                // Thử parse JSON trước
                try {
                    if (responseBody.trim().startsWith("{")) {
                        ObjectMapper mapper = new ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> jsonMap = mapper.readValue(responseBody, Map.class);
                        String message = (String) jsonMap.get("message");
                        System.out.println("📝 Parsed message from JSON (HttpServerErrorException): " + message);
                        if (message != null && message.contains("OVERLAP:")) {
                            overlapInfo = message.substring(message.indexOf("OVERLAP:") + 8);
                            System.out.println("✅ Found OVERLAP info: " + overlapInfo);
                        } else if (message != null) {
                            overlapInfo = message;
                        }
                    }
                } catch (Exception jsonEx) {
                    System.out.println("⚠️ JSON parse failed (HttpServerErrorException), trying plain text: " + jsonEx.getMessage());
                    // Không phải JSON, parse plain text
                    if (responseBody.contains("OVERLAP:")) {
                        overlapInfo = responseBody.substring(responseBody.indexOf("OVERLAP:") + 8);
                        int endIndex = overlapInfo.length();
                        if (overlapInfo.contains("\n")) {
                            endIndex = overlapInfo.indexOf("\n");
                        }
                        if (overlapInfo.contains("\"")) {
                            int quoteIndex = overlapInfo.indexOf("\"");
                            if (quoteIndex < endIndex) endIndex = quoteIndex;
                        }
                        overlapInfo = overlapInfo.substring(0, endIndex).trim();
                    }
                }
                
                if (overlapInfo != null && !overlapInfo.isEmpty()) {
                    errorMessage = "overlap:" + overlapInfo;
                } else {
                    errorMessage = "overlap:Thời gian đặt lịch bị trùng với lịch đã có. Vui lòng chọn thời gian khác.";
                }
            }
            
            throw new RuntimeException(errorType + ":" + errorMessage, e);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            String errorType = "general";
            
            // Kiểm tra lỗi overlap
            if (errorMsg != null && (errorMsg.contains("overlap") || errorMsg.contains("overlaps") || 
                errorMsg.contains("trùng") || errorMsg.contains("Time range overlaps"))) {
                errorMsg = "Thời gian đặt lịch bị trùng với lịch đã có. Vui lòng chọn thời gian khác.";
                errorType = "overlap";
            } else if (errorMsg != null && errorMsg.contains("415")) {
                errorMsg = "Lỗi định dạng dữ liệu. Vui lòng thử lại.";
            } else if (errorMsg != null && (errorMsg.contains("Connection refused") || errorMsg.contains("connect"))) {
                errorMsg = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
            } else if (errorMsg != null && errorMsg.contains("timeout")) {
                errorMsg = "Yêu cầu quá thời gian chờ. Vui lòng thử lại.";
            } else if (errorMsg != null && errorMsg.contains("500")) {
                errorMsg = "Không thể đặt lịch. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.";
                errorType = "server";
            } else if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
            }
            
            throw new RuntimeException(errorType + ":" + errorMsg, e);
        }
    }
}

