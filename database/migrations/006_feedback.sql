
USE alumni_mentoring_portal;

-- 1. Create feedback table for completed mentoring sessions
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    mentor_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comments TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_feedback_session
        FOREIGN KEY (session_id) REFERENCES session_history(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_feedback_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_feedback_mentor
        FOREIGN KEY (mentor_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_feedback_session
        UNIQUE (session_id),

    CONSTRAINT chk_feedback_rating
        CHECK (rating >= 1 AND rating <= 5),

    INDEX idx_feedback_mentor (mentor_id, rating),
    INDEX idx_feedback_student (student_id)
) ENGINE=InnoDB;

-- 2. Add approval_status to users table for Admin registration approval workflow
-- Allows 'PENDING', 'APPROVED', 'REJECTED' with existing users defaulting to 'APPROVED'
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS approval_status ENUM('PENDING', 'APPROVED', 'REJECTED')
    NOT NULL DEFAULT 'APPROVED';

