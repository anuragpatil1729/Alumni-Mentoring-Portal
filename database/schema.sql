CREATE DATABASE IF NOT EXISTS alumni_mentoring_portal
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE alumni_mentoring_portal;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(50) NOT NULL,
  email VARCHAR(254) NOT NULL,
  mobile_number VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('student', 'alumni') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_email (email)
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
