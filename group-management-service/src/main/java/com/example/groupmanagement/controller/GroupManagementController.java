package com.example.groupmanagement.controller;

import com.example.groupmanagement.dto.*;
import com.example.groupmanagement.entity.*;
import com.example.groupmanagement.exception.ValidationException;
import com.example.groupmanagement.repository.*;
import com.example.groupmanagement.service.GroupContractService;
import com.example.groupmanagement.service.GroupManagementService;
import com.example.groupmanagement.service.UserValidationService;
import com.example.groupmanagement.util.GroupValidationUtil;
import com.example.groupmanagement.util.MemberValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupManagementController {

    private static final Logger logger = LoggerFactory.getLogger(GroupManagementController.class);

    @Autowired
    private GroupManagementService groupManagementService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private VotingRepository votingRepository;

    @Autowired
    private VotingResultRepository votingResultRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private GroupContractRepository groupContractRepository;

    @Autowired
    private ContractSignatureRepository contractSignatureRepository;

    @Autowired
    private GroupContractService groupContractService;

    @Autowired
    private UserValidationService userValidationService;

    @Autowired
    private RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${microservices.user-account.url:http://user-account-service:8081}")
    private String userAccountServiceUrl;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Group Management Service"));
    }

    // ========================================
    // GROUP MANAGEMENT ENDPOINTS
    // ========================================

    @GetMapping
    public List<GroupResponseDto> getAllGroups() {
        List<Group> groups = groupManagementService.getAllGroups();
        return groups.stream()
                .map(g -> {
                    Integer memberCount = groupMemberRepository.countByGroup_GroupId(g.getGroupId());
                    Integer voteCount = votingRepository.countByGroup_GroupId(g.getGroupId());
                    return GroupResponseDto.fromEntity(g, memberCount, voteCount);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponseDto> getGroupById(@PathVariable Long id) {
        return groupManagementService.getGroupById(id)
                .map(g -> {
                    Integer memberCount = groupMemberRepository.countByGroup_GroupId(g.getGroupId());
                    Integer voteCount = votingRepository.countByGroup_GroupId(g.getGroupId());
                    return ResponseEntity.ok(GroupResponseDto.fromEntity(g, memberCount, voteCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@Valid @RequestBody CreateGroupRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        try {
            GroupValidationUtil.validateGroupName(requestDto.getGroupName());

            Group group = new Group();
            group.setGroupName(requestDto.getGroupName());
            group.setAdminId(requestDto.getAdminId());
            group.setStatus(Group.GroupStatus.Active);
            
            Group savedGroup = groupManagementService.createGroup(group);
            
            // If adminId and ownershipPercent are provided, automatically add the creator as Admin member
            if (requestDto.getAdminId() != null && requestDto.getOwnershipPercent() != null) {
                GroupMember adminMember = new GroupMember();
                adminMember.setGroup(savedGroup);
                adminMember.setUserId(requestDto.getAdminId());
                adminMember.setRole(GroupMember.MemberRole.Admin);
                adminMember.setOwnershipPercent(requestDto.getOwnershipPercent());
                adminMember.setJoinedAt(LocalDateTime.now());
                groupMemberRepository.save(adminMember);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroup);
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "code", e.getErrorCode()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(id);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Group group = groupOpt.get();
            if (requestData.containsKey("groupName")) {
                String groupName = (String) requestData.get("groupName");
                GroupValidationUtil.validateGroupName(groupName);
                group.setGroupName(groupName);
            }
            if (requestData.containsKey("adminId")) {
                group.setAdminId(((Number) requestData.get("adminId")).longValue());
            }
            if (requestData.containsKey("status")) {
                group.setStatus(Group.GroupStatus.valueOf((String) requestData.get("status")));
            }

            Group updated = groupRepository.save(group);
            return ResponseEntity.ok(updated);
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "code", e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupManagementService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public List<GroupResponseDto> getGroupsByUserId(@PathVariable Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(m -> {
                    Group g = m.getGroup();
                    Integer memberCount = groupMemberRepository.countByGroup_GroupId(g.getGroupId());
                    Integer voteCount = votingRepository.countByGroup_GroupId(g.getGroupId());
                    GroupResponseDto dto = GroupResponseDto.fromEntity(g, memberCount, voteCount);
                    dto.setMemberId(m.getMemberId());
                    dto.setMemberRole(m.getRole().name());
                    dto.setOwnershipPercent(m.getOwnershipPercent());
                    dto.setHasOwnership(m.getOwnershipPercent() != null && m.getOwnershipPercent() > 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ========================================
    // MEMBER MANAGEMENT ENDPOINTS
    // ========================================

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addGroupMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequestDto requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        try {
            Long userId = requestDto.getUserId();
            Long currentUserId = requestDto.getCurrentUserId();
            Double ownershipPercent = requestDto.getOwnershipPercent();
            String role = requestDto.getRole();

            logger.info("🔵 [GroupManagementController] POST /api/groups/{}/members", groupId);
            logger.info("Request: userId={}, ownershipPercent={}, role={}, currentUserId={}", userId, ownershipPercent, role, currentUserId);

            // Step 1: Check group existence
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Group not found"));
            }
            Group targetGroup = groupOpt.get();

            // Step 2: Check current user authorization (unless it's a self-join via contract)
            boolean isSelfJoin = userId.equals(currentUserId);
            if (!isSelfJoin && !isAdminOfGroup(currentUserId, groupId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only Admin can add other members"));
            }

            // Step 3: Validate user exists (external service call)
            if (!userValidationService.isUserExists(userId)) {
                return ResponseEntity.status(404).body(Map.of("error", "User does not exist in the system"));
            }

            // Step 4: Check if already a member
            Optional<GroupMember> existingMember = groupMemberRepository.findByGroup_GroupIdAndUserId(groupId, userId);
            if (existingMember.isPresent()) {
                return ResponseEntity.status(409).body(Map.of("error", "User is already a member of this group"));
            }

            // Step 5: Validate ownership threshold
            List<GroupMember> members = groupMemberRepository.findByGroup_GroupId(groupId);
            double currentTotal = members.stream().mapToDouble(m -> m.getOwnershipPercent() != null ? m.getOwnershipPercent() : 0.0).sum();
            MemberValidationUtil.validateTotalOwnership(currentTotal, ownershipPercent);

            // Step 6: Contract check (Mandatory for joining)
            GroupContract activeContract = ensureGroupContractExists(targetGroup, currentUserId);
            if (!isSelfJoin) {
                // If admin is adding, check if user has signed or auto-sign if allowed
                boolean hasSigned = contractSignatureRepository.existsByGroupContractAndUserId(activeContract, userId);
                if (!hasSigned) {
                    autoSignContractForMember(activeContract, userId, currentUserId);
                }
            } else {
                // For self-join, check if user has signed the contract
                boolean hasSigned = contractSignatureRepository.existsByGroupContractAndUserId(activeContract, userId);
                if (!hasSigned) {
                    return ResponseEntity.status(403).body(Map.of("error", "You must sign the group contract before joining"));
                }
            }

            // Step 7: Create and save member
            GroupMember member = new GroupMember();
            member.setGroup(targetGroup);
            member.setUserId(userId);
            member.setRole("Admin".equalsIgnoreCase(role) ? GroupMember.MemberRole.Admin : GroupMember.MemberRole.Member);
            member.setOwnershipPercent(ownershipPercent);
            member.setJoinedAt(LocalDateTime.now());

            GroupMember saved = groupMemberRepository.save(member);
            
            // Re-evaluate admin if needed
            updateGroupAdminByOwnership(groupId);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "code", e.getErrorCode()));
        } catch (Exception e) {
            logger.error("Error adding member", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add member", "message", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> updateGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> requestData) {
        try {
            Long currentUserId = requestData.containsKey("currentUserId") ? ((Number) requestData.get("currentUserId")).longValue() : null;
            if (currentUserId == null) {
                return ResponseEntity.status(400).body(Map.of("error", "currentUserId is required"));
            }

            if (!isAdminOfGroup(currentUserId, groupId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only Admin can update members"));
            }

            Optional<GroupMember> memberOpt = groupMemberRepository.findById(memberId);
            if (memberOpt.isEmpty() || !memberOpt.get().getGroup().getGroupId().equals(groupId)) {
                return ResponseEntity.notFound().build();
            }

            GroupMember member = memberOpt.get();
            
            if (requestData.containsKey("role")) {
                String role = (String) requestData.get("role");
                // Don't allow changing own role if it's the last admin
                if (member.getUserId().equals(currentUserId) && "Member".equalsIgnoreCase(role)) {
                    long adminCount = countAdminsInGroup(groupId);
                    if (adminCount <= 1) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Cannot demote the last admin"));
                    }
                }
                member.setRole("Admin".equalsIgnoreCase(role) ? GroupMember.MemberRole.Admin : GroupMember.MemberRole.Member);
            }

            if (requestData.containsKey("ownershipPercent")) {
                Double newOwnership = ((Number) requestData.get("ownershipPercent")).doubleValue();
                MemberValidationUtil.validateOwnershipPercent(newOwnership);
                
                List<GroupMember> otherMembers = groupMemberRepository.findByGroup_GroupId(groupId).stream()
                        .filter(m -> !m.getMemberId().equals(memberId))
                        .collect(Collectors.toList());
                double currentTotal = otherMembers.stream().mapToDouble(m -> m.getOwnershipPercent() != null ? m.getOwnershipPercent() : 0.0).sum();
                MemberValidationUtil.validateTotalOwnership(currentTotal, newOwnership);
                
                member.setOwnershipPercent(newOwnership);
            }

            GroupMember saved = groupMemberRepository.save(member);
            updateGroupAdminByOwnership(groupId);
            return ResponseEntity.ok(saved);

        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "code", e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> deleteGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @RequestParam Long currentUserId) {
        
        if (!isAdminOfGroup(currentUserId, groupId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only Admin can delete members"));
        }

        Optional<GroupMember> memberOpt = groupMemberRepository.findById(memberId);
        if (memberOpt.isEmpty() || !memberOpt.get().getGroup().getGroupId().equals(groupId)) {
            return ResponseEntity.notFound().build();
        }

        GroupMember member = memberOpt.get();
        if (member.getUserId().equals(currentUserId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot delete yourself. Use leave request instead."));
        }

        if (member.getRole() == GroupMember.MemberRole.Admin) {
            long adminCount = countAdminsInGroup(groupId);
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete the last admin"));
            }
        }

        groupMemberRepository.delete(member);
        updateGroupAdminByOwnership(groupId);
        return ResponseEntity.noContent().build();
    }

    // ========================================
    // VOTING ENDPOINTS
    // ========================================

    @GetMapping("/{groupId}/votes")
    public List<Voting> getGroupVotes(@PathVariable Long groupId) {
        return votingRepository.findByGroup_GroupId(groupId);
    }

    @PostMapping("/{groupId}/votes")
    public ResponseEntity<?> createVote(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateVotingRequestDto requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Voting voting = new Voting();
        voting.setGroup(groupOpt.get());
        voting.setTopic(requestDto.getTopic());
        voting.setOptionA(requestDto.getOptionA());
        voting.setOptionB(requestDto.getOptionB());
        voting.setDeadline(requestDto.getDeadline());
        voting.setStatus(Voting.VotingStatus.OPEN);
        voting.setCreatedAt(LocalDateTime.now());

        Voting saved = votingRepository.save(voting);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/votes/{voteId}/results")
    public ResponseEntity<?> submitVote(@PathVariable Long voteId, @RequestBody Map<String, Object> voteData) {
        try {
            Optional<Voting> votingOpt = votingRepository.findById(voteId);
            if (votingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Voting voting = votingOpt.get();
            Long groupId = voting.getGroup().getGroupId();

            Long userId = voteData.containsKey("userId") ? ((Number) voteData.get("userId")).longValue() : null;
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            Optional<GroupMember> memberOpt = groupMemberRepository.findByGroup_GroupIdAndUserId(groupId, userId);
            if (memberOpt.isEmpty()) {
                return ResponseEntity.status(403).body(Map.of("error", "You are not a member of this group"));
            }
            GroupMember member = memberOpt.get();

            List<VotingResult> existingResults = votingResultRepository.findByVoting_VoteId(voteId);
            if (existingResults.stream().anyMatch(r -> r.getGroupMember().getMemberId().equals(member.getMemberId()))) {
                return ResponseEntity.status(409).body(Map.of("error", "You have already voted"));
            }

            String choiceStr = (String) voteData.get("choice");
            VotingResult.VoteChoice choice = VotingResult.VoteChoice.valueOf(choiceStr);

            VotingResult result = new VotingResult();
            result.setVoting(voting);
            result.setGroupMember(member);
            result.setChoice(choice);
            result.setVotedAt(LocalDateTime.now());
            votingResultRepository.save(result);

            // Update voting statistics
            List<VotingResult> allResults = votingResultRepository.findByVoting_VoteId(voteId);
            voting.setTotalVotes(allResults.size());
            
            // Logic to determine final result if needed
            long agreeVotes = allResults.stream().filter(r -> r.getChoice() == VotingResult.VoteChoice.A).count();
            int totalMembers = groupMemberRepository.countByGroup_GroupId(groupId);
            
            if (totalMembers > 0 && (double) agreeVotes / totalMembers > 0.5) {
                voting.setFinalResult("Accepted");
            } else if (totalMembers > 0 && allResults.size() >= totalMembers) {
                voting.setFinalResult("Rejected");
            }
            
            votingRepository.save(voting);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // LEAVE REQUEST ENDPOINTS
    // ========================================

    @PostMapping("/{groupId}/leave-request")
    public ResponseEntity<?> createLeaveRequest(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateLeaveRequestDto requestDto,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        Long userId = requestDto.getUserId();
        Optional<GroupMember> memberOpt = groupMemberRepository.findByGroup_GroupIdAndUserId(groupId, userId);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "You are not a member of this group"));
        }

        Optional<LeaveRequest> pending = leaveRequestRepository.findByGroup_GroupIdAndUserIdAndStatus(groupId, userId, LeaveRequest.LeaveStatus.Pending);
        if (pending.isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "You already have a pending leave request"));
        }

        LeaveRequest request = new LeaveRequest();
        request.setGroup(memberOpt.get().getGroup());
        request.setGroupMember(memberOpt.get());
        request.setUserId(userId);
        request.setReason(requestDto.getReason());
        request.setStatus(LeaveRequest.LeaveStatus.Pending);
        request.setRequestedAt(LocalDateTime.now());

        return ResponseEntity.ok(leaveRequestRepository.save(request));
    }

    @GetMapping("/{groupId}/leave-requests")
    public ResponseEntity<?> getLeaveRequests(@PathVariable Long groupId, @RequestParam Long currentUserId) {
        if (!isAdminOfGroup(currentUserId, groupId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only Admin can view leave requests"));
        }
        return ResponseEntity.ok(leaveRequestRepository.findByGroup_GroupId(groupId));
    }

    @PostMapping("/{groupId}/leave-requests/{requestId}/approve")
    @Transactional
    public ResponseEntity<?> approveLeaveRequest(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApproveLeaveRequestDto requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        Long currentUserId = requestDto.getCurrentUserId();
        if (!isAdminOfGroup(currentUserId, groupId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only Admin can approve leave requests"));
        }

        Optional<LeaveRequest> requestOpt = leaveRequestRepository.findById(requestId);
        if (requestOpt.isEmpty() || !requestOpt.get().getGroup().getGroupId().equals(groupId)) {
            return ResponseEntity.notFound().build();
        }

        LeaveRequest request = requestOpt.get();
        if (request.getStatus() != LeaveRequest.LeaveStatus.Pending) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request already processed"));
        }

        request.setStatus(LeaveRequest.LeaveStatus.Approved);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(currentUserId);
        request.setAdminNote(requestDto.getAdminNote());
        leaveRequestRepository.save(request);

        // Delete the member
        GroupMember member = request.getGroupMember();
        groupMemberRepository.delete(member);
        
        // Re-evaluate admin
        updateGroupAdminByOwnership(groupId);

        return ResponseEntity.ok(Map.of("message", "Leave request approved and member removed"));
    }

    @PostMapping("/{groupId}/leave-requests/{requestId}/reject")
    public ResponseEntity<?> rejectLeaveRequest(
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @Valid @RequestBody ApproveLeaveRequestDto requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        Long currentUserId = requestDto.getCurrentUserId();
        if (!isAdminOfGroup(currentUserId, groupId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only Admin can reject leave requests"));
        }

        Optional<LeaveRequest> requestOpt = leaveRequestRepository.findById(requestId);
        if (requestOpt.isEmpty() || !requestOpt.get().getGroup().getGroupId().equals(groupId)) {
            return ResponseEntity.notFound().build();
        }

        LeaveRequest request = requestOpt.get();
        if (request.getStatus() != LeaveRequest.LeaveStatus.Pending) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request already processed"));
        }

        request.setStatus(LeaveRequest.LeaveStatus.Rejected);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(currentUserId);
        request.setAdminNote(requestDto.getAdminNote());
        
        return ResponseEntity.ok(leaveRequestRepository.save(request));
    }

    // ========================================
    // CONTRACT MANAGEMENT ENDPOINTS
    // ========================================

    @PostMapping("/{groupId}/contracts")
    public ResponseEntity<?> createContract(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupContractDto requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation error", "details", bindingResult.getFieldErrors()));
        }

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GroupContract contract = new GroupContract();
        contract.setGroup(groupOpt.get());
        contract.setContractCode(requestDto.getContractCode());
        contract.setContractContent(requestDto.getContractContent());
        contract.setContractStatus(toContractStatus(requestDto.getContractStatus()));
        contract.setCreatedBy(requestDto.getCreatedBy());
        contract.setCreationDate(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(groupContractRepository.save(contract));
    }

    @PutMapping("/contracts/{contractId}/sign")
    @Transactional
    public ResponseEntity<?> signContract(@PathVariable Long contractId, @RequestBody Map<String, Object> requestData) {
        try {
            Long userId = requestData.containsKey("userId") ? ((Number) requestData.get("userId")).longValue() : null;
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            String method = (String) requestData.getOrDefault("signatureMethod", "electronic");
            String ip = (String) requestData.getOrDefault("ipAddress", "unknown");

            ContractSignature signature = groupContractService.signContractWithAuthorization(contractId, userId, method, ip);
            return ResponseEntity.ok(signature);
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/contracts/{contractId}/signatures")
    public ResponseEntity<?> getContractSignatures(@PathVariable Long contractId) {
        Optional<GroupContract> contractOpt = groupContractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(contractSignatureRepository.findByGroupContract(contractOpt.get()));
    }

    // ========================================
    // HELPERS
    // ========================================

    private boolean isAdminOfGroup(Long userId, Long groupId) {
        return groupMemberRepository.findByGroup_GroupIdAndUserId(groupId, userId)
                .map(m -> m.getRole() == GroupMember.MemberRole.Admin)
                .orElse(false);
    }

    private long countAdminsInGroup(Long groupId) {
        return groupMemberRepository.findByGroup_GroupId(groupId).stream()
                .filter(m -> m.getRole() == GroupMember.MemberRole.Admin)
                .count();
    }

    private void updateGroupAdminByOwnership(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroup_GroupId(groupId);
        if (members.isEmpty()) return;

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) return;

        GroupMember topMember = members.stream()
                .max(Comparator.comparingDouble(m -> m.getOwnershipPercent() != null ? m.getOwnershipPercent() : 0.0))
                .orElse(null);

        if (topMember != null && !topMember.getUserId().equals(group.getAdminId())) {
            group.setAdminId(topMember.getUserId());
            groupRepository.save(group);
            
            // Update roles if necessary
            for (GroupMember m : members) {
                if (m.getUserId().equals(topMember.getUserId())) {
                    m.setRole(GroupMember.MemberRole.Admin);
                } else if (m.getRole() == GroupMember.MemberRole.Admin && members.stream().filter(mem -> mem.getRole() == GroupMember.MemberRole.Admin).count() > 1) {
                    // Only demote if there's more than one admin (optional logic, depends on business rule)
                }
            }
            groupMemberRepository.saveAll(members);
        }
    }

    private GroupContract.ContractStatus toContractStatus(String status) {
        if (status == null) return GroupContract.ContractStatus.PENDING;
        try {
            return GroupContract.ContractStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return GroupContract.ContractStatus.PENDING;
        }
    }

    private GroupContract ensureGroupContractExists(Group group, Long userId) {
        return groupContractRepository.findTopByGroup_GroupIdOrderByCreationDateDesc(group.getGroupId())
                .orElseGet(() -> {
                    GroupContract contract = new GroupContract();
                    contract.setGroup(group);
                    contract.setContractCode("LC-" + group.getGroupName().replaceAll("\\s+", "") + "-" + System.currentTimeMillis());
                    contract.setContractContent("Group ownership contract for " + group.getGroupName());
                    contract.setContractStatus(GroupContract.ContractStatus.PENDING);
                    contract.setCreatedBy(userId);
                    contract.setCreationDate(LocalDateTime.now());
                    return groupContractRepository.save(contract);
                });
    }

    private boolean autoSignContractForMember(GroupContract contract, Long userId, Long adminId) {
        ContractSignature sig = new ContractSignature();
        sig.setGroupContract(contract);
        sig.setUserId(userId);
        sig.setSignatureMethod("Admin-Auto-Sign-" + adminId);
        sig.setIpAddress("system");
        sig.setSignedAt(LocalDateTime.now());
        contractSignatureRepository.save(sig);
        return true;
    }
}