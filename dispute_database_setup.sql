-- ===============================
-- DATABASE: Dispute_Management_DB
-- Quản lý Tranh chấp & Giám sát
-- ===============================

DROP DATABASE IF EXISTS Dispute_Management_DB;
CREATE DATABASE Dispute_Management_DB;
USE Dispute_Management_DB;

-- ==========================================
-- BẢNG CHÍNH
-- ==========================================

-- 1. Tranh chấp (Dispute)
CREATE TABLE Dispute (
    disputeId INT AUTO_INCREMENT PRIMARY KEY,
    groupId INT NOT NULL COMMENT 'ID nhóm liên quan',
    vehicleId INT COMMENT 'ID xe liên quan (nếu có)',
    reservationId INT COMMENT 'ID đặt lịch liên quan (nếu có)',
    costId INT COMMENT 'ID chi phí liên quan (nếu có)',
    paymentId INT COMMENT 'ID thanh toán liên quan (nếu có)',
    
    -- Thông tin người tạo tranh chấp
    createdBy INT NOT NULL COMMENT 'User ID người tạo',
    reportedUserId INT COMMENT 'User ID người bị báo cáo (nếu có)',
    
    -- Thông tin tranh chấp
    title VARCHAR(255) NOT NULL COMMENT 'Tiêu đề tranh chấp',
    description TEXT NOT NULL COMMENT 'Mô tả chi tiết',
    category ENUM(
        'RESERVATION',      -- Tranh chấp về đặt lịch
        'COST_SHARING',    -- Tranh chấp về chia chi phí
        'VEHICLE_DAMAGE',  -- Hư hỏng xe
        'USAGE_VIOLATION', -- Vi phạm quy định sử dụng
        'PAYMENT',         -- Thanh toán
        'OWNERSHIP',       -- Quyền sở hữu
        'OTHER'            -- Khác
    ) NOT NULL DEFAULT 'OTHER',
    
    -- Trạng thái
    status ENUM(
        'PENDING',         -- Chờ xử lý
        'IN_REVIEW',       -- Đang xem xét
        'RESOLVED',        -- Đã giải quyết
        'CLOSED',          -- Đã đóng
        'ESCALATED'        -- Đã chuyển lên cấp cao
    ) NOT NULL DEFAULT 'PENDING',
    
    -- Độ ưu tiên
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    
    -- Thông tin xử lý
    assignedTo INT COMMENT 'Staff/Admin ID được giao xử lý',
    resolvedBy INT COMMENT 'Staff/Admin ID đã giải quyết',
    resolutionNote TEXT COMMENT 'Ghi chú giải quyết',
    
    -- Timestamps
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolvedAt DATETIME COMMENT 'Thời gian giải quyết',
    closedAt DATETIME COMMENT 'Thời gian đóng'
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bình luận/Trao đổi về tranh chấp (DisputeComment)
CREATE TABLE DisputeComment (
    commentId INT AUTO_INCREMENT PRIMARY KEY,
    disputeId INT NOT NULL,
    userId INT NOT NULL COMMENT 'User ID người bình luận',
    userRole ENUM('CO_OWNER', 'STAFF', 'ADMIN') NOT NULL DEFAULT 'CO_OWNER',
    content TEXT NOT NULL COMMENT 'Nội dung bình luận',
    isInternal BOOLEAN DEFAULT FALSE COMMENT 'TRUE = chỉ Admin/Staff thấy, FALSE = tất cả thấy',
    attachments JSON COMMENT 'Danh sách file đính kèm (URLs)',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (disputeId) REFERENCES Dispute(disputeId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Giải pháp/Quyết định (DisputeResolution)
CREATE TABLE DisputeResolution (
    resolutionId INT AUTO_INCREMENT PRIMARY KEY,
    disputeId INT NOT NULL UNIQUE,
    resolvedBy INT NOT NULL COMMENT 'Staff/Admin ID',
    resolutionType ENUM(
        'ACCEPTED',        -- Chấp nhận yêu cầu
        'REJECTED',        -- Từ chối yêu cầu
        'COMPROMISE',      -- Thỏa hiệp
        'REFUND',          -- Hoàn tiền
        'PENALTY',         -- Phạt
        'WARNING',         -- Cảnh báo
        'OTHER'            -- Khác
    ) NOT NULL,
    resolutionDetails TEXT NOT NULL COMMENT 'Chi tiết giải pháp',
    actionTaken TEXT COMMENT 'Hành động đã thực hiện',
    compensationAmount DOUBLE DEFAULT 0 COMMENT 'Số tiền bồi thường (nếu có)',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (disputeId) REFERENCES Dispute(disputeId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Lịch sử thay đổi trạng thái (DisputeHistory)
CREATE TABLE DisputeHistory (
    historyId INT AUTO_INCREMENT PRIMARY KEY,
    disputeId INT NOT NULL,
    changedBy INT NOT NULL COMMENT 'User ID thay đổi',
    oldStatus VARCHAR(50),
    newStatus VARCHAR(50),
    changeNote TEXT COMMENT 'Ghi chú thay đổi',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (disputeId) REFERENCES Dispute(disputeId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. File đính kèm (DisputeAttachment)
CREATE TABLE DisputeAttachment (
    attachmentId INT AUTO_INCREMENT PRIMARY KEY,
    disputeId INT NOT NULL,
    commentId INT COMMENT 'Nếu NULL thì là file của dispute chính',
    fileName VARCHAR(255) NOT NULL,
    fileUrl VARCHAR(500) NOT NULL,
    fileType VARCHAR(50) COMMENT 'image, pdf, document, etc.',
    fileSize BIGINT COMMENT 'Size in bytes',
    uploadedBy INT NOT NULL,
    uploadedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (disputeId) REFERENCES Dispute(disputeId) ON DELETE CASCADE,
    FOREIGN KEY (commentId) REFERENCES DisputeComment(commentId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- INDEXES để tối ưu truy vấn
-- ==========================================

CREATE INDEX idx_dispute_group ON Dispute(groupId);
CREATE INDEX idx_dispute_status ON Dispute(status);
CREATE INDEX idx_dispute_priority ON Dispute(priority);
CREATE INDEX idx_dispute_created_by ON Dispute(createdBy);
CREATE INDEX idx_dispute_assigned_to ON Dispute(assignedTo);
CREATE INDEX idx_dispute_created_at ON Dispute(createdAt);
CREATE INDEX idx_comment_dispute ON DisputeComment(disputeId);
CREATE INDEX idx_history_dispute ON DisputeHistory(disputeId);

-- ==========================================
-- DỮ LIỆU MẪU
-- ==========================================

-- Tranh chấp mẫu
INSERT INTO Dispute (groupId, vehicleId, createdBy, reportedUserId, title, description, category, status, priority, assignedTo) VALUES 
(1, 1, 2, 3, 'Tranh chấp về việc sử dụng xe quá giờ', 
 'Người dùng 3 đã sử dụng xe vượt quá thời gian đã đặt lịch, gây ảnh hưởng đến lịch trình của tôi.',
 'RESERVATION', 'PENDING', 'HIGH', NULL),
 
(1, 1, 1, 2, 'Tranh chấp về chi phí sạc điện',
 'Chi phí sạc điện được tính không đúng, tôi đã sạc ít hơn nhưng bị tính nhiều hơn.',
 'COST_SHARING', 'IN_REVIEW', 'MEDIUM', 1),
 
(2, 2, 4, 2, 'Phát hiện hư hỏng xe sau khi sử dụng',
 'Sau khi nhận xe, tôi phát hiện có vết xước trên cửa xe. Người dùng trước đó không báo cáo.',
 'VEHICLE_DAMAGE', 'PENDING', 'URGENT', NULL);

-- Bình luận mẫu
INSERT INTO DisputeComment (disputeId, userId, userRole, content, isInternal) VALUES 
(1, 2, 'CO_OWNER', 'Tôi đã liên hệ với người dùng 3 nhưng chưa nhận được phản hồi.', FALSE),
(1, 1, 'ADMIN', 'Đang xem xét vấn đề này. Sẽ liên hệ với cả hai bên để làm rõ.', FALSE),
(2, 1, 'ADMIN', 'Đã kiểm tra hóa đơn sạc điện. Sẽ điều chỉnh lại chi phí.', FALSE),
(3, 2, 'CO_OWNER', 'Tôi không nhớ có va chạm gì khi sử dụng xe.', FALSE);

-- Lịch sử mẫu
INSERT INTO DisputeHistory (disputeId, changedBy, oldStatus, newStatus, changeNote) VALUES 
(1, 2, NULL, 'PENDING', 'Tạo tranh chấp mới'),
(2, 1, 'PENDING', 'IN_REVIEW', 'Admin bắt đầu xem xét'),
(3, 4, NULL, 'PENDING', 'Tạo tranh chấp mới');

-- ==========================================
-- VIEWS tiện lợi
-- ==========================================

-- View: Tranh chấp với thông tin chi tiết
CREATE OR REPLACE VIEW DisputeDetailView AS
SELECT 
    d.disputeId,
    d.groupId,
    d.vehicleId,
    d.reservationId,
    d.costId,
    d.createdBy,
    d.reportedUserId,
    d.title,
    d.description,
    d.category,
    d.status,
    d.priority,
    d.assignedTo,
    d.resolvedBy,
    d.resolutionNote,
    d.createdAt,
    d.updatedAt,
    d.resolvedAt,
    d.closedAt,
    COUNT(DISTINCT dc.commentId) as commentCount,
    COUNT(DISTINCT da.attachmentId) as attachmentCount
FROM Dispute d
LEFT JOIN DisputeComment dc ON d.disputeId = dc.disputeId
LEFT JOIN DisputeAttachment da ON d.disputeId = da.disputeId AND da.commentId IS NULL
GROUP BY d.disputeId;

-- View: Thống kê tranh chấp theo trạng thái
CREATE OR REPLACE VIEW DisputeStatusSummary AS
SELECT 
    status,
    priority,
    COUNT(*) as count,
    MIN(createdAt) as oldestDispute,
    MAX(createdAt) as newestDispute
FROM Dispute
GROUP BY status, priority;

-- ==========================================
-- XEM DỮ LIỆU
-- ==========================================

SELECT '=== DANH SÁCH TRANH CHẤP ===' as '';
SELECT 
    disputeId,
    groupId,
    title,
    category,
    status,
    priority,
    DATE_FORMAT(createdAt, '%Y-%m-%d %H:%i') as 'Thời gian tạo'
FROM Dispute
ORDER BY 
    CASE priority 
        WHEN 'URGENT' THEN 1
        WHEN 'HIGH' THEN 2
        WHEN 'MEDIUM' THEN 3
        WHEN 'LOW' THEN 4
    END,
    createdAt DESC;

SELECT '=== THỐNG KÊ THEO TRẠNG THÁI ===' as '';
SELECT 
    status,
    COUNT(*) as 'Số lượng',
    GROUP_CONCAT(DISTINCT priority) as 'Độ ưu tiên'
FROM Dispute
GROUP BY status;

SELECT '=== THỐNG KÊ THEO LOẠI ===' as '';
SELECT 
    category,
    COUNT(*) as 'Số lượng',
    GROUP_CONCAT(DISTINCT status) as 'Trạng thái'
FROM Dispute
GROUP BY category;

SELECT '✅ DISPUTE_MANAGEMENT_DB HOÀN TẤT!' as '';

