package com.example.costpayment.service;

import com.example.costpayment.dto.*;
import com.example.costpayment.entity.FundTransaction;
import com.example.costpayment.entity.GroupFund;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface: Quản lý Quỹ chung
 * Phương án C: Yêu cầu rút tiền + Voting + Phê duyệt
 */
public interface FundService {

    // ========================================
    // QUẢN LÝ QUỸ
    // ========================================

    /**
     * Lấy thông tin quỹ theo fundId
     */
    Optional<GroupFund> getFundById(Integer fundId);

    /**
     * Lấy thông tin quỹ theo groupId
     */
    Optional<GroupFund> getFundByGroupId(Integer groupId);

    /**
     * Tạo quỹ mới cho nhóm
     */
    GroupFund createFundForGroup(Integer groupId);

    /**
     * Lấy tổng quan quỹ
     */
    FundSummaryDto getFundSummary(Integer fundId);

    // ========================================
    // NẠP TIỀN (USER/ADMIN)
    // ========================================

    /**
     * Nạp tiền vào quỹ
     * - Ai cũng có thể nạp
     * - Không cần phê duyệt
     */
    FundTransaction deposit(DepositRequestDto request);

    // ========================================
    // RÚT TIỀN - USER (CẦN VOTE)
    // ========================================

    /**
     * Tạo yêu cầu rút tiền (USER)
     * - Status: Pending
     * - Cần vote để duyệt
     */
    FundTransaction createWithdrawRequest(WithdrawRequestDto request);

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt
     */
    List<FundTransaction> getPendingRequests(Integer fundId);

    // ========================================
    // RÚT TIỀN - ADMIN (TRỰC TIẾP)
    // ========================================

    /**
     * Rút tiền trực tiếp (ADMIN)
     * - Không cần vote
     * - Status: Completed ngay
     */
    FundTransaction adminDirectWithdraw(WithdrawRequestDto request);

    // ========================================
    // PHÊ DUYỆT YÊU CẦU (ADMIN)
    // ========================================

    /**
     * Admin phê duyệt yêu cầu rút tiền
     * - Sau khi vote pass
     * - Hoặc Admin quyết định trực tiếp
     */
    FundTransaction approveWithdrawRequest(ApproveRequestDto request);

    /**
     * Admin từ chối yêu cầu rút tiền
     */
    FundTransaction rejectWithdrawRequest(ApproveRequestDto request);

    /**
     * User hủy yêu cầu rút tiền của chính mình
     * - Chỉ có thể hủy khi status = Pending
     * - Chỉ người tạo yêu cầu mới có thể hủy
     */
    FundTransaction cancelWithdrawRequest(Integer transactionId, Integer userId);

    // ========================================
    // USER VOTE CHO WITHDRAWAL REQUEST
    // ========================================

    /**
     * User vote cho withdrawal request (approve hoặc reject)
     * - User trong cùng nhóm có thể vote
     * - Nếu vote reject: Transaction sẽ bị reject ngay
     * - Nếu vote approve: Transaction chuyển sang Approved (chờ admin xác nhận cuối cùng)
     */
    FundTransaction voteOnWithdrawRequest(VoteRequestDto request);

    /**
     * Lấy danh sách withdrawal requests cần vote của user
     * (các requests từ user khác trong cùng nhóm)
     */
    List<FundTransaction> getPendingVoteRequestsForUser(Integer userId);

    // ========================================
    // LỊCH SỬ GIAO DỊCH
    // ========================================

    /**
     * Lấy tất cả giao dịch của quỹ
     */
    List<FundTransaction> getAllTransactions(Integer fundId);

    /**
     * Lấy giao dịch của user
     */
    List<FundTransaction> getTransactionsByUser(Integer userId);

    /**
     * Lấy giao dịch theo loại
     */
    List<FundTransaction> getTransactionsByType(Integer fundId, String type);

    /**
     * Lấy giao dịch theo khoảng thời gian
     */
    List<FundTransaction> getTransactionsByDateRange(
        Integer fundId, LocalDateTime startDate, LocalDateTime endDate
    );

    /**
     * Lấy chi tiết giao dịch
     */
    Optional<FundTransaction> getTransactionById(Integer transactionId);

    // ========================================
    // THỐNG KÊ
    // ========================================

    /**
     * Tổng tiền nạp
     */
    Double getTotalDeposit(Integer fundId);

    /**
     * Tổng tiền rút
     */
    Double getTotalWithdraw(Integer fundId);

    /**
     * Số dư hiện tại
     */
    Double getCurrentBalance(Integer fundId);

    /**
     * Đếm số yêu cầu chờ duyệt
     */
    Long countPendingRequests(Integer fundId);

    // ========================================
    // VOTE COUNT
    // ========================================

    /**
     * Đếm số phiếu đồng ý cho transaction
     */
    long countApprovesByTransactionId(Integer transactionId);

    /**
     * Đếm số phiếu từ chối cho transaction
     */
    long countRejectsByTransactionId(Integer transactionId);

    /**
     * Đếm tổng số phiếu cho transaction
     */
    long countVotesByTransactionId(Integer transactionId);

    /**
     * Xử lý tất cả pending transactions (kiểm tra vote và tự động hoàn tất hoặc từ chối)
     * Dùng để xử lý các transactions đã có đủ votes từ trước
     */
    int processAllPendingTransactions();
}

