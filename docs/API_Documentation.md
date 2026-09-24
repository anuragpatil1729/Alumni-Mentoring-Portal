# Alumni Mentoring Portal — REST API Documentation

**Backend Engine:** Java OpenJDK JDBC REST Server  
**Database:** MySQL Server 8.0+ / MariaDB 10.5+  
**Default Base URL:** `http://localhost:5001`  
**TCP Socket Server:** `localhost:5002`  
**Version:** 1.0.0  

---

## Table of Contents
1. [Overview & Standards](#1-overview--standards)
2. [Global Headers & CORS](#2-global-headers--cors)
3. [System & Health Endpoints](#3-system--health-endpoints)
4. [Authentication & Registration Endpoints](#4-authentication--registration-endpoints)
5. [Mentor Directory & DSA Search Endpoints](#5-mentor-directory--dsa-search-endpoints)
6. [User Profile Management Endpoints](#6-user-profile-management-endpoints)
7. [Avatar Media Storage & Streaming Endpoints](#7-avatar-media-storage--streaming-endpoints)
8. [Mentorship Request System Endpoints (Week 5)](#8-mentorship-request-system-endpoints)
9. [Standard Error Responses & Status Codes](#9-standard-error-responses--status-codes)

---

## 1. Overview & Standards

The Alumni Mentoring Portal provides a high-performance REST API developed in native Java with direct JDBC database persistence to **MySQL / MariaDB**. All endpoints accept and return `application/json` payloads unless streaming raw binary media (such as avatar images).

- **Data Formats**: Request and response bodies are JSON objects (`UTF-8`).
- **Dates & Timestamps**: ISO 8601 strings (`YYYY-MM-DDTHH:MM:SSZ`).
- **Database Atomicity**: Registration and user updates operate under strict JDBC transactional boundaries (`setAutoCommit(false)` with atomic commit and rollback).
- **Security**: Passwords are saved as cryptographically salted hashes using `SHA-256` (`sha256$<salt_hex>$<hash_hex>`). All queries utilize `PreparedStatement` to ensure complete protection against SQL injection.

---

## 2. Global Headers & CORS

All HTTP endpoints automatically attach the following Cross-Origin Resource Sharing (CORS) headers to support decouple frontend architectures:

```http
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD
Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With
```

Clients sending pre-flight `OPTIONS` requests receive `HTTP 204 No Content`.

---

## 3. System & Health Endpoints

### 3.1 Root Service Information
- **URL:** `GET /`
- **Description:** Verifies that the Java backend runtime is reachable and returns application metadata.

#### Response (HTTP 200 OK)
```json
{
  "message": "Alumni Mentoring Portal Backend API is running (Java OpenJDK JDBC Engine)",
  "status": "OK",
  "frontendUrl": "http://localhost:5173",
  "healthCheck": "/api/health"
}
```

---

### 3.2 Service Health Check
- **URL:** `GET /api/health`
- **Description:** Verifies service uptime and database responsiveness.

#### Response (HTTP 200 OK)
```json
{
  "status": "OK",
  "timestamp": "2026-09-24T01:30:00.000Z"
}
```

---

## 4. Authentication & Registration Endpoints

### 4.1 User Authentication (Login)
- **URL:** `POST /api/auth/login`
- **Description:** Authenticates a student or alumni user using their registered email and plaintext password. Comparison of email is case-insensitive, while password verification is strictly case-sensitive.

#### Request Body
```json
{
  "email": "anurag.patil@microsoft.com",
  "password": "Alumni@123"
}
```

#### Response (HTTP 200 OK — Alumni)
```json
{
  "success": true,
  "message": "Login successful!",
  "user": {
    "id": 101,
    "fullName": "Anurag Patil",
    "email": "anurag.patil@microsoft.com",
    "mobileNumber": "9876543210",
    "role": "alumni",
    "department": "Computer Engineering",
    "graduationYear": 2022,
    "company": "Microsoft",
    "designation": "Senior AI Engineer",
    "linkedinProfile": "https://linkedin.com/in/anurag-patil",
    "avatarUrl": "/api/users/101/avatar"
  }
}
```

#### Response (HTTP 200 OK — Student)
```json
{
  "success": true,
  "message": "Login successful!",
  "user": {
    "id": 104,
    "fullName": "Andrew Garfield",
    "email": "andrewgarfield@gmail.com",
    "mobileNumber": "9876543213",
    "role": "student",
    "department": "Computer Engineering",
    "graduationYear": 2026,
    "studentId": "STU2026_001"
  }
}
```

#### Error Responses
- **HTTP 400 Bad Request**: Missing email or password (`{"success": false, "message": "Email and password are required."}`).
- **HTTP 401 Unauthorized**: Invalid email or incorrect password (`{"success": false, "message": "Invalid email or password."}`).

---

### 4.2 Check Credential Availability
- **URL:** `GET /api/auth/check-availability` or `POST /api/auth/check-availability`
- **Description:** Pre-registration validation endpoint to check whether an email address or mobile phone number is already registered in the database.

#### Query Parameters / Request Body
| Parameter | Type | Required | Description |
|---|---|---|---|
| `email` | string | Optional | Email address to evaluate |
| `mobileNumber` | string | Optional | 10-digit mobile number to evaluate |

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "email": "new.student@college.edu",
  "emailAvailable": true,
  "emailMessage": "Email is available.",
  "mobileNumber": "9876543299",
  "mobileAvailable": false,
  "mobileMessage": "This mobile number is already in use."
}
```

---

### 4.3 Evaluate Password Strength
- **URL:** `POST /api/auth/password-strength` or `GET /api/auth/password-strength`
- **Description:** Real-time evaluator calculating complexity scores (0 to 4), classification levels (`Weak`, `Fair`, `Good`, `Strong`), and actionable suggestions based on length, casing, numbers, and special characters.

#### Request Body
```json
{
  "password": "StrongPassword@2026"
}
```

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "valid": true,
  "strength": {
    "score": 4,
    "level": "Strong",
    "hasMinLength": true,
    "hasUpperCase": true,
    "hasLowerCase": true,
    "hasDigit": true,
    "hasSpecialChar": true,
    "suggestions": []
  }
}
```

---

### 4.4 Register Student
- **URL:** `POST /api/auth/register/student`
- **Description:** Atomically registers a new student user, inserting records into both `users` and `students` tables in MySQL / MariaDB.

#### Request Body
```json
{
  "fullName": "Aarav Sharma",
  "email": "aarav.s@college.edu",
  "mobileNumber": "9876543288",
  "password": "Password@123",
  "studentId": "STU-2026-088",
  "department": "Computer Engineering",
  "graduationYear": 2026
}
```

#### Response (HTTP 201 Created)
```json
{
  "success": true,
  "message": "Student account created successfully!",
  "id": 105,
  "email": "aarav.s@college.edu",
  "role": "student"
}
```

#### Error Responses
- **HTTP 400 Bad Request**: Input validation failure (`errors` dictionary detailing invalid fields).
- **HTTP 409 Conflict**: Email or mobile number already exists in the database.

---

### 4.5 Register Alumni Mentor
- **URL:** `POST /api/auth/register/alumni`
- **Description:** Atomically registers an alumni mentor, storing records in `users` and `alumni` tables in MySQL / MariaDB.

#### Request Body
```json
{
  "fullName": "Divya Deshmukh",
  "email": "divya.d@oracle.com",
  "mobileNumber": "9876543289",
  "password": "Password@123",
  "department": "Information Technology",
  "graduationYear": 2020,
  "company": "Oracle",
  "designation": "Principal Cloud Engineer",
  "experienceYears": 6,
  "industry": "Information Technology",
  "skills": "Java, Cloud, Kubernetes, Microservices",
  "bio": "Cloud architect with extensive experience in enterprise systems.",
  "maxMentees": 4,
  "linkedInProfile": "https://linkedin.com/in/divya-deshmukh"
}
```

#### Response (HTTP 201 Created)
```json
{
  "success": true,
  "message": "Alumni account created successfully!",
  "id": 106,
  "email": "divya.d@oracle.com",
  "role": "alumni"
}
```

---

### 4.6 Unified Registration Endpoint
- **URL:** `POST /api/auth/register` (Also aliased to `/api/servlet/register` and `/cgi-bin/register`)
- **Description:** Multiplexed registration accepting a required `"role": "student" | "alumni"` field in the payload. Dispatches internally to the respective handler.

---

### 4.7 Safe Registration Profile Lookup
- **URL:** `GET /api/auth/registrations/{email}`
- **Description:** Retrieves a sanitized public profile by email address without exposing password hashes.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "registration": {
    "id": 101,
    "fullName": "Anurag Patil",
    "email": "anurag.patil@microsoft.com",
    "mobileNumber": "9876543210",
    "role": "alumni",
    "department": "Computer Engineering",
    "graduationYear": 2022,
    "company": "Microsoft",
    "designation": "Senior AI Engineer",
    "skills": "Java, Python, C++, Azure",
    "maxMentees": 5
  }
}
```

---

## 5. Mentor Directory & DSA Search Endpoints

### 5.1 List All Mentors
- **URL:** `GET /api/mentors`
- **Description:** Returns an array of all registered alumni mentors.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "count": 10,
  "data": [
    {
      "id": 101,
      "fullName": "Anurag Patil",
      "email": "anurag.patil@microsoft.com",
      "company": "Microsoft",
      "designation": "Senior AI Engineer",
      "department": "Computer Engineering",
      "graduationYear": 2022,
      "experienceYears": 4,
      "skills": "Java, Python, Azure, Machine Learning",
      "industry": "Information Technology",
      "bio": "AI Engineer building distributed intelligence systems.",
      "linkedInProfile": "https://linkedin.com/in/anurag-patil",
      "avatarUrl": "/api/users/101/avatar"
    }
  ]
}
```

---

### 5.2 Get Mentor by ID
- **URL:** `GET /api/mentors/{id}`
- **Description:** Fetches complete profile details for a specific mentor.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "data": {
    "id": 101,
    "fullName": "Anurag Patil",
    "email": "anurag.patil@microsoft.com",
    "company": "Microsoft",
    "designation": "Senior AI Engineer",
    "department": "Computer Engineering",
    "graduationYear": 2022,
    "experienceYears": 4,
    "skills": "Java, Python, Azure",
    "industry": "Information Technology",
    "bio": "AI Engineer building distributed intelligence systems.",
    "linkedInProfile": "https://linkedin.com/in/anurag-patil",
    "avatarUrl": "/api/users/101/avatar"
  }
}
```

---

### 5.3 Algorithmic Mentor Search (Linear & Binary)
- **URL:** `GET /api/mentors/search`
- **Description:** Executes high-speed algorithmic searching with real-time performance telemetry. Supports multi-field **Linear Search** ($O(N)$) or sorted key **Binary Search** ($O(\log N)$).

#### Query Parameters
| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | string | `""` | Search query term |
| `algorithm` | string | `linear` | Algorithm mode: `linear` or `binary` |
| `key` | string | `fullName` | Target sort key for binary search (`fullName`, `company`, `graduationYear`, `experienceYears`) |
| `exact` | boolean | `false` | Whether to perform exact match in binary mode |
| `department` | string | Optional | Filter by department |
| `industry` | string | Optional | Filter by industry |
| `company` | string | Optional | Filter by company name |
| `minExperience`| int | Optional | Minimum years of experience |
| `maxExperience`| int | Optional | Maximum years of experience |
| `graduationYear`| int | Optional | Filter by graduation year |

#### Example Request
```http
GET /api/mentors/search?query=Microsoft&algorithm=linear&department=Computer+Engineering
```

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "query": "Microsoft",
  "algorithm": "linear",
  "searchKey": "multi-field",
  "metrics": {
    "totalRecords": 10,
    "resultsCount": 2,
    "comparisons": 10,
    "executionTimeMs": 0.2451
  },
  "data": [
    {
      "id": 101,
      "fullName": "Anurag Patil",
      "company": "Microsoft",
      "designation": "Senior AI Engineer",
      "department": "Computer Engineering",
      "skills": "Java, Python, Azure",
      "experienceYears": 4
    }
  ]
}
```

---

## 6. User Profile Management Endpoints

### 6.1 Get User Profile
- **URL:** `GET /api/profile?id={userId}` (or `/api/profile/{userId}`)
- **Description:** Retrieves the profile for any registered user (student or alumni).

#### Response (HTTP 200 OK — Student)
```json
{
  "success": true,
  "profile": {
    "id": 104,
    "fullName": "Andrew Garfield",
    "email": "andrewgarfield@gmail.com",
    "mobileNumber": "9876543213",
    "role": "student",
    "department": "Computer Engineering",
    "graduationYear": 2026,
    "studentId": "STU2026_001",
    "avatarUrl": null
  }
}
```

---

### 6.2 Update User Profile
- **URL:** `PUT /api/profile?id={userId}` or `POST /api/profile?id={userId}`
- **Description:** Updates the profile attributes for the specified user. Enforces role-specific validation and unique mobile number verification excluding the current user.

#### Request Body (Student Profile Update)
```json
{
  "fullName": "Andrew Garfield",
  "mobileNumber": "9876543213",
  "department": "Information Technology",
  "graduationYear": 2027
}
```

#### Request Body (Alumni Profile Update)
```json
{
  "fullName": "Anurag Patil",
  "mobileNumber": "9876543210",
  "department": "Computer Engineering",
  "graduationYear": 2022,
  "company": "Microsoft AI Core",
  "designation": "Principal AI Systems Architect",
  "experienceYears": 5,
  "industry": "Information Technology",
  "skills": "Java, Python, Distributed AI, LLMs",
  "bio": "Leading generative AI systems engineering.",
  "maxMentees": 6,
  "linkedInProfile": "https://linkedin.com/in/anurag-patil"
}
```

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "message": "Profile updated successfully!",
  "profile": {
    "id": 101,
    "fullName": "Anurag Patil",
    "company": "Microsoft AI Core",
    "designation": "Principal AI Systems Architect"
  }
}
```

---

## 7. Avatar Media Storage & Streaming Endpoints

Avatars are stored directly inside the MySQL / MariaDB `users` table as `MEDIUMBLOB` with MIME type tracking (`image/jpeg`, `image/png`, `image/webp`).

### 7.1 Stream Avatar Image Binary
- **URL:** `GET /api/users/{userId}/avatar`
- **Description:** Streams raw binary bytes of the user's profile image directly from the database.
- **Headers Returned:**
  - `Content-Type`: `image/png` | `image/jpeg` | `image/webp`
  - `Cache-Control`: `public, max-age=86400`
  - `Content-Length`: Size in bytes

---

### 7.2 Get Avatar Metadata (HEAD)
- **URL:** `HEAD /api/users/{userId}/avatar`
- **Description:** Retrieves headers (including `Content-Length` and `Content-Type`) without payload transmission.

---

### 7.3 Upload Profile Avatar
- **URL:** `POST /api/users/{userId}/avatar`
- **Description:** Uploads and replaces the user's avatar. Accepts either JSON payload with base64 encoded image or raw binary stream. Maximum allowed size is 5MB.

#### JSON Request Body
```json
{
  "userId": 101,
  "imageData": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE...",
  "mimeType": "image/png"
}
```

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "message": "Avatar updated successfully in MySQL BLOB.",
  "userId": 101,
  "avatarUrl": "/api/users/101/avatar",
  "sizeBytes": 14205,
  "mimeType": "image/png"
}
```

---

### 7.4 Delete Avatar Image
- **URL:** `DELETE /api/users/{userId}/avatar`
- **Description:** Clears the avatar BLOB and MIME type from the user record, reverting the profile to initials fallback.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "deleted": true,
  "message": "Avatar removed successfully."
}
```

---

## 8. Mentorship Request System Endpoints

### 8.1 Create Mentorship Request
- **URL:** `POST /api/requests`
- **Description:** Submits a 1-on-1 mentorship request from a student to an alumni mentor. Validates that:
  - Both student and mentor accounts exist and have matching roles.
  - Student is not attempting to request themselves.
  - There is no active `PENDING` request between the pair (prevents duplicate spam).
  - Mentor has not reached their active mentee capacity limit (`max_mentees`).

#### Request Body
```json
{
  "studentId": 104,
  "mentorId": 101,
  "sessionGoal": "System Design & AI Interview Preparation",
  "message": "Hello Anurag, I am preparing for distributed systems interviews and would love your guidance on LLM architectures."
}
```

#### Response (HTTP 201 Created)
```json
{
  "success": true,
  "message": "Mentorship request sent successfully!",
  "requestId": 12,
  "status": "PENDING"
}
```

---

### 8.2 Get Requests by Student
- **URL:** `GET /api/requests?studentId={studentUserId}`
- **Description:** Lists all mentorship requests submitted by a specific student, including current statuses and mentor response notes.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "count": 1,
  "requests": [
    {
      "id": 12,
      "studentId": 104,
      "mentorId": 101,
      "mentorName": "Anurag Patil",
      "mentorCompany": "Microsoft",
      "sessionGoal": "System Design & AI Interview Preparation",
      "status": "PENDING",
      "createdAt": "2026-09-24T06:15:00"
    }
  ]
}
```

---

### 8.3 Get Requests by Mentor
- **URL:** `GET /api/requests?mentorId={mentorUserId}`
- **Description:** Lists all mentorship requests received by an alumni mentor for their review.

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "count": 1,
  "requests": [
    {
      "id": 12,
      "studentId": 104,
      "studentName": "Andrew Garfield",
      "studentDepartment": "Computer Engineering",
      "studentGraduationYear": 2026,
      "sessionGoal": "System Design & AI Interview Preparation",
      "message": "Hello Anurag, I am preparing...",
      "status": "PENDING",
      "createdAt": "2026-09-24T06:15:00"
    }
  ]
}
```

---

### 8.4 Update Request Status (Accept / Decline / Cancel)
- **URL:** `PUT /api/requests?id={requestId}`
- **Description:** Mentors accept or decline pending requests with personalized notes; students can cancel pending requests.

#### Request Body (Acceptance with Meeting Note)
```json
{
  "status": "ACCEPTED",
  "mentorResponse": "Happy to connect! Let's schedule a 30-min call via Microsoft Teams next Tuesday at 5 PM."
}
```

#### Response (HTTP 200 OK)
```json
{
  "success": true,
  "message": "Request marked as ACCEPTED.",
  "requestId": 12,
  "status": "ACCEPTED"
}
```

---

## 9. Standard Error Responses & Status Codes

All errors return structured JSON objects containing actionable diagnostic details:

```json
{
  "success": false,
  "message": "Human-readable description of error condition.",
  "errors": {
    "fieldName": "Specific validation failure message."
  }
}
```

### HTTP Status Code Reference

| Status Code | Meaning | Typical Portal Trigger |
|:---|:---|:---|
| **200 OK** | Request succeeded | Successful login, profile retrieval, search results, avatar stream. |
| **201 Created** | Resource created | Registration completed, mentorship request submitted. |
| **204 No Content**| Pre-flight success | HTTP `OPTIONS` CORS handshake. |
| **400 Bad Request**| Malformed payload | Missing required fields, invalid date/id format, size exceeding 5MB. |
| **401 Unauthorized**| Authentication failed | Invalid email or incorrect password. |
| **404 Not Found** | Resource missing | User ID or mentor ID not present in database. |
| **405 Method Not Allowed**| Wrong verb | Sending `GET` to `/api/auth/login` or `PUT` to `/api/health`. |
| **409 Conflict** | Business rule conflict| Duplicate email or mobile; duplicate pending request; mentor capacity full. |
| **500 Server Error**| Internal exception | Database connection timeout or unexpected SQL fault. |

---
*Documentation maintained for Alumni Mentoring Portal.*
