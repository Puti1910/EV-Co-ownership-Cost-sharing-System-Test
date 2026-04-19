DROP DATABASE IF EXISTS vehicle_management;
CREATE DATABASE vehicle_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vehicle_management;

-- ========= 1. Danh mục nhóm xe =========
CREATE TABLE vehiclegroup (
    group_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name      VARCHAR(255) NOT NULL,
    description     TEXT,
    creation_date   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========= 2. Xe thuộc nhóm =========
CREATE TABLE vehicle (
    vehicle_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    vehicle_number  VARCHAR(20) NOT NULL,
    vehicle_type    VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'available',
    FOREIGN KEY (group_id) REFERENCES vehiclegroup(group_id),
    UNIQUE KEY uk_vehicle_number (vehicle_number)
) ENGINE=InnoDB;

-- ========= 3. Lịch sử sử dụng xe =========
CREATE TABLE vehiclehistory (
    history_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    usage_start     TIMESTAMP NOT NULL,
    usage_end       TIMESTAMP NULL,
    FOREIGN KEY (group_id) REFERENCES vehiclegroup(group_id)
) ENGINE=InnoDB;

-- ========= 4. Danh mục GÓI DỊCH VỤ =========
CREATE TABLE service (
    service_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name    VARCHAR(255) NOT NULL,
    service_type    VARCHAR(50) NOT NULL,   -- maintenance / repair / inspection ...
    created_date    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========= 5. Đăng ký dịch vụ cho xe =========
CREATE TABLE vehicleservice (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id      BIGINT NOT NULL,
    vehicle_id      BIGINT NOT NULL,
    service_name    VARCHAR(255),
    service_description TEXT,
    service_type    VARCHAR(50),
    request_date    DATETIME DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(50) DEFAULT 'pending',
    completion_date DATETIME NULL,
    group_ref_id    BIGINT NULL,
    requested_by_user_id BIGINT NOT NULL,
    requested_by_user_name VARCHAR(255),
    preferred_start_datetime DATETIME NULL,
    preferred_end_datetime   DATETIME NULL,
    FOREIGN KEY (service_id) REFERENCES service(service_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
) ENGINE=InnoDB;

-- ========= 6. Dữ liệu mẫu lớn (50+ bản ghi mỗi bảng) =========

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE vehicleservice;
TRUNCATE vehicle;
TRUNCATE vehiclegroup;
SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO vehiclegroup (group_id, group_name, description) VALUES
(1, 'Nhóm EV 001', 'Mô tả cho nhóm 1'),
(2, 'Nhóm EV 002', 'Mô tả cho nhóm 2'),
(3, 'Nhóm EV 003', 'Mô tả cho nhóm 3'),
(4, 'Nhóm EV 004', 'Mô tả cho nhóm 4'),
(5, 'Nhóm EV 005', 'Mô tả cho nhóm 5'),
(6, 'Nhóm EV 006', 'Mô tả cho nhóm 6'),
(7, 'Nhóm EV 007', 'Mô tả cho nhóm 7'),
(8, 'Nhóm EV 008', 'Mô tả cho nhóm 8'),
(9, 'Nhóm EV 009', 'Mô tả cho nhóm 9'),
(10, 'Nhóm EV 010', 'Mô tả cho nhóm 10'),
(11, 'Nhóm EV 011', 'Mô tả cho nhóm 11'),
(12, 'Nhóm EV 012', 'Mô tả cho nhóm 12'),
(13, 'Nhóm EV 013', 'Mô tả cho nhóm 13'),
(14, 'Nhóm EV 014', 'Mô tả cho nhóm 14'),
(15, 'Nhóm EV 015', 'Mô tả cho nhóm 15'),
(16, 'Nhóm EV 016', 'Mô tả cho nhóm 16'),
(17, 'Nhóm EV 017', 'Mô tả cho nhóm 17'),
(18, 'Nhóm EV 018', 'Mô tả cho nhóm 18'),
(19, 'Nhóm EV 019', 'Mô tả cho nhóm 19'),
(20, 'Nhóm EV 020', 'Mô tả cho nhóm 20');

INSERT INTO vehicle (vehicle_id, group_id, vehicle_number, vehicle_type, status) VALUES
(1, 1, '51A-00001', 'SUV', 'available'),
(2, 2, '51A-00002', 'Hatchback', 'available'),
(3, 3, '51A-00003', 'Truck', 'available');

INSERT INTO service (service_id, service_name, service_type) VALUES
(1, 'Dịch vụ Repair 001', 'repair'),
(2, 'Dịch vụ Inspection 002', 'inspection');

INSERT INTO vehicleservice (id, service_id, vehicle_id, service_name, service_description, service_type, status, group_ref_id, requested_by_user_id, requested_by_user_name) VALUES
(1, 1, 1, 'Yêu cầu dịch vụ 1', 'Mô tả chi tiết 1', 'maintenance', 'pending', 1, 484, 'User 484');

SELECT '✅ vehicle_management đã được khởi tạo' AS status_message;
