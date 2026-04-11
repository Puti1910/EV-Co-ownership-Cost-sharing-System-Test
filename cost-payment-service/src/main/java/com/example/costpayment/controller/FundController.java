package com.example.costpayment.controller;

import com.example.costpayment.dto.*;
import com.example.costpayment.entity.FundTransaction;
import com.example.costpayment.entity.GroupFund;
import com.example.costpayment.service.FundService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller: Quản lý Quỹ chung
 * Phương án C: Yêu cầu rút tiền + Voting + Phê duyệt
 */
@RestController
@RequestMapping("/api/funds")
@CrossOrigin(origins = "*")
public class FundController {

    private static final Logger logger = LoggerFactory.getLogger(FundController.class);

    @Autowired
    private FundService fundService;

    // ========================================
    // QUẢN LÝ QUỸ
    // ========================================

    /**
     * Lấy thông tin quỹ theo groupId
     * GET /api/funds/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getFundByGroupId(@PathVariable Integer groupId) {
        try {
            return fundService.getFundByGroupId(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting fund for groupId={}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tạo quỹ mới cho nhóm
     * POST /api/funds/group/{groupId}
     */
    @PostMapping("/group/{groupId}")
    public ResponseEntity<?> createFundForGroup(@PathVariable Integer groupId) {
        try {
            GroupFund fund = fundService.createFundForGroup(groupId);
            return ResponseEntity.status(HttpStatus.CREATED).body(fund);
        } catch (Exception e) {
            logger.error("Error creating fund for groupId={}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy tổng quan quỹ
     * GET /api/funds/{fundId}/summary
     */
    @GetMapping("/{fundId}/summary")
    public ResponseEntity<?> getFundSummary(@PathVariable Integer fundId) {
        try {
            FundSummaryDto summary = fundService.getFundSummary(fundId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting fund summary for fundId={}: {}", fundId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // NẠP TIỀN (USER/ADMIN)
    // ========================================

    /**
     * Nạp tiền vào quỹ
     * POST /api/funds/deposit
     * Body: { fundId, userId, amount, purpose }
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequestDto request) {
        try {
            logger.info("Received deposit request: fundId={}, userId={}, amount={}, purpose={}", 
                request.getFundId(), request.getUserId(), request.getAmount(), request.getPurpose());
            
            FundTransaction transaction = fundService.deposit(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Nạp tiền thành công");
            response.put("transaction", transaction);
            
            logger.info("Deposit successful: transactionId={}", transaction.getTransactionId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid deposit request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing deposit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // RÚT TIỀN - USER (CẦN VOTE)
    // ========================================

    /**
     * Tạo yêu cầu rút tiền (USER)
     * POST /api/funds/withdraw/request
     * Body: { fundId, userId, amount, purpose, receiptUrl? }
     */
    @PostMapping("/withdraw/request")
    public ResponseEntity<?> createWithdrawRequest(@Valid @RequestBody WithdrawRequestDto request) {
        try {
            FundTransaction transaction = fundService.createWithdrawRequest(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Yêu cầu rút tiền đã được tạo. Chờ bỏ phiếu và phê duyệt.");
            response.put("transaction", transaction);
            response.put("status", "Pending");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Invalid withdraw request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating withdraw request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt
     * GET /api/funds/{fundId}/pending-requests
     */
    @GetMapping("/{fundId}/pending-requests")
    public ResponseEntity<?> getPendingRequests(@PathVariable Integer fundId) {
        try {
            logger.info("Getting pending requests for fundId={}", fundId);
            List<FundTransaction> requests = fundService.getPendingRequests(fundId);
            logger.info("Found {} pending requests for fundId={}", requests != null ? requests.size() : 0, fundId);
            
            if (requests == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            
            // Thêm voteCount vào mỗi request
            List<Map<String, Object>> requestsWithVotes = requests.stream()
                .filter(transaction -> transaction != null && transaction.getTransactionId() != null)
                .map(transaction -> {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("transactionId", transaction.getTransactionId());
                        map.put("fundId", transaction.getFundId());
                        map.put("userId", transaction.getUserId());
                        map.put("amount", transaction.getAmount());
                        map.put("purpose", transaction.getPurpose());
                        map.put("date", transaction.getDate());
                        map.put("status", transaction.getStatus());
                        map.put("transactionType", transaction.getTransactionType());
                        map.put("approvedBy", transaction.getApprovedBy());
                        map.put("approvedAt", transaction.getApprovedAt());
                        
                        // Thêm voteCount
                        try {
                            long approveCount = fundService.countApprovesByTransactionId(transaction.getTransactionId());
                            long rejectCount = fundService.countRejectsByTransactionId(transaction.getTransactionId());
                            long totalCount = fundService.countVotesByTransactionId(transaction.getTransactionId());
                            
                            Map<String, Object> voteCount = new HashMap<>();
                            voteCount.put("approve", approveCount);
                            voteCount.put("reject", rejectCount);
                            voteCount.put("total", totalCount);
                            map.put("voteCount", voteCount);
                        } catch (Exception e) {
                            logger.error("Error getting vote counts for transaction {}: {}", 
                                transaction.getTransactionId(), e.getMessage());
                            map.put("voteCount", Map.of("approve", 0L, "reject", 0L, "total", 0L));
                        }
                        
                        return map;
                    } catch (Exception e) {
                        logger.error("Error processing transaction {}: {}", 
                            transaction != null ? transaction.getTransactionId() : "null", e.getMessage(), e);
                        // Trả về map cơ bản nếu có lỗi
                        Map<String, Object> map = new HashMap<>();
                        if (transaction != null) {
                            map.put("transactionId", transaction.getTransactionId());
                            map.put("fundId", transaction.getFundId());
                            map.put("userId", transaction.getUserId());
                            map.put("amount", transaction.getAmount());
                            map.put("purpose", transaction.getPurpose());
                            map.put("date", transaction.getDate());
                            map.put("status", transaction.getStatus());
                            map.put("transactionType", transaction.getTransactionType());
                            map.put("approvedBy", transaction.getApprovedBy());
                            map.put("approvedAt", transaction.getApprovedAt());
                        }
                        map.put("voteCount", Map.of("approve", 0L, "reject", 0L, "total", 0L));
                        return map;
                    }
                }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(requestsWithVotes);
        } catch (Exception e) {
            logger.error("Error getting pending requests for fundId={}: {}", fundId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // RÚT TIỀN - ADMIN (TRỰC TIẾP)
    // ========================================

    /**
     * Admin rút tiền trực tiếp (không cần vote)
     * POST /api/funds/withdraw/admin
     * Body: { fundId, userId (adminId), amount, purpose, receiptUrl? }
     */
    @PostMapping("/withdraw/admin")
    public ResponseEntity<?> adminDirectWithdraw(@Valid @RequestBody WithdrawRequestDto request) {
        try {
            FundTransaction transaction = fundService.adminDirectWithdraw(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Admin đã rút tiền thành công");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Invalid admin withdraw: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing admin withdraw: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // PHÊ DUYỆT YÊU CẦU (ADMIN)
    // ========================================

    /**
     * Admin phê duyệt yêu cầu rút tiền
     * POST /api/funds/withdraw/approve
     * Body: { transactionId, adminId, approved: true, note? }
     */
    @PostMapping("/withdraw/approve")
    public ResponseEntity<?> approveWithdrawRequest(@Valid @RequestBody ApproveRequestDto request) {
        try {
            FundTransaction transaction;
            if (request.getApproved()) {
                transaction = fundService.approveWithdrawRequest(request);
            } else {
                transaction = fundService.rejectWithdrawRequest(request);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", request.getApproved() 
                ? "✅ Yêu cầu đã được phê duyệt" 
                : "❌ Yêu cầu đã bị từ chối");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Cannot approve/reject: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving/rejecting request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Admin phê duyệt giao dịch (riêng lẻ)
     * POST /api/funds/transactions/{transactionId}/approve
     */
    @PostMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<?> approveTransaction(@PathVariable Integer transactionId) {
        try {
            ApproveRequestDto request = new ApproveRequestDto();
            request.setTransactionId(transactionId);
            request.setApproved(true);
            request.setAdminId(1); // TODO: Get from session
            
            FundTransaction transaction = fundService.approveWithdrawRequest(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Yêu cầu đã được phê duyệt");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Cannot approve: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Admin từ chối giao dịch (riêng lẻ)
     * POST /api/funds/transactions/{transactionId}/reject
     * Body: { reason? }
     */
    @PostMapping("/transactions/{transactionId}/reject")
    public ResponseEntity<?> rejectTransaction(
        @PathVariable Integer transactionId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            ApproveRequestDto request = new ApproveRequestDto();
            request.setTransactionId(transactionId);
            request.setApproved(false);
            request.setAdminId(1); // TODO: Get from session
            if (body != null && body.containsKey("reason")) {
                request.setNote(body.get("reason"));
            }
            
            FundTransaction transaction = fundService.rejectWithdrawRequest(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "❌ Yêu cầu đã bị từ chối");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Cannot reject: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // USER VOTE CHO WITHDRAWAL REQUEST
    // ========================================

    /**
     * User vote cho withdrawal request (approve hoặc reject)
     * POST /api/funds/transactions/{transactionId}/vote
     * Body: { userId, approve: true/false, note? }
     */
    @PostMapping("/transactions/{transactionId}/vote")
    public ResponseEntity<?> voteOnWithdrawRequest(
        @PathVariable Integer transactionId,
        @Valid @RequestBody VoteRequestDto request
    ) {
        try {
            request.setTransactionId(transactionId);
            FundTransaction transaction = fundService.voteOnWithdrawRequest(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", request.getApprove() 
                ? "✅ Bạn đã đồng ý yêu cầu rút tiền này" 
                : "❌ Bạn đã từ chối yêu cầu rút tiền này");
            response.put("transaction", transaction);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Cannot vote: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error voting on withdraw request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách withdrawal requests cần vote của user
     * GET /api/funds/pending-vote-requests/user/{userId}
     */
    @GetMapping("/pending-vote-requests/user/{userId}")
    public ResponseEntity<?> getPendingVoteRequestsForUser(@PathVariable Integer userId) {
        try {
            List<FundTransaction> requests = fundService.getPendingVoteRequestsForUser(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            logger.error("Error getting pending vote requests for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // LỊCH SỬ GIAO DỊCH
    // ========================================

    /**
     * Lấy tất cả giao dịch của quỹ
     * GET /api/funds/{fundId}/transactions
     */
    @GetMapping("/{fundId}/transactions")
    public ResponseEntity<?> getAllTransactions(@PathVariable Integer fundId) {
        try {
            List<FundTransaction> transactions = fundService.getAllTransactions(fundId);
            
            if (transactions == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            
            // Map sang Map để tránh lỗi serialize
            List<Map<String, Object>> transactionsList = transactions.stream()
                .filter(transaction -> transaction != null && transaction.getTransactionId() != null)
                .map(transaction -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("transactionId", transaction.getTransactionId());
                    map.put("fundId", transaction.getFundId());
                    map.put("userId", transaction.getUserId());
                    map.put("amount", transaction.getAmount());
                    map.put("purpose", transaction.getPurpose());
                    map.put("date", transaction.getDate());
                    map.put("status", transaction.getStatus());
                    map.put("transactionType", transaction.getTransactionType());
                    map.put("approvedBy", transaction.getApprovedBy());
                    map.put("approvedAt", transaction.getApprovedAt());
                    map.put("voteId", transaction.getVoteId());
                    map.put("receiptUrl", transaction.getReceiptUrl());
                    
                    // Thêm voteCount cho withdrawal requests
                    if (transaction.getTransactionType() != null && 
                        transaction.getTransactionType() == FundTransaction.TransactionType.Withdraw &&
                        transaction.getTransactionId() != null) {
                        try {
                            long approveCount = fundService.countApprovesByTransactionId(transaction.getTransactionId());
                            long rejectCount = fundService.countRejectsByTransactionId(transaction.getTransactionId());
                            long totalCount = fundService.countVotesByTransactionId(transaction.getTransactionId());
                            
                            Map<String, Object> voteCount = new HashMap<>();
                            voteCount.put("approve", approveCount);
                            voteCount.put("reject", rejectCount);
                            voteCount.put("total", totalCount);
                            map.put("voteCount", voteCount);
                        } catch (Exception e) {
                            logger.error("Error getting vote counts for transaction {}: {}", 
                                transaction.getTransactionId(), e.getMessage());
                            map.put("voteCount", Map.of("approve", 0L, "reject", 0L, "total", 0L));
                        }
                    } else {
                        map.put("voteCount", Map.of("approve", 0L, "reject", 0L, "total", 0L));
                    }
                    
                    return map;
                }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(transactionsList);
        } catch (Exception e) {
            logger.error("Error getting transactions for fundId={}: {}", fundId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy giao dịch của user
     * GET /api/funds/transactions/user/{userId}
     */
    @GetMapping("/transactions/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Integer userId) {
        try {
            List<FundTransaction> transactions = fundService.getTransactionsByUser(userId);
            
            // Map sang Map để tránh lỗi serialize và thêm voteCount
            List<Map<String, Object>> transactionsList = transactions.stream().map(transaction -> {
                Map<String, Object> map = new HashMap<>();
                map.put("transactionId", transaction.getTransactionId());
                map.put("fundId", transaction.getFundId());
                map.put("userId", transaction.getUserId());
                map.put("amount", transaction.getAmount());
                map.put("purpose", transaction.getPurpose());
                map.put("date", transaction.getDate());
                map.put("status", transaction.getStatus());
                map.put("transactionType", transaction.getTransactionType());
                map.put("approvedBy", transaction.getApprovedBy());
                map.put("approvedAt", transaction.getApprovedAt());
                map.put("voteId", transaction.getVoteId());
                map.put("receiptUrl", transaction.getReceiptUrl());
                
                // Thêm voteCount cho withdrawal requests
                if (transaction.getTransactionType() != null && 
                    transaction.getTransactionType() == FundTransaction.TransactionType.Withdraw) {
                    try {
                        long approveCount = fundService.countApprovesByTransactionId(transaction.getTransactionId());
                        long rejectCount = fundService.countRejectsByTransactionId(transaction.getTransactionId());
                        long totalCount = fundService.countVotesByTransactionId(transaction.getTransactionId());
                        
                        Map<String, Object> voteCount = new HashMap<>();
                        voteCount.put("approve", approveCount);
                        voteCount.put("reject", rejectCount);
                        voteCount.put("total", totalCount);
                        map.put("voteCount", voteCount);
                    } catch (Exception e) {
                        logger.error("Error getting vote counts for transaction {}: {}", 
                            transaction.getTransactionId(), e.getMessage());
                        map.put("voteCount", Map.of("approve", 0L, "reject", 0L, "total", 0L));
                    }
                }
                
                return map;
            }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(transactionsList);
        } catch (Exception e) {
            logger.error("Error getting user transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy giao dịch theo loại
     * GET /api/funds/{fundId}/transactions/type/{type}
     * type = Deposit | Withdraw
     */
    @GetMapping("/{fundId}/transactions/type/{type}")
    public ResponseEntity<?> getTransactionsByType(
        @PathVariable Integer fundId,
        @PathVariable String type
    ) {
        try {
            List<FundTransaction> transactions = fundService.getTransactionsByType(fundId, type);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error getting transactions by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy giao dịch theo khoảng thời gian
     * GET /api/funds/{fundId}/transactions/range?start=...&end=...
     */
    @GetMapping("/{fundId}/transactions/range")
    public ResponseEntity<?> getTransactionsByDateRange(
        @PathVariable Integer fundId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        try {
            List<FundTransaction> transactions = fundService.getTransactionsByDateRange(fundId, start, end);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error getting transactions by date range: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy chi tiết giao dịch
     * GET /api/funds/transactions/{transactionId}
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<?> getTransactionById(@PathVariable Integer transactionId) {
        try {
            return fundService.getTransactionById(transactionId)
                .map(transaction -> {
                    // Map sang Map để tránh lỗi serialize và thêm voteCount
                    Map<String, Object> map = new HashMap<>();
                    map.put("transactionId", transaction.getTransactionId());
                    map.put("fundId", transaction.getFundId());
                    map.put("userId", transaction.getUserId());
                    map.put("amount", transaction.getAmount());
                    map.put("purpose", transaction.getPurpose());
                    map.put("date", transaction.getDate());
                    map.put("status", transaction.getStatus());
                    map.put("transactionType", transaction.getTransactionType());
                    map.put("approvedBy", transaction.getApprovedBy());
                    map.put("approvedAt", transaction.getApprovedAt());
                    map.put("voteId", transaction.getVoteId());
                    map.put("receiptUrl", transaction.getReceiptUrl());
                    
                    // Lấy thông tin fund để có groupId và currentBalance
                    if (transaction.getFundId() != null) {
                        fundService.getFundById(transaction.getFundId()).ifPresent(fund -> {
                            map.put("groupId", fund.getGroupId());
                            map.put("currentBalance", fund.getCurrentBalance());
                        });
                    }
                    
                    // Thêm voteCount cho withdrawal requests
                    if (transaction.getTransactionType() == FundTransaction.TransactionType.Withdraw) {
                        long approveCount = fundService.countApprovesByTransactionId(transaction.getTransactionId());
                        long rejectCount = fundService.countRejectsByTransactionId(transaction.getTransactionId());
                        long totalCount = fundService.countVotesByTransactionId(transaction.getTransactionId());
                        
                        Map<String, Object> voteCount = new HashMap<>();
                        voteCount.put("approve", approveCount);
                        voteCount.put("reject", rejectCount);
                        voteCount.put("total", totalCount);
                        map.put("voteCount", voteCount);
                    }
                    
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * User hủy yêu cầu rút tiền của chính mình
     * DELETE /api/funds/transactions/{transactionId}
     * Query param: userId
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<?> cancelTransaction(
        @PathVariable Integer transactionId,
        @RequestParam Integer userId
    ) {
        try {
            FundTransaction transaction = fundService.cancelWithdrawRequest(transactionId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Đã xóa yêu cầu rút tiền khỏi hệ thống");
            response.put("transactionId", transactionId);
            // Transaction đã bị xóa, không cần trả về transaction object
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Cannot cancel transaction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // THỐNG KÊ
    // ========================================

    /**
     * Thống kê quỹ
     * GET /api/funds/{fundId}/statistics
     */
    @GetMapping("/{fundId}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable Integer fundId) {
        try {
            // Kiểm tra fund có tồn tại không
            Optional<GroupFund> fundOpt = fundService.getFundById(fundId);
            if (!fundOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "totalDeposit", 0.0,
                    "totalWithdraw", 0.0,
                    "currentBalance", 0.0,
                    "pendingRequests", 0L
                ));
            }
            
            GroupFund fund = fundOpt.get();
            Map<String, Object> stats = new HashMap<>();
            
            // Lấy thống kê với xử lý lỗi cho từng method
            try {
                Double totalDeposit = fundService.getTotalDeposit(fundId);
                stats.put("totalDeposit", totalDeposit != null ? totalDeposit : 0.0);
            } catch (Exception e) {
                logger.warn("Error getting totalDeposit for fundId={}: {}", fundId, e.getMessage());
                stats.put("totalDeposit", 0.0);
            }
            
            try {
                Double totalWithdraw = fundService.getTotalWithdraw(fundId);
                stats.put("totalWithdraw", totalWithdraw != null ? totalWithdraw : 0.0);
            } catch (Exception e) {
                logger.warn("Error getting totalWithdraw for fundId={}: {}", fundId, e.getMessage());
                stats.put("totalWithdraw", 0.0);
            }
            
            // Sử dụng currentBalance từ fund object thay vì gọi lại service
            stats.put("currentBalance", fund.getCurrentBalance() != null ? fund.getCurrentBalance() : 0.0);
            
            try {
                Long pendingRequests = fundService.countPendingRequests(fundId);
                stats.put("pendingRequests", pendingRequests != null ? pendingRequests : 0L);
            } catch (Exception e) {
                logger.warn("Error getting pendingRequests for fundId={}: {}", fundId, e.getMessage());
                stats.put("pendingRequests", 0L);
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting statistics for fundId={}: {}", fundId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Báo cáo tài chính minh bạch cho quỹ chung
     * GET /api/funds/{fundId}/financial-report?startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/{fundId}/financial-report")
    public ResponseEntity<?> getFinancialReport(
            @PathVariable Integer fundId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            logger.info("Getting financial report for fundId={}, period={} to {}", fundId, startDate, endDate);
            
            // Lấy thông tin quỹ
            Optional<GroupFund> fundOpt = fundService.getFundById(fundId);
            if (!fundOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            GroupFund fund = fundOpt.get();
            
            // Lấy giao dịch trong khoảng thời gian
            List<FundTransaction> transactions = fundService.getTransactionsByDateRange(fundId, startDate, endDate);
            
            // Phân loại giao dịch
            List<FundTransaction> deposits = transactions.stream()
                .filter(t -> "Deposit".equals(t.getTransactionType().toString()))
                .collect(java.util.stream.Collectors.toList());
            
            List<FundTransaction> withdraws = transactions.stream()
                .filter(t -> "Withdraw".equals(t.getTransactionType().toString()))
                .collect(java.util.stream.Collectors.toList());
            
            // Tính tổng
            double totalDeposit = deposits.stream()
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                .sum();
            
            double totalWithdraw = withdraws.stream()
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                .sum();
            
            // Thống kê theo user
            Map<Integer, Map<String, Object>> userStats = new HashMap<>();
            for (FundTransaction transaction : transactions) {
                Integer userId = transaction.getUserId();
                if (userId != null) {
                    userStats.putIfAbsent(userId, new HashMap<>());
                    Map<String, Object> stats = userStats.get(userId);
                    
                    if ("Deposit".equals(transaction.getTransactionType().toString())) {
                        stats.put("totalDeposit", 
                            ((Double) stats.getOrDefault("totalDeposit", 0.0)) + transaction.getAmount());
                        stats.put("depositCount", 
                            ((Integer) stats.getOrDefault("depositCount", 0)) + 1);
                    } else if ("Withdraw".equals(transaction.getTransactionType().toString())) {
                        stats.put("totalWithdraw", 
                            ((Double) stats.getOrDefault("totalWithdraw", 0.0)) + transaction.getAmount());
                        stats.put("withdrawCount", 
                            ((Integer) stats.getOrDefault("withdrawCount", 0)) + 1);
                    }
                }
            }
            
            Map<String, Object> report = new HashMap<>();
            report.put("fundId", fundId);
            report.put("groupId", fund.getGroupId());
            report.put("currentBalance", fund.getCurrentBalance());
            report.put("totalContributed", fund.getTotalContributed());
            report.put("period", Map.of("start", startDate, "end", endDate));
            report.put("summary", Map.of(
                "totalDeposit", totalDeposit,
                "totalWithdraw", totalWithdraw,
                "netChange", totalDeposit - totalWithdraw,
                "transactionCount", transactions.size()
            ));
            report.put("deposits", deposits.size());
            report.put("withdraws", withdraws.size());
            report.put("userStatistics", userStats);
            report.put("transactions", transactions.stream()
                .map(t -> {
                    Map<String, Object> tMap = new HashMap<>();
                    tMap.put("transactionId", t.getTransactionId());
                    tMap.put("type", t.getTransactionType().toString());
                    tMap.put("amount", t.getAmount());
                    tMap.put("userId", t.getUserId());
                    tMap.put("purpose", t.getPurpose());
                    tMap.put("status", t.getStatus().toString());
                    tMap.put("createdAt", t.getDate());
                    return tMap;
                })
                .collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting financial report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xử lý tất cả pending transactions (kiểm tra vote và tự động hoàn tất hoặc từ chối)
     * Endpoint này có thể được gọi để xử lý các transactions đã có đủ votes từ trước
     */
    @PostMapping("/process-pending")
    public ResponseEntity<Map<String, Object>> processAllPendingTransactions() {
        try {
            int processedCount = fundService.processAllPendingTransactions();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xử lý " + processedCount + " pending transactions",
                "processedCount", processedCount
            ));
        } catch (Exception e) {
            logger.error("Error processing all pending transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

