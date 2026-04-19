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

INSERT INTO vehicle_groups (group_id, group_name, active) VALUES
('1', 'Nhóm EV 001', 'ACTIVE'),
('2', 'Nhóm EV 002', 'ACTIVE');

INSERT INTO vehicles (vehicle_id, external_vehicle_id, vehicle_name, license_plate, vehicle_type, group_id, status) VALUES
(1, '1', 'Tesla Model 3', '30A-12345', 'ELECTRIC', '1', 'AVAILABLE'),
(2, '2', 'VinFast VF8', '30B-67890', 'ELECTRIC', '2', 'AVAILABLE');

INSERT INTO reservations (reservation_id, vehicle_id, user_id, start_datetime, end_datetime, purpose, status) VALUES
(1, 1, 3, '2024-11-15 08:00:00', '2024-11-15 12:00:00', 'Đi công tác Hà Nội', 'BOOKED');

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
('admin', 'admin@example.com', 'admin123', 'ADMIN', NOW());

INSERT INTO `groups` (name, description, created_at) VALUES
('Nhóm EV 001', 'Nhóm sở hữu Tesla', NOW());

INSERT INTO vehicles (id, vehicle_name, vehicle_type, license_plate, group_id, status) VALUES
(1, 'Tesla Model 3', 'ELECTRIC', '30A-12345', 1, 'AVAILABLE');

INSERT INTO users (username, email, full_name) VALUES
('namvohoai23', 'namvohoai23@gmail.com', 'Võ Hoài Nam');

/* ==============================
   PHẦN 7: DỮ LIỆU MẪU (AIService)
   ============================== */

USE ai_ev;

TRUNCATE ai_recommendations;
TRUNCATE fairness_score;
TRUNCATE usage_analysis;
TRUNCATE ownership_info;

INSERT INTO ownership_info (user_id, vehicle_id, group_id, ownership_percentage, role) VALUES
(3, 1, 1, 100.0, 'OWNER');

/* ==============================
   PHẦN 8: KIỂM TRA
   ============================== */

SHOW DATABASES LIKE 'co_ownership%';
SHOW DATABASES LIKE 'ai_ev';