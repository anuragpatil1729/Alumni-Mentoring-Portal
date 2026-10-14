
USE alumni_mentoring_portal;

-- Store password reset tokens securely.
-- Java must generate a random token and store only its SHA-256 hash.

CREATE TABLE IF NOT EXISTS password_resets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_password_resets_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_password_resets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_password_resets_user (user_id),
    INDEX idx_password_resets_expiry (expires_at)
) ENGINE=InnoDB;


-- Store the history of sessions or mentoring interactions.

CREATE TABLE IF NOT EXISTS session_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    student_id BIGINT UNSIGNED NOT NULL,
    alumni_id BIGINT UNSIGNED NOT NULL,
    mentorship_request_id BIGINT UNSIGNED NULL,

    started_at DATETIME NULL,
    paused_at DATETIME NULL,
    completed_at DATETIME NULL,

    duration_minutes INT UNSIGNED NOT NULL DEFAULT 0,

    status ENUM(
        'STARTED',
        'PAUSED',
        'COMPLETED',
        'FAILED'
    ) NOT NULL DEFAULT 'STARTED',

    priority ENUM(
        'LOW',
        'MEDIUM',
        'HIGH',
        'URGENT'
    ) NOT NULL DEFAULT 'MEDIUM',

    failure_reason VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_session_history_student
        FOREIGN KEY (student_id)
        REFERENCES students(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_session_history_alumni
        FOREIGN KEY (alumni_id)
        REFERENCES alumni(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_session_history_request
        FOREIGN KEY (mentorship_request_id)
        REFERENCES mentorship_requests(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_session_history_duration
        CHECK (duration_minutes >= 0),

    INDEX idx_session_history_student
        (student_id, created_at),

    INDEX idx_session_history_alumni
        (alumni_id, created_at),

    INDEX idx_session_history_status_priority
        (status, priority),

    INDEX idx_session_history_request
        (mentorship_request_id)
) ENGINE=InnoDB;
