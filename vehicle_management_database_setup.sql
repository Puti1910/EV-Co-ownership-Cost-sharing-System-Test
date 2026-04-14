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
    requested_by_user_id BIGINT NULL,
    requested_by_user_name VARCHAR(150),
    preferred_start_datetime DATETIME NULL,
    preferred_end_datetime   DATETIME NULL,
    FOREIGN KEY (service_id) REFERENCES service(service_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
) ENGINE=InnoDB;

-- ========= 6. Dữ liệu mẫu lớn (50+ bản ghi mỗi bảng) =========

-- 10 Nhóm xe
INSERT INTO vehiclegroup (group_name, description) VALUES
('HCM Urban Fleet', 'Đội xe phục vụ khu vực nội thành TP.HCM'),
('Hanoi Express', 'Dịch vụ vận chuyển nhanh tại Hà Nội'),
('Da Nang Coastal', 'Nhóm xe du lịch khu vực miền Trung'),
('Can Tho Logistics', 'Vận tải hàng hóa khu vực miền Bắc'),
('Hai Phong Port', 'Hỗ trợ logistics khu vực cảng biển'),
('Long An Green', 'Đội xe điện thân thiện môi trường Long An'),
('Binh Duong Pro', 'Giải pháp di chuyển cho doanh nghiệp Bình Dương'),
('Dong Nai Commuter', 'Xe điện đưa đón nhân viên Đồng Nai'),
('Quang Ninh Tourism', 'Dịch vụ xe điện tham quan Vịnh Hạ Long'),
('Standard Support', 'Nhóm xe hỗ trợ kỹ thuật và cứu hộ');

-- Nạp thêm 40 nhóm xe để đủ 50
INSERT INTO vehiclegroup (group_name, description) SELECT CONCAT('Group ', id), 'Mô tả chi tiết nhóm xe' FROM (
    WITH RECURSIVE seq AS (SELECT 1 AS id UNION ALL SELECT id + 1 FROM seq WHERE id < 40)
    SELECT id + 10 AS id FROM seq
) AS d;

-- 50 Xe
INSERT INTO vehicle (group_id, vehicle_number, vehicle_type, status) VALUES
(1, '51A-123.45', 'Sedan', 'available'), (1, '51A-234.56', 'SUV', 'in_service'), (1, '51A-345.67', 'Sedan', 'maintenance'),
(2, '30B-111.11', 'SUV', 'available'), (2, '30B-222.22', 'SUV', 'in_service'), (2, '30B-333.33', 'Van', 'available'),
(3, '43C-444.44', 'Sedan', 'available'), (3, '43C-555.55', 'Hatchback', 'available'),
(4, '65D-666.66', 'Truck', 'available'), (5, '15E-777.77', 'Truck', 'in_service');

-- Nạp thêm 40 xe để đủ 50
INSERT INTO vehicle (group_id, vehicle_number, vehicle_type, status) SELECT 
    (MOD(id, 10) + 1), 
    CONCAT('EV-', id), 
    ELT(MOD(id, 4) + 1, 'Sedan', 'SUV', 'Hatchback', 'Electric Bus'),
    ELT(MOD(id, 3) + 1, 'available', 'in_service', 'maintenance')
FROM (
    WITH RECURSIVE seq AS (SELECT 1 AS id UNION ALL SELECT id + 1 FROM seq WHERE id < 40)
    SELECT id + 10 AS id FROM seq
) AS d;

-- 20 Loại dịch vụ
INSERT INTO service (service_name, service_type) VALUES
('Bảo dưỡng pin định kỳ', 'maintenance'),
('Kiểm tra phanh & lốp', 'maintenance'),
('Thay lốp xe', 'repair'),
('Sửa chữa động cơ điện', 'repair'),
('Vệ sinh nội ngoại thất', 'maintenance'),
('Kiểm định an toàn năm', 'inspection'),
('Cập nhật Firmware v2.0', 'inspection'),
('Thay thế kính chắn gió', 'repair'),
('Kiểm tra hệ thống điều hòa', 'maintenance'),
('Cứu hộ khẩn cấp', 'repair');

-- Thêm dịch vụ cho đủ 50
INSERT INTO service (service_name, service_type) SELECT CONCAT('Dịch vụ chuyên sâu ', id), ELT(MOD(id, 3) + 1, 'maintenance', 'repair', 'inspection') FROM (
    WITH RECURSIVE seq AS (SELECT 1 AS id UNION ALL SELECT id + 1 FROM seq WHERE id < 40)
    SELECT id + 10 AS id FROM seq
) AS d;

-- 50+ Vehicleservice (Đăng ký dịch vụ)
INSERT INTO vehicleservice (service_id, vehicle_id, service_name, service_description, service_type, status, group_ref_id, requested_by_user_id, requested_by_user_name)
SELECT 
    (MOD(id, 50) + 1),
    (MOD(id, 50) + 1),
    'Bảo trì định kỳ hệ số',
    'Kiểm tra toàn diện định kỳ theo quy trình chuẩn của hãng.',
    'maintenance',
    ELT(MOD(id, 3) + 1, 'pending', 'in_progress', 'completed'),
    (MOD(id, 10) + 1),
    (100 + id),
    CONCAT('User_', id)
FROM (
    WITH RECURSIVE seq AS (SELECT 1 AS id UNION ALL SELECT id + 1 FROM seq WHERE id < 60)
    SELECT id FROM seq
) AS d;