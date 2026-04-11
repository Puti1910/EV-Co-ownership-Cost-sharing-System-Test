-- ==========================================
-- DATABASE: CoOwnershipDB (User Account Service)
-- Phiên bản bám sát entity User hiện tại
-- ==========================================

DROP DATABASE IF EXISTS CoOwnershipDB;
CREATE DATABASE CoOwnershipDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE CoOwnershipDB;

-- ==========================================
-- BẢNG Users (khớp với entity User.java)
-- ==========================================

CREATE TABLE IF NOT EXISTS Users (
    user_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150),
    phone_number        VARCHAR(20) UNIQUE,
    is_verified         TINYINT(1) NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role                ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',

    profile_status      ENUM('PENDING','APPROVED','REJECTED','SUSPENDED')  DEFAULT 'PENDING',

    date_of_birth       DATE,

    id_card_number      VARCHAR(25) UNIQUE,
    id_card_issue_date  DATE,
    id_card_issue_place VARCHAR(150),

    license_number      VARCHAR(25) UNIQUE,
    license_class       VARCHAR(10),
    license_issue_date  DATE,
    license_expiry_date DATE,

    id_card_front_url   VARCHAR(512),
    id_card_back_url    VARCHAR(512),
    license_image_url   VARCHAR(512),
    portrait_image_url  VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON Users(role);
CREATE INDEX idx_users_profile_status ON Users(profile_status);

-- ==========================================
-- BẢNG Refresh Tokens
-- ==========================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    issued_at   DATETIME NOT NULL,
    expires_at  DATETIME NOT NULL,
    revoked     TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_user_id ON refresh_tokens(user_id);

-- ==========================================
-- DỮ LIỆU MẪU
-- ==========================================

INSERT INTO Users (email, password_hash, full_name, phone_number, is_verified, role, profile_status)
VALUES
(
    'admin@evshare.local',
    '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', -- Admin@123
    'System Admin',
    '0900000000',
    1,
    'ROLE_ADMIN',
    'APPROVED'
),
(
    'user@evshare.local',
    '$2a$10$G1t1gE0NrfFrS7UZ5EONYOE1rVcmJVJOnY/fx/n6PvJEa3TBUnIFY', -- User@123
    'Demo User',
    '0911111111',
    0,
    'ROLE_USER',
    'PENDING'
);

SELECT '✅ CoOwnershipDB đã được khởi tạo (ROLE_USER/ROLE_ADMIN)' AS status_message;

