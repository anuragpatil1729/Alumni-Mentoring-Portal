
Design search page layout and request lifecycle.	
Build Search UI.	
Implement manual Linear/Binary Search algorithm for alumni.	
Test search algorithm accuracy.


# Alumni Mentoring Portal

An interactive web application designed to connect students with alumni mentors.

---

## Recent Updates: Manual Server-Side Registration Validation

Server-side manual input validation has been implemented for **Student** and **Alumni** registration endpoints to enforce data integrity and strict format checks before processing.

### Key Validation Features

- **Required Field Checks**: Ensures all mandatory fields are present and non-empty.
- **Email Validation**: Validates RFC 5322 email syntax and length restrictions.
- **Mobile Number Validation**: Supports 10-digit standard numbers (`^[6-9]\d{9}$`) and E.164 international formats.
- **Password Strength Rules**: Enforces password requirements:
  - Minimum 8 characters, maximum 128 characters.
  - Must include at least 1 uppercase letter (`A-Z`).
  - Must include at least 1 lowercase letter (`a-z`).
  - Must include at least 1 number (`0-9`).
  - Must include at least 1 special character (`!@#$%^&*()_+-=[]{};:'",.<>?/`).
- **Student-Specific Validation**:
  - `studentId` / `rollNumber`: Alphanumeric format (3-20 characters).
  - `department` / `branch`: Required string.
  - `graduationYear` / `passoutYear`: Valid 4-digit current or future year.
- **Alumni-Specific Validation**:
  - `graduationYear` / `passoutYear`: Valid 4-digit past or current year (1950–present).
  - `department` / `branch`: Required string.
  - `company`: Required current company name.
  - `designation`: Required job title.
  - `linkedInProfile`: Optional field with LinkedIn URL format validation.

---

## API Endpoints

### Registration Endpoints

| Method | Endpoint | Description | Middleware / Validation |
| ------ | -------- | ----------- | ----------------------- |
| `POST` | `/api/auth/register/student` | Register a new Student | `validateStudentRegistrationMiddleware` |
| `POST` | `/api/auth/register/alumni` | Register a new Alumni | `validateAlumniRegistrationMiddleware` |
| `POST` | `/api/auth/register` | Unified registration endpoint | `validateRegistrationMiddleware` |
| `POST` | `/api/servlet/register` | Servlet-style JSON registration endpoint | Shared registration processor |
| `POST` | `/cgi-bin/register` | CGI-style URL-encoded form registration endpoint | Shared registration processor |
| `TCP`  | `localhost:5002` | Newline-delimited JSON socket registration channel | Set `SOCKET_PORT` to override, `ENABLE_SOCKET_SERVER=false` to disable |
| `GET`  | `/api/health` | Backend health check | None |

### Error Response Format (HTTP `400 Bad Request`)

When validation fails, the API responds with a `400 Bad Request` status code and a structured field error dictionary:

```json
{
  "success": false,
  "message": "Student registration validation failed",
  "errors": {
    "email": "Please provide a valid email address.",
    "mobileNumber": "Mobile number must be a valid 10-digit number (e.g. 9876543210 or +919876543210).",
    "password": "Password must be 8-128 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.",
    "studentId": "Student ID / Roll Number is required."
  }
}
```

---

## Setup & Testing

### 1. Installation
```bash
cd backend
npm install
```

### MySQL configuration

Create the database and registration tables before starting the API:

```bash
mysql -u root -p < database/schema.sql
```

Set these environment variables as appropriate: `DB_HOST`, `DB_PORT`, `DB_USER`,
`DB_PASSWORD`, `DB_NAME`, and `DB_CONNECTION_LIMIT`. The defaults target a local
MySQL server and database named `alumni_mentoring_portal`.

Validated registrations are inserted transactionally into `users` plus either
`students` or `alumni`; passwords are stored only as salted scrypt hashes. Retrieve
safe, non-password registration data with `GET /api/auth/registrations/:email`.

### 2. Start Backend Server
```bash
npm start
```
*Server will listen on port `5001` (or `process.env.PORT`). The optional TCP registration socket listens on `5002` by default (`SOCKET_PORT`) and can be disabled with `ENABLE_SOCKET_SERVER=false`.*

### 3. Run Automated Tests
```bash
npm test
```
*Executes unit, HTTP API, Servlet-style, CGI-style form, and TCP socket tests in `tests/validation.test.js`.*
