
USE alumni_mentoring_portal;

-- =====================================================
-- 1. ALUMNI EVENTS
-- Alumni can create events; admins approve or reject them.
-- =====================================================

CREATE TABLE IF NOT EXISTS events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue VARCHAR(255) NOT NULL,
    organizer_alumni_id BIGINT UNSIGNED NOT NULL,
    event_type ENUM(
        'GENERAL',
        'JUDGE',
        'GUEST_SESSION',
        'SPONSOR'
    ) NOT NULL DEFAULT 'GENERAL',
    status ENUM(
        'PENDING',
        'APPROVED',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_organizer
        FOREIGN KEY (organizer_alumni_id)
        REFERENCES alumni(user_id)
        ON DELETE RESTRICT,

    INDEX idx_events_date_status (date, status),
    INDEX idx_events_organizer (organizer_alumni_id),
    INDEX idx_events_type_status (event_type, status),

    CONSTRAINT chk_events_valid_time
        CHECK (end_time > start_time)
) ENGINE=InnoDB;


-- =====================================================
-- 2. ALUMNI OPPORTUNITIES
-- Alumni can publish opportunities for eligible users.
-- =====================================================

CREATE TABLE IF NOT EXISTS opportunities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    alumni_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    organization VARCHAR(200) NOT NULL,
    opportunity_type VARCHAR(50) NOT NULL,
    location VARCHAR(200) NULL,
    eligibility TEXT NULL,
    deadline DATE NULL,
    application_link VARCHAR(2048) NULL,
    status ENUM(
        'OPEN',
        'CLOSED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_opportunities_alumni
        FOREIGN KEY (alumni_id)
        REFERENCES alumni(user_id)
        ON DELETE RESTRICT,

    INDEX idx_opportunities_alumni (alumni_id),
    INDEX idx_opportunities_status_deadline (status, deadline),
    INDEX idx_opportunities_type (opportunity_type)
) ENGINE=InnoDB;
