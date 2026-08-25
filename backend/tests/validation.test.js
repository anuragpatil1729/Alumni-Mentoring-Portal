/**
 * Validation Test Suite for Student and Alumni Registration Fields
 */

const assert = require('assert');
const app = require('../src/app');
const {
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput,
  isValidEmail,
  isValidMobile,
  isValidPassword
} = require('../src/utils/validation');
const { VALIDATION_MESSAGES } = require('../src/utils/constants');

let passedTests = 0;
let failedTests = 0;

function test(name, fn) {
  try {
    fn();
    console.log(`  ✓ ${name}`);
    passedTests++;
  } catch (err) {
    console.error(`  ✗ ${name}`);
    console.error(`    Error: ${err.message}`);
    failedTests++;
  }
}

async function asyncTest(name, fn) {
  try {
    await fn();
    console.log(`  ✓ ${name}`);
    passedTests++;
  } catch (err) {
    console.error(`  ✗ ${name}`);
    console.error(`    Error: ${err.message}`);
    failedTests++;
  }
}

console.log('====================================================');
console.log('  RUNNING MANUAL INPUT VALIDATION TEST SUITE');
console.log('====================================================\n');

// 1. Email Format Unit Tests
console.log('[1] Email Format Validation Tests');
test('Valid email addresses should pass', () => {
  assert.strictEqual(isValidEmail('student@example.com'), true);
  assert.strictEqual(isValidEmail('john.doe123@university.edu.in'), true);
  assert.strictEqual(isValidEmail('alumni-user@tech.org'), true);
});

test('Invalid email addresses should fail', () => {
  assert.strictEqual(isValidEmail('plainaddress'), false);
  assert.strictEqual(isValidEmail('@no-user.com'), false);
  assert.strictEqual(isValidEmail('user@.com'), false);
  assert.strictEqual(isValidEmail('user@domain'), false);
  assert.strictEqual(isValidEmail(''), false);
  assert.strictEqual(isValidEmail(null), false);
});

// 2. Mobile Number Unit Tests
console.log('\n[2] Mobile Number Validation Tests');
test('Valid mobile numbers should pass', () => {
  assert.strictEqual(isValidMobile('9876543210'), true);
  assert.strictEqual(isValidMobile('+919876543210'), true);
  assert.strictEqual(isValidMobile('8123456789'), true);
  assert.strictEqual(isValidMobile('7000000000'), true);
});

test('Invalid mobile numbers should fail', () => {
  assert.strictEqual(isValidMobile('12345'), false); // Too short
  assert.strictEqual(isValidMobile('1234567890'), false); // Doesn't start with 6-9
  assert.strictEqual(isValidMobile('abcdefghij'), false); // Non-digit
  assert.strictEqual(isValidMobile(''), false);
  assert.strictEqual(isValidMobile(null), false);
});

// 3. Password Criteria Unit Tests
console.log('\n[3] Password Criteria Validation Tests');
test('Valid strong passwords should pass', () => {
  assert.strictEqual(isValidPassword('P@ssword123'), true);
  assert.strictEqual(isValidPassword('Secure#2026'), true);
  assert.strictEqual(isValidPassword('Complex!Pass1'), true);
});

test('Weak passwords missing criteria should fail', () => {
  assert.strictEqual(isValidPassword('short1!'), false); // < 8 chars
  assert.strictEqual(isValidPassword('password123!'), false); // Missing uppercase
  assert.strictEqual(isValidPassword('PASSWORD123!'), false); // Missing lowercase
  assert.strictEqual(isValidPassword('PasswordNoNumber!'), false); // Missing number
  assert.strictEqual(isValidPassword('Password12345'), false); // Missing special char
  assert.strictEqual(isValidPassword(''), false);
});

// 4. Student Registration Validation Tests
console.log('\n[4] Student Registration Field Validation Tests');
test('Valid student registration payload should pass validation', () => {
  const validStudent = {
    fullName: 'Rahul Sharma',
    email: 'rahul.sharma@college.edu',
    mobileNumber: '9876543210',
    password: 'Password@123',
    role: 'student',
    studentId: 'STU2024001',
    department: 'Computer Science',
    graduationYear: 2026
  };
  const result = validateStudentRegistration(validStudent);
  assert.strictEqual(result.isValid, true);
  assert.deepStrictEqual(result.errors, {});
});

test('Student registration missing required fields should return structured error object', () => {
  const emptyStudent = {};
  const result = validateStudentRegistration(emptyStudent);
  assert.strictEqual(result.isValid, false);
  assert.strictEqual(result.errors.fullName, VALIDATION_MESSAGES.FULL_NAME_REQUIRED);
  assert.strictEqual(result.errors.email, VALIDATION_MESSAGES.EMAIL_REQUIRED);
  assert.strictEqual(result.errors.mobileNumber, VALIDATION_MESSAGES.MOBILE_REQUIRED);
  assert.strictEqual(result.errors.password, VALIDATION_MESSAGES.PASSWORD_REQUIRED);
  assert.strictEqual(result.errors.studentId, VALIDATION_MESSAGES.STUDENT_ID_REQUIRED);
  assert.strictEqual(result.errors.department, VALIDATION_MESSAGES.STUDENT_DEPT_REQUIRED);
  assert.strictEqual(result.errors.graduationYear, VALIDATION_MESSAGES.STUDENT_GRAD_YEAR_REQUIRED);
});

test('Student registration with invalid field formats should flag format error messages', () => {
  const invalidStudent = {
    fullName: '1', // Too short/invalid name
    email: 'invalid-email-format',
    mobileNumber: '12345',
    password: 'weak',
    studentId: '$$$',
    department: 'Computer Science',
    graduationYear: 1990 // Past year invalid for student
  };
  const result = validateStudentRegistration(invalidStudent);
  assert.strictEqual(result.isValid, false);
  assert.strictEqual(result.errors.fullName, VALIDATION_MESSAGES.FULL_NAME_INVALID);
  assert.strictEqual(result.errors.email, VALIDATION_MESSAGES.EMAIL_INVALID);
  assert.strictEqual(result.errors.mobileNumber, VALIDATION_MESSAGES.MOBILE_INVALID);
  assert.strictEqual(result.errors.password, VALIDATION_MESSAGES.PASSWORD_INVALID);
  assert.strictEqual(result.errors.studentId, VALIDATION_MESSAGES.STUDENT_ID_INVALID);
  assert.strictEqual(result.errors.graduationYear, VALIDATION_MESSAGES.STUDENT_GRAD_YEAR_INVALID);
});

// 5. Alumni Registration Validation Tests
console.log('\n[5] Alumni Registration Field Validation Tests');
test('Valid alumni registration payload should pass validation', () => {
  const validAlumni = {
    fullName: 'Priya Patel',
    email: 'priya.patel@alumni.org',
    mobileNumber: '9123456789',
    password: 'Alumni@Password2024',
    role: 'alumni',
    graduationYear: 2018,
    department: 'Information Technology',
    company: 'Google',
    designation: 'Senior Software Engineer',
    linkedInProfile: 'https://www.linkedin.com/in/priyapatel'
  };
  const result = validateAlumniRegistration(validAlumni);
  assert.strictEqual(result.isValid, true);
  assert.deepStrictEqual(result.errors, {});
});

test('Alumni registration missing required fields should return structured error object', () => {
  const emptyAlumni = {};
  const result = validateAlumniRegistration(emptyAlumni);
  assert.strictEqual(result.isValid, false);
  assert.strictEqual(result.errors.fullName, VALIDATION_MESSAGES.FULL_NAME_REQUIRED);
  assert.strictEqual(result.errors.email, VALIDATION_MESSAGES.EMAIL_REQUIRED);
  assert.strictEqual(result.errors.mobileNumber, VALIDATION_MESSAGES.MOBILE_REQUIRED);
  assert.strictEqual(result.errors.password, VALIDATION_MESSAGES.PASSWORD_REQUIRED);
  assert.strictEqual(result.errors.graduationYear, VALIDATION_MESSAGES.ALUMNI_GRAD_YEAR_REQUIRED);
  assert.strictEqual(result.errors.department, VALIDATION_MESSAGES.ALUMNI_DEPT_REQUIRED);
  assert.strictEqual(result.errors.company, VALIDATION_MESSAGES.ALUMNI_COMPANY_REQUIRED);
  assert.strictEqual(result.errors.designation, VALIDATION_MESSAGES.ALUMNI_DESIGNATION_REQUIRED);
});

test('Alumni registration with future graduation year or invalid linkedin should fail', () => {
  const invalidAlumni = {
    fullName: 'Anil Kumar',
    email: 'anil@domain.com',
    mobileNumber: '9876543210',
    password: 'Valid123!Pass',
    role: 'alumni',
    graduationYear: 2030, // Future year invalid for alumni
    department: 'Mechanical Engineering',
    company: 'Tesla',
    designation: 'Engineer',
    linkedInProfile: 'not-a-linkedin-url'
  };
  const result = validateAlumniRegistration(invalidAlumni);
  assert.strictEqual(result.isValid, false);
  assert.strictEqual(result.errors.graduationYear, VALIDATION_MESSAGES.ALUMNI_GRAD_YEAR_INVALID);
  assert.strictEqual(result.errors.linkedInProfile, VALIDATION_MESSAGES.LINKEDIN_INVALID);
});

// 6. HTTP API Endpoint Integration Tests
console.log('\n[6] HTTP API Endpoint Server Integration Tests');

async function runHttpTests() {
  const server = app.listen(0);
  const port = server.address().port;
  const baseUrl = `http://localhost:${port}`;

  async function postJson(path, payload) {
    const res = await fetch(`${baseUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const json = await res.json();
    return { status: res.status, body: json };
  }

  await asyncTest('POST /api/auth/register/student with valid data returns HTTP 201', async () => {
    const response = await postJson('/api/auth/register/student', {
      fullName: 'Aarav Gupta',
      email: 'aarav@college.edu',
      mobileNumber: '9876543210',
      password: 'Student!Pass2026',
      studentId: 'STU1001',
      department: 'ECE',
      graduationYear: 2027
    });
    assert.strictEqual(response.status, 201);
    assert.strictEqual(response.body.success, true);
    assert.strictEqual(response.body.data.email, 'aarav@college.edu');
  });

  await asyncTest('POST /api/auth/register/student with invalid data returns HTTP 400 Bad Request', async () => {
    const response = await postJson('/api/auth/register/student', {
      fullName: 'A',
      email: 'bad-email',
      mobileNumber: '123',
      password: 'pass',
      studentId: '',
      department: '',
      graduationYear: 1980
    });
    assert.strictEqual(response.status, 400);
    assert.strictEqual(response.body.success, false);
    assert.ok(response.body.errors.email);
    assert.ok(response.body.errors.mobileNumber);
    assert.ok(response.body.errors.password);
    assert.ok(response.body.errors.studentId);
  });

  await asyncTest('POST /api/auth/register/alumni with valid data returns HTTP 201', async () => {
    const response = await postJson('/api/auth/register/alumni', {
      fullName: 'Siddharth Varma',
      email: 'sid@alumni.org',
      mobileNumber: '9988776655',
      password: 'Alumni#Secure2024',
      graduationYear: 2020,
      department: 'CSE',
      company: 'Microsoft',
      designation: 'Staff Engineer'
    });
    assert.strictEqual(response.status, 201);
    assert.strictEqual(response.body.success, true);
    assert.strictEqual(response.body.data.company, 'Microsoft');
  });

  await asyncTest('POST /api/auth/register/alumni with invalid data returns HTTP 400 Bad Request', async () => {
    const response = await postJson('/api/auth/register/alumni', {
      fullName: '',
      email: 'invalid-email',
      mobileNumber: '9988776655',
      password: 'Alumni#Secure2024',
      graduationYear: 2040,
      department: '',
      company: '',
      designation: ''
    });
    assert.strictEqual(response.status, 400);
    assert.strictEqual(response.body.success, false);
    assert.ok(response.body.errors.fullName);
    assert.ok(response.body.errors.email);
    assert.ok(response.body.errors.graduationYear);
    assert.ok(response.body.errors.company);
  });

  await asyncTest('POST /api/auth/register (unified) routes correctly based on role', async () => {
    const studentRes = await postJson('/api/auth/register', {
      role: 'student',
      fullName: 'Meera Nair',
      email: 'meera@student.edu',
      mobileNumber: '9112233445',
      password: 'Student#Password1',
      studentId: 'STU2025',
      department: 'Biotech',
      graduationYear: 2026
    });
    assert.strictEqual(studentRes.status, 201);
    assert.strictEqual(studentRes.body.data.role, 'student');
  });

  server.close();

  console.log('\n====================================================');
  console.log(`  RESULTS: ${passedTests} passed, ${failedTests} failed`);
  console.log('====================================================\n');

  if (failedTests > 0) {
    process.exit(1);
  }
}

runHttpTests().catch(err => {
  console.error('Fatal Test Runner Error:', err);
  process.exit(1);
});
