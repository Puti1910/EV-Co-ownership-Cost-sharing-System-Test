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

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE reservation_checkpoints;
TRUNCATE reservations;
TRUNCATE vehicles;
TRUNCATE vehicle_groups;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO vehicle_groups (group_id, group_name, description, creation_date, active) VALUES
                                                                                          ('GRP001', 'Nhóm Tesla', 'Sở hữu chung xe Tesla', '2024-01-10 09:00:00', 'ACTIVE'),
                                                                                          ('GRP002', 'Nhóm VinFast', 'Sở hữu chung xe VinFast', '2024-02-05 08:00:00', 'ACTIVE');

INSERT INTO vehicles (vehicle_id, vehicle_name, license_plate, vehicle_type, group_id, status) VALUES
                                                                                                   (1, 'Tesla Model 3', '30A-12345', 'ELECTRIC', 'GRP001', 'AVAILABLE'),
                                                                                                   (2, 'VinFast VF8', '30B-67890', 'ELECTRIC', 'GRP002', 'AVAILABLE'),
                                                                                                   (3, 'Tesla Model Y', '30A-22222', 'ELECTRIC', 'GRP001', 'MAINTENANCE');

INSERT INTO reservations (reservation_id, vehicle_id, user_id, start_datetime, end_datetime, purpose, status) VALUES
                                                                                                                  (1, 1, 101, '2024-11-15 08:00:00', '2024-11-15 12:00:00', 'Đi công tác Hà Nội', 'BOOKED'),
                                                                                                                  (2, 1, 102, '2024-11-15 14:00:00', '2024-11-15 18:00:00', 'Đi chơi cuối tuần', 'BOOKED'),
                                                                                                                  (3, 2, 103, '2024-11-16 09:00:00', '2024-11-16 17:00:00', 'Đi Đà Lạt', 'BOOKED'),
                                                                                                                  (4, 3, 104, '2024-11-17 07:00:00', '2024-11-17 19:00:00', 'Đi thăm gia đình', 'BOOKED');

INSERT INTO reservation_checkpoints (reservation_id, checkpoint_type, status, qr_token, issued_by, expires_at, notes)
VALUES
    (1, 'CHECK_IN', 'PENDING', 'QR-CHKIN-1', 'Admin', DATE_ADD(NOW(), INTERVAL 1 DAY), 'Check-in'),
    (1, 'CHECK_OUT', 'PENDING', 'QR-CHKOUT-1', 'Admin', DATE_ADD(NOW(), INTERVAL 2 DAY), 'Check-out');

/* ==============================
   PHẦN 6: DỮ LIỆU MẪU (ReservationAdminService)
   ============================== */

USE co_ownership_booking_admin;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE reservations;
TRUNCATE vehicles;
TRUNCATE `groups`;
TRUNCATE admin_users;
TRUNCATE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO admin_users (username, email, password, role, created_at) VALUES
                                                                          ('admin', 'admin@example.com', 'admin123', 'ADMIN', NOW()),
                                                                          ('manager', 'manager@example.com', 'manager123', 'MANAGER', NOW());

INSERT INTO `groups` (name, description, created_at) VALUES
                                                         ('Nhóm Tesla', 'Nhóm sở hữu Tesla', NOW()),
                                                         ('Nhóm VinFast', 'Nhóm sở hữu VinFast', NOW());

INSERT INTO vehicles (vehicle_name, vehicle_type, license_plate, group_id, status) VALUES
                                                                                       ('Tesla Model 3', 'ELECTRIC', '30A-12345', 1, 'AVAILABLE'),
                                                                                       ('VinFast VF8', 'ELECTRIC', '30B-67890', 2, 'AVAILABLE');

INSERT INTO users (username, email, full_name) VALUES
                                                   ('user101', 'user101@example.com', 'Nguyễn Văn A'),
                                                   ('user102', 'user102@example.com', 'Trần Thị B');

INSERT INTO reservations (reservation_id, user_id, vehicle_id, vehicle_name, user_name,
                          start_datetime, end_datetime, purpose, status, created_at)
VALUES
    (1, 1, 1, 'Tesla Model 3', 'Nguyễn Văn A', '2024-11-15 08:00:00', '2024-11-15 12:00:00', 'Đi công tác Hà Nội', 'BOOKED', NOW()),
    (2, 2, 1, 'Tesla Model 3', 'Trần Thị B', '2024-11-15 14:00:00', '2024-11-15 18:00:00', 'Đi chơi cuối tuần', 'BOOKED', NOW());

/* ==============================
   PHẦN 7: DỮ LIỆU MẪU (AIService)
   ============================== */

USE ai_ev;

TRUNCATE ai_recommendations;
TRUNCATE fairness_score;
TRUNCATE usage_analysis;
TRUNCATE ownership_info;

INSERT INTO ownership_info (user_id, vehicle_id, group_id, ownership_percentage, role, joined_date)
VALUES
    (101, 1, 1, 40.0, 'OWNER', '2024-01-15'),
    (102, 1, 1, 35.0, 'MEMBER', '2024-01-15'),
    (103, 2, 2, 50.0, 'OWNER', '2024-02-01');

INSERT INTO usage_analysis (user_id, vehicle_id, group_id, total_hours_used, total_kilometers,
                            booking_count, cancellation_count, usage_percentage, cost_incurred,
                            period_start, period_end)
VALUES
    (101, 1, 1, 120, 1500, 15, 2, 45, 5000000, '2024-01-01', '2024-11-14'),
    (102, 1, 1, 80, 1000, 10, 1, 30, 3500000, '2024-01-01', '2024-11-14');

INSERT INTO fairness_score (user_id, vehicle_id, group_id, ownership_percentage, usage_percentage,
                            difference, fairness_score, priority, period_start, period_end)
VALUES
    (101, 1, 1, 40.0, 45.0, 5.0, 85.0, 'NORMAL', '2024-01-01', '2024-11-14'),
    (102, 1, 1, 35.0, 30.0, -5.0, 90.0, 'HIGH', '2024-01-01', '2024-11-14');

INSERT INTO ai_recommendations (group_id, vehicle_id, type, title, description, severity,
                                target_user_id, status, period_start, period_end, created_at)
VALUES
    (1, 1, 'USAGE_FAIRNESS', 'Ưu tiên cho user 102', 'User 102 dùng ít hơn quyền sở hữu', 'INFO', 102, 'ACTIVE', '2024-11-15', '2024-12-15', NOW());

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