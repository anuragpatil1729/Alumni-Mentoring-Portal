USE alumni_mentoring_portal;

CREATE TABLE IF NOT EXISTS scheduled_meets (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  meet_type ENUM('ONE_ON_ONE', 'PUBLIC') NOT NULL DEFAULT 'ONE_ON_ONE',
  mentor_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NULL,
  created_by_teacher_id BIGINT UNSIGNED NOT NULL,
  meet_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  meeting_link VARCHAR(500) NULL,
  venue VARCHAR(255) NULL,
  max_participants INT NULL DEFAULT NULL,
  status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_sm_mentor FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_sm_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_sm_teacher FOREIGN KEY (created_by_teacher_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_sm_date (meet_date),
  INDEX idx_sm_mentor (mentor_id),
  INDEX idx_sm_student (student_id),
  INDEX idx_sm_teacher (created_by_teacher_id),
  INDEX idx_sm_type_status (meet_type, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS meet_attendees (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  meet_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ma_meet FOREIGN KEY (meet_id) REFERENCES scheduled_meets(id) ON DELETE CASCADE,
  CONSTRAINT fk_ma_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_meet_student UNIQUE (meet_id, student_id),
  INDEX idx_ma_meet (meet_id),
  INDEX idx_ma_student (student_id)
) ENGINE=InnoDB;
