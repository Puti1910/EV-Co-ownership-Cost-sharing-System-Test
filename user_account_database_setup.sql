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
-- BẢNG users (khớp với entity User.java)
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
                                     user_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150),
    phone_number        VARCHAR(20) UNIQUE,
    is_verified         TINYINT(1) NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NULL,
    last_login_at       DATETIME     NULL,
    role                ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',
    role_requested      ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',

    profile_status      ENUM('PENDING','APPROVED','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    VARCHAR(500),

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
    portrait_image_url  VARCHAR(512),

    kyc_submitted_at    DATETIME,
    kyc_reviewed_at     DATETIME,
    kyc_reviewer_id     BIGINT,
    CONSTRAINT fk_users_reviewer FOREIGN KEY (kyc_reviewer_id) REFERENCES users(user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_profile_status ON users(profile_status);

-- ==========================================
-- BẢNG user_documents
-- ==========================================

CREATE TABLE IF NOT EXISTS user_documents (
                                              document_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id         BIGINT NOT NULL,
                                              document_type   ENUM('ID_FRONT','ID_BACK','LICENSE','PORTRAIT','OTHER') NOT NULL,
    status          ENUM('UPLOADED','IN_REVIEW','VERIFIED','REJECTED') NOT NULL DEFAULT 'UPLOADED',
    file_name       VARCHAR(255),
    storage_url     VARCHAR(600) NOT NULL,
    content_type    VARCHAR(120),
    file_size       BIGINT,
    uploaded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME,
    reviewer_id     BIGINT,
    rejection_reason VARCHAR(500),
    metadata        JSON,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG refresh_tokens
-- ==========================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                           id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           user_id     BIGINT NOT NULL,
                                           token       VARCHAR(255) NOT NULL UNIQUE,
    issued_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME NOT NULL,
    revoked     TINYINT(1) NOT NULL DEFAULT 0,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(64),
    INDEX idx_refresh_user_id (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG user_tokens
-- ==========================================

CREATE TABLE IF NOT EXISTS user_tokens (
                                           token_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           user_id     BIGINT NOT NULL,
                                           token       VARCHAR(500) NOT NULL,
    token_type  ENUM('REFRESH') NOT NULL DEFAULT 'REFRESH',
    issued_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME NOT NULL,
    revoked     TINYINT(1) NOT NULL DEFAULT 0,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(64),
    INDEX idx_tokens_user (user_id),
    INDEX idx_tokens_token (token(191)),
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- BẢNG kyc_audit_logs
-- ==========================================

CREATE TABLE IF NOT EXISTS kyc_audit_logs (
                                              log_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id    BIGINT NOT NULL,
                                              actor_id   BIGINT NOT NULL,
                                              action     ENUM('SUBMIT','APPROVE','REJECT','COMMENT','UPDATE_DOC') NOT NULL,
    note       VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_logs_user (user_id),
    INDEX idx_logs_actor (actor_id),
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_logs_actor FOREIGN KEY (actor_id) REFERENCES users(user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- DỮ LIỆU MẪU
-- ==========================================

INSERT INTO users (email, password_hash, full_name, phone_number, is_verified, role, role_requested, profile_status)
VALUES
    (
        'admin@evshare.local',
        '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', -- Admin@123
        'System Admin',
        '0900000000',
        1,
        'ROLE_ADMIN',
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
        'ROLE_USER',
        'PENDING'
    );

SELECT '✅ CoOwnershipDB đã được khởi tạo (ROLE_USER/ROLE_ADMIN)' AS status_message;

