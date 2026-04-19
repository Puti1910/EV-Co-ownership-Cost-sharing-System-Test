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

INSERT INTO users (user_id, email, password_hash, full_name, phone_number, is_verified, role, role_requested, profile_status) VALUES
(1, 'admin@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'System Admin', '0900000000', 1, 'ROLE_ADMIN', 'ROLE_ADMIN', 'APPROVED'),
(2, 'user@evshare.local', '$2a$10$G1t1gE0NrfFrS7UZ5EONYOE1rVcmJVJOnY/fx/n6PvJEa3TBUnIFY', 'Demo User', '0911111111', 0, 'ROLE_USER', 'ROLE_USER', 'PENDING'),
(3, 'namvohoai23@gmail.com', '$2a$10$m6pZ7tV1tD.K.F8bYkY/eue6lS.S5Xp5Oq5O7u1O3o4v/U9K8h/0.', 'Hoai Nam Admin', '0922222222', 1, 'ROLE_ADMIN', 'ROLE_ADMIN', 'APPROVED'),
(484, 'user484@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 484', '0900000484', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(485, 'user485@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 485', '0900000485', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(486, 'user486@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 486', '0900000486', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(487, 'user487@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 487', '0900000487', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(488, 'user488@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 488', '0900000488', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(489, 'user489@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 489', '0900000489', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(490, 'user490@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 490', '0900000490', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(491, 'user491@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 491', '0900000491', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(492, 'user492@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 492', '0900000492', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(493, 'user493@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 493', '0900000493', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(494, 'user494@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 494', '0900000494', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(495, 'user495@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 495', '0900000495', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(496, 'user496@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 496', '0900000496', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(497, 'user497@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 497', '0900000497', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(498, 'user498@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 498', '0900000498', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(499, 'user499@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 499', '0900000499', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(500, 'user500@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 500', '0900000500', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(501, 'user501@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 501', '0900000501', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(502, 'user502@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 502', '0900000502', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(503, 'user503@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 503', '0900000503', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(504, 'user504@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 504', '0900000504', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(505, 'user505@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 505', '0900000505', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(506, 'user506@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 506', '0900000506', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(507, 'user507@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 507', '0900000507', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(508, 'user508@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 508', '0900000508', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(509, 'user509@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 509', '0900000509', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(510, 'user510@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 510', '0900000510', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(511, 'user511@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 511', '0900000511', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(512, 'user512@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 512', '0900000512', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(513, 'user513@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 513', '0900000513', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(514, 'user514@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 514', '0900000514', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(515, 'user515@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 515', '0900000515', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(516, 'user516@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 516', '0900000516', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(517, 'user517@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 517', '0900000517', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(518, 'user518@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 518', '0900000518', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(519, 'user519@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 519', '0900000519', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(520, 'user520@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 520', '0900000520', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(521, 'user521@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 521', '0900000521', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(522, 'user522@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 522', '0900000522', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(523, 'user523@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 523', '0900000523', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(524, 'user524@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 524', '0900000524', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(525, 'user525@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 525', '0900000525', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(526, 'user526@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 526', '0900000526', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(527, 'user527@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 527', '0900000527', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(528, 'user528@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 528', '0900000528', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(529, 'user529@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 529', '0900000529', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(530, 'user530@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 530', '0900000530', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(531, 'user531@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 531', '0900000531', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(532, 'user532@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 532', '0900000532', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(533, 'user533@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 533', '0900000533', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(534, 'user534@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 534', '0900000534', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(535, 'user535@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 535', '0900000535', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(536, 'user536@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 536', '0900000536', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(537, 'user537@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 537', '0900000537', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(538, 'user538@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 538', '0900000538', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(539, 'user539@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 539', '0900000539', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(540, 'user540@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 540', '0900000540', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(541, 'user541@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 541', '0900000541', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(542, 'user542@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 542', '0900000542', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(543, 'user543@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 543', '0900000543', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(544, 'user544@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 544', '0900000544', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(545, 'user545@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 545', '0900000545', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(546, 'user546@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 546', '0900000546', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(547, 'user547@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 547', '0900000547', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(548, 'user548@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 548', '0900000548', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(549, 'user549@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 549', '0900000549', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(550, 'user550@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 550', '0900000550', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(551, 'user551@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 551', '0900000551', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(552, 'user552@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 552', '0900000552', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(553, 'user553@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 553', '0900000553', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(554, 'user554@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 554', '0900000554', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(555, 'user555@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 555', '0900000555', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(556, 'user556@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 556', '0900000556', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(557, 'user557@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 557', '0900000557', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(558, 'user558@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 558', '0900000558', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(559, 'user559@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 559', '0900000559', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(560, 'user560@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 560', '0900000560', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(561, 'user561@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 561', '0900000561', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(562, 'user562@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 562', '0900000562', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(563, 'user563@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 563', '0900000563', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(564, 'user564@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 564', '0900000564', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(565, 'user565@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 565', '0900000565', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(566, 'user566@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 566', '0900000566', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(567, 'user567@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 567', '0900000567', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(568, 'user568@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 568', '0900000568', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(569, 'user569@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 569', '0900000569', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(570, 'user570@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 570', '0900000570', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(571, 'user571@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 571', '0900000571', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(572, 'user572@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 572', '0900000572', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(573, 'user573@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 573', '0900000573', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(574, 'user574@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 574', '0900000574', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(575, 'user575@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 575', '0900000575', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(576, 'user576@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 576', '0900000576', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(577, 'user577@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 577', '0900000577', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(578, 'user578@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 578', '0900000578', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(579, 'user579@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 579', '0900000579', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(580, 'user580@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 580', '0900000580', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(581, 'user581@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 581', '0900000581', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(582, 'user582@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 582', '0900000582', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED'),
(583, 'user583@evshare.local', '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 'User Test 583', '0900000583', 1, 'ROLE_USER', 'ROLE_USER', 'APPROVED');

SELECT '✅ CoOwnershipDB đã được khởi tạo (ROLE_USER/ROLE_ADMIN)' AS status_message;
