-- ===============================
-- DATABASE: Cost_Payment_DB
-- Version ĐƠN GIẢN - Đủ làm chức năng
-- ===============================

DROP DATABASE IF EXISTS Cost_Payment_DB;
CREATE DATABASE Cost_Payment_DB;
USE Cost_Payment_DB;

-- ==========================================
-- BẢNG CHÍNH
-- ==========================================

-- 1. Chi phí
CREATE TABLE cost (
    `costId` INT AUTO_INCREMENT PRIMARY KEY,
    `vehicleId` INT NOT NULL,
    `costType` ENUM('ElectricCharge','Maintenance','Insurance','Inspection','Cleaning','Other') DEFAULT 'Other',
    `amount` DECIMAL(15, 2) NOT NULL,
    `description` TEXT,
    `status` ENUM('PENDING','SHARED') DEFAULT 'PENDING',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Chia sẻ chi phí
CREATE TABLE costshare (
    `shareId` INT AUTO_INCREMENT PRIMARY KEY,
    `costId` INT NOT NULL,
    `userId` INT NOT NULL,
    `percent` DECIMAL(5, 2) DEFAULT 0,
    `amountShare` DECIMAL(15, 2) DEFAULT 0,
    `calculatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`costId`) REFERENCES cost(`costId`) ON DELETE CASCADE
);

-- 3. Thanh toán
CREATE TABLE payment (
    `paymentId` INT AUTO_INCREMENT PRIMARY KEY,
    `userId` INT NOT NULL,
    `costId` INT,
    `method` ENUM('EWALLET','BANKING','CASH') DEFAULT 'EWALLET',
    `amount` DECIMAL(15, 2) NOT NULL,
    `transactionCode` VARCHAR(100),
    `paymentDate` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('PENDING','PAID','OVERDUE','CANCELLED') DEFAULT 'PENDING',
    FOREIGN KEY (`costId`) REFERENCES cost(`costId`) ON DELETE SET NULL
);

-- 4. Quỹ chung
CREATE TABLE groupfund (
    `fundId` INT AUTO_INCREMENT PRIMARY KEY,
    `groupId` INT NOT NULL,
    `totalContributed` DECIMAL(15, 2) DEFAULT 0,
    `currentBalance` DECIMAL(15, 2) DEFAULT 0,
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `note` TEXT
);

-- 5. Giao dịch quỹ (HỖ TRỢ PHÊ DUYỆT)
CREATE TABLE fundtransaction (
    `transactionId` INT AUTO_INCREMENT PRIMARY KEY,
    `fundId` INT NOT NULL,
    `userId` INT,
    `transactionType` ENUM('Deposit','Withdraw') DEFAULT 'Deposit',
    `amount` DECIMAL(15, 2) NOT NULL,
    `purpose` VARCHAR(255),
    `date` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('Pending','Approved','Rejected','Completed') DEFAULT 'Completed',
    `approvedBy` INT,
    `approvedAt` DATETIME,
    `voteId` INT,
    `receiptUrl` VARCHAR(500),
    FOREIGN KEY (`fundId`) REFERENCES groupfund(`fundId`) ON DELETE CASCADE
);

-- 6. Bỏ phiếu giao dịch quỹ
CREATE TABLE IF NOT EXISTS transactionvote (
    voteId INT AUTO_INCREMENT PRIMARY KEY,
    transactionId INT NOT NULL,
    userId INT NOT NULL,
    approve BOOLEAN NOT NULL COMMENT 'true = đồng ý, false = từ chối',
    note VARCHAR(500),
    votedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_transaction_user (transactionId, userId),
    INDEX idx_transaction (transactionId),
    INDEX idx_user (userId),
    FOREIGN KEY (transactionId) REFERENCES fundtransaction(transactionId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Theo dõi km (THÊM MỚI - Cho chức năng chia theo km)
CREATE TABLE usagetracking (
    `usageId` INT AUTO_INCREMENT PRIMARY KEY,
    `groupId` INT NOT NULL,
    `userId` INT NOT NULL,
    `month` INT NOT NULL,
    `year` INT NOT NULL,
    `kmDriven` DOUBLE NOT NULL DEFAULT 0,
    `recordedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (`groupId`, `userId`, `month`, `year`)
);

-- 8. Chi tiết chia chi phí (CostSplitDetail)
CREATE TABLE costsplitdetail (
    `splitDetailId` INT AUTO_INCREMENT PRIMARY KEY,
    `costId` INT NOT NULL,
    `memberId` INT NOT NULL,
    `amount` DECIMAL(15, 2) NOT NULL,
    `percentage` DECIMAL(5, 2) NOT NULL,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`costId`) REFERENCES cost(`costId`) ON DELETE CASCADE,
    INDEX idx_cost (`costId`),
    INDEX idx_member (`memberId`)
);

-- ==========================================
-- DỮ LIỆU MẪU ĐƠN GIẢN
-- ==========================================

-- Km đã chạy tháng 10/2024 (Group 1)
INSERT INTO usagetracking (`groupId`, `userId`, `month`, `year`, `kmDriven`) VALUES 
(1, 1, 10, 2024, 600),  -- User 1: 600km (60%)
(1, 2, 10, 2024, 300),  -- User 2: 300km (30%)
(1, 3, 10, 2024, 100),  -- User 3: 100km (10%)
-- Data test cho Group 5 (các tháng 1, 2, 6, 11, 12 năm 2024, 2020, 2021, 2099, 2100)
(5, 7, 1, 2024, 500), (5, 8, 1, 2024, 300), (5, 9, 1, 2024, 200),
(5, 7, 2, 2024, 500), (5, 8, 2, 2024, 300), (5, 9, 2, 2024, 200),
(5, 7, 11, 2024, 500), (5, 8, 11, 2024, 300), (5, 9, 11, 2024, 200),
(5, 7, 12, 2024, 500), (5, 8, 12, 2024, 300), (5, 9, 12, 2024, 200),
(5, 7, 6, 2020, 500), (5, 8, 6, 2020, 300), (5, 9, 6, 2020, 200),
(5, 7, 6, 2021, 500), (5, 8, 6, 2021, 300), (5, 9, 6, 2021, 200),
(5, 7, 6, 2099, 500), (5, 8, 6, 2099, 300), (5, 9, 6, 2099, 200),
(5, 7, 6, 2100, 500), (5, 8, 6, 2100, 300), (5, 9, 6, 2100, 200);

-- Chi phí mẫu
INSERT INTO cost (`vehicleId`, `costType`, `amount`, `description`) VALUES 
-- Chia theo SỞ HỮU (50%, 30%, 20%)
(1, 'Insurance', 6000000, 'Bảo hiểm năm 2024'),
(1, 'Maintenance', 5000000, 'Bảo dưỡng định kỳ'),

-- Chia theo KM (60%, 30%, 10%)
(1, 'ElectricCharge', 500000, 'Sạc điện tháng 10/2024'),

-- Chia ĐỀU (33.33% mỗi người)
(1, 'Cleaning', 150000, 'Rửa xe');

-- Test data cho API AutoSplit boundary values
INSERT INTO cost (`costId`, `vehicleId`, `costType`, `amount`, `description`) VALUES
(10, 5, 'ElectricCharge', 1500000, 'Sạc điện xe 5 (Test Group 5 boundary values)');

-- Chia chi phí TỰ ĐỘNG
-- Cost 1: Bảo hiểm (theo ownership 50%, 30%, 20%)
INSERT INTO costshare (`costId`, `userId`, `percent`, `amountShare`) VALUES
(1, 1, 50.0, 3000000),
(1, 2, 30.0, 1800000),
(1, 3, 20.0, 1200000);

-- Cost 2: Bảo dưỡng (theo ownership)
INSERT INTO costshare (`costId`, `userId`, `percent`, `amountShare`) VALUES
(2, 1, 50.0, 2500000),
(2, 2, 30.0, 1500000),
(2, 3, 20.0, 1000000);

-- Cost 3: Sạc điện (theo km: 60%, 30%, 10%)
INSERT INTO costshare (`costId`, `userId`, `percent`, `amountShare`) VALUES
(3, 1, 60.0, 300000),
(3, 2, 30.0, 150000),
(3, 3, 10.0, 50000);

-- Cost 4: Rửa xe (chia đều)
INSERT INTO costshare (`costId`, `userId`, `percent`, `amountShare`) VALUES
(4, 1, 33.33, 50000),
(4, 2, 33.33, 50000),
(4, 3, 33.34, 50000);

-- Thanh toán mẫu
INSERT INTO payment (`userId`, `costId`, `method`, `amount`, `status`) VALUES
(1, 1, 'BANKING', 3000000, 'PAID'),
(2, 1, 'EWALLET', 1800000, 'PAID'),
(3, 1, 'BANKING', 1200000, 'PENDING'),
(1, 3, 'EWALLET', 300000, 'PAID');

-- Quỹ
INSERT INTO groupfund (`groupId`, `totalContributed`, `currentBalance`) VALUES 
(1, 1000000, 800000);

-- Giao dịch quỹ mẫu
INSERT INTO fundtransaction (`fundId`, `userId`, `transactionType`, `amount`, `purpose`, `status`) VALUES
(1, 1, 'Deposit', 500000, 'Nạp tiền vào quỹ', 'Completed'),
(1, 2, 'Deposit', 300000, 'Đóng góp quỹ', 'Completed'),
(1, 3, 'Deposit', 200000, 'Nạp quỹ tháng 10', 'Completed');

-- ==========================================
-- XEM DỮ LIỆU
-- ==========================================

SELECT '=== 1. CHI PHÍ ===' as '';
SELECT `costId`, `costType`, FORMAT(`amount`,0) as amount, `description` FROM cost;

SELECT '=== 2. KM THÁNG 10/2024 ===' as '';
SELECT 
    `userId`,
    `kmDriven` as 'KM',
    ROUND(`kmDriven`/(SELECT SUM(`kmDriven`) FROM usagetracking WHERE `month`=10)*100, 2) as '%'
FROM usagetracking WHERE `month`=10;

SELECT '=== 3. CHIA CHI PHÍ ===' as '';
SELECT 
    cs.`costId`,
    c.`costType`,
    cs.`userId`,
    CONCAT(cs.`percent`,'%') as '%',
    FORMAT(cs.`amountShare`,0) as 'Số tiền'
FROM costshare cs JOIN cost c ON cs.`costId`=c.`costId`;

SELECT '=== 4. NỢ CỦA USER ===' as '';
SELECT 
    cs.`userId`,
    FORMAT(SUM(cs.`amountShare`),0) as 'Phải trả',
    FORMAT(SUM(CASE WHEN p.`status`='PAID' THEN p.`amount` ELSE 0 END),0) as 'Đã trả',
    FORMAT(SUM(cs.`amountShare`)-SUM(CASE WHEN p.`status`='PAID' THEN p.`amount` ELSE 0 END),0) as 'Còn nợ'
FROM costshare cs LEFT JOIN payment p ON cs.`costId`=p.`costId` AND cs.`userId`=p.`userId`
GROUP BY cs.`userId`;

SELECT '=== 5. QUỸ CHUNG ===' as '';
SELECT 
    `fundId`,
    `groupId`,
    FORMAT(`totalContributed`,0) as 'Tổng đóng góp',
    FORMAT(`currentBalance`,0) as 'Số dư hiện tại'
FROM groupfund;

SELECT '=== 6. GIAO DỊCH QUỸ ===' as '';
SELECT 
    `transactionId`,
    `fundId`,
    `userId`,
    `transactionType` as 'Loại',
    FORMAT(`amount`,0) as 'Số tiền',
    `purpose` as 'Mục đích',
    `status` as 'Trạng thái',
    DATE_FORMAT(`date`, '%d/%m/%Y %H:%i') as 'Thời gian'
FROM fundtransaction
ORDER BY `date` DESC;

SELECT '✅ DATABASE SETUP HOÀN TẤT!' as '';