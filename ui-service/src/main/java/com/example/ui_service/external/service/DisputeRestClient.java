package com.example.ui_service.external.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DisputeRestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DisputeRestClient(RestTemplate restTemplate,
                             @Value("${external.disputes.base-url:http://localhost:8084/api/disputes}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<Map<String, Object>> getAllDisputes() {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(baseUrl, Map[].class);
            Map[] disputes = response.getBody();
            if (disputes == null || disputes.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(disputes);
        } catch (Exception e) {
            System.err.println("Error fetching disputes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getDisputesByStatus(String status) {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                baseUrl + "?status=" + status, Map[].class);
            Map[] disputes = response.getBody();
            if (disputes == null || disputes.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(disputes);
        } catch (Exception e) {
            System.err.println("Error fetching disputes by status: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getDisputesByCreator(Integer createdBy) {
        if (createdBy == null) {
            return Collections.emptyList();
        }
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                baseUrl + "?createdBy=" + createdBy, Map[].class);
            Map[] disputes = response.getBody();
            if (disputes == null || disputes.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(disputes);
        } catch (Exception e) {
            System.err.println("Error fetching disputes by creator: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getUnassignedDisputes() {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                baseUrl + "/unassigned", Map[].class);
            Map[] disputes = response.getBody();
            if (disputes == null || disputes.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(disputes);
        } catch (Exception e) {
            System.err.println("Error fetching unassigned disputes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getPendingDisputes() {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                baseUrl + "/pending", Map[].class);
            Map[] disputes = response.getBody();
            if (disputes == null || disputes.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(disputes);
        } catch (Exception e) {
            System.err.println("Error fetching pending disputes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getDisputeById(Integer disputeId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/" + disputeId, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching dispute: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> createDispute(Map<String, Object> disputeData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(disputeData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error creating dispute: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> updateDispute(Integer disputeId, Map<String, Object> disputeData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(disputeData, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/" + disputeId, HttpMethod.PUT, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error updating dispute: " + e.getMessage());
            return null;
        }
    }

    public boolean assignDispute(Integer disputeId, Integer staffId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Integer> requestBody = Map.of("staffId", staffId);
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/" + disputeId + "/assign", HttpMethod.PUT, request, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Error assigning dispute: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDispute(Integer disputeId) {
        try {
            restTemplate.delete(baseUrl + "/" + disputeId);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting dispute: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getComments(Integer disputeId, Boolean includeInternal) {
        try {
            String url = baseUrl + "/" + disputeId + "/comments";
            if (includeInternal != null && includeInternal) {
                url += "?includeInternal=true";
            }
            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);
            Map[] comments = response.getBody();
            if (comments == null || comments.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(comments);
        } catch (Exception e) {
            System.err.println("Error fetching comments: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> addComment(Integer disputeId, Map<String, Object> commentData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(commentData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/" + disputeId + "/comments", request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error adding comment: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getResolution(Integer disputeId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/" + disputeId + "/resolution", Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error fetching resolution: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> createResolution(Integer disputeId, Map<String, Object> resolutionData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(resolutionData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/" + disputeId + "/resolution", request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error creating resolution: " + e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getHistory(Integer disputeId) {
        try {
            ResponseEntity<Map[]> response = restTemplate.getForEntity(
                baseUrl + "/" + disputeId + "/history", Map[].class);
            Map[] history = response.getBody();
            if (history == null || history.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.asList(history);
        } catch (Exception e) {
            System.err.println("Error fetching history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getStatistics() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/statistics", Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching statistics: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}

