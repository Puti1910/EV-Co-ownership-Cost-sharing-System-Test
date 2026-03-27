-- ===============================
-- DATABASE: Group_Management_DB + Cost_Management_DB
-- Version ĐẦY ĐỦ - Bao gồm Quỹ chung
-- 
-- LƯU Ý: 
-- - Cost_Payment_DB được setup riêng trong file cost_database_setup.sql
-- - Cost_Management_DB (trong file này) dùng cho quỹ chung và có foreign key đến Group_Management_DB
-- ===============================

-- ==========================================
-- PART 1: GROUP MANAGEMENT DATABASE
-- ==========================================

-- Tắt foreign key checks để có thể DROP database khi có cross-database foreign keys
SET FOREIGN_KEY_CHECKS = 0;

-- Xóa các database có foreign key tham chiếu đến Group_Management_DB trước
-- (Cost_Management_DB có foreign key tham chiếu đến Voting trong Group_Management_DB)
DROP DATABASE IF EXISTS Cost_Management_DB;
-- Cost_Payment_DB không có foreign key đến Group_Management_DB nên không cần xóa ở đây
-- (Cost_Payment_DB được setup riêng trong cost_database_setup.sql)

-- Sau đó mới xóa Group_Management_DB
DROP DATABASE IF EXISTS Group_Management_DB;

-- Bật lại foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Tạo lại Group_Management_DB
CREATE DATABASE Group_Management_DB;
USE Group_Management_DB;

-- ==========================================
-- BẢNG CHÍNH
-- ==========================================

-- 1. Nhóm
CREATE TABLE `Group` (
    groupId INT AUTO_INCREMENT PRIMARY KEY,
    groupName VARCHAR(100) NOT NULL,
    adminId INT NULL,
    vehicleId INT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Active', 'Inactive') DEFAULT 'Active'
);

-- 2. Thành viên (THÊM ownershipPercent - Cho chức năng chia theo sở hữu)
CREATE TABLE GroupMember (
    memberId INT AUTO_INCREMENT PRIMARY KEY,
    groupId INT NOT NULL,
    userId INT NOT NULL,
    role ENUM('Admin', 'Member') DEFAULT 'Member',
    ownershipPercent DOUBLE DEFAULT 0,
    joinedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE
);

-- 3. Bỏ phiếu
CREATE TABLE Voting (
    voteId INT AUTO_INCREMENT PRIMARY KEY,
    groupId INT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    optionA VARCHAR(100),
    optionB VARCHAR(100),
    finalResult VARCHAR(100),
    totalVotes INT DEFAULT 0,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    deadline DATETIME NULL COMMENT 'Hạn chót bỏ phiếu',
    status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'Trạng thái bỏ phiếu: OPEN, CLOSED, CANCELLED',
    closedAt DATETIME NULL COMMENT 'Thời điểm đóng bỏ phiếu',
    createdBy INT NULL COMMENT 'User ID tạo bỏ phiếu',
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE
);

-- 4. Kết quả bỏ phiếu
CREATE TABLE VotingResult (
    resultId INT AUTO_INCREMENT PRIMARY KEY,
    voteId INT NOT NULL,
    memberId INT NOT NULL,
    choice ENUM('A','B') NOT NULL,
    votedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (voteId) REFERENCES Voting(voteId) ON DELETE CASCADE,
    FOREIGN KEY (memberId) REFERENCES GroupMember(memberId) ON DELETE CASCADE
);

-- 5. Yêu cầu rời nhóm
CREATE TABLE LeaveRequest (
    requestId INT AUTO_INCREMENT PRIMARY KEY,
    groupId INT NOT NULL,
    memberId INT NOT NULL,
    userId INT NOT NULL,
    reason TEXT,
    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
    requestedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    processedAt DATETIME,
    processedBy INT,
    adminNote TEXT,
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE,
    FOREIGN KEY (memberId) REFERENCES GroupMember(memberId) ON DELETE CASCADE
);

-- ==========================================
-- DỮ LIỆU MẪU ĐƠN GIẢN
-- ==========================================

-- Nhóm
INSERT INTO `Group` (groupName, adminId, vehicleId, status) VALUES 
('EV Group Tesla Model 3', 1, 1, 'Active'),
('EV Group BMW i3', 2, 2, 'Active'),
('CarShare Saigon Rivian R1T', 5, 3, 'Active'),
('Đồng Sở Hữu VinFast VF8', 6, 4, 'Active'),
('EV Adventure Đà Lạt', 7, 5, 'Active');

-- Thành viên với % sở hữu
INSERT INTO GroupMember (groupId, userId, role, ownershipPercent) VALUES 
-- Group 1: Tổng 100%
(1, 1, 'Admin', 50.0),   -- User 1: 50%
(1, 2, 'Member', 30.0),  -- User 2: 30%
(1, 3, 'Member', 20.0),  -- User 3: 20%

-- Group 2: Để trống 20% cho người mới
(2, 2, 'Admin', 40.0),   -- User 2: 40%
(2, 4, 'Member', 40.0),  -- User 4: 40%

-- Group 3: Còn 30%
(3, 5, 'Admin', 35.0),
(3, 6, 'Member', 35.0),

-- Group 4: Chỉ có admin, còn 80%
(4, 6, 'Admin', 20.0),

-- Group 5: Để trống 100%
(5, 7, 'Admin', 0.0);

-- Bỏ phiếu
INSERT INTO Voting (groupId, topic, optionA, optionB, totalVotes) VALUES 
(1, 'Có nên mua phụ kiện?', 'Có', 'Không', 0),
(2, 'Có nên đổi màu xe?', 'Đổi', 'Giữ', 0),
(3, 'Chọn lịch bảo dưỡng tháng 12?', 'Tuần 1', 'Tuần 2', 0);

-- ==========================================
-- XEM DỮ LIỆU
-- ==========================================

SELECT '=== NHÓM VÀ THÀNH VIÊN ===' as '';
SELECT 
    g.groupId,
    g.groupName,
    gm.userId,
    gm.role,
    CONCAT(gm.ownershipPercent,'%') as 'Sở hữu'
FROM GroupMember gm JOIN `Group` g ON gm.groupId=g.groupId
ORDER BY gm.groupId, gm.ownershipPercent DESC;

SELECT '=== KIỂM TRA TỔNG % ===' as '';
SELECT 
    groupId,
    SUM(ownershipPercent) as 'Tổng',
    IF(SUM(ownershipPercent)=100, '✅ OK', '❌ Sai') as 'Status'
FROM GroupMember GROUP BY groupId;

SELECT '✅ GROUP_MANAGEMENT_DB HOÀN TẤT!' as '';

-- ==========================================
-- PART 2: COST MANAGEMENT DATABASE
-- ==========================================

-- Tắt foreign key checks để có thể tạo cross-database foreign key
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS Cost_Management_DB;
USE Cost_Management_DB;

-- ==========================================
-- BẢNG QUỸ CHUNG
-- ==========================================

-- 1. Quỹ nhóm
CREATE TABLE GroupFund (
    fundId INT AUTO_INCREMENT PRIMARY KEY,
    groupId INT NOT NULL UNIQUE COMMENT 'ID nhóm từ Group_Management_DB',
    totalContributed DOUBLE DEFAULT 0 COMMENT 'Tổng tiền đã đóng góp',
    currentBalance DOUBLE DEFAULT 0 COMMENT 'Số dư hiện tại',
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    note TEXT COMMENT 'Ghi chú'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Giao dịch quỹ (CÓ HỆ THỐNG PHÊ DUYỆT)
CREATE TABLE FundTransaction (
    transactionId INT AUTO_INCREMENT PRIMARY KEY,
    fundId INT NOT NULL,
    userId INT NOT NULL COMMENT 'ID người thực hiện',
    transactionType ENUM('Deposit','Withdraw') NOT NULL COMMENT 'Loại: Nạp/Rút',
    amount DOUBLE NOT NULL COMMENT 'Số tiền',
    purpose TEXT COMMENT 'Mục đích',
    date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    
    -- HỆ THỐNG PHÊ DUYỆT (Phương án C)
    status ENUM('Pending','Approved','Rejected','Completed') 
        DEFAULT 'Completed' 
        COMMENT 'Pending=Chờ vote, Approved=Đã duyệt, Rejected=Từ chối, Completed=Hoàn tất',
    approvedBy INT 
        COMMENT 'UserId của Admin phê duyệt',
    approvedAt DATETIME 
        COMMENT 'Thời gian phê duyệt',
    voteId INT 
        COMMENT 'ID của vote liên quan',
    receiptUrl VARCHAR(500) 
        COMMENT 'Link hóa đơn/chứng từ',
    
    FOREIGN KEY (fundId) REFERENCES GroupFund(fundId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo Foreign Key nối với Voting (cross-database)
ALTER TABLE FundTransaction
ADD CONSTRAINT fk_fundtransaction_vote
FOREIGN KEY (`voteId`) REFERENCES Group_Management_DB.Voting(`voteId`) 
ON DELETE SET NULL;

-- 3. View tiện lợi: Giao dịch kèm thông tin vote
CREATE OR REPLACE VIEW FundTransactionWithVote AS
SELECT 
    ft.transactionId,
    ft.fundId,
    ft.userId,
    ft.transactionType,
    ft.amount,
    ft.purpose,
    ft.date,
    ft.status,
    ft.approvedBy,
    ft.approvedAt,
    ft.voteId,
    ft.receiptUrl,
    v.topic as voteTopic,
    v.finalResult as voteResult,
    v.totalVotes
FROM FundTransaction ft
LEFT JOIN Group_Management_DB.Voting v ON ft.voteId = v.voteId;

-- ==========================================
-- DỮ LIỆU MẪU - QUỸ CHUNG
-- ==========================================

-- Tạo quỹ cho các nhóm
INSERT INTO GroupFund (groupId, totalContributed, currentBalance, note) VALUES 
(1, 5000000, 3500000, 'Quỹ chung EV Group Tesla Model 3'),
(2, 3000000, 2800000, 'Quỹ chung EV Group BMW i3'),
(3, 1500000, 1200000, 'Quỹ chung CarShare Saigon Rivian'),
(4, 0, 0, 'Quỹ chung VinFast VF8 - chờ đóng góp'),
(5, 0, 0, 'Quỹ EV Adventure Đà Lạt - nhóm mới');

-- Giao dịch mẫu
INSERT INTO FundTransaction (fundId, userId, transactionType, amount, purpose, status, approvedAt) VALUES 
-- Group 1
(1, 1, 'Deposit', 2000000, 'Nạp quỹ ban đầu', 'Completed', '2025-10-01 10:00:00'),
(1, 2, 'Deposit', 1500000, 'Đóng góp tháng 10', 'Completed', '2025-10-05 14:30:00'),
(1, 3, 'Deposit', 1500000, 'Đóng góp tháng 10', 'Completed', '2025-10-05 15:00:00'),
(1, 1, 'Withdraw', 1000000, 'Mua bảo hiểm xe', 'Completed', '2025-10-15 09:00:00'),
(1, 2, 'Withdraw', 500000, 'Bảo dưỡng định kỳ', 'Completed', '2025-10-20 11:30:00'),

-- Group 2
(2, 2, 'Deposit', 1800000, 'Nạp quỹ ban đầu', 'Completed', '2025-10-01 11:00:00'),
(2, 4, 'Deposit', 1200000, 'Đóng góp tháng 10', 'Completed', '2025-10-05 16:00:00'),
(2, 2, 'Withdraw', 200000, 'Đổ xăng', 'Completed', '2025-10-10 08:00:00'),

-- Group 3
(3, 5, 'Deposit', 1000000, 'Góp vốn mở rộng', 'Completed', '2025-10-02 09:00:00'),
(3, 6, 'Deposit', 500000, 'Đóng góp tháng 10', 'Completed', '2025-10-06 12:00:00');

-- Yêu cầu rút tiền đang chờ duyệt (Pending)
INSERT INTO FundTransaction (fundId, userId, transactionType, amount, purpose, status, receiptUrl) VALUES 
(1, 3, 'Withdraw', 800000, 'Sửa chữa đột xuất', 'Pending', 'https://example.com/receipt1.pdf');

-- ==========================================
-- XEM DỮ LIỆU COST MANAGEMENT
-- ==========================================

SELECT '=== TỔNG QUAN QUỸ ===' as '';
SELECT 
    gf.fundId,
    gf.groupId,
    CONCAT(FORMAT(gf.totalContributed, 0), ' VND') as 'Tổng đóng góp',
    CONCAT(FORMAT(gf.currentBalance, 0), ' VND') as 'Số dư hiện tại',
    gf.note
FROM GroupFund gf;

SELECT '=== LỊCH SỬ GIAO DỊCH ===' as '';
SELECT 
    transactionId,
    fundId,
    userId,
    transactionType as 'Loại',
    CONCAT(FORMAT(amount, 0), ' VND') as 'Số tiền',
    purpose as 'Mục đích',
    status as 'Trạng thái',
    DATE_FORMAT(date, '%Y-%m-%d %H:%i') as 'Thời gian'
FROM FundTransaction
ORDER BY date DESC;

SELECT '=== YÊU CẦU CHỜ DUYỆT ===' as '';
SELECT 
    transactionId,
    fundId,
    userId,
    CONCAT(FORMAT(amount, 0), ' VND') as 'Số tiền',
    purpose as 'Mục đích',
    DATE_FORMAT(date, '%Y-%m-%d %H:%i') as 'Thời gian'
FROM FundTransaction
WHERE status = 'Pending';

SELECT '=== THỐNG KÊ THEO NHÓM ===' as '';
SELECT 
    fundId,
    transactionType as 'Loại',
    COUNT(*) as 'Số giao dịch',
    CONCAT(FORMAT(SUM(amount), 0), ' VND') as 'Tổng tiền'
FROM FundTransaction
WHERE status = 'Completed'
GROUP BY fundId, transactionType
ORDER BY fundId, transactionType;

SELECT '✅ COST_MANAGEMENT_DB HOÀN TẤT!' as '';

-- Bật lại foreign key checks sau khi tạo xong tất cả
SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- GROUP_Management_DB - Contract Extensions
-- ==========================================
-- Run this script after the base group_database_setup.sql
-- to add contract management tables that enforce the
-- “sign before join” rule for vehicle co-ownership groups.

USE Group_Management_DB;

-- 1. Nhóm hợp đồng
CREATE TABLE IF NOT EXISTS GroupContract (
    contract_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    contract_code VARCHAR(100) NOT NULL UNIQUE,
    contract_content TEXT,
    contract_status ENUM('pending', 'signed', 'archived') DEFAULT 'pending',
    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    signed_date DATETIME NULL,
    created_by INT,
    CONSTRAINT fk_group_contract_group
        FOREIGN KEY (group_id) REFERENCES `Group`(groupId)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_group_contract_group_id ON GroupContract(group_id);

-- 2. Chữ ký hợp đồng
CREATE TABLE IF NOT EXISTS ContractSignature (
    signature_id INT AUTO_INCREMENT PRIMARY KEY,
    contract_id INT NOT NULL,
    user_id INT NOT NULL,
    signed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    signature_method VARCHAR(50),
    ip_address VARCHAR(45),
    CONSTRAINT fk_contract_signature_contract
        FOREIGN KEY (contract_id) REFERENCES GroupContract(contract_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_contract_signature UNIQUE (contract_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_contract_signature_user ON ContractSignature(user_id);

-- Sample contract for demo groups
INSERT INTO GroupContract (group_id, contract_code, contract_content, contract_status, created_by)
SELECT g.groupId,
       CONCAT('LC-', UPPER(REPLACE(g.groupName, ' ', '')), '-', YEAR(NOW())),
       CONCAT('Hợp đồng sở hữu chung cho nhóm ', g.groupName),
       'pending',
       g.adminId
FROM `Group` g
LEFT JOIN GroupContract c ON c.group_id = g.groupId
WHERE c.contract_id IS NULL;

SELECT '✅ Contract tables created/updated successfully.' AS '';


