package com.example.disputemanagement.controller;

import com.example.disputemanagement.entity.*;
import com.example.disputemanagement.service.DisputeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "*")
public class DisputeController {
    
    private static final Logger logger = LoggerFactory.getLogger(DisputeController.class);
    
    @Autowired
    private DisputeService disputeService;
    
    // ========== DISPUTE ENDPOINTS ==========
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            long count = disputeService.getAllDisputes().size();
            return ResponseEntity.ok("Database connected. Disputes count: " + count);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Database error: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllDisputes(
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer assignedTo,
            @RequestParam(required = false) Integer createdBy) {
        try {
            List<Dispute> disputes;
            
            if (groupId != null) {
                disputes = disputeService.getDisputesByGroup(groupId);
            } else if (status != null) {
                disputes = disputeService.getDisputesByStatus(Dispute.DisputeStatus.valueOf(status));
            } else if (priority != null) {
                disputes = disputeService.getDisputesByPriority(Dispute.DisputePriority.valueOf(priority));
            } else if (assignedTo != null) {
                disputes = disputeService.getAllDisputes().stream()
                    .filter(d -> assignedTo.equals(d.getAssignedTo()))
                    .toList();
            } else if (createdBy != null) {
                disputes = disputeService.getAllDisputes().stream()
                    .filter(d -> createdBy.equals(d.getCreatedBy()))
                    .toList();
            } else {
                disputes = disputeService.getAllDisputes();
            }
            
            return ResponseEntity.ok(disputes);
        } catch (Exception e) {
            logger.error("Error fetching disputes", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/unassigned")
    public ResponseEntity<?> getUnassignedDisputes() {
        try {
            List<Dispute> disputes = disputeService.getUnassignedDisputes();
            return ResponseEntity.ok(disputes);
        } catch (Exception e) {
            logger.error("Error fetching unassigned disputes", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDisputes() {
        try {
            List<Dispute> disputes = disputeService.getPendingDisputesOrderedByPriority();
            return ResponseEntity.ok(disputes);
        } catch (Exception e) {
            logger.error("Error fetching pending disputes", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{disputeId}")
    public ResponseEntity<?> getDisputeById(@PathVariable Integer disputeId) {
        try {
            Optional<Dispute> dispute = disputeService.getDisputeById(disputeId);
            if (dispute.isPresent()) {
                return ResponseEntity.ok(dispute.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching dispute", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createDispute(@RequestBody Dispute dispute) {
        try {
            Dispute created = disputeService.createDispute(dispute);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating dispute", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{disputeId}")
    public ResponseEntity<?> updateDispute(@PathVariable Integer disputeId, @RequestBody Dispute dispute) {
        try {
            Dispute updated = disputeService.updateDispute(disputeId, dispute);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating dispute", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{disputeId}/assign")
    public ResponseEntity<?> assignDispute(@PathVariable Integer disputeId, @RequestBody Map<String, Integer> request) {
        try {
            Integer staffId = request.get("staffId");
            if (staffId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "staffId is required"));
            }
            Dispute assigned = disputeService.assignDispute(disputeId, staffId);
            return ResponseEntity.ok(assigned);
        } catch (Exception e) {
            logger.error("Error assigning dispute", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{disputeId}")
    public ResponseEntity<?> deleteDispute(@PathVariable Integer disputeId) {
        try {
            disputeService.deleteDispute(disputeId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Dispute deleted"));
        } catch (Exception e) {
            logger.error("Error deleting dispute", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== COMMENT ENDPOINTS ==========
    
    @GetMapping("/{disputeId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Integer disputeId,
                                         @RequestParam(required = false, defaultValue = "false") Boolean includeInternal) {
        try {
            List<DisputeComment> comments = disputeService.getCommentsByDispute(disputeId, includeInternal);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("Error fetching comments", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{disputeId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Integer disputeId, @RequestBody DisputeComment comment) {
        try {
            DisputeComment created = disputeService.addComment(disputeId, comment);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error adding comment", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId) {
        try {
            disputeService.deleteComment(commentId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Error deleting comment", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== RESOLUTION ENDPOINTS ==========
    
    @GetMapping("/{disputeId}/resolution")
    public ResponseEntity<?> getResolution(@PathVariable Integer disputeId) {
        try {
            Optional<DisputeResolution> resolution = disputeService.getResolutionByDispute(disputeId);
            if (resolution.isPresent()) {
                return ResponseEntity.ok(resolution.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching resolution", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{disputeId}/resolution")
    public ResponseEntity<?> createResolution(@PathVariable Integer disputeId, @RequestBody DisputeResolution resolution) {
        try {
            DisputeResolution created = disputeService.createResolution(disputeId, resolution);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating resolution", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== HISTORY ENDPOINTS ==========
    
    @GetMapping("/{disputeId}/history")
    public ResponseEntity<?> getHistory(@PathVariable Integer disputeId) {
        try {
            List<DisputeHistory> history = disputeService.getHistoryByDispute(disputeId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching history", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== ATTACHMENT ENDPOINTS ==========
    
    @GetMapping("/{disputeId}/attachments")
    public ResponseEntity<?> getAttachments(@PathVariable Integer disputeId) {
        try {
            List<DisputeAttachment> attachments = disputeService.getAttachmentsByDispute(disputeId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            logger.error("Error fetching attachments", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/attachments")
    public ResponseEntity<?> addAttachment(@RequestBody DisputeAttachment attachment) {
        try {
            DisputeAttachment created = disputeService.addAttachment(attachment);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error adding attachment", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== STATISTICS ENDPOINTS ==========
    
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = Map.of(
                "total", disputeService.getAllDisputes().size(),
                "pending", disputeService.countByStatus(Dispute.DisputeStatus.PENDING),
                "inReview", disputeService.countByStatus(Dispute.DisputeStatus.IN_REVIEW),
                "resolved", disputeService.countByStatus(Dispute.DisputeStatus.RESOLVED),
                "closed", disputeService.countByStatus(Dispute.DisputeStatus.CLOSED),
                "urgent", disputeService.countByPriority(Dispute.DisputePriority.URGENT),
                "high", disputeService.countByPriority(Dispute.DisputePriority.HIGH),
                "unassigned", disputeService.getUnassignedDisputes().size()
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching statistics", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

