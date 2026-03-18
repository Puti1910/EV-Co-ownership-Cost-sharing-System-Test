DROP DATABASE IF EXISTS vehicle_management;
CREATE DATABASE vehicle_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vehicle_management;

-- ========= 1. Danh mục nhóm xe =========
CREATE TABLE vehiclegroup (
                              group_id        INT AUTO_INCREMENT PRIMARY KEY,
                              group_name      VARCHAR(255) NOT NULL,
                              description     TEXT,
                              creation_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========= 2. Xe thuộc nhóm =========
CREATE TABLE vehicle (
                         vehicle_id      VARCHAR(20) PRIMARY KEY,
                         group_id        INT NOT NULL,
                         vehicle_number  VARCHAR(20) NOT NULL,
                         vehicle_type    VARCHAR(50) NOT NULL,
                         status          VARCHAR(50) NOT NULL DEFAULT 'available',
                         FOREIGN KEY (group_id) REFERENCES vehiclegroup(group_id),
                         UNIQUE KEY uk_vehicle_number (vehicle_number)
) ENGINE=InnoDB;

-- ========= 3. Lịch sử sử dụng xe =========
CREATE TABLE vehiclehistory (
                                history_id      INT AUTO_INCREMENT PRIMARY KEY,
                                group_id        INT NOT NULL,
                                user_id         INT NOT NULL,
                                usage_start     TIMESTAMP NOT NULL,
                                usage_end       TIMESTAMP NULL,
                                FOREIGN KEY (group_id) REFERENCES vehiclegroup(group_id)
) ENGINE=InnoDB;

-- ========= 4. Danh mục GÓI DỊCH VỤ =========
CREATE TABLE service (
                         service_id      VARCHAR(20) PRIMARY KEY,
                         service_name    VARCHAR(255) NOT NULL,
                         service_type    VARCHAR(50) NOT NULL,   -- maintenance / repair / inspection ...
                         created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========= 5. Đăng ký dịch vụ cho xe =========
CREATE TABLE vehicleservice (
                                service_id      VARCHAR(20) NOT NULL,
                                vehicle_id      VARCHAR(20) NOT NULL,
                                service_name    VARCHAR(255),          -- redundantly lưu để admin xem nhanh
                                service_description TEXT,
                                service_type    VARCHAR(50),
                                request_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                status          VARCHAR(50) DEFAULT 'pending',
                                completion_date TIMESTAMP NULL,
                                group_ref_id    INT NULL,              -- mapping sang group_management
                                requested_by_user_id INT NULL,
                                requested_by_user_name VARCHAR(150),
                                preferred_start_datetime DATETIME NULL,
                                preferred_end_datetime   DATETIME NULL,
                                PRIMARY KEY (service_id, vehicle_id),
                                FOREIGN KEY (service_id) REFERENCES service(service_id),
                                FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
) ENGINE=InnoDB;

-- ========= 6. Dữ liệu mẫu =========
INSERT INTO vehiclegroup (group_name, description, creation_date) VALUES
                                                                      ('EV Fleet Alpha', 'Nhóm xe điện phục vụ nội thành và khách hàng doanh nghiệp.', '2024-01-15 09:00:00'),
                                                                      ('EV Fleet Beta',  'Nhóm xe điện phục vụ khách hàng khu vực ngoại thành.',      '2024-02-10 08:30:00');

INSERT INTO vehicle (vehicle_id, group_id, vehicle_number, vehicle_type, status) VALUES
                                                                         ('1', 1, 'EV-ALPHA-01', 'SUV',       'available'),
                                                                         ('2', 1, 'EV-ALPHA-02', 'Sedan',     'in_service'),
                                                                         ('3', 2, 'EV-BETA-01',  'Hatchback', 'maintenance');

INSERT INTO vehiclehistory (group_id, user_id, usage_start, usage_end) VALUES
                                                                           (1, 101, '2024-10-01 08:00:00', '2024-10-01 18:00:00'),
                                                                           (1, 102, '2024-10-03 07:30:00', '2024-10-03 19:15:00'),
                                                                           (2, 201, '2024-10-05 09:00:00', NULL);

INSERT INTO service (service_id, service_name, service_type) VALUES
                                                                 ('SRV001', 'Bảo dưỡng định kỳ',     'maintenance'),
                                                                 ('SRV002', 'Thay lốp trước',        'repair'),
                                                                 ('SRV003', 'Kiểm định an toàn',     'inspection'),
                                                                 ('SRV004', 'Sửa chữa hệ thống phanh','repair');

INSERT INTO vehicleservice
(service_id, vehicle_id, service_name, service_description, service_type, request_date, status, completion_date, group_ref_id, requested_by_user_id, requested_by_user_name)
VALUES
    ('SRV001', '1', 'Bảo dưỡng định kỳ',      'Kiểm tra tổng quát pin và hệ thống điện.', 'maintenance', '2024-10-02 10:30:00', 'pending',   NULL,                    1, 101, 'Nam Vũ'),
    ('SRV002', '1', 'Thay lốp trước',         'Lốp trước mòn, cần thay mới.',             'repair',      '2024-10-04 11:00:00', 'in_progress', NULL,                  1, 101, 'Nam Vũ'),
    ('SRV003', '2', 'Kiểm định an toàn',      'Kiểm định an toàn cuối năm.',              'inspection',  '2024-09-28 09:15:00', 'pending',    NULL,                    1, 102, 'Lan Trần'),
    ('SRV004', '3', 'Sửa chữa hệ thống phanh','Kiểm tra và sửa chữa hệ thống phanh.',    'repair',      '2024-10-06 14:00:00', 'completed', '2024-10-08 10:20:00', 2, 201, 'Hải Phạm');

-- ========= 7. View nhanh để admin kiểm tra =========
SELECT '=== GROUPS ===' AS section;
SELECT group_id, group_name, creation_date FROM vehiclegroup;

SELECT '=== VEHICLES ===' AS section;
SELECT vehicle_id, vehicle_number, vehicle_type, status FROM vehicle;

SELECT '=== SERVICE CATALOG ===' AS section;
SELECT service_id, service_name, service_type FROM service;

SELECT '=== BOOKINGS ===' AS section;
SELECT service_id, vehicle_id, service_name, service_type, status, request_date FROM vehicleservice;
SET SQL_SAFE_UPDATES = 0;

DELETE FROM vehicle_management.vehicleservice;

SET SQL_SAFE_UPDATES = 1;    -- (tuỳ chọn, bật lại)