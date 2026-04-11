package com.example.ui_service.external.service;

import com.example.ui_service.external.model.LegalContractDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class LegalContractRestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public LegalContractRestClient(RestTemplate restTemplate,
                                   @Value("${external.legal-contracts.base-url:http://localhost:8084/api/legalcontracts}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<LegalContractDTO> getAllContracts() {
        try {
            System.out.println("🔵 [LegalContractRestClient] Calling API: " + baseUrl + "/all");
            ResponseEntity<Map[]> response = restTemplate.getForEntity(baseUrl + "/all", Map[].class);
            System.out.println("✅ [LegalContractRestClient] Response status: " + response.getStatusCode());
            Map[] contracts = response.getBody();
            if (contracts == null || contracts.length == 0) {
                System.out.println("⚠️ [LegalContractRestClient] No contracts found or empty response");
                return Collections.emptyList();
            }
            System.out.println("✅ [LegalContractRestClient] Found " + contracts.length + " contracts");
            List<LegalContractDTO> result = new ArrayList<>();
            for (Map<String, Object> contract : contracts) {
                result.add(parseContract(contract));
            }
            return result;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("❌ [LegalContractRestClient] Connection error - Service may be down: " + e.getMessage());
            System.err.println("   Base URL: " + baseUrl);
            e.printStackTrace();
            return Collections.emptyList();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ [LegalContractRestClient] HTTP error: " + e.getStatusCode() + " - " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("❌ [LegalContractRestClient] Error fetching contracts: " + e.getMessage());
            System.err.println("   Base URL: " + baseUrl);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public LegalContractDTO getContractById(Integer contractId) {
        try {
            System.out.println("🔵 [LegalContractRestClient] Getting contract by ID: " + contractId);
            System.out.println("   URL: " + baseUrl + "/" + contractId);
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/" + contractId, Map.class);
            System.out.println("✅ [LegalContractRestClient] Response status: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                System.out.println("✅ [LegalContractRestClient] Response body keys: " + body.keySet());
                // Handle ApiResponse wrapper
                if (body.containsKey("data")) {
                    return parseContract((Map<String, Object>) body.get("data"));
                }
                return parseContract(body);
            }
            System.err.println("⚠️ [LegalContractRestClient] Response không thành công hoặc body null");
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ [LegalContractRestClient] HTTP error fetching contract: " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getResponseBodyAsString() != null) {
                System.err.println("   Response body: " + e.getResponseBodyAsString());
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ [LegalContractRestClient] Error fetching contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<LegalContractDTO> getContractsByGroupId(Integer groupId) {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(baseUrl + "/group/" + groupId, Map[].class);
            Map[] contracts = response.getBody();
            if (contracts == null || contracts.length == 0) {
                return Collections.emptyList();
            }
            List<LegalContractDTO> result = new ArrayList<>();
            for (Map<String, Object> contract : contracts) {
                result.add(parseContract(contract));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<LegalContractDTO> getContractsByStatus(String status) {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(baseUrl + "/status/" + status, Map[].class);
            Map[] contracts = response.getBody();
            if (contracts == null || contracts.length == 0) {
                return Collections.emptyList();
            }
            List<LegalContractDTO> result = new ArrayList<>();
            for (Map<String, Object> contract : contracts) {
                result.add(parseContract(contract));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public LegalContractDTO createContract(Map<String, Object> contractData) {
        try {
            System.out.println("🔵 [LegalContractRestClient] Creating contract with data: " + contractData);
            System.out.println("   URL: " + baseUrl + "/create");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(contractData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/create", request, Map.class);
            System.out.println("✅ [LegalContractRestClient] Response status: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                System.out.println("✅ [LegalContractRestClient] Response body keys: " + body.keySet());
                if (body.containsKey("data")) {
                    return parseContract((Map<String, Object>) body.get("data"));
                }
                return parseContract(body);
            }
            System.err.println("⚠️ [LegalContractRestClient] Response không thành công hoặc body null");
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ [LegalContractRestClient] HTTP error creating contract: " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getResponseBodyAsString() != null) {
                System.err.println("   Response body: " + e.getResponseBodyAsString());
            }
            e.printStackTrace();
            return null;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("❌ [LegalContractRestClient] Connection error creating contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("❌ [LegalContractRestClient] Error creating contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public LegalContractDTO updateContract(Integer contractId, Map<String, Object> contractData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(contractData, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/update/" + contractId, HttpMethod.PUT, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    return parseContract((Map<String, Object>) body.get("data"));
                }
                return parseContract(body);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error updating contract: " + e.getMessage());
            return null;
        }
    }

    public LegalContractDTO signContract(Integer contractId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/sign/" + contractId, HttpMethod.PUT, null, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    return parseContract((Map<String, Object>) body.get("data"));
                }
                return parseContract(body);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error signing contract: " + e.getMessage());
            return null;
        }
    }

    public LegalContractDTO archiveContract(Integer contractId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/archive/" + contractId, HttpMethod.PUT, null, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (body.containsKey("data")) {
                    return parseContract((Map<String, Object>) body.get("data"));
                }
                return parseContract(body);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error archiving contract: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteContract(Integer contractId) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔵 [LegalContractRestClient] ===== BẮT ĐẦU XÓA HỢP ĐỒNG =====");
        System.out.println("   Contract ID: " + contractId);
        System.out.println("   Base URL: " + baseUrl);
        System.out.println("   Full URL: " + baseUrl + "/" + contractId);
        System.out.println("   Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("=".repeat(80) + "\n");
        
        try {
            System.out.println("🔵 [LegalContractRestClient] Đang gửi DELETE request...");
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/" + contractId, HttpMethod.DELETE, null, Map.class);
            
            System.out.println("✅ [LegalContractRestClient] Đã nhận được response");
            System.out.println("   Status Code: " + response.getStatusCode());
            System.out.println("   Status Value: " + response.getStatusCode().value());
            System.out.println("   Is 2xx: " + response.getStatusCode().is2xxSuccessful());
            System.out.println("   Response Body: " + response.getBody());
            System.out.println("   Response Body Type: " + (response.getBody() != null ? response.getBody().getClass().getName() : "null"));
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ [LegalContractRestClient] HTTP Status là 2xx (thành công)");
                
                if (response.getBody() == null) {
                    System.err.println("⚠️ [LegalContractRestClient] Response body là null, nhưng status là 2xx");
                    System.out.println("✅ [LegalContractRestClient] Coi như xóa thành công (status 2xx)");
                    System.out.println("=".repeat(80));
                    System.out.println("✅ [LegalContractRestClient] ===== XÓA THÀNH CÔNG (status 2xx, body null) =====");
                    System.out.println("=".repeat(80) + "\n");
                    return true;
                }
                
                Map<String, Object> body = response.getBody();
                System.out.println("🔵 [LegalContractRestClient] Phân tích response body...");
                System.out.println("   Body keys: " + body.keySet());
                
                // Kiểm tra ApiResponse format - success có thể là boolean hoặc string
                if (body.containsKey("success")) {
                    Object successObj = body.get("success");
                    System.out.println("   Found 'success' field: " + successObj + " (type: " + (successObj != null ? successObj.getClass().getName() : "null") + ")");
                    
                    boolean success = false;
                    
                    if (successObj instanceof Boolean) {
                        success = (Boolean) successObj;
                        System.out.println("   Parsed as Boolean: " + success);
                    } else if (successObj instanceof String) {
                        success = Boolean.parseBoolean((String) successObj);
                        System.out.println("   Parsed as String: " + success);
                    } else if (successObj != null) {
                        success = Boolean.parseBoolean(successObj.toString());
                        System.out.println("   Parsed from toString: " + success);
                    }
                    
                    if (success) {
                        System.out.println("=".repeat(80));
                        System.out.println("✅ [LegalContractRestClient] ===== XÓA THÀNH CÔNG (success=true) =====");
                        System.out.println("=".repeat(80) + "\n");
                        return true;
                    } else {
                        String errorMsg = (String) body.getOrDefault("error", 
                                body.getOrDefault("message", "Unknown error"));
                        System.err.println("=".repeat(80));
                        System.err.println("❌ [LegalContractRestClient] ===== XÓA THẤT BẠI (success=false) =====");
                        System.err.println("   Error: " + errorMsg);
                        System.err.println("=".repeat(80) + "\n");
                        return false;
                    }
                } else {
                    System.out.println("   Không tìm thấy 'success' field");
                }
                
                // Nếu không có success field, kiểm tra data field
                if (body.containsKey("data")) {
                    Object data = body.get("data");
                    System.out.println("   Found 'data' field: " + data);
                    if (data instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        System.out.println("   Data is Map, keys: " + dataMap.keySet());
                        if (dataMap.containsKey("deleted")) {
                            Object deletedObj = dataMap.get("deleted");
                            System.out.println("   Found 'deleted' in data: " + deletedObj);
                            if (Boolean.TRUE.equals(deletedObj)) {
                                System.out.println("=".repeat(80));
                                System.out.println("✅ [LegalContractRestClient] ===== XÓA THÀNH CÔNG (data.deleted=true) =====");
                                System.out.println("=".repeat(80) + "\n");
                                return true;
                            }
                        }
                    }
                }
                
                // Nếu status code là 2xx và không có lỗi rõ ràng, coi như thành công
                System.out.println("=".repeat(80));
                System.out.println("✅ [LegalContractRestClient] ===== XÓA THÀNH CÔNG (status 2xx, không có lỗi) =====");
                System.out.println("=".repeat(80) + "\n");
                return true;
            } else {
                System.err.println("=".repeat(80));
                System.err.println("❌ [LegalContractRestClient] ===== XÓA THẤT BẠI (HTTP Status không phải 2xx) =====");
                System.err.println("   Status Code: " + response.getStatusCode());
                System.err.println("=".repeat(80) + "\n");
                return false;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("❌ [LegalContractRestClient] ===== HTTP CLIENT ERROR =====");
            System.err.println("   Status Code: " + e.getStatusCode());
            System.err.println("   Status Value: " + e.getStatusCode().value());
            System.err.println("   Message: " + e.getMessage());
            if (e.getResponseBodyAsString() != null) {
                System.err.println("   Response Body: " + e.getResponseBodyAsString());
            }
            System.err.println("   Stack Trace:");
            e.printStackTrace();
            System.err.println("=".repeat(80) + "\n");
            return false;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("❌ [LegalContractRestClient] ===== CONNECTION ERROR =====");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            System.err.println("   Stack Trace:");
            e.printStackTrace();
            System.err.println("=".repeat(80) + "\n");
            return false;
        } catch (Exception e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("❌ [LegalContractRestClient] ===== UNEXPECTED ERROR =====");
            System.err.println("   Error Type: " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Stack Trace:");
            e.printStackTrace();
            System.err.println("=".repeat(80) + "\n");
            return false;
        }
    }

    public List<Map<String, Object>> getContractHistory(Integer contractId) {
        try {
            System.out.println("🔵 [LegalContractRestClient] Getting contract history for ID: " + contractId);
            System.out.println("   URL: " + baseUrl + "/" + contractId + "/history");
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                    baseUrl + "/" + contractId + "/history", Map[].class);
            System.out.println("✅ [LegalContractRestClient] History response status: " + response.getStatusCode());
            Map[] history = response.getBody();
            if (history == null || history.length == 0) {
                System.out.println("⚠️ [LegalContractRestClient] No history found for contract " + contractId);
                return Collections.emptyList();
            }
            System.out.println("✅ [LegalContractRestClient] Found " + history.length + " history entries");
            return Arrays.asList(history);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ [LegalContractRestClient] HTTP error fetching history: " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getResponseBodyAsString() != null) {
                System.err.println("   Response body: " + e.getResponseBodyAsString());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("❌ [LegalContractRestClient] Error fetching contract history: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private LegalContractDTO parseContract(Map<String, Object> contract) {
        LegalContractDTO dto = new LegalContractDTO();
        
        System.out.println("🔵 [parseContract] Parsing contract: " + contract);
        
        Object contractIdObj = contract.get("contractId");
        if (contractIdObj != null) {
            dto.setContractId(contractIdObj instanceof Integer ? (Integer) contractIdObj 
                    : Integer.parseInt(contractIdObj.toString()));
        }

        Object groupIdObj = contract.get("groupId");
        if (groupIdObj != null) {
            dto.setGroupId(groupIdObj instanceof Integer ? (Integer) groupIdObj 
                    : Integer.parseInt(groupIdObj.toString()));
        }

        Object contractCodeObj = contract.get("contractCode");
        if (contractCodeObj != null) {
            dto.setContractCode(contractCodeObj.toString());
        }

        Object contractStatusObj = contract.get("contractStatus");
        if (contractStatusObj != null) {
            dto.setContractStatus(contractStatusObj.toString());
        }

        Object creationDateObj = contract.get("creationDate");
        if (creationDateObj != null) {
            try {
                if (creationDateObj instanceof String) {
                    String dateStr = creationDateObj.toString();
                    // Handle different date formats
                    if (dateStr.contains("T")) {
                        dto.setCreationDate(Instant.parse(dateStr));
                    } else {
                        // Try parsing as timestamp
                        dto.setCreationDate(Instant.parse(dateStr + "T00:00:00Z"));
                    }
                } else if (creationDateObj instanceof Number) {
                    dto.setCreationDate(Instant.ofEpochMilli(((Number) creationDateObj).longValue()));
                } else if (creationDateObj instanceof java.time.Instant) {
                    dto.setCreationDate((java.time.Instant) creationDateObj);
                }
            } catch (Exception e) {
                System.err.println("⚠️ [parseContract] Error parsing creationDate: " + creationDateObj + " - " + e.getMessage());
            }
        }

        Object signedDateObj = contract.get("signedDate");
        if (signedDateObj != null) {
            try {
                if (signedDateObj instanceof String) {
                    String dateStr = signedDateObj.toString();
                    // Handle different date formats
                    if (dateStr.contains("T")) {
                        dto.setSignedDate(Instant.parse(dateStr));
                    } else {
                        // Try parsing as timestamp
                        dto.setSignedDate(Instant.parse(dateStr + "T00:00:00Z"));
                    }
                } else if (signedDateObj instanceof Number) {
                    dto.setSignedDate(Instant.ofEpochMilli(((Number) signedDateObj).longValue()));
                } else if (signedDateObj instanceof java.time.Instant) {
                    dto.setSignedDate((java.time.Instant) signedDateObj);
                }
            } catch (Exception e) {
                System.err.println("⚠️ [parseContract] Error parsing signedDate: " + signedDateObj + " - " + e.getMessage());
            }
        }

        System.out.println("✅ [parseContract] Parsed DTO: ID=" + dto.getContractId() + ", Code=" + dto.getContractCode());
        return dto;
    }
}

