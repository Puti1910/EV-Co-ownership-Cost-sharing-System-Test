-- ===============================
-- DATABASE: Group_Management_DB + Cost_Management_DB
-- Version 2.0 (BIGINT IDs & 1-N Support)
-- ===============================

-- Tắt foreign key checks để có thể DROP database khi có cross-database foreign keys
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS Cost_Management_DB;
DROP DATABASE IF EXISTS Group_Management_DB;

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- PART 1: GROUP MANAGEMENT DATABASE
-- ==========================================

CREATE DATABASE Group_Management_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Group_Management_DB;

-- 1. Nhóm
CREATE TABLE `Group` (
    groupId BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupName VARCHAR(100) NOT NULL,
    adminId BIGINT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Active', 'Inactive') DEFAULT 'Active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Thành viên
CREATE TABLE GroupMember (
    memberId BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    role ENUM('Admin', 'Member') DEFAULT 'Member',
    ownershipPercent DOUBLE DEFAULT 0,
    joinedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bỏ phiếu
CREATE TABLE Voting (
    voteId BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupId BIGINT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    optionA VARCHAR(100),
    optionB VARCHAR(100),
    finalResult VARCHAR(100),
    totalVotes INT DEFAULT 0,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    deadline DATETIME NULL COMMENT 'Hạn chót bỏ phiếu',
    status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'Trạng thái bỏ phiếu: OPEN, CLOSED, CANCELLED',
    closedAt DATETIME NULL COMMENT 'Thời điểm đóng bỏ phiếu',
    createdBy BIGINT NULL COMMENT 'User ID tạo bỏ phiếu',
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Kết quả bỏ phiếu
CREATE TABLE VotingResult (
    resultId BIGINT AUTO_INCREMENT PRIMARY KEY,
    voteId BIGINT NOT NULL,
    memberId BIGINT NOT NULL,
    choice ENUM('A','B') NOT NULL,
    votedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (voteId) REFERENCES Voting(voteId) ON DELETE CASCADE,
    FOREIGN KEY (memberId) REFERENCES GroupMember(memberId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Yêu cầu rời nhóm
CREATE TABLE LeaveRequest (
    requestId BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupId BIGINT NOT NULL,
    memberId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    reason TEXT,
    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
    requestedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    processedAt DATETIME,
    processedBy BIGINT,
    adminNote TEXT,
    FOREIGN KEY (groupId) REFERENCES `Group`(groupId) ON DELETE CASCADE,
    FOREIGN KEY (memberId) REFERENCES GroupMember(memberId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Hợp đồng nhóm
CREATE TABLE GroupContract (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    contract_code VARCHAR(100) NOT NULL UNIQUE,
    contract_content TEXT,
    contract_status ENUM('pending', 'signed', 'archived') DEFAULT 'pending',
    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    signed_date DATETIME NULL,
    created_by BIGINT,
    CONSTRAINT fk_group_contract_group FOREIGN KEY (group_id) REFERENCES `Group`(groupId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Chữ ký hợp đồng
CREATE TABLE ContractSignature (
    signature_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    signed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    signature_method VARCHAR(50),
    ip_address VARCHAR(45),
    CONSTRAINT fk_contract_signature_contract FOREIGN KEY (contract_id) REFERENCES GroupContract(contract_id) ON DELETE CASCADE,
    CONSTRAINT uq_contract_signature UNIQUE (contract_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ==========================================
-- PART 2: COST MANAGEMENT DATABASE
-- ==========================================

CREATE DATABASE Cost_Management_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Cost_Management_DB;

-- 1. Quỹ nhóm
CREATE TABLE GroupFund (
    fundId BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupId BIGINT NOT NULL UNIQUE COMMENT 'ID nhóm từ Group_Management_DB',
    totalContributed DOUBLE DEFAULT 0 COMMENT 'Tổng tiền đã đóng góp',
    currentBalance DOUBLE DEFAULT 0 COMMENT 'Số dư hiện tại',
    updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    note TEXT COMMENT 'Ghi chú'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Giao dịch quỹ
CREATE TABLE FundTransaction (
    transactionId BIGINT AUTO_INCREMENT PRIMARY KEY,
    fundId BIGINT NOT NULL,
    userId BIGINT NOT NULL COMMENT 'ID người thực hiện',
    transactionType ENUM('Deposit','Withdraw') NOT NULL COMMENT 'Loại: Nạp/Rút',
    amount DOUBLE NOT NULL COMMENT 'Số tiền',
    purpose TEXT COMMENT 'Mục đích',
    date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    status ENUM('Pending','Approved','Rejected','Completed') DEFAULT 'Completed',
    approvedBy BIGINT COMMENT 'UserId của Admin phê duyệt',
    approvedAt DATETIME COMMENT 'Thời gian phê duyệt',
    voteId BIGINT COMMENT 'ID của vote liên quan',
    receiptUrl VARCHAR(500) COMMENT 'Link hóa đơn/chứng từ',
    FOREIGN KEY (fundId) REFERENCES GroupFund(fundId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tạo Foreign Key nối với Voting (cross-database)
ALTER TABLE FundTransaction
ADD CONSTRAINT fk_fundtransaction_vote
FOREIGN KEY (`voteId`) REFERENCES Group_Management_DB.Voting(`voteId`) 
ON DELETE SET NULL;


-- ==========================================
-- DỮ LIỆU MẪU
-- ==========================================

USE Group_Management_DB;

INSERT INTO `Group` (groupName, adminId, status) VALUES 
('EV Group Tesla Model 3', 1, 'Active'),
('EV Group BMW i3', 2, 'Active');

INSERT INTO GroupMember (groupId, userId, role, ownershipPercent) VALUES 
(1, 1, 'Admin', 50.0),
(1, 2, 'Member', 30.0),
(1, 3, 'Member', 20.0),
(2, 2, 'Admin', 60.0),
(2, 4, 'Member', 40.0);

INSERT INTO GroupContract (group_id, contract_code, contract_content, contract_status, created_by) VALUES
(1, 'LC-TESLA3-2025', 'Hợp đồng sở hữu chung Tesla Model 3', 'pending', 1),
(2, 'LC-BMWI3-2025', 'Hợp đồng sở hữu chung BMW i3', 'pending', 2);

USE Cost_Management_DB;
INSERT INTO GroupFund (groupId, totalContributed, currentBalance, note) VALUES 
(1, 5000000, 3500000, 'Quỹ chung EV Group Tesla Model 3'),
(2, 3000000, 2800000, 'Quỹ chung EV Group BMW i3');

SET FOREIGN_KEY_CHECKS = 1;
