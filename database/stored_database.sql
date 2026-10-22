-- Stored Database Dump for Alumni Mentoring Portal
-- Schema
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

-- Seed data
USE alumni_mentoring_portal;

INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(101, 'Anurag Patil', 'anurag.patil@microsoft.com', '9876543210', 'sha256$61cc66b038a77f94057147f0a6f2ddd0$e47c7927149b94b8c3db632ecc86128e3f4870c16e17bff8fbb099cc6caf2b47', 'alumni'),
(102, 'Vishwesh Bhilare', 'bhilarevishwesh@microsoft.com', '9876543211', 'sha256$708b1b0f5e4ca50527b2634561e7e41e$167636d95b98f10f1c98e2533c89ceac6fee643b08e4574a55b369910d9346c4', 'alumni'),
(103, 'Rohan Mali', 'rohanmali@google.com', '9876543212', 'sha256$5ca36a4503d3cd36a08ef2977030ba32$b0acda76eec20d61b87106a282df2563fe9d4a8121f8247c89c90324ec19f8c6', 'alumni'),
(104, 'Priya Sharma', 'priya.sharma@amazon.com', '9876543213', 'sha256$586fb5b81483c7ee0ad937ef83473831$e5bc046bb1ca552c8296c7f6db22110b70ee9ee48b33162b6ef95739296e6be5', 'alumni'),
(105, 'Aditya Kulkarni', 'aditya.kulkarni@nvidia.com', '9876543214', 'sha256$324633aff761d4846765f3ecc670d4f6$f6bef91dd2e011ee2b596c5d21a210a3743813c5d4e5c07ed6af0773dfea9ce5', 'alumni'),
(106, 'Sneha Deshmukh', 'sneha.d@goldmansachs.com', '9876543215', 'sha256$81898e984259b7e7c11daaf1f9b721d3$eb19a20dddcf7caa40c9ca43696674cfab2d5cda09e5be8eeda3b0b7d37c2549', 'alumni'),
(107, 'Tanvi Joshi', 'tanvi.j@deloitte.com', '9876543216', 'sha256$91872292192f29dc015d14d7e61fda16$0b82b876db7a011497ff412f20b1469122796cfe559f042a3876f0009bdbc01a', 'alumni'),
(108, 'Kunal Verma', 'kunal.verma@apple.com', '9876543217', 'sha256$9e1fe8a352c1285d52251127b7ea8dc9$f496ef1298ab223f7dced53a4b62e4206d692049967fbd8074512d10a91a2d4d', 'alumni'),
(109, 'Aarav Mehta', 'aarav.mehta@meta.com', '9876543218', 'sha256$1d1afff546350ef0d67afedadb277595$f1cee8901e14e47e139e80dd3619dd7c609e993c1d1d765db9d42cb375005b7e', 'alumni'),
(110, 'Neha Nair', 'neha.nair@mckinsey.com', '9876543219', 'sha256$b49164c09b0addf4526f60c4a746eacd$af728aa1cc225f532b56bd11333ab8f939303486d64636a76bcde25f4a7a1bad', 'alumni')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = VALUES(password_hash);

INSERT INTO alumni (user_id, department, graduation_year, company, designation, linkedin_profile, experience_years, industry, skills, bio, max_mentees) VALUES
(101, 'Computer Engineering', 2022, 'Microsoft', 'Senior AI Engineer', 'https://linkedin.com/in/anurag-patil', 4, 'Information Technology', 'Java, Python, C++, Azure, Machine Learning, System Design', 'AI Engineer building distributed intelligence systems and LLM inference pipelines.', 5),
(102, 'Computer Engineering', 2022, 'Microsoft', 'Cloud Solutions Architect', 'https://linkedin.com/in/vishwesh-bhilare', 4, 'Information Technology', 'Java, Python, C, C++, Cloud Architecture, Kubernetes', 'Passionate about cloud-native infrastructure, distributed systems, and scalable microservices architectures.', 4),
(103, 'Computer Engineering', 2022, 'Google', 'Software Engineer II', 'https://linkedin.com/in/rohan-mali', 3, 'Information Technology', 'AI, Networking, OS, DBMS, Go, Kubernetes', 'Site Reliability & Core Systems engineer at Google. Mentoring students on operating systems, algorithms, and tech interview prep.', 3),
(104, 'Information Technology', 2021, 'Amazon', 'Software Development Engineer', 'https://linkedin.com/in/priya-sharma', 5, 'Information Technology', 'Java, Spring Boot, AWS, Distributed Systems, Microservices', 'Backend developer working on large-scale e-commerce transaction systems.', 5),
(105, 'Electronics & Telecommunication', 2020, 'Nvidia', 'Deep Learning Systems Engineer', 'https://linkedin.com/in/aditya-nvidia', 6, 'Information Technology', 'CUDA, C++, Python, PyTorch, TensorRT, GPU Computing', 'Specializing in accelerated computing, GPU kernel optimization, and high-performance deep learning inference.', 3),
(106, 'Computer Engineering', 2023, 'Goldman Sachs', 'Quantitative Strategist', 'https://linkedin.com/in/sneha-deshmukh', 2, 'Finance', 'Python, SQL, R, Financial Modeling, Algorithms, Statistics', 'Quantitative analysis and low-latency financial systems engineering.', 4),
(107, 'Information Technology', 2021, 'Deloitte', 'Cybersecurity Consultant', 'https://linkedin.com/in/tanvi-joshi', 4, 'Consulting', 'Cybersecurity, Penetration Testing, Risk Management, Network Security', 'Consultant helping Fortune 500 companies protect enterprise infrastructure and cloud posture.', 4),
(108, 'Computer Engineering', 2019, 'Apple', 'Senior iOS Frameworks Engineer', 'https://linkedin.com/in/kunal-verma', 7, 'Information Technology', 'Swift, Objective-C, SwiftUI, CoreData, Mobile Architecture', 'Working on native mobile frameworks and developer tools.', 6),
(109, 'Computer Engineering', 2020, 'Meta', 'Product Software Engineer', 'https://linkedin.com/in/aarav-mehta', 5, 'Information Technology', 'React, TypeScript, GraphQL, Relay, Node.js, Web Performance', 'Frontend infrastructure and web performance engineer.', 4),
(110, 'Mechanical Engineering', 2018, 'McKinsey & Company', 'Digital Transformation Consultant', 'https://linkedin.com/in/neha-nair', 7, 'Consulting', 'Strategy, Product Management, Analytics, Agile Transformation', 'Guiding engineering students looking to transition into strategy consulting.', 5)
ON DUPLICATE KEY UPDATE company = VALUES(company);

INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(201, 'Rahul Sharma', 'rahul.student@college.edu', '9876500001', 'sha256$6059a8bf10e80b72028ab18804f47191$fd1cf7c5833ee530098a3c8d2a288098bb3524ecbdd8c6909ffa1c0b64421675', 'student'),
(202, 'Ananya Patel', 'ananya.p@college.edu', '9876500002', 'sha256$cf8eda7f3ad3b697dac38ba63070e609$0ddc4a59ec097c07e8f3057cba208eba0c9d76010528bda55f29d97b5bf31cc6', 'student')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = VALUES(password_hash);

INSERT INTO students (user_id, student_id, department, graduation_year) VALUES
(201, 'STU-2024-001', 'Computer Engineering', 2026),
(202, 'STU-2024-002', 'Information Technology', 2025)
ON DUPLICATE KEY UPDATE student_id = VALUES(student_id);

-- Seed Administrator and Teacher
INSERT INTO users (id, full_name, email, mobile_number, password_hash, role) VALUES
(1, 'System Administrator', 'admin@college.edu', '9999999999', 'sha256$ca0fc3423a78e231952620ff1f065eba$1b684c010f2eb76c5f082287f0802d283aa35ef96643871ff9801c98633bb7a8', 'admin'),
(401, 'Prof. S. Sharma', 'teacher.sharma@college.edu', '9876599999', 'sha256$04b60d9d08cc23e4cf7c530fca9944c6$4f33f407c8a0cc63dc6c339bfbdb96dd7e08fbe273c7981bcfbd37018e98e317', 'teacher')
ON DUPLICATE KEY UPDATE role = VALUES(role), is_active = 1;

-- Seed Institutional Events
INSERT INTO events (id, title, description, date, start_time, end_time, venue, organizer_alumni_id, event_type, status) VALUES
(1, 'Annual Tech Symposium 2026', 'Flagship college project exhibition, technical papers, and industry judging panels.', '2026-11-15', '09:00:00', '17:00:00', 'Main Auditorium & CS Labs', 101, 'JUDGE', 'APPROVED'),
(2, 'Cloud & AI Architecture Workshop', 'Hands-on session covering Azure microservices and LLM pipeline deployment.', '2026-11-20', '14:00:00', '16:30:00', 'Seminar Hall B', 102, 'GUEST_SESSION', 'APPROVED'),
(3, 'FinTech Hackathon Mentorship', '24-hour algorithmic trading and decentralized finance challenge.', '2026-12-05', '10:00:00', '18:00:00', 'Innovation Center', 106, 'SPONSOR', 'PENDING')
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);

-- Seed Teacher Event Assignments
INSERT INTO teacher_event_assignments (id, teacher_id, event_id, assignment_role, assigned_by) VALUES
(1, 401, 1, 'COORDINATOR', 1),
(2, 401, 2, 'EVENT_SUPERVISOR', 1)
ON DUPLICATE KEY UPDATE assignment_role = VALUES(assignment_role);

-- Seed Internal Marks
INSERT INTO internal_marks (id, student_id, event_id, teacher_id, marks, max_marks, remarks) VALUES
(1, 201, 1, 401, 92.50, 100.00, 'Excellent distributed systems architecture presentation and working demo.')
ON DUPLICATE KEY UPDATE marks = VALUES(marks);

-- Seed Opportunities
INSERT INTO opportunities (id, alumni_id, title, description, organization, opportunity_type, location, eligibility, deadline, application_link, status) VALUES
(1, 101, 'AI Systems Engineering Internship', 'Summer internship working on large-scale model optimization and cloud deployment.', 'Microsoft', 'Internship', 'Hyderabad / Hybrid', 'Final and pre-final year CS/IT students', '2026-12-31', 'https://careers.microsoft.com', 'OPEN'),
(2, 103, 'Graduate Software Engineer - Core Infrastructure', 'Full-time entry role on Google core infrastructure and network monitoring.', 'Google', 'Full-time', 'Bangalore', 'Graduating batches with strong algorithms and OS fundamentals', '2026-12-15', 'https://careers.google.com', 'OPEN')
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);

-- End of stored dump
