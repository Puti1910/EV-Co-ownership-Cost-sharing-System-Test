package com.example.costpayment.service.impl;

import com.example.costpayment.dto.*;
import com.example.costpayment.entity.FundTransaction;
import com.example.costpayment.entity.FundTransaction.TransactionStatus;
import com.example.costpayment.entity.FundTransaction.TransactionType;
import com.example.costpayment.entity.GroupFund;
import com.example.costpayment.entity.TransactionVote;
import com.example.costpayment.repository.FundTransactionRepository;
import com.example.costpayment.repository.GroupFundRepository;
import com.example.costpayment.repository.TransactionVoteRepository;
import com.example.costpayment.service.FundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Implementation: Quản lý Quỹ chung
 * Phương án C: Yêu cầu rút tiền + Voting + Phê duyệt
 */
@Service
public class FundServiceImpl implements FundService {

    private static final Logger logger = LoggerFactory.getLogger(FundServiceImpl.class);

    @Autowired
    private GroupFundRepository groupFundRepository;

    @Autowired
    private FundTransactionRepository transactionRepository;

    @Autowired
    private TransactionVoteRepository voteRepository;

    @Value("${group-management.service.url:${API_GATEWAY_URL:http://localhost:8084}}")
    private String groupManagementServiceUrl;

    @Value("${internal.service.token:}")
    private String internalServiceToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // ========================================
    // QUẢN LÝ QUỸ
    // ========================================

    @Override
    public Optional<GroupFund> getFundById(Integer fundId) {
        return groupFundRepository.findById(fundId);
    }

    @Override
    public Optional<GroupFund> getFundByGroupId(Integer groupId) {
        return groupFundRepository.findByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupFund createFundForGroup(Integer groupId) {
        // Kiểm tra đã tồn tại chưa
        Optional<GroupFund> existing = groupFundRepository.findByGroupId(groupId);
        if (existing.isPresent()) {
            logger.warn("Fund already exists for groupId={}", groupId);
            return existing.get();
        }

        GroupFund fund = new GroupFund();
        fund.setGroupId(groupId);
        fund.setTotalContributed(0.0);
        fund.setCurrentBalance(0.0);
        fund.setUpdatedAt(LocalDateTime.now());
        fund.setNote("Quỹ chung nhóm " + groupId);

        GroupFund saved = groupFundRepository.save(fund);
        logger.info("Created fund for groupId={}, fundId={}", groupId, saved.getFundId());
        return saved;
    }

    @Override
    public FundSummaryDto getFundSummary(Integer fundId) {
        GroupFund fund = groupFundRepository.findById(fundId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ với ID: " + fundId));

        Long pendingCount = transactionRepository.countPendingTransactions(fundId);
        Double totalDeposit = transactionRepository.getTotalDeposit(fundId);
        Double totalWithdraw = transactionRepository.getTotalWithdraw(fundId);

        FundSummaryDto summary = new FundSummaryDto();
        summary.setFundId(fund.getFundId());
        summary.setGroupId(fund.getGroupId());
        summary.setTotalContributed(fund.getTotalContributed());
        summary.setCurrentBalance(fund.getCurrentBalance());
        summary.setTotalDeposit(totalDeposit);
        summary.setTotalWithdraw(totalWithdraw);
        summary.setPendingRequests(pendingCount);
        summary.setUpdatedAt(fund.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return summary;
    }

    // ========================================
    // NẠP TIỀN (USER/ADMIN)
    // ========================================

    @Override
    @Transactional
    public FundTransaction deposit(DepositRequestDto request) {
        // Validate
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        }

        // Lấy quỹ
        GroupFund fund = groupFundRepository.findById(request.getFundId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        // Tạo giao dịch
        FundTransaction transaction = FundTransaction.createDeposit(
            request.getFundId(),
            request.getUserId(),
            request.getAmount(),
            request.getPurpose() != null ? request.getPurpose() : "Nạp tiền vào quỹ"
        );

        // Cập nhật quỹ
        fund.deposit(request.getAmount());
        groupFundRepository.save(fund);

        // Lưu giao dịch
        FundTransaction saved = transactionRepository.save(transaction);
        logger.info("Deposit: userId={}, amount={}, fundId={}", 
            request.getUserId(), request.getAmount(), request.getFundId());

        return saved;
    }

    // ========================================
    // RÚT TIỀN - USER (CẦN VOTE)
    // ========================================

    @Override
    @Transactional
    public FundTransaction createWithdrawRequest(WithdrawRequestDto request) {
        // Validate
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        }

        // Kiểm tra số dư
        GroupFund fund = groupFundRepository.findById(request.getFundId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        if (!fund.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException(
                String.format("Số dư không đủ. Hiện có: %.2f VND, yêu cầu: %.2f VND",
                    fund.getCurrentBalance(), request.getAmount())
            );
        }

        // Tạo yêu cầu (status = Pending)
        FundTransaction transaction = FundTransaction.createWithdrawRequest(
            request.getFundId(),
            request.getUserId(),
            request.getAmount(),
            request.getPurpose()
        );
        transaction.setReceiptUrl(request.getReceiptUrl());

        FundTransaction saved = transactionRepository.save(transaction);
        logger.info("Withdraw request created: userId={}, amount={}, transactionId={}", 
            request.getUserId(), request.getAmount(), saved.getTransactionId());

        // Tự động tạo vote đồng ý cho chính người tạo request
        TransactionVote autoVote = new TransactionVote();
        autoVote.setTransactionId(saved.getTransactionId());
        autoVote.setUserId(request.getUserId());
        autoVote.setApprove(true); // Tự động đồng ý
        autoVote.setNote("Tự động đồng ý khi tạo yêu cầu");
        autoVote.setVotedAt(LocalDateTime.now());
        voteRepository.save(autoVote);
        logger.info("Auto vote approve created for requester: transactionId={}, userId={}", 
            saved.getTransactionId(), request.getUserId());

        return saved;
    }

    @Override
    public List<FundTransaction> getPendingRequests(Integer fundId) {
        logger.info("getPendingRequests called with fundId={}", fundId);
        List<FundTransaction> requests = transactionRepository.findPendingWithdrawRequests(fundId);
        logger.info("Repository returned {} pending requests for fundId={}", requests != null ? requests.size() : 0, fundId);
        if (requests != null && !requests.isEmpty()) {
            requests.forEach(req -> logger.info("Pending request: transactionId={}, fundId={}, userId={}, status={}", 
                req.getTransactionId(), req.getFundId(), req.getUserId(), req.getStatus()));
        }
        return requests;
    }

    // ========================================
    // RÚT TIỀN - ADMIN (TRỰC TIẾP)
    // ========================================

    @Override
    @Transactional
    public FundTransaction adminDirectWithdraw(WithdrawRequestDto request) {
        // Validate
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        }

        // Lấy quỹ
        GroupFund fund = groupFundRepository.findById(request.getFundId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        // Kiểm tra số dư
        if (!fund.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException(
                String.format("Số dư không đủ. Hiện có: %.2f VND, yêu cầu: %.2f VND",
                    fund.getCurrentBalance(), request.getAmount())
            );
        }

        // Tạo giao dịch (status = Completed)
        FundTransaction transaction = FundTransaction.createDirectWithdraw(
            request.getFundId(),
            request.getUserId(), // adminId
            request.getAmount(),
            request.getPurpose()
        );
        transaction.setReceiptUrl(request.getReceiptUrl());

        // Cập nhật quỹ
        fund.withdraw(request.getAmount());
        groupFundRepository.save(fund);

        // Lưu giao dịch
        FundTransaction saved = transactionRepository.save(transaction);
        logger.info("Admin direct withdraw: adminId={}, amount={}, fundId={}", 
            request.getUserId(), request.getAmount(), request.getFundId());

        return saved;
    }

    // ========================================
    // PHÊ DUYỆT YÊU CẦU (ADMIN)
    // ========================================

    @Override
    @Transactional
    public FundTransaction approveWithdrawRequest(ApproveRequestDto request) {
        // Lấy giao dịch
        FundTransaction transaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

        // Kiểm tra trạng thái: CHỈ approve khi status = Pending
        if (transaction.getStatus() != TransactionStatus.Pending) {
            throw new IllegalStateException(
                "Chỉ có thể phê duyệt yêu cầu đang ở trạng thái Pending. " +
                "Hiện tại trạng thái: " + transaction.getStatus());
        }

        // KIỂM TRA: Phải đảm bảo có >50% thành viên đồng ý mới được phê duyệt
        try {
            // Lấy số thành viên nhóm
            GroupFund fund = groupFundRepository.findById(transaction.getFundId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
            
            int totalMembers = getGroupMemberCount(fund.getGroupId());
            if (totalMembers <= 0) {
                throw new IllegalStateException("Không thể lấy số thành viên nhóm");
            }
            
            // Đếm số phiếu đồng ý
            long approveCount = voteRepository.countApprovesByTransactionId(request.getTransactionId());
            
            // Tính tỷ lệ: approveCount / (totalMembers - 1) vì trừ người tạo request
            int eligibleVoters = totalMembers - 1; // Trừ người tạo request
            if (eligibleVoters <= 0) {
                throw new IllegalStateException("Không có thành viên nào có thể vote");
            }
            
            double approvalRate = (double) approveCount / eligibleVoters;
            logger.info("Admin approval check: approveCount={}, eligibleVoters={}, approvalRate={}%", 
                approveCount, eligibleVoters, String.format("%.2f", approvalRate * 100));
            
            // PHẢI > 50% mới được phê duyệt (không phải >= 50%)
            if (approvalRate <= 0.5) {
                throw new IllegalStateException(
                    String.format("Không thể phê duyệt: chỉ có %.1f%% thành viên đồng ý (cần >50%%)", 
                        approvalRate * 100));
            }
        } catch (IllegalStateException e) {
            // Re-throw IllegalStateException
            throw e;
        } catch (Exception e) {
            logger.error("Error checking approval rate for admin approval: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể kiểm tra tỷ lệ đồng ý: " + e.getMessage());
        }

        // Lấy quỹ (lấy lại để đảm bảo có dữ liệu mới nhất)
        GroupFund fund = groupFundRepository.findById(transaction.getFundId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));

        // Kiểm tra số dư
        if (!fund.hasSufficientBalance(transaction.getAmount())) {
            throw new IllegalStateException("Số dư không đủ để thực hiện giao dịch này");
        }

        // Phê duyệt và hoàn tất
        transaction.setApprovedBy(request.getAdminId());
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.complete(); // Chuyển sang Completed

        // Cập nhật quỹ
        fund.withdraw(transaction.getAmount());
        groupFundRepository.save(fund);

        // Lưu giao dịch
        FundTransaction saved = transactionRepository.save(transaction);
        logger.info("Transaction approved by admin: transactionId={}, adminId={}, amount={}", 
            transaction.getTransactionId(), request.getAdminId(), transaction.getAmount());

        return saved;
    }

    @Override
    @Transactional
    public FundTransaction rejectWithdrawRequest(ApproveRequestDto request) {
        // Lấy giao dịch
        FundTransaction transaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

        // Từ chối
        transaction.reject(request.getAdminId());
        if (request.getNote() != null) {
            transaction.setPurpose(transaction.getPurpose() + " [Từ chối: " + request.getNote() + "]");
        }

        FundTransaction saved = transactionRepository.save(transaction);
        logger.info("Transaction rejected: transactionId={}, adminId={}", 
            transaction.getTransactionId(), request.getAdminId());

        return saved;
    }

    @Override
    @Transactional
    public FundTransaction cancelWithdrawRequest(Integer transactionId, Integer userId) {
        // Lấy giao dịch
        FundTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

        // Kiểm tra trạng thái
        if (transaction.getStatus() != TransactionStatus.Pending) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu đang ở trạng thái Pending");
        }

        // Kiểm tra quyền: chỉ người tạo yêu cầu mới có thể hủy
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền hủy yêu cầu này");
        }

        // Kiểm tra loại giao dịch: chỉ có thể hủy withdrawal request
        if (transaction.getTransactionType() != TransactionType.Withdraw) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu rút tiền");
        }

        // Xóa tất cả votes liên quan
        List<TransactionVote> votes = voteRepository.findByTransactionId(transactionId);
        if (!votes.isEmpty()) {
            voteRepository.deleteAll(votes);
            logger.info("🗑️ Deleted {} votes for transaction {}", votes.size(), transactionId);
        }

        // Xóa transaction khỏi database
        transactionRepository.delete(transaction);
        logger.info("✅ Transaction {} deleted by user: userId={}", transactionId, userId);

        // Trả về null vì transaction đã bị xóa
        return null;
    }

    // ========================================
    // USER VOTE CHO WITHDRAWAL REQUEST
    // ========================================

    @Override
    @Transactional
    public FundTransaction voteOnWithdrawRequest(VoteRequestDto request) {
        // Lấy giao dịch
        FundTransaction transaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));

        // Kiểm tra trạng thái
        if (transaction.getStatus() != TransactionStatus.Pending) {
            throw new IllegalStateException("Chỉ có thể vote cho giao dịch đang Pending");
        }

        // Kiểm tra không phải là người tạo request
        if (transaction.getUserId().equals(request.getUserId())) {
            throw new IllegalStateException("Bạn không thể vote cho yêu cầu của chính mình");
        }

        // Kiểm tra đã vote chưa
        Optional<TransactionVote> existingVote = voteRepository.findByTransactionIdAndUserId(
            request.getTransactionId(), request.getUserId());
        if (existingVote.isPresent()) {
            throw new IllegalStateException("Bạn đã vote cho yêu cầu này rồi");
        }

        // Lưu vote
        TransactionVote vote = new TransactionVote();
        vote.setTransactionId(request.getTransactionId());
        vote.setUserId(request.getUserId());
        vote.setApprove(request.getApprove());
        vote.setNote(request.getNote());
        vote.setVotedAt(LocalDateTime.now());
        voteRepository.save(vote);
        logger.info("User voted {}: transactionId={}, userId={}", 
            request.getApprove() ? "approve" : "reject",
            request.getTransactionId(), request.getUserId());

        // Sau khi vote (cả approve và reject), kiểm tra và xử lý transaction
        processPendingTransaction(transaction);
        
        // Nếu transaction đã bị xóa (do >50% reject), return null
        // Cần reload từ database để kiểm tra
        Optional<FundTransaction> reloaded = transactionRepository.findById(transaction.getTransactionId());
        if (reloaded.isEmpty()) {
            logger.info("Transaction {} has been deleted, returning null", transaction.getTransactionId());
            return null;
        }
        
        return reloaded.get();
    }

    /**
     * Xử lý pending transaction: kiểm tra vote và tự động hoàn tất hoặc từ chối
     */
    @Transactional
    private void processPendingTransaction(FundTransaction transaction) {
        try {
            // Chỉ xử lý các transaction đang pending hoặc approved
            if (transaction.getStatus() != TransactionStatus.Pending && 
                transaction.getStatus() != TransactionStatus.Approved) {
                return;
            }
            
            // Chỉ xử lý withdrawal requests
            if (transaction.getTransactionType() != TransactionType.Withdraw) {
                return;
            }
            
            // Lấy số thành viên nhóm để tính tỷ lệ
            GroupFund fund = groupFundRepository.findById(transaction.getFundId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
            
            int totalMembers = getGroupMemberCount(fund.getGroupId());
            logger.info("Processing pending transaction: transactionId={}, totalMembers={}, currentStatus={}", 
                transaction.getTransactionId(), totalMembers, transaction.getStatus());
            
            if (totalMembers <= 0) {
                logger.warn("Invalid totalMembers for transaction {}: {}", transaction.getTransactionId(), totalMembers);
                return;
            }
            
            // Đếm số phiếu đồng ý và từ chối
            long approveCount = voteRepository.countApprovesByTransactionId(transaction.getTransactionId());
            long rejectCount = voteRepository.countRejectsByTransactionId(transaction.getTransactionId());
            
            // Tính tỷ lệ
            double approvalRate = (double) approveCount / totalMembers;
            double rejectionRate = (double) rejectCount / totalMembers;
            
            logger.info("Vote check: transactionId={}, approveCount={}, rejectCount={}, totalMembers={}, " +
                "approvalRate={}%, rejectionRate={}%", 
                transaction.getTransactionId(), approveCount, rejectCount, totalMembers, 
                String.format("%.2f", approvalRate * 100), String.format("%.2f", rejectionRate * 100));
            
            // Nếu > 50% từ chối, xóa transaction và votes
            if (rejectionRate > 0.5) {
                Integer transactionId = transaction.getTransactionId();
                
                // Xóa tất cả votes liên quan
                List<TransactionVote> votes = voteRepository.findByTransactionId(transactionId);
                if (!votes.isEmpty()) {
                    voteRepository.deleteAll(votes);
                    logger.info("🗑️ Deleted {} votes for transaction {}", votes.size(), transactionId);
                }
                
                // Xóa transaction
                transactionRepository.delete(transaction);
                
                logger.info("✅ Transaction {} deleted due to >50% reject votes ({}%)", 
                    transactionId, String.format("%.2f", rejectionRate * 100));
                return;
            }
            
            // Nếu > 50% đồng ý, tự động trừ tiền và hoàn tất
            if (approvalRate > 0.5) {
                // Kiểm tra số dư trước khi trừ
                if (fund.hasSufficientBalance(transaction.getAmount())) {
                    // Trừ tiền quỹ
                    fund.withdraw(transaction.getAmount());
                    groupFundRepository.save(fund);
                    
                    // Chuyển status sang Completed
                    transaction.setStatus(TransactionStatus.Completed);
                    transaction.setApprovedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);
                    
                    logger.info("✅ Rút tiền thành công và yêu cầu đã được đóng! Transaction {} đã được hoàn tất. " +
                        "Số tiền: {} VND, ApprovalRate: {}% (>50%), Số dư còn lại: {} VND", 
                        transaction.getTransactionId(), 
                        transaction.getAmount(),
                        String.format("%.2f", approvalRate * 100),
                        fund.getCurrentBalance());
                } else {
                    // Số dư không đủ, từ chối
                    transaction.setStatus(TransactionStatus.Rejected);
                    transaction.setPurpose(transaction.getPurpose() + 
                        " [Từ chối: Số dư quỹ không đủ. Hiện có: " + fund.getCurrentBalance() + 
                        " VND, yêu cầu: " + transaction.getAmount() + " VND]");
                    transactionRepository.save(transaction);
                    logger.warn("⚠️ Rút tiền bị từ chối do số dư không đủ. TransactionId: {}, " +
                        "Số dư: {} VND, Yêu cầu: {} VND", 
                        transaction.getTransactionId(), 
                        fund.getCurrentBalance(), 
                        transaction.getAmount());
                }
            } else {
                logger.info("Approval rate not met: approvalRate={}%, required=>50% (strictly greater than half)", 
                    String.format("%.2f", approvalRate * 100));
            }
        } catch (Exception e) {
            logger.error("Error processing pending transaction {}: {}", 
                transaction.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Lấy số thành viên trong nhóm từ group-management-service qua API Gateway
     */
    private int getGroupMemberCount(Integer groupId) {
        try {
            String url = groupManagementServiceUrl + "/api/groups/" + groupId + "/members";
            
            // Tạo headers với X-Internal-Service để bypass authentication trong API Gateway
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "cost-payment-service");
            // Nếu có token, vẫn thêm vào để đảm bảo tương thích
            if (internalServiceToken != null && !internalServiceToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + internalServiceToken);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getBody() != null) {
                return response.getBody().size();
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error getting member count for groupId={}: {}", groupId, e.getMessage());
            return 0;
        }
    }

    @Override
    public List<FundTransaction> getPendingVoteRequestsForUser(Integer userId) {
        // Lấy tất cả pending withdrawal requests từ các funds mà user tham gia
        // Note: Cần lấy từ các nhóm mà user là member
        // Tạm thời trả về tất cả pending withdrawal requests (sẽ filter ở frontend hoặc cần thêm logic)
        return transactionRepository.findPendingWithdrawRequests(null); // null = tất cả funds
    }

    // ========================================
    // LỊCH SỬ GIAO DỊCH
    // ========================================

    @Override
    public List<FundTransaction> getAllTransactions(Integer fundId) {
        return transactionRepository.findByFundIdOrderByDateDesc(fundId);
    }

    @Override
    public List<FundTransaction> getTransactionsByUser(Integer userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Override
    public List<FundTransaction> getTransactionsByType(Integer fundId, String type) {
        TransactionType transactionType = TransactionType.valueOf(type);
        return transactionRepository.findByFundIdAndTransactionTypeOrderByDateDesc(fundId, transactionType);
    }

    @Override
    public List<FundTransaction> getTransactionsByDateRange(
        Integer fundId, LocalDateTime startDate, LocalDateTime endDate
    ) {
        return transactionRepository.findByDateRange(fundId, startDate, endDate);
    }

    @Override
    public Optional<FundTransaction> getTransactionById(Integer transactionId) {
        return transactionRepository.findById(transactionId);
    }

    // ========================================
    // THỐNG KÊ
    // ========================================

    @Override
    public Double getTotalDeposit(Integer fundId) {
        return transactionRepository.getTotalDeposit(fundId);
    }

    @Override
    public Double getTotalWithdraw(Integer fundId) {
        return transactionRepository.getTotalWithdraw(fundId);
    }

    @Override
    public Double getCurrentBalance(Integer fundId) {
        GroupFund fund = groupFundRepository.findById(fundId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy quỹ"));
        return fund.getCurrentBalance();
    }

    @Override
    public Long countPendingRequests(Integer fundId) {
        return transactionRepository.countPendingTransactions(fundId);
    }

    // ========================================
    // VOTE COUNT
    // ========================================

    @Override
    public long countApprovesByTransactionId(Integer transactionId) {
        return voteRepository.countApprovesByTransactionId(transactionId);
    }

    @Override
    public long countRejectsByTransactionId(Integer transactionId) {
        return voteRepository.countRejectsByTransactionId(transactionId);
    }

    @Override
    public long countVotesByTransactionId(Integer transactionId) {
        return voteRepository.countByTransactionId(transactionId);
    }

    @Override
    @Transactional
    public int processAllPendingTransactions() {
        try {
            // Lấy tất cả pending withdrawal requests
            List<FundTransaction> pendingTransactions = transactionRepository.findPendingWithdrawRequests(null);
            logger.info("Processing {} pending withdrawal transactions", pendingTransactions.size());
            
            int processedCount = 0;
            for (FundTransaction transaction : pendingTransactions) {
                try {
                    // Reload transaction để đảm bảo có dữ liệu mới nhất
                    Optional<FundTransaction> reloaded = transactionRepository.findById(transaction.getTransactionId());
                    if (reloaded.isPresent()) {
                        FundTransaction currentTransaction = reloaded.get();
                        // Chỉ xử lý nếu vẫn còn pending hoặc approved
                        if (currentTransaction.getStatus() == TransactionStatus.Pending || 
                            currentTransaction.getStatus() == TransactionStatus.Approved) {
                            processPendingTransaction(currentTransaction);
                            processedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
                }
            }
            
            logger.info("Processed {} pending transactions", processedCount);
            return processedCount;
        } catch (Exception e) {
            logger.error("Error processing all pending transactions: {}", e.getMessage(), e);
            return 0;
        }
    }
}

