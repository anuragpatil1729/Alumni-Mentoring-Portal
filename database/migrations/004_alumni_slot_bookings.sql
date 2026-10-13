
USE alumni_mentoring_portal;

CREATE TABLE IF NOT EXISTS alumni_slot_bookings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    event_id BIGINT UNSIGNED NOT NULL,
    alumni_id BIGINT UNSIGNED NOT NULL,
    booked_by BIGINT UNSIGNED NOT NULL,

    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    booking_purpose ENUM(
        'JUDGE',
        'GUEST_SESSION',
        'SPONSOR'
    ) NOT NULL,

    status ENUM(
        'BOOKED',
        'CANCELLED',
        'COMPLETED'
    ) NOT NULL DEFAULT 'BOOKED',

    notes VARCHAR(1000) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_asb_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_asb_alumni
        FOREIGN KEY (alumni_id)
        REFERENCES alumni(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_asb_booked_by
        FOREIGN KEY (booked_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_asb_valid_time
        CHECK (end_time > start_time),

    CONSTRAINT uq_asb_exact_booking
        UNIQUE (event_id, alumni_id, slot_date, start_time, end_time),

    INDEX idx_asb_alumni_schedule
        (alumni_id, slot_date, start_time, end_time, status),

    INDEX idx_asb_event_status
        (event_id, status),

    INDEX idx_asb_booked_by
        (booked_by)
) ENGINE=InnoDB;
