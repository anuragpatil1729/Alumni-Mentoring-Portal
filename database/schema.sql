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

