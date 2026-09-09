# AlumniConnect — Student & Alumni Mentoring Portal

[![Java](https://img.shields.io/badge/Java-OpenJDK_17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Build-Maven_3.8%2B-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/Database-MySQL_Connector%2FJ_9.2.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Frontend](https://img.shields.io/badge/Frontend-Vanilla_HTML5_%2F_CSS3_%2F_ES6-E34F26?logo=html5&logoColor=white)](frontend/)
[![Tests](https://img.shields.io/badge/JUnit_5-87_Tests_Passing-25A162?logo=junit5&logoColor=white)](backend/src/test/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**AlumniConnect** is a comprehensive, production-grade alumni mentorship portal connecting undergraduate and graduate students directly with verified industry alumni working across top technology companies, quantitative finance firms, and research institutions.

The platform features a **100% Java JDBC backend engine** and a **zero-dependency Vanilla HTML5/CSS3/JavaScript frontend** designed with clean, modern aesthetics, robust real-time validation, high-performance search algorithms, and a complete 1-on-1 mentorship request lifecycle.

---

## Table of Contents
- [Key Features](#key-features)
  - [Mentorship Request System & Lifecycle](#-1-on-1-mentorship-request-lifecycle-week-5)
  - [Role-Based Dashboard Separation](#-strict-role-based-dashboard-separation)
  - [Student & Alumni Profile Management](#-profile-management--avatar-system)
  - [Search Directory & DSA Algorithms](#-streamlined-mentor-search-directory)
  - [Real-Time Validation & Security](#-real-time-name--password-strength-testing)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Project Directory Structure](#project-directory-structure)
- [System Requirements](#system-requirements)
- [Database Setup](#database-setup)
- [Quick Start Guide](#quick-start-guide)
  - [1. Start Java Backend](#1-start-java-backend)
  - [2. Start Frontend](#2-start-frontend)
- [Database Seed Accounts](#database-seed-accounts)
- [API Reference](#api-reference)
- [Terminal Image Inspection Guide](#terminal-image-inspection-guide)
- [Search Engine & Algorithms](#search-engine--algorithms)
- [Automated Testing Suite (87 Tests)](#automated-testing-suite)
- [License](#license)

---

## Key Features

### 📬 1-on-1 Mentorship Request Lifecycle (Week 5)
- **End-to-End Request State Machine**: Manages request states: `PENDING` ➔ `ACCEPTED` / `REJECTED` / `CANCELLED`.
- **Duplicate Request Protection**: Prevents duplicate pending applications to the same mentor (`HTTP 409 Conflict`).
- **Re-Request Capabilities**: Once a previous request is resolved (`ACCEPTED` or `REJECTED`), students can submit new follow-up requests.
- **Mentor Capacity Limits**: Automatically validates the mentor's configured `max_mentees` threshold, blocking new requests when full.
- **Self-Request Prevention**: Validates that students cannot send mentorship requests to their own accounts.
- **Decision Workflow**: Mentors can accept or decline requests with personalized feedback and meeting scheduling notes.

### 👥 Strict Role-Based Dashboard Separation
- **Alumni Mentors**:
  - Exclusively access the **Received Requests** (`Received Mentorship Requests`) dashboard.
  - Review student applicants, view student department and graduation year, read goal statements, and click **Accept** or **Decline**.
  - Receive live visual pending counters in the navigation header.
  - Access **My Profile** to update bio, mentee capacity, company, and skills.
  - *Explore Mentors tab is hidden to maintain a focused mentor experience.*
- **Students**:
  - Exclusively access **Explore Mentors** and **My Profile**.
  - Mentor cards dynamically display request status pills (`⏳ Request Pending`, `✅ Approved · Connected`, `❌ Request Declined`).
  - Contextual modal status banners display previous approval notes from mentors with one-click follow-up request forms.
  - *Protected against accessing mentor-only management routes.*

### 📷 Profile Management & Avatar System
- **Database Image Storage**: Avatars are stored directly in MySQL as `MEDIUMBLOB` with MIME type tracking (`image/jpeg`, `image/png`, `image/webp`).
- **Direct Image REST API**: Dedicated endpoint `GET /api/users/{id}/avatar` and `HEAD /api/users/{id}/avatar` with HTTP caching and fallback to user initials.
- **Live Profile Editor**: Editable full name, contact details, bio, department, company, and mentee capacity.

### 🎓 Student Registration & Authentication
- **Two-Section Step Form**: Cleanly divides registration into **Personal Information** and **Academic Information**.
- **Student ID Format Check**: Enforces standard academic IDs (`STU-2024-001`, `CS2023045`) via regex `^[a-zA-Z0-9/-]{3,20}$`.
- **Session Persistence**: Safe session token and profile state stored in `localStorage` (`alumniConnectUser`).

### 💼 Alumni Mentor Registration
- **Comprehensive Profile Builder**: Includes company, designation, department, graduation year, years of experience, industry, comma-delimited skills, bio, and mentee capacity.
- **LinkedIn Profile Verification**: Validates syntax (`https://linkedin.com/in/...`).

### 🛡️ Real-Time Name & Password Strength Testing
- **Full Name Validator**: Enforces 2–50 characters, letters/spaces/hyphens/periods only, prohibits numbers or consecutive whitespace with instant inline feedback.
- **Interactive 4-Segment Password Meter**: Visual color transitions from Red (`Weak`) ➔ Orange (`Fair`) ➔ Amber (`Good`) ➔ Emerald Green (`Strong`).
- **Live Requirement Checklist**: Real-time tick indicators for length (8+), uppercase, lowercase, numbers, and special symbols (`!@#$%^&*`).
- **Backend Strength API**: Dedicated REST endpoint `POST /api/auth/password-strength` returning score (0–4), criterion flags, and improvement suggestions.

### 🔍 Streamlined Mentor Search Directory
- **Instant Search with Debouncing**: 280ms debounced input for fast responsive filtering across name, company, title, skills, and department.
- **Faceted Filters**: Quick dropdowns for Department, Industry sector, and Minimum Experience.
- **Telemetry Metrics Banner**: Real-time display of dataset pool, matched count, comparison operations, and execution latency in milliseconds.

---

## Architecture & Tech Stack

```
┌─────────────────────────────────────────────────────────────┐
│                 Vanilla HTML5 / CSS3 / ES6                  │
│   (Plus Jakarta Sans, Pure CSS Grid/Flexbox, No React/JSX)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP Fetch (Port 5001)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 100% Java Backend Engine                    │
│   • OpenJDK 17+ / Multi-Threaded HttpServerApp               │
│   • MentorshipService & MentorshipRequestDAO                │
│   • InputValidator & PasswordStrength Engine                │
│   • Manual DSA Search Engine (Linear & Quicksort + Binary)   │
│   • TCP Socket Server (Port 5002)                           │
│   • Servlet & CGI-Style Handlers                            │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC Driver (mysql-connector-j 9.2.0)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Local MySQL / MariaDB                       │
│   • Database: alumni_mentoring_portal                       │
│   • Tables: users, students, alumni, mentorship_requests    │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Technology | Description |
| :--- | :--- | :--- |
| **Frontend** | HTML5, Vanilla CSS3, ES6 JavaScript | Zero build tools, native browser APIs, responsive layouts. |
| **Backend** | Java 17+, Maven | Multi-threaded HTTP Server (`com.sun.net.httpserver`), JSON (`org.json`). |
| **Database** | MySQL / MariaDB | Relational database accessed via `com.mysql.cj.jdbc.Driver` with foreign keys and transactions. |
| **Network** | TCP Socket Server | Newline-delimited JSON stream server for cross-platform integration on port 5002. |
| **Testing** | JUnit 5 (`org.junit.jupiter`) | **87 automated test cases** covering JDBC, validations, algorithms, profiles, and requests. |

---

## Project Directory Structure

```
Alumni-Mentoring-Portal/
├── backend/                              # 100% Java Backend Engine
│   ├── pom.xml                           # Maven dependencies & build configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/alumni/
│   │   │   │   ├── Main.java             # Entry point (Starts HTTP & TCP servers)
│   │   │   │   ├── config/
│   │   │   │   │   └── DBConnection.java # JDBC MySQL connection pool manager (.env loader)
│   │   │   │   ├── dao/
│   │   │   │   │   ├── AlumniDAO.java    # Alumni profile queries & searches
│   │   │   │   │   ├── MentorshipRequestDAO.java # CRUD & state queries for mentorship requests
│   │   │   │   │   └── RegistrationDAO.java # User, student, and avatar registration
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java         # Base user entity
│   │   │   │   │   ├── Student.java      # Student profile entity
│   │   │   │   │   ├── Alumni.java       # Alumni mentor entity
│   │   │   │   │   └── MentorshipRequest.java # Request entity with status and responses
│   │   │   │   ├── search/
│   │   │   │   │   ├── SearchAlgorithms.java # Manual Linear & Binary Search engine
│   │   │   │   │   └── SearchResult.java # Telemetry & results container
│   │   │   │   ├── server/
│   │   │   │   │   ├── HttpServerApp.java# HTTP REST endpoints & routing
│   │   │   │   │   └── HttpUtils.java    # CORS headers & JSON parsing
│   │   │   │   ├── service/
│   │   │   │   │   ├── MentorshipService.java # Business validation, capacity limits, requests
│   │   │   │   │   ├── RegistrationService.java # Registration validation & auth
│   │   │   │   │   └── SocketServer.java # TCP Socket server (Port 5002)
│   │   │   │   └── validation/
│   │   │   │       ├── InputValidator.java  # Name, password strength, RFC email validators
│   │   │   │       ├── ValidationConstants.java # Regex patterns & constants
│   │   │   │       └── ValidationResult.java    # Validation status & field error dictionary
│   │   │   └── resources/
│   │   │       └── application.properties   # Port configuration & MySQL defaults
│   │   └── test/java/com/alumni/
│   │       ├── MentorshipRequestTest.java# 23 tests for request lifecycle & duplicate rules
│   │       ├── ProfileTest.java          # 13 tests for avatars & profile updates
│   │       ├── ValidationTest.java       # 28 validation & endpoint integration tests
│   │       ├── SearchAlgorithmsTest.java # 19 linear vs binary search telemetry tests
│   │       └── JDBCTest.java             # 4 database connectivity & CRUD tests
│   └── .env                              # MySQL credentials & environment config
├── frontend/                             # Pure Vanilla HTML/CSS/JS Frontend
│   ├── index.html                        # Application views (Auth, Search, Profile, Requests)
│   ├── css/
│   │   └── styles.css                    # Design system, variables, card styles, and animations
│   ├── js/
│   │   ├── api.js                        # HTTP client connecting to backend on port 5001
│   │   └── app.js                        # Routing, session, live meter, requests & search
│   └── package.json                      # Optional static file serving script (`npm run dev`)
├── database/
│   ├── schema.sql                        # DDL table definitions (users, students, alumni, requests)
│   └── seed.sql                          # Demo students and 15+ industry alumni mentors
└── docs/
    └── SEARCH_LIFECYCLE.md               # Search engine flow and algorithm specifications
```

---

## System Requirements

- **Java Development Kit**: OpenJDK 17 or higher
- **Build Tool**: Apache Maven 3.8+
- **Database**: MySQL Server 8.0+ or MariaDB 10.5+
- **Web Browser**: Chrome, Firefox, Safari, or Edge

---

## Database Setup

1. **Start your local MySQL service**:
   ```bash
   # On macOS via Homebrew:
   brew services start mysql
   ```

2. **Initialize Schema & Seed Data**:
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p alumni_mentoring_portal < database/seed.sql
   ```

3. **Verify Database Configuration**:
   Ensure `backend/.env` contains your MySQL credentials:
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=alumni_mentoring_portal
   DB_USER=alumni_user
   DB_PASSWORD=alumni_password
   PORT=5001
   SOCKET_PORT=5002
   ```

---

## Quick Start Guide

### 1. Start Java Backend

```bash
cd backend
mvn compile exec:java -Dexec.mainClass="com.alumni.Main"
```
*The backend server will start on **`http://localhost:5001`** and the TCP registration socket on port **`5002`**.*

### 2. Start Frontend

In a separate terminal tab:
```bash
cd frontend
npm run dev
```
*(Or use Python's built-in static server with zero dependencies):*
```bash
python3 -m http.server 5173
```
*Open your browser and navigate to **`http://localhost:5173/`**.*

---

## Database Seed Accounts

All accounts use cryptographically hashed passwords stored in MySQL:

| Role | Name | Email | Password |
| :--- | :--- | :--- | :--- |
| **Student** | Andrew Garfield | `andrewgarfield@gmail.com` | `Password@123` |
| **Student** | Rahul Sharma | `rahul.student@college.edu` | `Student@123` |
| **Student** | Ananya Patel | `ananya.p@college.edu` | `Student@123` |
| **Alumni (Mentor)** | Robert Pattinson (Tenet) | `robertpattinson@gmail.com` | `Password@123` |
| **Alumni (Mentor)** | Anurag Patil (Microsoft) | `anurag.patil@microsoft.com` | `Alumni@123` |
| **Alumni (Mentor)** | Rohan Mali (Google) | `rohanmali@google.com` | `Alumni@123` |
| **Alumni (Mentor)** | Priya Sharma (Amazon) | `priya.sharma@amazon.com` | `Alumni@123` |

---

## API Reference

### Mentorship Request Endpoints (Week 5)

| Method | Endpoint | Parameters / Payload | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/requests` | `{"studentId": 551, "mentorId": 488, "sessionGoal": "...", "message": "..."}` | Submit a new mentorship request from student to mentor |
| `GET`  | `/api/requests?studentId=...` | `studentId` (query) | Retrieve all mentorship requests sent by a student |
| `GET`  | `/api/requests?mentorId=...` | `mentorId` (query) | Retrieve all mentorship requests received by a mentor |
| `GET`  | `/api/requests?id=...` | `id` (query) | Retrieve specific mentorship request details |
| `PUT`  | `/api/requests` | `{"requestId": 18, "status": "ACCEPTED", "mentorResponse": "..."}` | Update request status (`ACCEPTED`, `REJECTED`, `CANCELLED`) |

### Profile & Avatar Endpoints

| Method | Endpoint | Parameters / Payload | Description |
| :--- | :--- | :--- | :--- |
| `GET`  | `/api/users/:id/avatar` | `id` (path) | Stream user avatar image binary (`image/jpeg`, `image/png`) |
| `HEAD` | `/api/users/:id/avatar` | `id` (path) | Check avatar existence and content headers |
| `GET`  | `/api/users/profile?id=...` | `id` (query) | Retrieve complete user profile details |
| `PUT`  | `/api/users/profile` | JSON profile payload | Update user details, bio, and mentee capacity |

### Authentication & Registration Endpoints

| Method | Endpoint | Description | Sample Request Payload |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate Student or Alumni | `{"email": "rahul.student@college.edu", "password": "Student@123"}` |
| `GET`  | `/api/auth/check-availability` | Real-time Email & Mobile In-Use Check | `?email=...&mobileNumber=...` |
| `POST` | `/api/auth/register/student` | Register a new Student | `{"fullName": "Aman Gupta", "email": "aman@college.edu", "mobileNumber": "9876543210", "password": "StrongP@ss1", "studentId": "STU-2024-042", "department": "Computer Engineering", "graduationYear": 2026}` |
| `POST` | `/api/auth/register/alumni` | Register a new Alumni Mentor | `{"fullName": "Ritu Roy", "email": "ritu@tech.com", "mobileNumber": "9876543215", "password": "StrongP@ss1", "department": "Information Technology", "graduationYear": 2021, "company": "Apple", "designation": "Software Engineer", "industry": "Information Technology", "skills": "Swift, iOS", "maxMentees": 5}` |
| `POST` | `/api/auth/password-strength` | Evaluate password strength | `{"password": "Password@123"}` |
| `GET`  | `/api/auth/registrations/:email` | Retrieve safe user profile | *URL parameter: email address* |
| `POST` | `/api/servlet/register` | Servlet-style Unified Form Endpoint | Form payload with `role: "student"` or `role: "alumni"` |
| `POST` | `/cgi-bin/register` | CGI-style URL-Encoded Form Endpoint | URL-encoded POST body |
| `TCP`  | `localhost:5002` | High-throughput Socket Server | Newline-delimited JSON registration message |

### Mentors & Directory Endpoints

| Method | Endpoint | Parameters | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/mentors/search` | `query`, `department`, `industry`, `minExperience` | Search mentors with manual algorithm matching & telemetry |
| `GET` | `/api/mentors` | — | Retrieve all alumni mentors |
| `GET` | `/api/mentors/:id` | `id` (path) | Retrieve individual mentor details by ID |
| `GET` | `/api/health` | — | Health check & system status |

---

## Terminal Image Inspection Guide

To verify avatars stored in the MySQL `users` table directly from the terminal:

### 1. View Image Metadata
```bash
mysql -u alumni_user -palumni_password alumni_mentoring_portal -e "
SELECT id, full_name, role, avatar_mime_type, ROUND(LENGTH(avatar_image) / 1024, 2) AS size_kb 
FROM users WHERE avatar_image IS NOT NULL;
"
```

### 2. Export & Inspect File Headers
```bash
mysql -u alumni_user -palumni_password alumni_mentoring_portal -N -s -r \
  -e "SELECT avatar_image FROM users WHERE id = 488;" > /tmp/avatar.jpg && file /tmp/avatar.jpg
```

### 3. Render High-Resolution Pixel Art Inside Terminal
```bash
mysql -u alumni_user -palumni_password alumni_mentoring_portal -N -s -r \
  -e "SELECT avatar_image FROM users WHERE id = 488;" > /tmp/avatar.jpg && \
  python3 -c "from PIL import Image; img = Image.open('/tmp/avatar.jpg').convert('RGB'); w = 80; h = int((img.size[1]/img.size[0])*w*0.5); img = img.resize((w, h*2), Image.Resampling.LANCZOS); [print(''.join(f'\033[38;2;{img.getpixel((x, y))[0]};{img.getpixel((x, y))[1]};{img.getpixel((x, y))[2]}m\033[48;2;{img.getpixel((x, y+1))[0]};{img.getpixel((x, y+1))[1]};{img.getpixel((x, y+1))[2]}m▀\033[0m' for x in range(w))) for y in range(0, h*2, 2)]"
```

### 4. Open in macOS Preview
```bash
open /tmp/avatar.jpg
```

---

## Search Engine & Algorithms

The backend incorporates custom Data Structures and Algorithms without relying on generic database queries:

1. **Manual Multi-Field Linear Search ($O(N)$)**:
   - Evaluates queries across `fullName`, `company`, `designation`, `skills`, `department`, and `bio`.
   - Tokenizes multi-word queries with whole-word preference bonuses.
2. **Manual Quicksort + Binary Search ($O(N \log N) + O(\log N)$)**:
   - In-memory Quicksort partitioning on chosen sorted keys (`fullName`, `company`, `graduationYear`, `experienceYears`).
   - Divide-and-conquer binary search with dual-pointer boundary expansion for duplicate keys and prefix queries.
3. **Telemetry Benchmarking**:
   - Operational comparison counters and high-precision execution timings (`System.nanoTime()`) returned in every search response.

---

## Automated Testing Suite

The project includes an extensive JUnit 5 test suite validating all core backend components:

```bash
cd backend
mvn clean test
```

### Test Coverage Breakdown (**87 Tests Run, 0 Failures**):
- **`MentorshipRequestTest` (23 tests)**: Request creation, self-request prevention, duplicate request blocking (`PENDING` lock), re-requesting after approval/rejection, mentor capacity validation, status transitions (`ACCEPTED`, `REJECTED`, `CANCELLED`), and HTTP `/api/requests` endpoints.
- **`ValidationTest` (28 tests)**: RFC email compliance, mobile number regex, full name constraints, 4-segment password strength evaluator, password mismatch, student ID regex, graduation years, HTTP registration, login flow, and password-strength REST endpoint.
- **`SearchAlgorithmsTest` (19 tests)**: Multi-attribute Linear Search scans, case insensitivity, Quicksort ordering, exact vs prefix Binary Search, boundary expansions, and execution telemetry metrics.
- **`ProfileTest` (13 tests)**: Profile picture uploads, avatar retrieval, MIME type verification, profile updates, and HTTP `/api/users/profile` endpoints.
- **`JDBCTest` (4 tests)**: Database connectivity, connection pool stability, and transactional integrity.

**Result: `Tests run: 87, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS — 100% Pass Rate)**

---

## License

This project is licensed under the MIT License — see the repository for details.
