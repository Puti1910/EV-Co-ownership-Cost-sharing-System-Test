package com.example.costpayment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled service để tự động xử lý các pending withdrawal requests
 * Kiểm tra và xử lý các transaction đã có đủ votes từ trước
 */
@Component
public class VoteProcessingScheduledService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoteProcessingScheduledService.class);
    
    @Autowired
    private FundService fundService;
    
    /**
     * Tự động xử lý các pending transactions mỗi 30 giây
     * Đảm bảo các transaction đã có đủ votes sẽ được xử lý ngay cả khi code mới deploy
     */
    @Scheduled(fixedRate = 30000) // 30 giây
    public void processPendingTransactions() {
        try {
            int processedCount = fundService.processAllPendingTransactions();
            if (processedCount > 0) {
                logger.info("✅ Scheduled job processed {} pending transactions", processedCount);
            }
        } catch (Exception e) {
            logger.error("Error in scheduled job to process pending transactions: {}", e.getMessage(), e);
        }
    }
}

