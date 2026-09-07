# Java MySQL JDBC Backend

A high-performance Java backend utilizing the official MySQL Connector/J JDBC driver (`com.mysql.cj.jdbc.Driver`) to store and retrieve Student and Alumni records in the local MySQL database (`alumni_mentoring_portal`).

---

## 1. Features
- **Official MySQL JDBC Driver**: Configured with `com.mysql:mysql-connector-j` (version 9.2.0).
- **Transactional Atomicity**: `RegistrationDAO` executes multi-table inserts into `users` and either `students` or `alumni` wrapped in strict JDBC transaction boundaries (`conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()`).
- **SQL Injection Prevention**: All SQL statements use `PreparedStatement` with typed parameter binding.
- **Environment & Property Fallbacks**: Dynamically reads database credentials from environment variables (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`) or `application.properties`.
- **Integrated REST API**: Lightweight HTTP server running on `http://localhost:8080` exposing health checks, mentor listing, and registration endpoints.
- **Automated JUnit Test Suite**: Unit and integration tests verifying connection health, transactional student/alumni registration, and querying.

---

## 2. Directory Structure

```
java-backend/
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── alumni
│   │   │           ├── config
│   │   │           │   └── DBConnection.java       # JDBC Driver & Connection Factory
│   │   │           ├── dao
│   │   │           │   ├── AlumniDAO.java          # Query alumni mentors
│   │   │           │   └── RegistrationDAO.java    # Transactional JDBC inserts
│   │   │           ├── model
│   │   │           │   ├── Alumni.java             # Alumni model
│   │   │           │   ├── Student.java            # Student model
│   │   │           │   └── User.java               # Base User model
│   │   │           ├── server
│   │   │           │   └── JavaHttpServer.java     # Lightweight HTTP REST server
│   │   │           └── Main.java                   # CLI & Server bootstrap
│   │   └── resources
│   │       └── application.properties              # Connection & port settings
│   └── test
│       └── java
│           └── com
│               └── alumni
│                   └── JDBCTest.java               # JUnit 5 JDBC integration tests
└── README.md
```

---

## 3. How to Build & Run

### Prerequisites
- JDK 17+ (JDK 25 installed)
- Apache Maven 3.9+
- MySQL Server running on `localhost:3306` with database `alumni_mentoring_portal`

### 1. Compile & Run Tests
```bash
cd java-backend
mvn clean test
```

### 2. Start Java Backend Server
```bash
cd java-backend
mvn compile exec:java -Dexec.mainClass="com.alumni.Main"
```
The server will start on `http://localhost:8080`.

---

## 4. REST API Endpoints (Port 8080)

### A. Health Check
```bash
curl http://localhost:8080/api/health
```
**Response:**
```json
{
  "database": "MySQL Local",
  "runtime": "Java OpenJDK JDBC Driver",
  "status": "OK"
}
```

### B. Get Alumni Mentors
```bash
curl http://localhost:8080/api/mentors
```

### C. Register Student (Stored via JDBC in MySQL)
```bash
curl -X POST http://localhost:8080/api/register/student \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Pooja Kulkarni",
    "email": "pooja.k@example.com",
    "mobileNumber": "9876543220",
    "studentId": "STU2026_099",
    "department": "Computer Engineering",
    "graduationYear": 2026
  }'
```

### D. Register Alumni Mentor (Stored via JDBC in MySQL)
```bash
curl -X POST http://localhost:8080/api/register/alumni \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Sameer Joshi",
    "email": "sameer.j@uber.com",
    "mobileNumber": "9876543221",
    "department": "Information Technology",
    "graduationYear": 2020,
    "company": "Uber",
    "designation": "Staff Distributed Systems Engineer",
    "experienceYears": 6,
    "industry": "Information Technology",
    "skills": "Go, Java, Distributed Systems, Kafka",
    "bio": "Staff engineer working on dispatch services.",
    "maxMentees": 4
  }'
```
Records inserted by the Java JDBC backend are immediately available to the portal's Search UI and Node.js backend.
