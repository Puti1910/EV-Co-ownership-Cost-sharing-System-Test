-- =====================================================
-- SCRIPT TỔNG HỢP (không dùng bảng users trong booking DB)
-- =====================================================

/* ==============================
   PHẦN 1: TẠO DATABASE
   ============================== */

CREATE DATABASE IF NOT EXISTS co_ownership_booking
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS co_ownership_booking_admin
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ai_ev
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON co_ownership_booking.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON co_ownership_booking_admin.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON ai_ev.* TO 'root'@'%';
FLUSH PRIVILEGES;

/* ==============================
   PHẦN 2: SCHEMA ReservationService - co_ownership_booking
   (KHÔNG có bảng users)
   ============================== */

USE co_ownership_booking;

-- vehicle_groups: chỉ lưu metadata nhóm xe, không chứa thông tin user
CREATE TABLE IF NOT EXISTS vehicle_groups (
                                              group_id    VARCHAR(20) PRIMARY KEY,
    group_name  VARCHAR(255),
    description TEXT,
    creation_date DATETIME(6),
    active      VARCHAR(50)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- vehicles: mỗi xe gắn với group_id, không join user
-- vehicle_id không dùng AUTO_INCREMENT để có thể set giá trị từ external_vehicle_id
CREATE TABLE IF NOT EXISTS vehicles (
                                        vehicle_id   BIGINT PRIMARY KEY,
                                        external_vehicle_id VARCHAR(50) NULL COMMENT 'ID từ vehicle_management database',
    vehicle_name VARCHAR(255),
    license_plate VARCHAR(50),
    vehicle_type VARCHAR(100),
    group_id     VARCHAR(20),
    status       VARCHAR(50) DEFAULT 'AVAILABLE',
    INDEX idx_group_id (group_id),
    INDEX idx_status (status),
    INDEX idx_external_vehicle_id (external_vehicle_id),
    CONSTRAINT fk_vehicle_group FOREIGN KEY (group_id)
    REFERENCES vehicle_groups(group_id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- reservations: chỉ lưu user_id (số nguyên) và vehicle_id
CREATE TABLE IF NOT EXISTS reservations (
                                            reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            vehicle_id     BIGINT NOT NULL,
                                            user_id        BIGINT NOT NULL,
                                            start_datetime DATETIME(6) NOT NULL,
    end_datetime   DATETIME(6) NOT NULL,
    purpose        TEXT,
    status         VARCHAR(50) DEFAULT 'BOOKED',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_user_id (user_id),
    INDEX idx_start (start_datetime),
    CONSTRAINT fk_res_vehicle FOREIGN KEY (vehicle_id)
    REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- reservation_checkpoints: dùng cho tính năng check-in/out
CREATE TABLE IF NOT EXISTS reservation_checkpoints (
                                                       checkpoint_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                       reservation_id BIGINT NOT NULL,
                                                       checkpoint_type VARCHAR(20) NOT NULL,
    status         VARCHAR(20) DEFAULT 'PENDING',
    qr_token       VARCHAR(128) NOT NULL UNIQUE,
    issued_by      VARCHAR(50),
    issued_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at     DATETIME(6),
    scanned_at     DATETIME(6),
    signed_at      DATETIME(6),
    signer_name    VARCHAR(255),
    signer_id_number VARCHAR(50),
    signature_data LONGTEXT,
    latitude       DECIMAL(10,7),
    longitude      DECIMAL(10,7),
    notes          TEXT,
    INDEX idx_reservation (reservation_id),
    INDEX idx_type (checkpoint_type),
    CONSTRAINT fk_checkpoint_reservation FOREIGN KEY (reservation_id)
    REFERENCES reservations(reservation_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* ==============================
   PHẦN 3: SCHEMA ReservationAdminService - co_ownership_booking_admin
   ============================== */

USE co_ownership_booking_admin;

CREATE TABLE IF NOT EXISTS admin_users (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           username VARCHAR(255) UNIQUE,
    email    VARCHAR(255),
    password VARCHAR(255),
    role     VARCHAR(50),
    created_at DATETIME(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `groups` (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(255),
    description TEXT,
    created_at DATETIME(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vehicles (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        vehicle_name  VARCHAR(255),
    vehicle_type  VARCHAR(100),
    license_plate VARCHAR(50),
    group_id      BIGINT,
    status        VARCHAR(50),
    INDEX idx_group_id (group_id),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(255),
    email    VARCHAR(255),
    full_name VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservations (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            reservation_id BIGINT,   -- id gốc từ booking DB (để đối chiếu)
                                            user_id   BIGINT,
                                            vehicle_id BIGINT,
                                            vehicle_name VARCHAR(255),
    user_name   VARCHAR(255),
    start_datetime DATETIME(6),
    end_datetime   DATETIME(6),
    purpose   TEXT,
    status    VARCHAR(50),
    created_at DATETIME(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* ==============================
   PHẦN 4: SCHEMA AIService - ai_ev
   ============================== */

USE ai_ev;

CREATE TABLE IF NOT EXISTS ownership_info (
                                              ownership_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id   BIGINT NOT NULL,
                                              vehicle_id BIGINT NOT NULL,
                                              group_id  BIGINT NOT NULL,
                                              ownership_percentage DOUBLE NOT NULL,
                                              role      VARCHAR(20) DEFAULT 'MEMBER',
    joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_vehicle (user_id, vehicle_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_analysis (
                                              analysis_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              vehicle_id BIGINT NOT NULL,
                                              group_id BIGINT NOT NULL,
                                              total_hours_used DOUBLE DEFAULT 0,
                                              total_kilometers DOUBLE DEFAULT 0,
                                              booking_count INT DEFAULT 0,
                                              cancellation_count INT DEFAULT 0,
                                              usage_percentage DOUBLE DEFAULT 0,
                                              cost_incurred DOUBLE DEFAULT 0,
                                              period_start DATETIME,
                                              period_end   DATETIME,
                                              analyzed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fairness_score (
                                              score_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              vehicle_id BIGINT NOT NULL,
                                              group_id BIGINT NOT NULL,
                                              ownership_percentage DOUBLE NOT NULL,
                                              usage_percentage DOUBLE NOT NULL,
                                              difference DOUBLE NOT NULL,
                                              fairness_score DOUBLE NOT NULL,
                                              priority VARCHAR(20) DEFAULT 'NORMAL',
    period_start DATETIME,
    period_end   DATETIME,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_recommendations (
                                                  recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  group_id BIGINT NOT NULL,
                                                  vehicle_id BIGINT NOT NULL,
                                                  type VARCHAR(50) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    severity VARCHAR(20) DEFAULT 'INFO',
    target_user_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    period_start DATETIME,
    period_end   DATETIME,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at      DATETIME
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* ==============================
   PHẦN 5: DỮ LIỆU MẪU (ReservationService)
   ============================== */

USE co_ownership_booking;
USE co_ownership_booking;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE vehicles;
TRUNCATE vehicle_groups;
SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO vehicle_groups (group_id, group_name, active) VALUES
('1', 'Nhóm EV 001', 'ACTIVE'),
('2', 'Nhóm EV 002', 'ACTIVE'),
('3', 'Nhóm EV 003', 'ACTIVE'),
('4', 'Nhóm EV 004', 'ACTIVE'),
('5', 'Nhóm EV 005', 'ACTIVE'),
('6', 'Nhóm EV 006', 'ACTIVE'),
('7', 'Nhóm EV 007', 'ACTIVE'),
('8', 'Nhóm EV 008', 'ACTIVE'),
('9', 'Nhóm EV 009', 'ACTIVE'),
('10', 'Nhóm EV 010', 'ACTIVE'),
('11', 'Nhóm EV 011', 'ACTIVE'),
('12', 'Nhóm EV 012', 'ACTIVE'),
('13', 'Nhóm EV 013', 'ACTIVE'),
('14', 'Nhóm EV 014', 'ACTIVE'),
('15', 'Nhóm EV 015', 'ACTIVE'),
('16', 'Nhóm EV 016', 'ACTIVE'),
('17', 'Nhóm EV 017', 'ACTIVE'),
('18', 'Nhóm EV 018', 'ACTIVE'),
('19', 'Nhóm EV 019', 'ACTIVE'),
('20', 'Nhóm EV 020', 'ACTIVE'),
('21', 'Nhóm EV 021', 'ACTIVE'),
('22', 'Nhóm EV 022', 'ACTIVE'),
('23', 'Nhóm EV 023', 'ACTIVE'),
('24', 'Nhóm EV 024', 'ACTIVE'),
('25', 'Nhóm EV 025', 'ACTIVE'),
('26', 'Nhóm EV 026', 'ACTIVE'),
('27', 'Nhóm EV 027', 'ACTIVE'),
('28', 'Nhóm EV 028', 'ACTIVE'),
('29', 'Nhóm EV 029', 'ACTIVE'),
('30', 'Nhóm EV 030', 'ACTIVE'),
('31', 'Nhóm EV 031', 'ACTIVE'),
('32', 'Nhóm EV 032', 'ACTIVE'),
('33', 'Nhóm EV 033', 'ACTIVE'),
('34', 'Nhóm EV 034', 'ACTIVE'),
('35', 'Nhóm EV 035', 'ACTIVE'),
('36', 'Nhóm EV 036', 'ACTIVE'),
('37', 'Nhóm EV 037', 'ACTIVE'),
('38', 'Nhóm EV 038', 'ACTIVE'),
('39', 'Nhóm EV 039', 'ACTIVE'),
('40', 'Nhóm EV 040', 'ACTIVE'),
('41', 'Nhóm EV 041', 'ACTIVE'),
('42', 'Nhóm EV 042', 'ACTIVE'),
('43', 'Nhóm EV 043', 'ACTIVE'),
('44', 'Nhóm EV 044', 'ACTIVE'),
('45', 'Nhóm EV 045', 'ACTIVE'),
('46', 'Nhóm EV 046', 'ACTIVE'),
('47', 'Nhóm EV 047', 'ACTIVE'),
('48', 'Nhóm EV 048', 'ACTIVE'),
('49', 'Nhóm EV 049', 'ACTIVE'),
('50', 'Nhóm EV 050', 'ACTIVE'),
('51', 'Nhóm EV 051', 'ACTIVE'),
('52', 'Nhóm EV 052', 'ACTIVE'),
('53', 'Nhóm EV 053', 'ACTIVE'),
('54', 'Nhóm EV 054', 'ACTIVE'),
('55', 'Nhóm EV 055', 'ACTIVE'),
('56', 'Nhóm EV 056', 'ACTIVE'),
('57', 'Nhóm EV 057', 'ACTIVE'),
('58', 'Nhóm EV 058', 'ACTIVE'),
('59', 'Nhóm EV 059', 'ACTIVE'),
('60', 'Nhóm EV 060', 'ACTIVE'),
('61', 'Nhóm EV 061', 'ACTIVE'),
('62', 'Nhóm EV 062', 'ACTIVE'),
('63', 'Nhóm EV 063', 'ACTIVE'),
('64', 'Nhóm EV 064', 'ACTIVE'),
('65', 'Nhóm EV 065', 'ACTIVE'),
('66', 'Nhóm EV 066', 'ACTIVE'),
('67', 'Nhóm EV 067', 'ACTIVE'),
('68', 'Nhóm EV 068', 'ACTIVE'),
('69', 'Nhóm EV 069', 'ACTIVE'),
('70', 'Nhóm EV 070', 'ACTIVE'),
('71', 'Nhóm EV 071', 'ACTIVE'),
('72', 'Nhóm EV 072', 'ACTIVE'),
('73', 'Nhóm EV 073', 'ACTIVE'),
('74', 'Nhóm EV 074', 'ACTIVE'),
('75', 'Nhóm EV 075', 'ACTIVE'),
('76', 'Nhóm EV 076', 'ACTIVE'),
('77', 'Nhóm EV 077', 'ACTIVE'),
('78', 'Nhóm EV 078', 'ACTIVE'),
('79', 'Nhóm EV 079', 'ACTIVE'),
('80', 'Nhóm EV 080', 'ACTIVE'),
('81', 'Nhóm EV 081', 'ACTIVE'),
('82', 'Nhóm EV 082', 'ACTIVE'),
('83', 'Nhóm EV 083', 'ACTIVE'),
('84', 'Nhóm EV 084', 'ACTIVE'),
('85', 'Nhóm EV 085', 'ACTIVE'),
('86', 'Nhóm EV 086', 'ACTIVE'),
('87', 'Nhóm EV 087', 'ACTIVE'),
('88', 'Nhóm EV 088', 'ACTIVE'),
('89', 'Nhóm EV 089', 'ACTIVE'),
('90', 'Nhóm EV 090', 'ACTIVE'),
('91', 'Nhóm EV 091', 'ACTIVE'),
('92', 'Nhóm EV 092', 'ACTIVE'),
('93', 'Nhóm EV 093', 'ACTIVE'),
('94', 'Nhóm EV 094', 'ACTIVE'),
('95', 'Nhóm EV 095', 'ACTIVE'),
('96', 'Nhóm EV 096', 'ACTIVE'),
('97', 'Nhóm EV 097', 'ACTIVE'),
('98', 'Nhóm EV 098', 'ACTIVE'),
('99', 'Nhóm EV 099', 'ACTIVE'),
('100', 'Nhóm EV 100', 'ACTIVE');

INSERT INTO vehicles (vehicle_id, external_vehicle_id, vehicle_name, license_plate, vehicle_type, group_id, status) VALUES
(1, '1', 'Xe EV 001', '51A-00001', 'SUV', '1', 'AVAILABLE'),
(2, '2', 'Xe EV 002', '51A-00002', 'Hatchback', '2', 'AVAILABLE'),
(3, '3', 'Xe EV 003', '51A-00003', 'Truck', '3', 'AVAILABLE'),
(4, '4', 'Xe EV 004', '51A-00004', 'Van', '4', 'AVAILABLE'),
(5, '5', 'Xe EV 005', '51A-00005', 'Sedan', '5', 'AVAILABLE'),
(6, '6', 'Xe EV 006', '51A-00006', 'SUV', '6', 'AVAILABLE'),
(7, '7', 'Xe EV 007', '51A-00007', 'Hatchback', '7', 'AVAILABLE'),
(8, '8', 'Xe EV 008', '51A-00008', 'Truck', '8', 'AVAILABLE'),
(9, '9', 'Xe EV 009', '51A-00009', 'Van', '9', 'AVAILABLE'),
(10, '10', 'Xe EV 010', '51A-00010', 'Sedan', '10', 'AVAILABLE'),
(11, '11', 'Xe EV 011', '51A-00011', 'SUV', '11', 'AVAILABLE'),
(12, '12', 'Xe EV 012', '51A-00012', 'Hatchback', '12', 'AVAILABLE'),
(13, '13', 'Xe EV 013', '51A-00013', 'Truck', '13', 'AVAILABLE'),
(14, '14', 'Xe EV 014', '51A-00014', 'Van', '14', 'AVAILABLE'),
(15, '15', 'Xe EV 015', '51A-00015', 'Sedan', '15', 'AVAILABLE'),
(16, '16', 'Xe EV 016', '51A-00016', 'SUV', '16', 'AVAILABLE'),
(17, '17', 'Xe EV 017', '51A-00017', 'Hatchback', '17', 'AVAILABLE'),
(18, '18', 'Xe EV 018', '51A-00018', 'Truck', '18', 'AVAILABLE'),
(19, '19', 'Xe EV 019', '51A-00019', 'Van', '19', 'AVAILABLE'),
(20, '20', 'Xe EV 020', '51A-00020', 'Sedan', '20', 'AVAILABLE'),
(21, '21', 'Xe EV 021', '51A-00021', 'SUV', '21', 'AVAILABLE'),
(22, '22', 'Xe EV 022', '51A-00022', 'Hatchback', '22', 'AVAILABLE'),
(23, '23', 'Xe EV 023', '51A-00023', 'Truck', '23', 'AVAILABLE'),
(24, '24', 'Xe EV 024', '51A-00024', 'Van', '24', 'AVAILABLE'),
(25, '25', 'Xe EV 025', '51A-00025', 'Sedan', '25', 'AVAILABLE'),
(26, '26', 'Xe EV 026', '51A-00026', 'SUV', '26', 'AVAILABLE'),
(27, '27', 'Xe EV 027', '51A-00027', 'Hatchback', '27', 'AVAILABLE'),
(28, '28', 'Xe EV 028', '51A-00028', 'Truck', '28', 'AVAILABLE'),
(29, '29', 'Xe EV 029', '51A-00029', 'Van', '29', 'AVAILABLE'),
(30, '30', 'Xe EV 030', '51A-00030', 'Sedan', '30', 'AVAILABLE'),
(31, '31', 'Xe EV 031', '51A-00031', 'SUV', '31', 'AVAILABLE'),
(32, '32', 'Xe EV 032', '51A-00032', 'Hatchback', '32', 'AVAILABLE'),
(33, '33', 'Xe EV 033', '51A-00033', 'Truck', '33', 'AVAILABLE'),
(34, '34', 'Xe EV 034', '51A-00034', 'Van', '34', 'AVAILABLE'),
(35, '35', 'Xe EV 035', '51A-00035', 'Sedan', '35', 'AVAILABLE'),
(36, '36', 'Xe EV 036', '51A-00036', 'SUV', '36', 'AVAILABLE'),
(37, '37', 'Xe EV 037', '51A-00037', 'Hatchback', '37', 'AVAILABLE'),
(38, '38', 'Xe EV 038', '51A-00038', 'Truck', '38', 'AVAILABLE'),
(39, '39', 'Xe EV 039', '51A-00039', 'Van', '39', 'AVAILABLE'),
(40, '40', 'Xe EV 040', '51A-00040', 'Sedan', '40', 'AVAILABLE'),
(41, '41', 'Xe EV 041', '51A-00041', 'SUV', '41', 'AVAILABLE'),
(42, '42', 'Xe EV 042', '51A-00042', 'Hatchback', '42', 'AVAILABLE'),
(43, '43', 'Xe EV 043', '51A-00043', 'Truck', '43', 'AVAILABLE'),
(44, '44', 'Xe EV 044', '51A-00044', 'Van', '44', 'AVAILABLE'),
(45, '45', 'Xe EV 045', '51A-00045', 'Sedan', '45', 'AVAILABLE'),
(46, '46', 'Xe EV 046', '51A-00046', 'SUV', '46', 'AVAILABLE'),
(47, '47', 'Xe EV 047', '51A-00047', 'Hatchback', '47', 'AVAILABLE'),
(48, '48', 'Xe EV 048', '51A-00048', 'Truck', '48', 'AVAILABLE'),
(49, '49', 'Xe EV 049', '51A-00049', 'Van', '49', 'AVAILABLE'),
(50, '50', 'Xe EV 050', '51A-00050', 'Sedan', '50', 'AVAILABLE'),
(51, '51', 'Xe EV 051', '51A-00051', 'SUV', '51', 'AVAILABLE'),
(52, '52', 'Xe EV 052', '51A-00052', 'Hatchback', '52', 'AVAILABLE'),
(53, '53', 'Xe EV 053', '51A-00053', 'Truck', '53', 'AVAILABLE'),
(54, '54', 'Xe EV 054', '51A-00054', 'Van', '54', 'AVAILABLE'),
(55, '55', 'Xe EV 055', '51A-00055', 'Sedan', '55', 'AVAILABLE'),
(56, '56', 'Xe EV 056', '51A-00056', 'SUV', '56', 'AVAILABLE'),
(57, '57', 'Xe EV 057', '51A-00057', 'Hatchback', '57', 'AVAILABLE'),
(58, '58', 'Xe EV 058', '51A-00058', 'Truck', '58', 'AVAILABLE'),
(59, '59', 'Xe EV 059', '51A-00059', 'Van', '59', 'AVAILABLE'),
(60, '60', 'Xe EV 060', '51A-00060', 'Sedan', '60', 'AVAILABLE'),
(61, '61', 'Xe EV 061', '51A-00061', 'SUV', '61', 'AVAILABLE'),
(62, '62', 'Xe EV 062', '51A-00062', 'Hatchback', '62', 'AVAILABLE'),
(63, '63', 'Xe EV 063', '51A-00063', 'Truck', '63', 'AVAILABLE'),
(64, '64', 'Xe EV 064', '51A-00064', 'Van', '64', 'AVAILABLE'),
(65, '65', 'Xe EV 065', '51A-00065', 'Sedan', '65', 'AVAILABLE'),
(66, '66', 'Xe EV 066', '51A-00066', 'SUV', '66', 'AVAILABLE'),
(67, '67', 'Xe EV 067', '51A-00067', 'Hatchback', '67', 'AVAILABLE'),
(68, '68', 'Xe EV 068', '51A-00068', 'Truck', '68', 'AVAILABLE'),
(69, '69', 'Xe EV 069', '51A-00069', 'Van', '69', 'AVAILABLE'),
(70, '70', 'Xe EV 070', '51A-00070', 'Sedan', '70', 'AVAILABLE'),
(71, '71', 'Xe EV 071', '51A-00071', 'SUV', '71', 'AVAILABLE'),
(72, '72', 'Xe EV 072', '51A-00072', 'Hatchback', '72', 'AVAILABLE'),
(73, '73', 'Xe EV 073', '51A-00073', 'Truck', '73', 'AVAILABLE'),
(74, '74', 'Xe EV 074', '51A-00074', 'Van', '74', 'AVAILABLE'),
(75, '75', 'Xe EV 075', '51A-00075', 'Sedan', '75', 'AVAILABLE'),
(76, '76', 'Xe EV 076', '51A-00076', 'SUV', '76', 'AVAILABLE'),
(77, '77', 'Xe EV 077', '51A-00077', 'Hatchback', '77', 'AVAILABLE'),
(78, '78', 'Xe EV 078', '51A-00078', 'Truck', '78', 'AVAILABLE'),
(79, '79', 'Xe EV 079', '51A-00079', 'Van', '79', 'AVAILABLE'),
(80, '80', 'Xe EV 080', '51A-00080', 'Sedan', '80', 'AVAILABLE'),
(81, '81', 'Xe EV 081', '51A-00081', 'SUV', '81', 'AVAILABLE'),
(82, '82', 'Xe EV 082', '51A-00082', 'Hatchback', '82', 'AVAILABLE'),
(83, '83', 'Xe EV 083', '51A-00083', 'Truck', '83', 'AVAILABLE'),
(84, '84', 'Xe EV 084', '51A-00084', 'Van', '84', 'AVAILABLE'),
(85, '85', 'Xe EV 085', '51A-00085', 'Sedan', '85', 'AVAILABLE'),
(86, '86', 'Xe EV 086', '51A-00086', 'SUV', '86', 'AVAILABLE'),
(87, '87', 'Xe EV 087', '51A-00087', 'Hatchback', '87', 'AVAILABLE'),
(88, '88', 'Xe EV 088', '51A-00088', 'Truck', '88', 'AVAILABLE'),
(89, '89', 'Xe EV 089', '51A-00089', 'Van', '89', 'AVAILABLE'),
(90, '90', 'Xe EV 090', '51A-00090', 'Sedan', '90', 'AVAILABLE'),
(91, '91', 'Xe EV 091', '51A-00091', 'SUV', '91', 'AVAILABLE'),
(92, '92', 'Xe EV 092', '51A-00092', 'Hatchback', '92', 'AVAILABLE'),
(93, '93', 'Xe EV 093', '51A-00093', 'Truck', '93', 'AVAILABLE'),
(94, '94', 'Xe EV 094', '51A-00094', 'Van', '94', 'AVAILABLE'),
(95, '95', 'Xe EV 095', '51A-00095', 'Sedan', '95', 'AVAILABLE'),
(96, '96', 'Xe EV 096', '51A-00096', 'SUV', '96', 'AVAILABLE'),
(97, '97', 'Xe EV 097', '51A-00097', 'Hatchback', '97', 'AVAILABLE'),
(98, '98', 'Xe EV 098', '51A-00098', 'Truck', '98', 'AVAILABLE'),
(99, '99', 'Xe EV 099', '51A-00099', 'Van', '99', 'AVAILABLE'),
(100, '100', 'Xe EV 100', '51A-00100', 'Sedan', '100', 'AVAILABLE'),
(101, '101', 'Xe EV EXTRA 001', '51B-00001', 'SUV', '1', 'AVAILABLE'),
(102, '102', 'Xe EV EXTRA 002', '51B-00002', 'Hatchback', '2', 'AVAILABLE'),
(103, '103', 'Xe EV EXTRA 003', '51B-00003', 'Truck', '3', 'AVAILABLE'),
(104, '104', 'Xe EV EXTRA 004', '51B-00004', 'Van', '4', 'AVAILABLE'),
(105, '105', 'Xe EV EXTRA 005', '51B-00005', 'Sedan', '5', 'AVAILABLE'),
(106, '106', 'Xe EV EXTRA 006', '51B-00006', 'SUV', '6', 'AVAILABLE'),
(107, '107', 'Xe EV EXTRA 007', '51B-00007', 'Hatchback', '7', 'AVAILABLE'),
(108, '108', 'Xe EV EXTRA 008', '51B-00008', 'Truck', '8', 'AVAILABLE'),
(109, '109', 'Xe EV EXTRA 009', '51B-00009', 'Van', '9', 'AVAILABLE'),
(110, '110', 'Xe EV EXTRA 010', '51B-00010', 'Sedan', '10', 'AVAILABLE');

USE ai_ev;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE ownership_info;
SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO ownership_info (user_id, vehicle_id, group_id, ownership_percentage, role) VALUES
(484, 1, 1, 100.0, 'OWNER'),
(485, 2, 2, 100.0, 'OWNER'),
(486, 3, 3, 100.0, 'OWNER'),
(487, 4, 4, 100.0, 'OWNER'),
(488, 5, 5, 100.0, 'OWNER'),
(489, 6, 6, 100.0, 'OWNER'),
(490, 7, 7, 100.0, 'OWNER'),
(491, 8, 8, 100.0, 'OWNER'),
(492, 9, 9, 100.0, 'OWNER'),
(493, 10, 10, 100.0, 'OWNER'),
(494, 11, 11, 100.0, 'OWNER'),
(495, 12, 12, 100.0, 'OWNER'),
(496, 13, 13, 100.0, 'OWNER'),
(497, 14, 14, 100.0, 'OWNER'),
(498, 15, 15, 100.0, 'OWNER'),
(499, 16, 16, 100.0, 'OWNER'),
(500, 17, 17, 100.0, 'OWNER'),
(501, 18, 18, 100.0, 'OWNER'),
(502, 19, 19, 100.0, 'OWNER'),
(503, 20, 20, 100.0, 'OWNER'),
(504, 21, 21, 100.0, 'OWNER'),
(505, 22, 22, 100.0, 'OWNER'),
(506, 23, 23, 100.0, 'OWNER'),
(507, 24, 24, 100.0, 'OWNER'),
(508, 25, 25, 100.0, 'OWNER'),
(509, 26, 26, 100.0, 'OWNER'),
(510, 27, 27, 100.0, 'OWNER'),
(511, 28, 28, 100.0, 'OWNER'),
(512, 29, 29, 100.0, 'OWNER'),
(513, 30, 30, 100.0, 'OWNER'),
(514, 31, 31, 100.0, 'OWNER'),
(515, 32, 32, 100.0, 'OWNER'),
(516, 33, 33, 100.0, 'OWNER'),
(517, 34, 34, 100.0, 'OWNER'),
(518, 35, 35, 100.0, 'OWNER'),
(519, 36, 36, 100.0, 'OWNER'),
(520, 37, 37, 100.0, 'OWNER'),
(521, 38, 38, 100.0, 'OWNER'),
(522, 39, 39, 100.0, 'OWNER'),
(523, 40, 40, 100.0, 'OWNER'),
(524, 41, 41, 100.0, 'OWNER'),
(525, 42, 42, 100.0, 'OWNER'),
(526, 43, 43, 100.0, 'OWNER'),
(527, 44, 44, 100.0, 'OWNER'),
(528, 45, 45, 100.0, 'OWNER'),
(529, 46, 46, 100.0, 'OWNER'),
(530, 47, 47, 100.0, 'OWNER'),
(531, 48, 48, 100.0, 'OWNER'),
(532, 49, 49, 100.0, 'OWNER'),
(533, 50, 50, 100.0, 'OWNER'),
(534, 51, 51, 100.0, 'OWNER'),
(535, 52, 52, 100.0, 'OWNER'),
(536, 53, 53, 100.0, 'OWNER'),
(537, 54, 54, 100.0, 'OWNER'),
(538, 55, 55, 100.0, 'OWNER'),
(539, 56, 56, 100.0, 'OWNER'),
(540, 57, 57, 100.0, 'OWNER'),
(541, 58, 58, 100.0, 'OWNER'),
(542, 59, 59, 100.0, 'OWNER'),
(543, 60, 60, 100.0, 'OWNER'),
(544, 61, 61, 100.0, 'OWNER'),
(545, 62, 62, 100.0, 'OWNER'),
(546, 63, 63, 100.0, 'OWNER'),
(547, 64, 64, 100.0, 'OWNER'),
(548, 65, 65, 100.0, 'OWNER'),
(549, 66, 66, 100.0, 'OWNER'),
(550, 67, 67, 100.0, 'OWNER'),
(551, 68, 68, 100.0, 'OWNER'),
(552, 69, 69, 100.0, 'OWNER'),
(553, 70, 70, 100.0, 'OWNER'),
(554, 71, 71, 100.0, 'OWNER'),
(555, 72, 72, 100.0, 'OWNER'),
(556, 73, 73, 100.0, 'OWNER'),
(557, 74, 74, 100.0, 'OWNER'),
(558, 75, 75, 100.0, 'OWNER'),
(559, 76, 76, 100.0, 'OWNER'),
(560, 77, 77, 100.0, 'OWNER'),
(561, 78, 78, 100.0, 'OWNER'),
(562, 79, 79, 100.0, 'OWNER'),
(563, 80, 80, 100.0, 'OWNER'),
(564, 81, 81, 100.0, 'OWNER'),
(565, 82, 82, 100.0, 'OWNER'),
(566, 83, 83, 100.0, 'OWNER'),
(567, 84, 84, 100.0, 'OWNER'),
(568, 85, 85, 100.0, 'OWNER'),
(569, 86, 86, 100.0, 'OWNER'),
(570, 87, 87, 100.0, 'OWNER'),
(571, 88, 88, 100.0, 'OWNER'),
(572, 89, 89, 100.0, 'OWNER'),
(573, 90, 90, 100.0, 'OWNER'),
(574, 91, 91, 100.0, 'OWNER'),
(575, 92, 92, 100.0, 'OWNER'),
(576, 93, 93, 100.0, 'OWNER'),
(577, 94, 94, 100.0, 'OWNER'),
(578, 95, 95, 100.0, 'OWNER'),
(579, 96, 96, 100.0, 'OWNER'),
(580, 97, 97, 100.0, 'OWNER'),
(581, 98, 98, 100.0, 'OWNER'),
(582, 99, 99, 100.0, 'OWNER'),
(583, 100, 100, 100.0, 'OWNER'),
(484, 101, 1, 100.0, 'OWNER'),
(485, 102, 2, 100.0, 'OWNER'),
(486, 103, 3, 100.0, 'OWNER'),
(487, 104, 4, 100.0, 'OWNER'),
(488, 105, 5, 100.0, 'OWNER'),
(489, 106, 6, 100.0, 'OWNER'),
(490, 107, 7, 100.0, 'OWNER'),
(491, 108, 8, 100.0, 'OWNER'),
(492, 109, 9, 100.0, 'OWNER'),
(493, 110, 10, 100.0, 'OWNER');



/* ==============================
   PHẦN 8: KIỂM TRA
   ============================== */

SHOW DATABASES LIKE 'co_ownership%';
SHOW DATABASES LIKE 'ai_ev';

SELECT 'co_ownership_booking' AS db, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'co_ownership_booking';

SELECT 'co_ownership_booking_admin' AS db, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'co_ownership_booking_admin';

SELECT 'ai_ev' AS db, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'ai_ev';

-- Đếm số bản ghi chính
SELECT 'co_ownership_booking', 'vehicles', COUNT(*) FROM co_ownership_booking.vehicles
UNION ALL
SELECT 'co_ownership_booking', 'reservations', COUNT(*) FROM co_ownership_booking.reservations
UNION ALL
SELECT 'co_ownership_booking_admin', 'vehicles', COUNT(*) FROM co_ownership_booking_admin.vehicles
UNION ALL
SELECT 'co_ownership_booking_admin', 'reservations', COUNT(*) FROM co_ownership_booking_admin.reservations
UNION ALL
SELECT 'ai_ev', 'ownership_info', COUNT(*) FROM ai_ev.ownership_info;