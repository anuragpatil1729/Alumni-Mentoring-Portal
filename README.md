Design search page layout and request lifecycle. Build Search UI. Implement manual Linear/Binary Search algorithm for alumni. Test search algorithm accuracy.

# AlumniConnect — Student & Alumni Mentoring Portal

[![Java](https://img.shields.io/badge/Java-OpenJDK_17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Build-Maven_3.8%2B-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/Database-MySQL_Connector%2FJ_9.2.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Frontend](https://img.shields.io/badge/Frontend-Vanilla_HTML5_%2F_CSS3_%2F_ES6-E34F26?logo=html5&logoColor=white)](frontend/)
[![Tests](https://img.shields.io/badge/JUnit_5-48_Tests_Passing-25A162?logo=junit5&logoColor=white)](backend/src/test/)

**AlumniConnect** is a full-stack alumni mentorship platform connecting undergraduate and graduate students directly with verified industry alumni working across top technology companies, consulting firms, and research labs.

The portal features a **100% Java JDBC backend engine** and a **zero-dependency Vanilla HTML5/CSS3/JavaScript frontend** designed with clean, state-of-the-art UI aesthetics, robust real-time validation, and high-performance search algorithms.

---

## Table of Contents
- [Key Features](#key-features)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Project Directory Structure](#project-directory-structure)
- [System Requirements](#system-requirements)
- [Database Setup](#database-setup)
- [Quick Start Guide](#quick-start-guide)
  - [1. Start Java Backend](#1-start-java-backend)
  - [2. Start Frontend](#2-start-frontend)
- [Demo Credentials](#demo-credentials)
- [API Reference](#api-reference)
- [Input Validation & Security](#input-validation--security)
- [Search Engine & Algorithms](#search-engine--algorithms)
- [Automated Testing Suite](#automated-testing-suite)

---

## Key Features

### 🎓 Student Registration & Profile
- **Two-Section Step Form**: Cleanly divides registration into **Personal Information** and **Academic Information**.
- **Student ID Format Check**: Enforces standard academic IDs (`STU-2024-001`, `CS2023045`) via regex `^[a-zA-Z0-9/-]{3,20}$`.
- **Automatic Session Onboarding**: New student registrations automatically log in and transition straight to the mentor directory.

### 💼 Alumni Mentor Registration
- **Comprehensive Profile Builder**: Includes current company, designation, department, graduation year, years of experience, industry, comma-delimited skills, personal bio, and mentee capacity.
- **LinkedIn Profile Verification**: Validates LinkedIn URL syntax (`https://linkedin.com/in/...`).

### 🔐 Mandatory Authentication Flow
- **Gatekeeper Pattern**: Directs unauthenticated visitors to the **Sign In** view before accessing the mentors directory or requesting sessions.
- **Session Persistence**: Stores session tokens and user profiles in `localStorage` (`alumniConnectUser`).
- **Dynamic Header Navigation**: Displays user avatar initial, user full name, and role badge (`STUDENT` or `MENTOR`), along with a quick **Logout** button.
- **One-Click Demo Accounts**: Instant credential autofill for testing both Student and Mentor personas.

### 🛡️ Real-Time Name & Password Strength Testing
- **Full Name Validator**: Enforces 2–50 character limits, letters/spaces/hyphens/periods only, prevents numbers or illegal symbols, and disallows consecutive whitespace with instant inline feedback.
- **Interactive 4-Segment Password Meter**: Visual color transitions from Red (`Weak`) ➔ Orange (`Fair`) ➔ Amber (`Good`) ➔ Emerald Green (`Strong`).
- **Live Requirement Checklist**: Real-time tick indicators for:
  - `✓ 8+ Characters`
  - `✓ Uppercase letter (A-Z)`
  - `✓ Lowercase letter (a-z)`
  - `✓ Number (0-9)`
  - `✓ Special symbol (!@#$%^&*)`
- **Live Password Match Indicator**: Instant visual confirmation showing `✓ Passwords match` or `✕ Passwords do not match`.
- **Backend Strength API**: Dedicated REST endpoint `POST /api/auth/password-strength` returning telemetry, score (0–4), criterion flags, and missing suggestions.

### 🔍 Streamlined Mentor Search Directory
- **Distraction-Free UI**: Removed intrusive algorithm toggle cards from the UI to provide a clean search experience.
- **Instant Search with Debouncing**: 280ms debounced input for fast responsive filtering across name, company, title, skills, and department.
- **Faceted Filters**: Quick dropdowns for Department, Industry sector, and Minimum Experience.
- **Telemetry Metrics Banner**: Real-time display of dataset pool, matched count, comparison operations, and execution latency in milliseconds.
- **1-on-1 Mentorship Request Modal**: Dialog allowing students to choose session goals (*Career Guidance*, *System Design Prep*, *Resume Review*, *Higher Studies*) and compose a personalized introductory note.

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
│   • Tables: users, students, alumni (InnoDB, Foreign Keys)  │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Technology | Description |
| :--- | :--- | :--- |
| **Frontend** | HTML5, Vanilla CSS3, ES6 JavaScript | Zero build tools, zero node runtime dependencies, native browser APIs. |
| **Backend** | Java 17+, Maven | Pure Java HTTP Server (`com.sun.net.httpserver`), JSON (`org.json`). |
| **Database** | MySQL / MariaDB | Relational database accessed via `com.mysql.cj.jdbc.Driver` with transactions. |
| **Network** | TCP Socket Server | Newline-delimited JSON stream server for cross-platform integration on port 5002. |
| **Testing** | JUnit 5 (`org.junit.jupiter`) | 48 automated test cases covering JDBC, validations, algorithms, and endpoints. |

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
│   │   │   │   │   └── RegistrationDAO.java # Transactional user & student registration
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java         # Base user entity
│   │   │   │   │   ├── Student.java      # Student profile entity
│   │   │   │   │   └── Alumni.java       # Alumni mentor entity
│   │   │   │   ├── search/
│   │   │   │   │   ├── SearchAlgorithms.java # Manual Linear & Binary Search engine
│   │   │   │   │   └── SearchResult.java # Telemetry & results container
│   │   │   │   ├── server/
│   │   │   │   │   ├── HttpServerApp.java# HTTP endpoints & REST routing
│   │   │   │   │   └── HttpUtils.java    # CORS headers & JSON request parsing
│   │   │   │   ├── service/
│   │   │   │   │   ├── RegistrationService.java # Business validation & registration logic
│   │   │   │   │   └── SocketServer.java # TCP Socket server (Port 5002)
│   │   │   │   └── validation/
│   │   │   │       ├── InputValidator.java  # Name, password strength, RFC email validators
│   │   │   │       ├── ValidationConstants.java # Regex patterns & error message constants
│   │   │   │       └── ValidationResult.java    # Validation status & field error dictionary
│   │   │   └── resources/
│   │   │       └── application.properties   # Port configuration & MySQL defaults
│   │   └── test/java/com/alumni/
│   │       ├── JDBCTest.java             # Database connectivity & CRUD test suite
│   │       ├── SearchAlgorithmsTest.java # Linear vs Binary search accuracy & telemetry tests
│   │       └── ValidationTest.java       # 25 validation & endpoint integration tests
│   └── .env                              # MySQL credentials & environment config
├── frontend/                             # Pure Vanilla HTML/CSS/JS Frontend
│   ├── index.html                        # Application structure (Login, Registration, Search)
│   ├── css/
│   │   └── styles.css                    # Design system, variables, animations, meter styling
│   ├── js/
│   │   ├── api.js                        # HTTP client connecting to backend on port 5001
│   │   └── app.js                        # Session state, form handling, live meter & search
│   └── package.json                      # Optional static file serving script (`npm run dev`)
├── database/
│   ├── schema.sql                        # DDL table definitions (users, students, alumni)
│   └── seed.sql                          # Demo students and 15+ industry alumni mentors
└── docs/
    └── SEARCH_LIFECYCLE.md               # Search engine flow and algorithm specifications
```

---

## System Requirements

- **Java Development Kit**: OpenJDK 17 or higher
- **Build Tool**: Apache Maven 3.8+
- **Database**: MySQL Server 8.0+ or MariaDB 10.5+
- **Web Browser**: Modern browser (Chrome, Firefox, Safari, Edge)

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

## Demo Credentials

You can use the one-click demo buttons on the login page or manually enter the credentials below:

| Role | Name | Email | Password |
| :--- | :--- | :--- | :--- |
| **Student** | Rahul Sharma | `rahul.student@college.edu` | `Student@123` |
| **Student** | Ananya Patel | `ananya.p@college.edu` | `Student@123` |
| **Alumni (Mentor)** | Anurag Patil (Microsoft) | `anurag.patil@microsoft.com` | `Alumni@123` |
| **Alumni (Mentor)** | Rohan Mali (Google) | `rohanmali@google.com` | `Alumni@123` |
| **Alumni (Mentor)** | Priya Sharma (Amazon) | `priya.sharma@amazon.com` | `Alumni@123` |

---

## API Reference

### Authentication & Registration Endpoints

| Method | Endpoint | Description | Sample Request Payload |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate Student or Alumni | `{"email": "rahul.student@college.edu", "password": "Student@123"}` |
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

## Input Validation & Security

All registration payloads undergo strict manual validation before database access:

- **Full Name**: 2–50 characters, only letters, spaces, hyphens, and periods (`^[a-zA-Z\s.-]{2,50}$`). Prohibits numbers, special symbols, and consecutive spaces.
- **Email**: RFC 5322 compliance pattern (`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`), max 254 chars.
- **Mobile Number**: Indian standard 10-digit mobile numbers (`^[6-9]\d{9}$`) or international E.164 formats.
- **Password Strength Rules**:
  - Minimum 8 characters, maximum 128 characters.
  - At least 1 uppercase letter (`A-Z`).
  - At least 1 lowercase letter (`a-z`).
  - At least 1 numeric digit (`0-9`).
  - At least 1 special character (`!@#$%^&*()_+-=[]{};':",.<>?/`).
- **Student ID**: 3–20 alphanumeric characters (`^[a-zA-Z0-9/-]{3,20}$`).
- **Graduation Year**: Validated against dynamic bounds (`[currentYear - 2, currentYear + 10]` for students; `[1950, currentYear]` for alumni).

### Structured Error Response (HTTP `400 Bad Request`)

```json
{
  "success": false,
  "message": "Student registration validation failed",
  "errors": {
    "fullName": "Full name can only contain letters, spaces, hyphens, and periods.",
    "email": "Please provide a valid email address.",
    "password": "Must be at least 8 characters. Include at least one special character (!@#$%^&*).",
    "studentId": "Student ID must be between 3 and 20 alphanumeric characters."
  }
}
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

### Test Coverage Summary:
- **`ValidationTest` (25 tests)**: RFC email, mobile formats, full name constraints, password strength evaluator (0–4 scores), password mismatch, student ID regex, graduation years, HTTP registration, login flow, and password-strength REST endpoint.
- **`SearchAlgorithmsTest` (19 tests)**: Linear search multi-attribute scans, case insensitivity, Quicksort ordering, exact vs prefix Binary Search, boundary expansions, and timing metrics.
- **`JDBCTest` (4 tests)**: MySQL JDBC driver loading, database connection stability, and transactional integrity.

**Result: `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS)**

---

## License

This project is licensed under the MIT License — see the repository for details.
