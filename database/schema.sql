CREATE DATABASE IF NOT EXISTS alumni_mentoring_portal
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE alumni_mentoring_portal;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(50) NOT NULL,
  email VARCHAR(254) NOT NULL,
  mobile_number VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('student', 'alumni', 'teacher', 'admin') NOT NULL,
  avatar_image MEDIUMBLOB NULL,
  avatar_mime_type VARCHAR(50) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

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
  CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS password_resets (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_password_resets_token_hash (token_hash),
  INDEX idx_password_resets_user (user_id),
  INDEX idx_password_resets_expiry (expires_at),
  CONSTRAINT fk_password_resets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
  user_id BIGINT UNSIGNED PRIMARY KEY,
  student_id VARCHAR(20) NOT NULL,
  department VARCHAR(100) NOT NULL,
  graduation_year SMALLINT UNSIGNED NOT NULL,
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_students_student_id (student_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS alumni (
  user_id BIGINT UNSIGNED PRIMARY KEY,
  department VARCHAR(100) NOT NULL,
  graduation_year SMALLINT UNSIGNED NOT NULL,
  company VARCHAR(150) NOT NULL,
  designation VARCHAR(150) NOT NULL,
  linkedin_profile VARCHAR(255) NULL,
  experience_years INT NULL,
  industry VARCHAR(100) NULL,
  skills TEXT NULL,
  bio TEXT NULL,
  max_mentees INT NULL,
  CONSTRAINT fk_alumni_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mentorship_requests (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  mentor_id BIGINT UNSIGNED NOT NULL,
  session_goal VARCHAR(100) NOT NULL,
  message TEXT NOT NULL,
  status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  mentor_response TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_mr_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_mr_mentor FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_mr_student (student_id),
  INDEX idx_mr_mentor (mentor_id),
  INDEX idx_mr_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS session_history (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  alumni_id BIGINT UNSIGNED NOT NULL,
  mentorship_request_id BIGINT UNSIGNED NULL,
  started_at DATETIME NULL,
  paused_at DATETIME NULL,
  completed_at DATETIME NULL,
  duration_minutes INT UNSIGNED NOT NULL DEFAULT 0,
  status ENUM('STARTED', 'PAUSED', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'STARTED',
  priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
  failure_reason VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_session_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_session_alumni FOREIGN KEY (alumni_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_session_request FOREIGN KEY (mentorship_request_id) REFERENCES mentorship_requests(id) ON DELETE SET NULL,
  INDEX idx_session_student (student_id),
  INDEX idx_session_alumni (alumni_id),
  INDEX idx_session_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  mentor_id BIGINT UNSIGNED NOT NULL,
  rating TINYINT UNSIGNED NOT NULL,
  comments TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES session_history(id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_mentor FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_feedback_session UNIQUE (session_id),
  CONSTRAINT chk_feedback_rating CHECK (rating >= 1 AND rating <= 5),
  INDEX idx_feedback_mentor (mentor_id, rating),
  INDEX idx_feedback_student (student_id)
) ENGINE=InnoDB;

-- =====================================================
-- ALUMNI EVENTS & OPPORTUNITIES (System Architecture)
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
  event_type ENUM('GENERAL', 'JUDGE', 'GUEST_SESSION', 'SPONSOR') NOT NULL DEFAULT 'GENERAL',
  status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_alumni_id) REFERENCES alumni(user_id) ON DELETE RESTRICT,
  INDEX idx_events_date_status (date, status),
  INDEX idx_events_organizer (organizer_alumni_id),
  INDEX idx_events_type_status (event_type, status),
  CONSTRAINT chk_events_valid_time CHECK (end_time > start_time)
) ENGINE=InnoDB;

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
  status ENUM('OPEN', 'CLOSED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_opportunities_alumni FOREIGN KEY (alumni_id) REFERENCES alumni(user_id) ON DELETE RESTRICT,
  INDEX idx_opportunities_alumni (alumni_id),
  INDEX idx_opportunities_status_deadline (status, deadline),
  INDEX idx_opportunities_type (opportunity_type)
) ENGINE=InnoDB;

-- =====================================================
-- TEACHER ASSIGNMENTS & INTERNAL MARKS (System Architecture)
-- =====================================================

CREATE TABLE IF NOT EXISTS teacher_event_assignments (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT UNSIGNED NOT NULL,
  event_id BIGINT UNSIGNED NOT NULL,
  assignment_role ENUM('COORDINATOR', 'INTERNAL_EVALUATOR', 'EVENT_SUPERVISOR') NOT NULL DEFAULT 'COORDINATOR',
  assigned_by BIGINT UNSIGNED NOT NULL,
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tea_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT fk_tea_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  CONSTRAINT fk_tea_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uq_teacher_event_role UNIQUE (teacher_id, event_id, assignment_role),
  INDEX idx_tea_event (event_id),
  INDEX idx_tea_teacher (teacher_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS internal_marks (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT UNSIGNED NOT NULL,
  event_id BIGINT UNSIGNED NOT NULL,
  teacher_id BIGINT UNSIGNED NOT NULL,
  marks DECIMAL(5,2) NOT NULL,
  max_marks DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  remarks VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_marks_student FOREIGN KEY (student_id) REFERENCES students(user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_marks_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  CONSTRAINT fk_marks_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT uq_marks_student_event UNIQUE (student_id, event_id),
  CONSTRAINT chk_marks_nonnegative CHECK (marks >= 0),
  CONSTRAINT chk_marks_max_positive CHECK (max_marks > 0),
  CONSTRAINT chk_marks_within_max CHECK (marks <= max_marks),
  INDEX idx_marks_student (student_id),
  INDEX idx_marks_event (event_id),
  INDEX idx_marks_teacher (teacher_id)
) ENGINE=InnoDB;

-- =====================================================
-- ALUMNI SLOT BOOKINGS (System Architecture)
-- =====================================================

CREATE TABLE IF NOT EXISTS alumni_slot_bookings (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  event_id BIGINT UNSIGNED NOT NULL,
  alumni_id BIGINT UNSIGNED NOT NULL,
  booked_by BIGINT UNSIGNED NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  booking_purpose ENUM('JUDGE', 'GUEST_SESSION', 'SPONSOR') NOT NULL,
  status ENUM('BOOKED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'BOOKED',
  notes VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_asb_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
  CONSTRAINT fk_asb_alumni FOREIGN KEY (alumni_id) REFERENCES alumni(user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_asb_booked_by FOREIGN KEY (booked_by) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT chk_asb_valid_time CHECK (end_time > start_time),
  CONSTRAINT uq_asb_exact_booking UNIQUE (event_id, alumni_id, slot_date, start_time, end_time),
  INDEX idx_asb_alumni_schedule (alumni_id, slot_date, start_time, end_time, status),
  INDEX idx_asb_event_status (event_id, status),
  INDEX idx_asb_booked_by (booked_by)
) ENGINE=InnoDB;

-- =====================================================
-- TEACHER SCHEDULED MEETS & PUBLIC SESSIONS (System Architecture)
-- =====================================================

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


