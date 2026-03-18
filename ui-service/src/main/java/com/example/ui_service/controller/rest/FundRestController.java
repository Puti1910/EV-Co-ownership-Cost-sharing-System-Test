package com.example.ui_service.controller.rest;

import com.example.ui_service.client.CostPaymentClient;
import com.example.ui_service.client.GroupManagementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller để proxy các request Fund từ frontend sang backend
 */
@RestController
@RequestMapping("/api/fund")
public class FundRestController {

    private static final Logger logger = LoggerFactory.getLogger(FundRestController.class);

    @Value("${cost-payment.service.url:http://localhost:8084}")
    private String costPaymentServiceUrl;

    @Autowired
    private GroupManagementClient groupManagementClient;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Lấy thống kê quỹ
     * GET /api/fund/stats?userId={userId}
     * Nếu không có userId, trả về tổng số dư của tất cả các nhóm mà user tham gia
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getFundStats(@RequestParam(required = false) Integer userId) {
        try {
            // Nếu không có userId, trả về mock data như cũ (backward compatibility)
            if (userId == null) {
            Map<String, Object> stats = Map.of(
                    "totalBalance", 0,
                    "totalIncome", 0,
                    "totalExpense", 0,
                    "pendingCount", 0,
                    "openingBalance", 0,
                    "myDeposits", 0,
                    "myWithdraws", 0,
                    "myPendingCount", 0
                );
                return ResponseEntity.ok(stats);
            }

            // Lấy danh sách các nhóm mà user tham gia
            List<Map<String, Object>> userGroups = groupManagementClient.getGroupsByUserIdAsMap(userId);
            logger.info("Found {} groups for userId={}", userGroups.size(), userId);

            // Tính tổng số dư, thu nhập, chi phí của tất cả các nhóm mà user tham gia
            double totalBalance = 0;
            double totalIncome = 0;
            double totalExpense = 0;
            int totalPendingCount = 0;
            double myDeposits = 0;
            double myWithdraws = 0;
            int myPendingCount = 0;

            // Lấy tất cả transactions của user một lần để tối ưu performance
            List<Map<String, Object>> allUserTransactions = null;
            try {
                ResponseEntity<List> userTransactionsResponse = restTemplate.exchange(
                    costPaymentServiceUrl + "/api/funds/transactions/user/" + userId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List>() {}
                );
                if (userTransactionsResponse.getStatusCode().is2xxSuccessful() && userTransactionsResponse.getBody() != null) {
                    allUserTransactions = userTransactionsResponse.getBody();
                }
            } catch (Exception e) {
                logger.warn("Failed to get user transactions for userId={}: {}", userId, e.getMessage());
            }

            // Tập hợp các fundId của các nhóm mà user tham gia
            java.util.Set<Integer> userFundIds = new java.util.HashSet<>();

            for (Map<String, Object> group : userGroups) {
                Integer groupId = (Integer) group.get("groupId");
                if (groupId == null) continue;

                try {
                    // Lấy fund của nhóm này
                    ResponseEntity<Map> fundResponse = restTemplate.getForEntity(
                        costPaymentServiceUrl + "/api/funds/group/" + groupId,
                        Map.class
                    );

                    if (fundResponse.getStatusCode().is2xxSuccessful() && fundResponse.getBody() != null) {
                        Map<String, Object> fund = fundResponse.getBody();
                        Integer fundId = (Integer) fund.get("fundId");
                        if (fundId != null) {
                            userFundIds.add(fundId);

                            // Lấy số dư hiện tại của fund
                            Object currentBalanceObj = fund.get("currentBalance");
                            if (currentBalanceObj != null) {
                                double balance = currentBalanceObj instanceof Number 
                                    ? ((Number) currentBalanceObj).doubleValue() 
                                    : Double.parseDouble(currentBalanceObj.toString());
                                totalBalance += balance;
                            }

                            // Lấy thống kê chi tiết của fund
                            try {
                                ResponseEntity<Map> statsResponse = restTemplate.getForEntity(
                                    costPaymentServiceUrl + "/api/funds/" + fundId + "/statistics",
                                    Map.class
                                );

                                if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.getBody() != null) {
                                    Map<String, Object> fundStats = statsResponse.getBody();
                                    
                                    // Tổng nạp tiền
                                    Object totalDepositObj = fundStats.get("totalDeposit");
                                    if (totalDepositObj != null) {
                                        double deposit = totalDepositObj instanceof Number 
                                            ? ((Number) totalDepositObj).doubleValue() 
                                            : Double.parseDouble(totalDepositObj.toString());
                                        totalIncome += deposit;
                                    }

                                    // Tổng rút tiền
                                    Object totalWithdrawObj = fundStats.get("totalWithdraw");
                                    if (totalWithdrawObj != null) {
                                        double withdraw = totalWithdrawObj instanceof Number 
                                            ? ((Number) totalWithdrawObj).doubleValue() 
                                            : Double.parseDouble(totalWithdrawObj.toString());
                                        totalExpense += withdraw;
                                    }

                                    // Số yêu cầu đang chờ
                                    Object pendingObj = fundStats.get("pendingRequests");
                                    if (pendingObj != null) {
                                        int pending = pendingObj instanceof Number 
                                            ? ((Number) pendingObj).intValue() 
                                            : Integer.parseInt(pendingObj.toString());
                                        totalPendingCount += pending;
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to get statistics for fundId={}: {}", fundId, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get fund for groupId={}: {}", groupId, e.getMessage());
                }
            }

            // Tính toán các giao dịch của user từ danh sách đã lấy
            if (allUserTransactions != null) {
                for (Map<String, Object> transaction : allUserTransactions) {
                    Integer transactionFundId = (Integer) transaction.get("fundId");
                    if (transactionFundId != null && userFundIds.contains(transactionFundId)) {
                        String transactionType = (String) transaction.get("transactionType");
                        String status = (String) transaction.get("status");
                        Object amountObj = transaction.get("amount");
                        
                        if (amountObj != null) {
                            double amount = amountObj instanceof Number 
                                ? ((Number) amountObj).doubleValue() 
                                : Double.parseDouble(amountObj.toString());

                            if ("Deposit".equals(transactionType)) {
                                myDeposits += amount;
                            } else if ("Withdraw".equals(transactionType)) {
                                // Chỉ tính các giao dịch rút tiền có trạng thái Completed
                                if ("Completed".equals(status)) {
                                    myWithdraws += amount;
                                }
                                if ("Pending".equals(status)) {
                                    myPendingCount++;
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBalance", totalBalance);
            stats.put("totalIncome", totalIncome);
            stats.put("totalExpense", totalExpense);
            stats.put("pendingCount", totalPendingCount);
            stats.put("openingBalance", 0); // TODO: Tính opening balance nếu cần
            stats.put("myDeposits", myDeposits);
            stats.put("myWithdraws", myWithdraws);
            stats.put("myPendingCount", myPendingCount);

            logger.info("Fund stats for userId={}: totalBalance={}, totalIncome={}, totalExpense={}", 
                userId, totalBalance, totalIncome, totalExpense);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting fund stats for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy tất cả giao dịch
     * GET /api/fund/transactions?status=...&userId=...
     * Nếu có userId, lấy transactions của user đó từ tất cả funds
     * Nếu không có userId, trả về empty list (backward compatibility)
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer userId
    ) {
        try {
            List<Map<String, Object>> transactions = null;
            
            // Nếu có userId, lấy transactions của user từ tất cả funds
            if (userId != null) {
                String url = costPaymentServiceUrl + "/api/funds/transactions/user/" + userId;
                
                try {
                    // Sử dụng LinkedHashMap để RestTemplate có thể deserialize JSON objects thành Map
                    ResponseEntity<List<LinkedHashMap<String, Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<LinkedHashMap<String, Object>>>() {}
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        // Convert LinkedHashMap sang Map<String, Object>
                        List<LinkedHashMap<String, Object>> rawList = response.getBody();
                        transactions = rawList.stream()
                            .map(item -> (Map<String, Object>) new HashMap<>(item))
                            .collect(java.util.stream.Collectors.toList());
                    } else {
                        transactions = java.util.Collections.emptyList();
                    }
                } catch (Exception e) {
                    logger.error("Error calling backend service for userId={}: {}", userId, e.getMessage(), e);
                    // Fallback: trả về empty list thay vì throw error
                    transactions = java.util.Collections.emptyList();
                }
            } else {
                // Backward compatibility: trả về empty list nếu không có userId
                logger.warn("No userId provided to /api/fund/transactions, returning empty list");
                transactions = java.util.Collections.emptyList();
            }
            
            // Filter by status if provided
            if (status != null && transactions != null && !transactions.isEmpty()) {
                transactions = transactions.stream()
                    .filter(t -> {
                        if (t != null && t instanceof Map) {
                            Object tStatus = ((Map<?, ?>) t).get("status");
                            return status.equals(tStatus) || status.equalsIgnoreCase(String.valueOf(tStatus));
                        }
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(transactions != null ? transactions : java.util.Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error getting transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy giao dịch theo userId
     * GET /api/fund/transactions/user/{userId}
     */
    @GetMapping("/transactions/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Integer userId) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/transactions/user/" + userId;
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List>() {}
            );
            
            List transactions = response.getBody();
            return ResponseEntity.ok(transactions != null ? transactions : java.util.Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error getting transactions for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Nạp tiền vào quỹ
     * POST /api/fund/deposit
     * Body: { fundId, userId, amount, purpose }
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/deposit";
            logger.info("Deposit request: {} to URL: {}", request, url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            logger.info("Deposit response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error depositing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tạo yêu cầu rút tiền (cần vote)
     * POST /api/fund/withdraw/request
     * Body: { fundId, userId, amount, purpose, receiptUrl? }
     */
    @PostMapping("/withdraw/request")
    public ResponseEntity<?> withdrawRequest(@RequestBody Map<String, Object> request) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/withdraw/request";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin rút tiền trực tiếp (không cần vote)
     * POST /api/fund/withdraw/admin
     * Body: { fundId, userId (adminId), amount, purpose, receiptUrl? }
     */
    @PostMapping("/withdraw/admin")
    public ResponseEntity<?> adminDirectWithdraw(@RequestBody Map<String, Object> request) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/withdraw/admin";
            logger.info("Admin direct withdraw request: {} to URL: {}", request, url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            logger.info("Admin direct withdraw response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error processing admin direct withdraw: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy quỹ theo groupId
     * GET /api/fund/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getFundByGroupId(@PathVariable Integer groupId,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/group/" + groupId;
            
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null && !authHeader.isEmpty()) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            } else {
                // Fund không tồn tại, trả về null hoặc empty object thay vì 404
                logger.info("Fund not found for groupId={}, returning null", groupId);
                return ResponseEntity.ok(null);
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Fund không tồn tại, trả về null thay vì 404
            logger.info("Fund not found for groupId={}, returning null", groupId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            logger.error("Error getting fund for groupId={}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error getting fund for group " + groupId + ": " + e.getMessage()));
        }
    }

    /**
     * Tạo quỹ mới cho nhóm
     * POST /api/fund/group/{groupId}/create
     */
    @PostMapping("/group/{groupId}/create")
    public ResponseEntity<?> createFundForGroup(@PathVariable Integer groupId) {
        try {
            // Gọi endpoint tạo fund trong cost-payment-service
            String url = costPaymentServiceUrl + "/api/funds/group/" + groupId;
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to create fund for group {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create fund: " + e.getMessage()));
        }
    }

    /**
     * Tạo giao dịch mới
     * POST /api/fund/transactions
     */
    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> request) {
        try {
            String type = (String) request.get("type");
            String endpoint;
            
            if ("Deposit".equals(type)) {
                endpoint = "/api/funds/deposit";
            } else if ("Withdraw".equals(type)) {
                endpoint = "/api/funds/withdraw/request";
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid transaction type"));
            }
            
            String url = costPaymentServiceUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy giao dịch theo ID
     * GET /api/fund/transactions/{transactionId}
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<?> getTransactionById(@PathVariable Integer transactionId) {
        try {
            // Tìm transaction trong tất cả funds
            // Note: This is a simplified approach - in production, you might want to store transactionId mapping
            String url = costPaymentServiceUrl + "/api/funds/transactions/" + transactionId;
            
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                return ResponseEntity.ok(response.getBody());
            } catch (Exception e) {
                // Transaction might not exist at this endpoint, try alternative approach
                logger.warn("Transaction {} not found directly, trying alternative method", transactionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Transaction not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Phê duyệt giao dịch
     * POST /api/fund/transactions/{transactionId}/approve
     */
    @PostMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<?> approveTransaction(
        @PathVariable Integer transactionId,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/transactions/" + transactionId + "/approve";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error approving transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Từ chối giao dịch
     * POST /api/fund/transactions/{transactionId}/reject
     */
    @PostMapping("/transactions/{transactionId}/reject")
    public ResponseEntity<?> rejectTransaction(
        @PathVariable Integer transactionId,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/transactions/" + transactionId + "/reject";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error rejecting transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * User vote cho withdrawal request (approve hoặc reject)
     * POST /api/fund/transactions/{transactionId}/vote
     */
    @PostMapping("/transactions/{transactionId}/vote")
    public ResponseEntity<?> voteOnWithdrawRequest(
        @PathVariable Integer transactionId,
        @RequestBody Map<String, Object> body
    ) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/transactions/" + transactionId + "/vote";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error voting on withdraw request {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách withdrawal requests cần vote của user
     * GET /api/fund/pending-vote-requests/user/{userId}
     */
    @GetMapping("/pending-vote-requests/user/{userId}")
    public ResponseEntity<?> getPendingVoteRequestsForUser(@PathVariable Integer userId) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/pending-vote-requests/user/" + userId;
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting pending vote requests for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * User hủy yêu cầu rút tiền của chính mình
     * DELETE /api/fund/transactions/{transactionId}?userId={userId}
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<?> deleteTransaction(
        @PathVariable Integer transactionId,
        @RequestParam Integer userId
    ) {
        try {
            String url = costPaymentServiceUrl + "/api/funds/transactions/" + transactionId + "?userId=" + userId;
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error cancelling transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

