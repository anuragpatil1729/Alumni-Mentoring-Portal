
USE alumni_mentoring_portal;

-- =====================================================
-- 1. TEACHER EVENT ASSIGNMENTS
-- Assign teachers to events and define their responsibilities.
-- =====================================================

CREATE TABLE IF NOT EXISTS teacher_event_assignments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT UNSIGNED NOT NULL,
    event_id BIGINT UNSIGNED NOT NULL,
    assignment_role ENUM(
        'COORDINATOR',
        'INTERNAL_EVALUATOR',
        'EVENT_SUPERVISOR'
    ) NOT NULL DEFAULT 'COORDINATOR',
    assigned_by BIGINT UNSIGNED NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tea_teacher
        FOREIGN KEY (teacher_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tea_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tea_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_teacher_event_role
        UNIQUE (teacher_id, event_id, assignment_role),

    INDEX idx_tea_event (event_id),
    INDEX idx_tea_teacher (teacher_id)
) ENGINE=InnoDB;


-- =====================================================
-- 2. INTERNAL MARKS
-- Teachers can record marks for students for an event.
-- =====================================================

CREATE TABLE IF NOT EXISTS internal_marks (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT UNSIGNED NOT NULL,
    event_id BIGINT UNSIGNED NOT NULL,
    teacher_id BIGINT UNSIGNED NOT NULL,
    marks DECIMAL(5,2) NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    remarks VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_marks_student
        FOREIGN KEY (student_id)
        REFERENCES students(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_marks_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_marks_teacher
        FOREIGN KEY (teacher_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_marks_student_event
        UNIQUE (student_id, event_id),

    CONSTRAINT chk_marks_nonnegative
        CHECK (marks >= 0),

    CONSTRAINT chk_marks_max_positive
        CHECK (max_marks > 0),

    CONSTRAINT chk_marks_within_max
        CHECK (marks <= max_marks),

    INDEX idx_marks_student (student_id),
    INDEX idx_marks_event (event_id),
    INDEX idx_marks_teacher (teacher_id)
) ENGINE=InnoDB;
