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

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE GroupMember;
DELETE FROM `Group`;
ALTER TABLE `Group` AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO `Group` (groupId, groupName, adminId, status) VALUES
(1, 'Nhóm EV 001', 484, 'Active'),
(2, 'Nhóm EV 002', 485, 'Active'),
(3, 'Nhóm EV 003', 486, 'Active'),
(4, 'Nhóm EV 004', 487, 'Active'),
(5, 'Nhóm EV 005', 488, 'Active'),
(6, 'Nhóm EV 006', 489, 'Active'),
(7, 'Nhóm EV 007', 490, 'Active'),
(8, 'Nhóm EV 008', 491, 'Active'),
(9, 'Nhóm EV 009', 492, 'Active'),
(10, 'Nhóm EV 010', 493, 'Active'),
(11, 'Nhóm EV 011', 494, 'Active'),
(12, 'Nhóm EV 012', 495, 'Active'),
(13, 'Nhóm EV 013', 496, 'Active'),
(14, 'Nhóm EV 014', 497, 'Active'),
(15, 'Nhóm EV 015', 498, 'Active'),
(16, 'Nhóm EV 016', 499, 'Active'),
(17, 'Nhóm EV 017', 500, 'Active'),
(18, 'Nhóm EV 018', 501, 'Active'),
(19, 'Nhóm EV 019', 502, 'Active'),
(20, 'Nhóm EV 020', 503, 'Active'),
(21, 'Nhóm EV 021', 504, 'Active'),
(22, 'Nhóm EV 022', 505, 'Active'),
(23, 'Nhóm EV 023', 506, 'Active'),
(24, 'Nhóm EV 024', 507, 'Active'),
(25, 'Nhóm EV 025', 508, 'Active'),
(26, 'Nhóm EV 026', 509, 'Active'),
(27, 'Nhóm EV 027', 510, 'Active'),
(28, 'Nhóm EV 028', 511, 'Active'),
(29, 'Nhóm EV 029', 512, 'Active'),
(30, 'Nhóm EV 030', 513, 'Active'),
(31, 'Nhóm EV 031', 514, 'Active'),
(32, 'Nhóm EV 032', 515, 'Active'),
(33, 'Nhóm EV 033', 516, 'Active'),
(34, 'Nhóm EV 034', 517, 'Active'),
(35, 'Nhóm EV 035', 518, 'Active'),
(36, 'Nhóm EV 036', 519, 'Active'),
(37, 'Nhóm EV 037', 520, 'Active'),
(38, 'Nhóm EV 038', 521, 'Active'),
(39, 'Nhóm EV 039', 522, 'Active'),
(40, 'Nhóm EV 040', 523, 'Active'),
(41, 'Nhóm EV 041', 524, 'Active'),
(42, 'Nhóm EV 042', 525, 'Active'),
(43, 'Nhóm EV 043', 526, 'Active'),
(44, 'Nhóm EV 044', 527, 'Active'),
(45, 'Nhóm EV 045', 528, 'Active'),
(46, 'Nhóm EV 046', 529, 'Active'),
(47, 'Nhóm EV 047', 530, 'Active'),
(48, 'Nhóm EV 048', 531, 'Active'),
(49, 'Nhóm EV 049', 532, 'Active'),
(50, 'Nhóm EV 050', 533, 'Active'),
(51, 'Nhóm EV 051', 534, 'Active'),
(52, 'Nhóm EV 052', 535, 'Active'),
(53, 'Nhóm EV 053', 536, 'Active'),
(54, 'Nhóm EV 054', 537, 'Active'),
(55, 'Nhóm EV 055', 538, 'Active'),
(56, 'Nhóm EV 056', 539, 'Active'),
(57, 'Nhóm EV 057', 540, 'Active'),
(58, 'Nhóm EV 058', 541, 'Active'),
(59, 'Nhóm EV 059', 542, 'Active'),
(60, 'Nhóm EV 060', 543, 'Active'),
(61, 'Nhóm EV 061', 544, 'Active'),
(62, 'Nhóm EV 062', 545, 'Active'),
(63, 'Nhóm EV 063', 546, 'Active'),
(64, 'Nhóm EV 064', 547, 'Active'),
(65, 'Nhóm EV 065', 548, 'Active'),
(66, 'Nhóm EV 066', 549, 'Active'),
(67, 'Nhóm EV 067', 550, 'Active'),
(68, 'Nhóm EV 068', 551, 'Active'),
(69, 'Nhóm EV 069', 552, 'Active'),
(70, 'Nhóm EV 070', 553, 'Active'),
(71, 'Nhóm EV 071', 554, 'Active'),
(72, 'Nhóm EV 072', 555, 'Active'),
(73, 'Nhóm EV 073', 556, 'Active'),
(74, 'Nhóm EV 074', 557, 'Active'),
(75, 'Nhóm EV 075', 558, 'Active'),
(76, 'Nhóm EV 076', 559, 'Active'),
(77, 'Nhóm EV 077', 560, 'Active'),
(78, 'Nhóm EV 078', 561, 'Active'),
(79, 'Nhóm EV 079', 562, 'Active'),
(80, 'Nhóm EV 080', 563, 'Active'),
(81, 'Nhóm EV 081', 564, 'Active'),
(82, 'Nhóm EV 082', 565, 'Active'),
(83, 'Nhóm EV 083', 566, 'Active'),
(84, 'Nhóm EV 084', 567, 'Active'),
(85, 'Nhóm EV 085', 568, 'Active'),
(86, 'Nhóm EV 086', 569, 'Active'),
(87, 'Nhóm EV 087', 570, 'Active'),
(88, 'Nhóm EV 088', 571, 'Active'),
(89, 'Nhóm EV 089', 572, 'Active'),
(90, 'Nhóm EV 090', 573, 'Active'),
(91, 'Nhóm EV 091', 574, 'Active'),
(92, 'Nhóm EV 092', 575, 'Active'),
(93, 'Nhóm EV 093', 576, 'Active'),
(94, 'Nhóm EV 094', 577, 'Active'),
(95, 'Nhóm EV 095', 578, 'Active'),
(96, 'Nhóm EV 096', 579, 'Active'),
(97, 'Nhóm EV 097', 580, 'Active'),
(98, 'Nhóm EV 098', 581, 'Active'),
(99, 'Nhóm EV 099', 582, 'Active'),
(100, 'Nhóm EV 100', 583, 'Active');

INSERT INTO GroupMember (groupId, userId, role, ownershipPercent) VALUES
(1, 484, 'Admin', 100.0),
(2, 485, 'Admin', 100.0),
(3, 486, 'Admin', 100.0),
(4, 487, 'Admin', 100.0),
(5, 488, 'Admin', 100.0),
(6, 489, 'Admin', 100.0),
(7, 490, 'Admin', 100.0),
(8, 491, 'Admin', 100.0),
(9, 492, 'Admin', 100.0),
(10, 493, 'Admin', 100.0),
(11, 494, 'Admin', 100.0),
(12, 495, 'Admin', 100.0),
(13, 496, 'Admin', 100.0),
(14, 497, 'Admin', 100.0),
(15, 498, 'Admin', 100.0),
(16, 499, 'Admin', 100.0),
(17, 500, 'Admin', 100.0),
(18, 501, 'Admin', 100.0),
(19, 502, 'Admin', 100.0),
(20, 503, 'Admin', 100.0),
(21, 504, 'Admin', 100.0),
(22, 505, 'Admin', 100.0),
(23, 506, 'Admin', 100.0),
(24, 507, 'Admin', 100.0),
(25, 508, 'Admin', 100.0),
(26, 509, 'Admin', 100.0),
(27, 510, 'Admin', 100.0),
(28, 511, 'Admin', 100.0),
(29, 512, 'Admin', 100.0),
(30, 513, 'Admin', 100.0),
(31, 514, 'Admin', 100.0),
(32, 515, 'Admin', 100.0),
(33, 516, 'Admin', 100.0),
(34, 517, 'Admin', 100.0),
(35, 518, 'Admin', 100.0),
(36, 519, 'Admin', 100.0),
(37, 520, 'Admin', 100.0),
(38, 521, 'Admin', 100.0),
(39, 522, 'Admin', 100.0),
(40, 523, 'Admin', 100.0),
(41, 524, 'Admin', 100.0),
(42, 525, 'Admin', 100.0),
(43, 526, 'Admin', 100.0),
(44, 527, 'Admin', 100.0),
(45, 528, 'Admin', 100.0),
(46, 529, 'Admin', 100.0),
(47, 530, 'Admin', 100.0),
(48, 531, 'Admin', 100.0),
(49, 532, 'Admin', 100.0),
(50, 533, 'Admin', 100.0),
(51, 534, 'Admin', 100.0),
(52, 535, 'Admin', 100.0),
(53, 536, 'Admin', 100.0),
(54, 537, 'Admin', 100.0),
(55, 538, 'Admin', 100.0),
(56, 539, 'Admin', 100.0),
(57, 540, 'Admin', 100.0),
(58, 541, 'Admin', 100.0),
(59, 542, 'Admin', 100.0),
(60, 543, 'Admin', 100.0),
(61, 544, 'Admin', 100.0),
(62, 545, 'Admin', 100.0),
(63, 546, 'Admin', 100.0),
(64, 547, 'Admin', 100.0),
(65, 548, 'Admin', 100.0),
(66, 549, 'Admin', 100.0),
(67, 550, 'Admin', 100.0),
(68, 551, 'Admin', 100.0),
(69, 552, 'Admin', 100.0),
(70, 553, 'Admin', 100.0),
(71, 554, 'Admin', 100.0),
(72, 555, 'Admin', 100.0),
(73, 556, 'Admin', 100.0),
(74, 557, 'Admin', 100.0),
(75, 558, 'Admin', 100.0),
(76, 559, 'Admin', 100.0),
(77, 560, 'Admin', 100.0),
(78, 561, 'Admin', 100.0),
(79, 562, 'Admin', 100.0),
(80, 563, 'Admin', 100.0),
(81, 564, 'Admin', 100.0),
(82, 565, 'Admin', 100.0),
(83, 566, 'Admin', 100.0),
(84, 567, 'Admin', 100.0),
(85, 568, 'Admin', 100.0),
(86, 569, 'Admin', 100.0),
(87, 570, 'Admin', 100.0),
(88, 571, 'Admin', 100.0),
(89, 572, 'Admin', 100.0),
(90, 573, 'Admin', 100.0),
(91, 574, 'Admin', 100.0),
(92, 575, 'Admin', 100.0),
(93, 576, 'Admin', 100.0),
(94, 577, 'Admin', 100.0),
(95, 578, 'Admin', 100.0),
(96, 579, 'Admin', 100.0),
(97, 580, 'Admin', 100.0),
(98, 581, 'Admin', 100.0),
(99, 582, 'Admin', 100.0),
(100, 583, 'Admin', 100.0),
(1, 1, 'Member', 0.0),
(2, 1, 'Member', 0.0);

(1, 484, 'Admin', 100.0),
(2, 485, 'Admin', 100.0),
(3, 486, 'Admin', 100.0),
(4, 487, 'Admin', 100.0),
(5, 488, 'Admin', 100.0),
(6, 489, 'Admin', 100.0),
(7, 490, 'Admin', 100.0),
(8, 491, 'Admin', 100.0),
(9, 492, 'Admin', 100.0),
(10, 493, 'Admin', 100.0),
(11, 494, 'Admin', 100.0),
(12, 495, 'Admin', 100.0),
(13, 496, 'Admin', 100.0),
(14, 497, 'Admin', 100.0),
(15, 498, 'Admin', 100.0),
(16, 499, 'Admin', 100.0),
(17, 500, 'Admin', 100.0),
(18, 501, 'Admin', 100.0),
(19, 502, 'Admin', 100.0),
(20, 503, 'Admin', 100.0),
(21, 504, 'Admin', 100.0),
(22, 505, 'Admin', 100.0),
(23, 506, 'Admin', 100.0),
(24, 507, 'Admin', 100.0),
(25, 508, 'Admin', 100.0),
(26, 509, 'Admin', 100.0),
(27, 510, 'Admin', 100.0),
(28, 511, 'Admin', 100.0),
(29, 512, 'Admin', 100.0),
(30, 513, 'Admin', 100.0),
(31, 514, 'Admin', 100.0),
(32, 515, 'Admin', 100.0),
(33, 516, 'Admin', 100.0),
(34, 517, 'Admin', 100.0),
(35, 518, 'Admin', 100.0),
(36, 519, 'Admin', 100.0),
(37, 520, 'Admin', 100.0),
(38, 521, 'Admin', 100.0),
(39, 522, 'Admin', 100.0),
(40, 523, 'Admin', 100.0),
(41, 524, 'Admin', 100.0),
(42, 525, 'Admin', 100.0),
(43, 526, 'Admin', 100.0),
(44, 527, 'Admin', 100.0),
(45, 528, 'Admin', 100.0),
(46, 529, 'Admin', 100.0),
(47, 530, 'Admin', 100.0),
(48, 531, 'Admin', 100.0),
(49, 532, 'Admin', 100.0),
(50, 533, 'Admin', 100.0),
(51, 534, 'Admin', 100.0),
(52, 535, 'Admin', 100.0),
(53, 536, 'Admin', 100.0),
(54, 537, 'Admin', 100.0),
(55, 538, 'Admin', 100.0),
(56, 539, 'Admin', 100.0),
(57, 540, 'Admin', 100.0),
(58, 541, 'Admin', 100.0),
(59, 542, 'Admin', 100.0),
(60, 543, 'Admin', 100.0),
(61, 544, 'Admin', 100.0),
(62, 545, 'Admin', 100.0),
(63, 546, 'Admin', 100.0),
(64, 547, 'Admin', 100.0),
(65, 548, 'Admin', 100.0),
(66, 549, 'Admin', 100.0),
(67, 550, 'Admin', 100.0),
(68, 551, 'Admin', 100.0),
(69, 552, 'Admin', 100.0),
(70, 553, 'Admin', 100.0),
(71, 554, 'Admin', 100.0),
(72, 555, 'Admin', 100.0),
(73, 556, 'Admin', 100.0),
(74, 557, 'Admin', 100.0),
(75, 558, 'Admin', 100.0),
(76, 559, 'Admin', 100.0),
(77, 560, 'Admin', 100.0),
(78, 561, 'Admin', 100.0),
(79, 562, 'Admin', 100.0),
(80, 563, 'Admin', 100.0),
(81, 564, 'Admin', 100.0),
(82, 565, 'Admin', 100.0),
(83, 566, 'Admin', 100.0),
(84, 567, 'Admin', 100.0),
(85, 568, 'Admin', 100.0),
(86, 569, 'Admin', 100.0),
(87, 570, 'Admin', 100.0),
(88, 571, 'Admin', 100.0),
(89, 572, 'Admin', 100.0),
(90, 573, 'Admin', 100.0),
(91, 574, 'Admin', 100.0),
(92, 575, 'Admin', 100.0),
(93, 576, 'Admin', 100.0),
(94, 577, 'Admin', 100.0),
(95, 578, 'Admin', 100.0),
(96, 579, 'Admin', 100.0),
(97, 580, 'Admin', 100.0),
(98, 581, 'Admin', 100.0),
(99, 582, 'Admin', 100.0),
(100, 583, 'Admin', 100.0);

(1, 484, 'Admin', 100.0),
(2, 485, 'Admin', 100.0),
(3, 486, 'Admin', 100.0),
(4, 487, 'Admin', 100.0),
(5, 488, 'Admin', 100.0),
(6, 489, 'Admin', 100.0),
(7, 490, 'Admin', 100.0),
(8, 491, 'Admin', 100.0),
(9, 492, 'Admin', 100.0),
(10, 493, 'Admin', 100.0),
(11, 494, 'Admin', 100.0),
(12, 495, 'Admin', 100.0),
(13, 496, 'Admin', 100.0),
(14, 497, 'Admin', 100.0),
(15, 498, 'Admin', 100.0),
(16, 499, 'Admin', 100.0),
(17, 500, 'Admin', 100.0),
(18, 501, 'Admin', 100.0),
(19, 502, 'Admin', 100.0),
(20, 503, 'Admin', 100.0),
(21, 504, 'Admin', 100.0),
(22, 505, 'Admin', 100.0),
(23, 506, 'Admin', 100.0),
(24, 507, 'Admin', 100.0),
(25, 508, 'Admin', 100.0),
(26, 509, 'Admin', 100.0),
(27, 510, 'Admin', 100.0),
(28, 511, 'Admin', 100.0),
(29, 512, 'Admin', 100.0),
(30, 513, 'Admin', 100.0),
(31, 514, 'Admin', 100.0),
(32, 515, 'Admin', 100.0),
(33, 516, 'Admin', 100.0),
(34, 517, 'Admin', 100.0),
(35, 518, 'Admin', 100.0),
(36, 519, 'Admin', 100.0),
(37, 520, 'Admin', 100.0),
(38, 521, 'Admin', 100.0),
(39, 522, 'Admin', 100.0),
(40, 523, 'Admin', 100.0),
(41, 524, 'Admin', 100.0),
(42, 525, 'Admin', 100.0),
(43, 526, 'Admin', 100.0),
(44, 527, 'Admin', 100.0),
(45, 528, 'Admin', 100.0),
(46, 529, 'Admin', 100.0),
(47, 530, 'Admin', 100.0),
(48, 531, 'Admin', 100.0),
(49, 532, 'Admin', 100.0),
(50, 533, 'Admin', 100.0),
(51, 534, 'Admin', 100.0),
(52, 535, 'Admin', 100.0),
(53, 536, 'Admin', 100.0),
(54, 537, 'Admin', 100.0),
(55, 538, 'Admin', 100.0),
(56, 539, 'Admin', 100.0),
(57, 540, 'Admin', 100.0),
(58, 541, 'Admin', 100.0),
(59, 542, 'Admin', 100.0),
(60, 543, 'Admin', 100.0),
(61, 544, 'Admin', 100.0),
(62, 545, 'Admin', 100.0),
(63, 546, 'Admin', 100.0),
(64, 547, 'Admin', 100.0),
(65, 548, 'Admin', 100.0),
(66, 549, 'Admin', 100.0),
(67, 550, 'Admin', 100.0),
(68, 551, 'Admin', 100.0),
(69, 552, 'Admin', 100.0),
(70, 553, 'Admin', 100.0),
(71, 554, 'Admin', 100.0),
(72, 555, 'Admin', 100.0),
(73, 556, 'Admin', 100.0),
(74, 557, 'Admin', 100.0),
(75, 558, 'Admin', 100.0),
(76, 559, 'Admin', 100.0),
(77, 560, 'Admin', 100.0),
(78, 561, 'Admin', 100.0),
(79, 562, 'Admin', 100.0),
(80, 563, 'Admin', 100.0),
(81, 564, 'Admin', 100.0),
(82, 565, 'Admin', 100.0),
(83, 566, 'Admin', 100.0),
(84, 567, 'Admin', 100.0),
(85, 568, 'Admin', 100.0),
(86, 569, 'Admin', 100.0),
(87, 570, 'Admin', 100.0),
(88, 571, 'Admin', 100.0),
(89, 572, 'Admin', 100.0),
(90, 573, 'Admin', 100.0),
(91, 574, 'Admin', 100.0),
(92, 575, 'Admin', 100.0),
(93, 576, 'Admin', 100.0),
(94, 577, 'Admin', 100.0),
(95, 578, 'Admin', 100.0),
(96, 579, 'Admin', 100.0),
(97, 580, 'Admin', 100.0),
(98, 581, 'Admin', 100.0),
(99, 582, 'Admin', 100.0),
(100, 583, 'Admin', 100.0);

(1, 484, 'Admin', 100.0),
(2, 485, 'Admin', 100.0),
(3, 486, 'Admin', 100.0),
(4, 487, 'Admin', 100.0),
(5, 488, 'Admin', 100.0),
(6, 489, 'Admin', 100.0),
(7, 490, 'Admin', 100.0),
(8, 491, 'Admin', 100.0),
(9, 492, 'Admin', 100.0),
(10, 493, 'Admin', 100.0),
(11, 494, 'Admin', 100.0),
(12, 495, 'Admin', 100.0),
(13, 496, 'Admin', 100.0),
(14, 497, 'Admin', 100.0),
(15, 498, 'Admin', 100.0),
(16, 499, 'Admin', 100.0),
(17, 500, 'Admin', 100.0),
(18, 501, 'Admin', 100.0),
(19, 502, 'Admin', 100.0),
(20, 503, 'Admin', 100.0),
(21, 504, 'Admin', 100.0),
(22, 505, 'Admin', 100.0),
(23, 506, 'Admin', 100.0),
(24, 507, 'Admin', 100.0),
(25, 508, 'Admin', 100.0),
(26, 509, 'Admin', 100.0),
(27, 510, 'Admin', 100.0),
(28, 511, 'Admin', 100.0),
(29, 512, 'Admin', 100.0),
(30, 513, 'Admin', 100.0),
(31, 514, 'Admin', 100.0),
(32, 515, 'Admin', 100.0),
(33, 516, 'Admin', 100.0),
(34, 517, 'Admin', 100.0),
(35, 518, 'Admin', 100.0),
(36, 519, 'Admin', 100.0),
(37, 520, 'Admin', 100.0),
(38, 521, 'Admin', 100.0),
(39, 522, 'Admin', 100.0),
(40, 523, 'Admin', 100.0),
(41, 524, 'Admin', 100.0),
(42, 525, 'Admin', 100.0),
(43, 526, 'Admin', 100.0),
(44, 527, 'Admin', 100.0),
(45, 528, 'Admin', 100.0),
(46, 529, 'Admin', 100.0),
(47, 530, 'Admin', 100.0),
(48, 531, 'Admin', 100.0),
(49, 532, 'Admin', 100.0),
(50, 533, 'Admin', 100.0),
(51, 534, 'Admin', 100.0),
(52, 535, 'Admin', 100.0),
(53, 536, 'Admin', 100.0),
(54, 537, 'Admin', 100.0),
(55, 538, 'Admin', 100.0),
(56, 539, 'Admin', 100.0),
(57, 540, 'Admin', 100.0),
(58, 541, 'Admin', 100.0),
(59, 542, 'Admin', 100.0),
(60, 543, 'Admin', 100.0),
(61, 544, 'Admin', 100.0),
(62, 545, 'Admin', 100.0),
(63, 546, 'Admin', 100.0),
(64, 547, 'Admin', 100.0),
(65, 548, 'Admin', 100.0),
(66, 549, 'Admin', 100.0),
(67, 550, 'Admin', 100.0),
(68, 551, 'Admin', 100.0),
(69, 552, 'Admin', 100.0),
(70, 553, 'Admin', 100.0),
(71, 554, 'Admin', 100.0),
(72, 555, 'Admin', 100.0),
(73, 556, 'Admin', 100.0),
(74, 557, 'Admin', 100.0),
(75, 558, 'Admin', 100.0),
(76, 559, 'Admin', 100.0),
(77, 560, 'Admin', 100.0),
(78, 561, 'Admin', 100.0),
(79, 562, 'Admin', 100.0),
(80, 563, 'Admin', 100.0),
(81, 564, 'Admin', 100.0),
(82, 565, 'Admin', 100.0),
(83, 566, 'Admin', 100.0),
(84, 567, 'Admin', 100.0),
(85, 568, 'Admin', 100.0),
(86, 569, 'Admin', 100.0),
(87, 570, 'Admin', 100.0),
(88, 571, 'Admin', 100.0),
(89, 572, 'Admin', 100.0),
(90, 573, 'Admin', 100.0),
(91, 574, 'Admin', 100.0),
(92, 575, 'Admin', 100.0),
(93, 576, 'Admin', 100.0),
(94, 577, 'Admin', 100.0),
(95, 578, 'Admin', 100.0),
(96, 579, 'Admin', 100.0),
(97, 580, 'Admin', 100.0),
(98, 581, 'Admin', 100.0),
(99, 582, 'Admin', 100.0),
(100, 583, 'Admin', 100.0);

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

TRUNCATE FundTransaction;
DELETE FROM GroupFund;
ALTER TABLE GroupFund AUTO_INCREMENT = 1;
INSERT INTO GroupFund (groupId, totalContributed, currentBalance, note) VALUES
(1, 10000000.0, 10000000.0, 'Quỹ nhóm 001'),
(2, 10000000.0, 10000000.0, 'Quỹ nhóm 002'),
(3, 10000000.0, 10000000.0, 'Quỹ nhóm 003'),
(4, 10000000.0, 10000000.0, 'Quỹ nhóm 004'),
(5, 10000000.0, 10000000.0, 'Quỹ nhóm 005'),
(6, 10000000.0, 10000000.0, 'Quỹ nhóm 006'),
(7, 10000000.0, 10000000.0, 'Quỹ nhóm 007'),
(8, 10000000.0, 10000000.0, 'Quỹ nhóm 008'),
(9, 10000000.0, 10000000.0, 'Quỹ nhóm 009'),
(10, 10000000.0, 10000000.0, 'Quỹ nhóm 010'),
(11, 10000000.0, 10000000.0, 'Quỹ nhóm 011'),
(12, 10000000.0, 10000000.0, 'Quỹ nhóm 012'),
(13, 10000000.0, 10000000.0, 'Quỹ nhóm 013'),
(14, 10000000.0, 10000000.0, 'Quỹ nhóm 014'),
(15, 10000000.0, 10000000.0, 'Quỹ nhóm 015'),
(16, 10000000.0, 10000000.0, 'Quỹ nhóm 016'),
(17, 10000000.0, 10000000.0, 'Quỹ nhóm 017'),
(18, 10000000.0, 10000000.0, 'Quỹ nhóm 018'),
(19, 10000000.0, 10000000.0, 'Quỹ nhóm 019'),
(20, 10000000.0, 10000000.0, 'Quỹ nhóm 020'),
(21, 10000000.0, 10000000.0, 'Quỹ nhóm 021'),
(22, 10000000.0, 10000000.0, 'Quỹ nhóm 022'),
(23, 10000000.0, 10000000.0, 'Quỹ nhóm 023'),
(24, 10000000.0, 10000000.0, 'Quỹ nhóm 024'),
(25, 10000000.0, 10000000.0, 'Quỹ nhóm 025'),
(26, 10000000.0, 10000000.0, 'Quỹ nhóm 026'),
(27, 10000000.0, 10000000.0, 'Quỹ nhóm 027'),
(28, 10000000.0, 10000000.0, 'Quỹ nhóm 028'),
(29, 10000000.0, 10000000.0, 'Quỹ nhóm 029'),
(30, 10000000.0, 10000000.0, 'Quỹ nhóm 030'),
(31, 10000000.0, 10000000.0, 'Quỹ nhóm 031'),
(32, 10000000.0, 10000000.0, 'Quỹ nhóm 032'),
(33, 10000000.0, 10000000.0, 'Quỹ nhóm 033'),
(34, 10000000.0, 10000000.0, 'Quỹ nhóm 034'),
(35, 10000000.0, 10000000.0, 'Quỹ nhóm 035'),
(36, 10000000.0, 10000000.0, 'Quỹ nhóm 036'),
(37, 10000000.0, 10000000.0, 'Quỹ nhóm 037'),
(38, 10000000.0, 10000000.0, 'Quỹ nhóm 038'),
(39, 10000000.0, 10000000.0, 'Quỹ nhóm 039'),
(40, 10000000.0, 10000000.0, 'Quỹ nhóm 040'),
(41, 10000000.0, 10000000.0, 'Quỹ nhóm 041'),
(42, 10000000.0, 10000000.0, 'Quỹ nhóm 042'),
(43, 10000000.0, 10000000.0, 'Quỹ nhóm 043'),
(44, 10000000.0, 10000000.0, 'Quỹ nhóm 044'),
(45, 10000000.0, 10000000.0, 'Quỹ nhóm 045'),
(46, 10000000.0, 10000000.0, 'Quỹ nhóm 046'),
(47, 10000000.0, 10000000.0, 'Quỹ nhóm 047'),
(48, 10000000.0, 10000000.0, 'Quỹ nhóm 048'),
(49, 10000000.0, 10000000.0, 'Quỹ nhóm 049'),
(50, 10000000.0, 10000000.0, 'Quỹ nhóm 050'),
(51, 10000000.0, 10000000.0, 'Quỹ nhóm 051'),
(52, 10000000.0, 10000000.0, 'Quỹ nhóm 052'),
(53, 10000000.0, 10000000.0, 'Quỹ nhóm 053'),
(54, 10000000.0, 10000000.0, 'Quỹ nhóm 054'),
(55, 10000000.0, 10000000.0, 'Quỹ nhóm 055'),
(56, 10000000.0, 10000000.0, 'Quỹ nhóm 056'),
(57, 10000000.0, 10000000.0, 'Quỹ nhóm 057'),
(58, 10000000.0, 10000000.0, 'Quỹ nhóm 058'),
(59, 10000000.0, 10000000.0, 'Quỹ nhóm 059'),
(60, 10000000.0, 10000000.0, 'Quỹ nhóm 060'),
(61, 10000000.0, 10000000.0, 'Quỹ nhóm 061'),
(62, 10000000.0, 10000000.0, 'Quỹ nhóm 062'),
(63, 10000000.0, 10000000.0, 'Quỹ nhóm 063'),
(64, 10000000.0, 10000000.0, 'Quỹ nhóm 064'),
(65, 10000000.0, 10000000.0, 'Quỹ nhóm 065'),
(66, 10000000.0, 10000000.0, 'Quỹ nhóm 066'),
(67, 10000000.0, 10000000.0, 'Quỹ nhóm 067'),
(68, 10000000.0, 10000000.0, 'Quỹ nhóm 068'),
(69, 10000000.0, 10000000.0, 'Quỹ nhóm 069'),
(70, 10000000.0, 10000000.0, 'Quỹ nhóm 070'),
(71, 10000000.0, 10000000.0, 'Quỹ nhóm 071'),
(72, 10000000.0, 10000000.0, 'Quỹ nhóm 072'),
(73, 10000000.0, 10000000.0, 'Quỹ nhóm 073'),
(74, 10000000.0, 10000000.0, 'Quỹ nhóm 074'),
(75, 10000000.0, 10000000.0, 'Quỹ nhóm 075'),
(76, 10000000.0, 10000000.0, 'Quỹ nhóm 076'),
(77, 10000000.0, 10000000.0, 'Quỹ nhóm 077'),
(78, 10000000.0, 10000000.0, 'Quỹ nhóm 078'),
(79, 10000000.0, 10000000.0, 'Quỹ nhóm 079'),
80: (80, 10000000.0, 10000000.0, 'Quỹ nhóm 080'),
(81, 10000000.0, 10000000.0, 'Quỹ nhóm 081'),
(82, 10000000.0, 10000000.0, 'Quỹ nhóm 082'),
(83, 10000000.0, 10000000.0, 'Quỹ nhóm 083'),
(84, 10000000.0, 10000000.0, 'Quỹ nhóm 084'),
(85, 10000000.0, 10000000.0, 'Quỹ nhóm 085'),
(86, 10000000.0, 10000000.0, 'Quỹ nhóm 086'),
(87, 10000000.0, 10000000.0, 'Quỹ nhóm 087'),
(88, 10000000.0, 10000000.0, 'Quỹ nhóm 088'),
(89, 10000000.0, 10000000.0, 'Quỹ nhóm 089'),
(90, 10000000.0, 10000000.0, 'Quỹ nhóm 090'),
(91, 10000000.0, 10000000.0, 'Quỹ nhóm 091'),
(92, 10000000.0, 10000000.0, 'Quỹ nhóm 092'),
(93, 10000000.0, 10000000.0, 'Quỹ nhóm 093'),
(94, 10000000.0, 10000000.0, 'Quỹ nhóm 094'),
(95, 10000000.0, 10000000.0, 'Quỹ nhóm 095'),
(96, 10000000.0, 10000000.0, 'Quỹ nhóm 096'),
(97, 10000000.0, 10000000.0, 'Quỹ nhóm 097'),
(98, 10000000.0, 10000000.0, 'Quỹ nhóm 098'),
(99, 10000000.0, 10000000.0, 'Quỹ nhóm 099'),
(100, 10000000.0, 10000000.0, 'Quỹ nhóm 100');

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


