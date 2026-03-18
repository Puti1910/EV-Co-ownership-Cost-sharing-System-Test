package com.example.groupmanagement.controller;

import com.example.groupmanagement.entity.Group;
import com.example.groupmanagement.entity.GroupMember;
import com.example.groupmanagement.entity.Voting;
import com.example.groupmanagement.entity.VotingResult;
import com.example.groupmanagement.repository.GroupRepository;
import com.example.groupmanagement.repository.GroupMemberRepository;
import com.example.groupmanagement.repository.VotingRepository;
import com.example.groupmanagement.repository.VotingResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller: Báo cáo và Phân tích cho Group Management
 * UC7: Quản lý Nhóm Đồng Sở Hữu - Báo cáo
 * UC8: Bỏ phiếu/Quyết định chung - Báo cáo
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private VotingRepository votingRepository;

    @Autowired
    private VotingResultRepository votingResultRepository;

    /**
     * Báo cáo tổng quan nhóm
     * GET /api/reports/groups/{groupId}/overview
     */
    @GetMapping("/groups/{groupId}/overview")
    public ResponseEntity<?> getGroupOverview(@PathVariable Integer groupId) {
        try {
            logger.info("Getting group overview for groupId={}", groupId);
            
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (!groupOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Group group = groupOpt.get();
            List<GroupMember> members = groupMemberRepository.findByGroup_GroupId(groupId);
            List<Voting> votings = votingRepository.findByGroup_GroupId(groupId);
            
            // Thống kê thành viên
            long adminCount = members.stream()
                .filter(m -> m.getRole() == GroupMember.MemberRole.Admin)
                .count();
            
            double totalOwnership = members.stream()
                .mapToDouble(m -> m.getOwnershipPercent() != null ? m.getOwnershipPercent() : 0.0)
                .sum();
            
            // Thống kê bỏ phiếu
            long openVotings = votings.stream()
                .filter(v -> v.getStatus() == Voting.VotingStatus.OPEN)
                .count();
            
            long closedVotings = votings.stream()
                .filter(v -> v.getStatus() == Voting.VotingStatus.CLOSED)
                .count();
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("groupId", groupId);
            overview.put("groupName", group.getGroupName());
            overview.put("status", group.getStatus().toString());
            overview.put("createdAt", group.getCreatedAt());
            overview.put("members", Map.of(
                "total", members.size(),
                "admins", adminCount,
                "regularMembers", members.size() - adminCount,
                "totalOwnership", totalOwnership
            ));
            overview.put("votings", Map.of(
                "total", votings.size(),
                "open", openVotings,
                "closed", closedVotings
            ));
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Error getting group overview: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo bỏ phiếu của nhóm
     * GET /api/reports/groups/{groupId}/votings
     */
    @GetMapping("/groups/{groupId}/votings")
    public ResponseEntity<?> getGroupVotingReport(@PathVariable Integer groupId) {
        try {
            logger.info("Getting voting report for groupId={}", groupId);
            
            List<Voting> votings = votingRepository.findByGroup_GroupId(groupId);
            
            List<Map<String, Object>> votingReports = votings.stream()
                .map(voting -> {
                    List<VotingResult> results = votingResultRepository.findByVoting_VoteId(voting.getVoteId());
                    
                    long voteA = results.stream()
                        .filter(r -> r.getChoice() == VotingResult.VoteChoice.A)
                        .count();
                    
                    long voteB = results.stream()
                        .filter(r -> r.getChoice() == VotingResult.VoteChoice.B)
                        .count();
                    
                    Map<String, Object> report = new HashMap<>();
                    report.put("voteId", voting.getVoteId());
                    report.put("topic", voting.getTopic());
                    report.put("optionA", voting.getOptionA());
                    report.put("optionB", voting.getOptionB());
                    report.put("status", voting.getStatus().toString());
                    report.put("deadline", voting.getDeadline());
                    report.put("createdAt", voting.getCreatedAt());
                    report.put("closedAt", voting.getClosedAt());
                    report.put("totalVotes", results.size());
                    report.put("votesA", voteA);
                    report.put("votesB", voteB);
                    report.put("finalResult", voting.getFinalResult());
                    report.put("isExpired", voting.isExpired());
                    report.put("isOpen", voting.isOpen());
                    
                    return report;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", groupId);
            response.put("totalVotings", votings.size());
            response.put("votings", votingReports);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting voting report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo thành viên của nhóm
     * GET /api/reports/groups/{groupId}/members
     */
    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<?> getGroupMembersReport(@PathVariable Integer groupId) {
        try {
            logger.info("Getting members report for groupId={}", groupId);
            
            List<GroupMember> members = groupMemberRepository.findByGroup_GroupId(groupId);
            
            List<Map<String, Object>> memberReports = members.stream()
                .map(member -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("memberId", member.getMemberId());
                    report.put("userId", member.getUserId());
                    report.put("role", member.getRole().toString());
                    report.put("ownershipPercent", member.getOwnershipPercent());
                    report.put("joinedAt", member.getJoinedAt());
                    
                    // Đếm số lần bỏ phiếu
                    List<VotingResult> votingResults = votingResultRepository.findByGroupMember_MemberId(member.getMemberId());
                    report.put("totalVotes", votingResults.size());
                    
                    return report;
                })
                .collect(Collectors.toList());
            
            // Thống kê tổng quan
            long adminCount = members.stream()
                .filter(m -> m.getRole() == GroupMember.MemberRole.Admin)
                .count();
            
            double totalOwnership = members.stream()
                .mapToDouble(m -> m.getOwnershipPercent() != null ? m.getOwnershipPercent() : 0.0)
                .sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", groupId);
            response.put("totalMembers", members.size());
            response.put("adminCount", adminCount);
            response.put("regularMemberCount", members.size() - adminCount);
            response.put("totalOwnership", totalOwnership);
            response.put("members", memberReports);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting members report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo bỏ phiếu theo thời gian
     * GET /api/reports/groups/{groupId}/votings/timeline?startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/groups/{groupId}/votings/timeline")
    public ResponseEntity<?> getVotingTimeline(
            @PathVariable Integer groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting voting timeline for groupId={}, period={} to {}", groupId, startDate, endDate);
            
            List<Voting> allVotings = votingRepository.findByGroup_GroupId(groupId);
            
            List<Voting> filteredVotings = allVotings;
            if (startDate != null || endDate != null) {
                filteredVotings = allVotings.stream()
                    .filter(v -> {
                        LocalDateTime createdAt = v.getCreatedAt();
                        if (startDate != null && createdAt.isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && createdAt.isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            }
            
            // Nhóm theo tháng
            Map<String, List<Map<String, Object>>> byMonth = new LinkedHashMap<>();
            for (Voting voting : filteredVotings) {
                String monthKey = voting.getCreatedAt().getYear() + "-" + 
                    String.format("%02d", voting.getCreatedAt().getMonthValue());
                
                byMonth.putIfAbsent(monthKey, new ArrayList<>());
                
                Map<String, Object> votingInfo = new HashMap<>();
                votingInfo.put("voteId", voting.getVoteId());
                votingInfo.put("topic", voting.getTopic());
                votingInfo.put("status", voting.getStatus().toString());
                votingInfo.put("createdAt", voting.getCreatedAt());
                votingInfo.put("deadline", voting.getDeadline());
                votingInfo.put("totalVotes", voting.getTotalVotes());
                
                byMonth.get(monthKey).add(votingInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupId", groupId);
            response.put("period", Map.of("start", startDate, "end", endDate));
            response.put("totalVotings", filteredVotings.size());
            response.put("votingsByMonth", byMonth);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting voting timeline: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

