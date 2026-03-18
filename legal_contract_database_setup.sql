-- ===============================
-- DATABASE: legal_contract
-- Schema + Sample Data for Legal Contract Service
-- ===============================

-- Xóa tất cả tables trước để tránh lỗi foreign key
DROP DATABASE IF EXISTS legal_contract;

-- Tạo lại database
CREATE DATABASE legal_contract CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal_contract;

-- ==========================================
-- TABLE DEFINITIONS
-- ==========================================

CREATE TABLE LegalContract (
    contract_id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    contract_code VARCHAR(100) NOT NULL,
    contract_status VARCHAR(50) DEFAULT 'pending',
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    signed_date TIMESTAMP NULL DEFAULT NULL,
    -- Note: Foreign key removed to allow independent microservice operation
    -- Group validation should be done at application level
    UNIQUE KEY uk_contract_code (contract_code),
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ContractHistory (
    history_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES LegalContract(contract_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ContractSignatures (
    signature_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    signer_id INT NOT NULL,
    signature_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES LegalContract(contract_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE CheckInOutLog (
    checkinout_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    checkin_time TIMESTAMP NULL DEFAULT NULL,
    checkout_time TIMESTAMP NULL DEFAULT NULL,
    status VARCHAR(50) DEFAULT 'checked-in',
    qr_scan_time TIMESTAMP NULL DEFAULT NULL,
    signature_time TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (contract_id) REFERENCES LegalContract(contract_id) ON DELETE CASCADE,
    -- Note: Foreign key to vehicle_management removed for microservice independence
    -- Vehicle validation should be done at application level
    INDEX idx_vehicle_id (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- SAMPLE DATA
-- ==========================================
-- Note: group_id values are examples. In production, ensure these IDs exist in your group management system.
-- Foreign key constraint has been removed for microservice independence.

INSERT INTO LegalContract (group_id, contract_code, contract_status, creation_date, signed_date) VALUES
(1, 'LC-ALPHA-2024-001', 'signed', '2024-03-01 10:00:00', '2024-03-05 15:30:00'),
(2, 'LC-BETA-2024-002', 'pending', '2024-05-12 09:45:00', NULL);

INSERT INTO ContractHistory (contract_id, action, action_date) VALUES
(1, 'Contract created by admin.', '2024-03-01 10:00:00'),
(1, 'Contract reviewed by group owner.', '2024-03-03 11:15:00'),
(1, 'Contract signed by all parties.', '2024-03-05 15:30:00'),
(2, 'Contract draft generated for review.', '2024-05-12 09:45:00'),
(2, 'Pending signature from external partner.', '2024-05-14 14:20:00');

INSERT INTO ContractSignatures (contract_id, signer_id, signature_date) VALUES
(1, 5001, '2024-03-05 14:50:00'),
(1, 5002, '2024-03-05 15:05:00'),
(1, 5003, '2024-03-05 15:20:00');

-- Note: vehicle_id values are examples. In production, ensure these IDs exist in your vehicle management system.
INSERT INTO CheckInOutLog (contract_id, vehicle_id, checkin_time, checkout_time, status, qr_scan_time, signature_time) VALUES
(1, 1, '2024-03-06 08:00:00', '2024-03-06 18:30:00', 'checked-out', '2024-03-06 07:55:00', '2024-03-06 18:35:00'),
(1, 2, '2024-03-07 08:15:00', NULL, 'checked-in', '2024-03-07 08:10:00', NULL),
(2, 3, '2024-05-15 09:00:00', NULL, 'checked-in', '2024-05-15 08:55:00', NULL);

-- ==========================================
-- QUICK VIEWS
-- ==========================================

SELECT '=== LEGAL CONTRACTS ===' AS section;
SELECT contract_id, contract_code, contract_status, signed_date FROM LegalContract;

SELECT '=== CONTRACT HISTORIES ===' AS section;
SELECT history_id, contract_id, action, action_date FROM ContractHistory;

SELECT '=== CONTRACT SIGNATURES ===' AS section;
SELECT signature_id, contract_id, signer_id, signature_date FROM ContractSignatures;

SELECT '=== CHECK-IN / CHECK-OUT LOGS ===' AS section;
SELECT checkinout_id, contract_id, vehicle_id, status FROM CheckInOutLog;


