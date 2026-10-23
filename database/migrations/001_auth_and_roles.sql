
USE alumni_mentoring_portal;

-- 1. Allow the required roles while retaining existing student/alumni accounts.
ALTER TABLE users
    MODIFY COLUMN role ENUM('student', 'alumni', 'teacher', 'admin')
    NOT NULL;

-- 2. Support safe account deactivation.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Store hashed authentication tokens, never raw tokens.
CREATE TABLE IF NOT EXISTS auth_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,

    UNIQUE KEY uq_auth_tokens_hash (token_hash),
    INDEX idx_auth_tokens_user (user_id),
    INDEX idx_auth_tokens_expiry (expires_at),

    CONSTRAINT fk_auth_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
