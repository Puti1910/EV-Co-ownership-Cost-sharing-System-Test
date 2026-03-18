package com.example.ui_service.client;

import com.example.ui_service.dto.GroupDto;
import com.example.ui_service.dto.GroupMemberDto;
import com.example.ui_service.dto.VoteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroupManagementClient {

    @Value("${microservices.group-management.url:http://localhost:8084}")
    private String groupManagementUrl;

    @Autowired
    private RestTemplate restTemplate;

    public List<GroupDto> getAllGroups() {
        try {
            GroupDto[] groups = restTemplate.getForObject(groupManagementUrl + "/api/groups", GroupDto[].class);
            return groups != null ? Arrays.asList(groups) : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching groups: " + e.getMessage());
            return List.of();
        }
    }

    public GroupDto createGroup(GroupDto groupDto) {
        try {
            return restTemplate.postForObject(groupManagementUrl + "/api/groups", groupDto, GroupDto.class);
        } catch (Exception e) {
            System.err.println("Error creating group: " + e.getMessage());
            return null;
        }
    }

    public List<GroupMemberDto> getGroupMembers(Integer groupId) {
        try {
            GroupMemberDto[] members = restTemplate.getForObject(groupManagementUrl + "/api/groups/" + groupId + "/members", GroupMemberDto[].class);
            return members != null ? Arrays.asList(members) : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching group members: " + e.getMessage());
            return List.of();
        }
    }

    public GroupMemberDto addGroupMember(Integer groupId, GroupMemberDto memberDto) {
        try {
            return restTemplate.postForObject(groupManagementUrl + "/api/groups/" + groupId + "/members", memberDto, GroupMemberDto.class);
        } catch (Exception e) {
            System.err.println("Error adding group member: " + e.getMessage());
            return null;
        }
    }

    public List<VoteDto> getGroupVotes(Integer groupId) {
        try {
            VoteDto[] votes = restTemplate.getForObject(groupManagementUrl + "/api/groups/" + groupId + "/votes", VoteDto[].class);
            return votes != null ? Arrays.asList(votes) : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching group votes: " + e.getMessage());
            return List.of();
        }
    }

    public VoteDto createVote(Integer groupId, VoteDto voteDto) {
        try {
            return restTemplate.postForObject(groupManagementUrl + "/api/groups/" + groupId + "/votes", voteDto, VoteDto.class);
        } catch (Exception e) {
            System.err.println("Error creating vote: " + e.getMessage());
            return null;
        }
    }

    // Generic Map-based methods for REST API
    public List<Map<String, Object>> getAllGroupsAsMap() {
        try {
            System.out.println("=== GROUP MANAGEMENT CLIENT: Fetching groups from " + groupManagementUrl + "/api/groups ===");
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> groups = response.getBody();
                System.out.println("Successfully fetched " + (groups != null ? groups.size() : 0) + " groups");
                return groups != null ? groups : List.of();
            } else {
                System.err.println("Failed to fetch groups. Status: " + response.getStatusCode());
                return List.of();
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("ERROR: Cannot connect to Group Management Service at " + groupManagementUrl);
            System.err.println("Please ensure the service is running on port 8082");
            e.printStackTrace();
            return List.of();
        } catch (Exception e) {
            System.err.println("Error fetching groups as map: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Map<String, Object>> getGroupsByUserIdAsMap(Integer userId) {
        try {
            System.out.println("=== GROUP MANAGEMENT CLIENT: Fetching groups for userId=" + userId + " ===");
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> groups = response.getBody();
                System.out.println("Successfully fetched " + (groups != null ? groups.size() : 0) + " groups for userId=" + userId);
                return groups != null ? groups : List.of();
            } else {
                System.err.println("Failed to fetch groups for userId=" + userId + ". Status: " + response.getStatusCode());
                return List.of();
            }
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            System.err.println("HTTP error fetching groups for userId=" + userId + ": " + e.getStatusCode());
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching groups for userId=" + userId + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public Map<String, Object> getGroupByIdAsMap(Integer groupId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(groupManagementUrl + "/api/groups/" + groupId, Map.class);
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error fetching group by ID: " + e.getMessage());
            return null;
        }
    }
    
    public void deleteGroup(Integer groupId) {
        try {
            restTemplate.delete(groupManagementUrl + "/api/groups/" + groupId);
        } catch (Exception e) {
            System.err.println("Error deleting group: " + e.getMessage());
        }
    }

    public Map<String, Object> createGroupAsMap(Map<String, Object> groupData) {
        try {
            Map<?, ?> response = restTemplate.postForObject(groupManagementUrl + "/api/groups", groupData, Map.class);
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error creating group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> updateGroupAsMap(Integer groupId, Map<String, Object> groupData) {
        try {
            System.out.println("=== GroupManagementClient: Updating group " + groupId + " with data: " + groupData + " ===");
            ResponseEntity<Map> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/" + groupId,
                HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(groupData),
                Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> updatedGroup = castToMap(response.getBody());
                System.out.println("=== GroupManagementClient: Group updated successfully: " + updatedGroup + " ===");
                return updatedGroup;
            } else {
                System.err.println("Error updating group: Response status is not 2xx or body is null");
                return null;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Error updating group (HTTP Client Error): " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getStatusCode().value() == 404) {
                System.err.println("Group with ID " + groupId + " not found");
            }
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Error updating group: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, Object>> getGroupMembersAsMap(Integer groupId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/" + groupId + "/members",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching group members: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> addGroupMemberAsMap(Integer groupId, Map<String, Object> memberData) {
        try {
            System.out.println("🔵 [GroupManagementClient] Adding member to group " + groupId + ": " + memberData);
            Map<?, ?> response = restTemplate.postForObject(groupManagementUrl + "/api/groups/" + groupId + "/members", memberData, Map.class);
            Map<String, Object> result = castToMap(response);
            System.out.println("✅ [GroupManagementClient] Member added successfully: " + result);
            return result;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("❌ [GroupManagementClient] HTTP Error adding group member: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new RuntimeException("Failed to add member: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("❌ [GroupManagementClient] Error adding group member: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add member: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> updateGroupMemberAsMap(Integer groupId, Integer memberId, Map<String, Object> memberData) {
        try {
            restTemplate.put(groupManagementUrl + "/api/groups/" + groupId + "/members/" + memberId, memberData);
            // Return the updated member by fetching all members and finding the one
            List<Map<String, Object>> members = getGroupMembersAsMap(groupId);
            return members.stream()
                .filter(m -> m.get("memberId").equals(memberId))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            System.err.println("Error updating group member: " + e.getMessage());
            return null;
        }
    }

    public void deleteGroupMember(Integer groupId, Integer memberId, Integer currentUserId) {
        String url = groupManagementUrl + "/api/groups/" + groupId + "/members/" + memberId
                + "?currentUserId=" + currentUserId;
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            System.err.println("HTTP error deleting group member: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error deleting group member: " + e.getMessage());
            throw new RuntimeException("Failed to delete group member", e);
        }
    }

    public List<Map<String, Object>> getGroupVotesAsMap(Integer groupId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/" + groupId + "/votes",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching group votes: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> createVoteAsMap(Integer groupId, Map<String, Object> voteData) {
        try {
            Map<?, ?> response = restTemplate.postForObject(
                groupManagementUrl + "/api/groups/" + groupId + "/votes",
                voteData,
                Map.class
            );
            return castToMap(response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error creating vote: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                return Map.of("error", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to create vote: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error creating vote: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to create vote: " + e.getMessage());
        }
    }

    public Map<String, Object> submitVoteAsMap(Integer voteId, Map<String, Object> voteData) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity = 
                new org.springframework.http.HttpEntity<>(voteData, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/votes/" + voteId + "/results",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error submitting vote: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                // Try to parse error response
                return Map.of("error", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to submit vote: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error submitting vote: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to submit vote: " + e.getMessage());
        }
    }

    public Map<String, Object> getMyMembershipInfo(Integer groupId, Integer userId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                groupManagementUrl + "/api/groups/" + groupId + "/members/me/" + userId,
                Map.class
            );
            return castToMap(response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error getting membership info: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                return Map.of("error", "Not found", "message", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to get membership info: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error getting membership info: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to get membership info: " + e.getMessage());
        }
    }

    public Map<String, Object> viewGroupMembers(Integer groupId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                groupManagementUrl + "/api/groups/" + groupId + "/members/view",
                Map.class
            );
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error viewing group members: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to view members", "message", e.getMessage());
        }
    }

    public Map<String, Object> createLeaveRequest(Integer groupId, Map<String, Object> requestData) {
        try {
            Map<?, ?> response = restTemplate.postForObject(
                groupManagementUrl + "/api/groups/" + groupId + "/leave-request",
                requestData,
                Map.class
            );
            return castToMap(response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error creating leave request: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                return Map.of("error", "Failed to create leave request", "message", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to create leave request: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error creating leave request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to create leave request: " + e.getMessage());
        }
    }

    public Map<String, Object> getLeaveRequests(Integer groupId, Integer currentUserId) {
        try {
            String url = groupManagementUrl + "/api/groups/" + groupId + "/leave-requests";
            if (currentUserId != null) {
                url += "?currentUserId=" + currentUserId;
            }
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody() != null ? response.getBody() : Map.of("requests", List.of(), "total", 0, "pending", 0);
        } catch (Exception e) {
            System.err.println("Error getting leave requests: " + e.getMessage());
            e.printStackTrace();
            return Map.of("requests", List.of(), "total", 0, "pending", 0);
        }
    }

    // ============================
    // Contract endpoints
    // ============================

    public List<Map<String, Object>> getAllContractsAsMap() {
        return getAllContractsAsMap(null);
    }

    public List<Map<String, Object>> getAllContractsAsMap(Integer userId) {
        try {
            String url = groupManagementUrl + "/api/groups/contracts";
            if (userId != null) {
                url += "?userId=" + userId;
            }
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching contracts: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Map<String, Object>> getContractsByGroupIdAsMap(Integer groupId) {
        return getContractsByGroupIdAsMap(groupId, null);
    }

    public List<Map<String, Object>> getContractsByGroupIdAsMap(Integer groupId, Integer userId) {
        try {
            String url = groupManagementUrl + "/api/groups/" + groupId + "/contracts";
            if (userId != null) {
                url += "?userId=" + userId;
            }
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching contracts for group " + groupId + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public Map<String, Object> getContractByIdAsMap(Integer contractId) {
        return getContractByIdAsMap(contractId, null);
    }

    public Map<String, Object> getContractByIdAsMap(Integer contractId, Integer userId) {
        try {
            String url = groupManagementUrl + "/api/groups/contracts/" + contractId;
            if (userId != null) {
                url += "?userId=" + userId;
            }
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error fetching contract: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> createContractAsMap(Integer groupId, Map<String, Object> contractData) {
        try {
            Map<?, ?> response = restTemplate.postForObject(
                    groupManagementUrl + "/api/groups/" + groupId + "/contracts",
                    contractData,
                    Map.class
            );
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error creating contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> signContractAsMap(Integer contractId, Map<String, Object> payload) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    groupManagementUrl + "/api/groups/contracts/" + contractId + "/sign",
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP error signing contract: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Error signing contract: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to sign contract", "message", e.getMessage());
        }
    }

    public List<Map<String, Object>> getContractSignatures(Integer contractId) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    groupManagementUrl + "/api/groups/contracts/" + contractId + "/signatures",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching contract signatures: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> approveLeaveRequest(Integer groupId, Integer requestId, Map<String, Object> requestData) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity = 
                new org.springframework.http.HttpEntity<>(requestData != null ? requestData : Map.of(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/" + groupId + "/leave-requests/" + requestId + "/approve",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error approving leave request: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                return Map.of("error", "Failed to approve leave request", "message", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to approve leave request: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error approving leave request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to approve leave request: " + e.getMessage());
        }
    }

    public Map<String, Object> rejectLeaveRequest(Integer groupId, Integer requestId, Map<String, Object> requestData) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity = 
                new org.springframework.http.HttpEntity<>(requestData != null ? requestData : Map.of(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                groupManagementUrl + "/api/groups/" + groupId + "/leave-requests/" + requestId + "/reject",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error rejecting leave request: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            try {
                return Map.of("error", "Failed to reject leave request", "message", e.getResponseBodyAsString());
            } catch (Exception ex) {
                return Map.of("error", "Failed to reject leave request: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error rejecting leave request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to reject leave request: " + e.getMessage());
        }
    }

    public Map<String, Object> getMyLeaveRequestStatus(Integer groupId, Integer userId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                groupManagementUrl + "/api/groups/" + groupId + "/leave-requests/me/" + userId,
                Map.class
            );
            return castToMap(response);
        } catch (Exception e) {
            System.err.println("Error getting leave request status: " + e.getMessage());
            e.printStackTrace();
            return Map.of("error", "Failed to get leave request status", "message", e.getMessage());
        }
    }

    private Map<String, Object> castToMap(Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return source == null ? null : Map.of();
        }
        Map<String, Object> target = new HashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }
}