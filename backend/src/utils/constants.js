/**
 * Application Constants and Validation Rules
 */

const ROLES = {
  STUDENT: 'student',
  ALUMNI: 'alumni'
};

const VALIDATION_MESSAGES = {
  // Common Required Fields
  FULL_NAME_REQUIRED: 'Full name is required.',
  FULL_NAME_INVALID: 'Full name must contain only letters and spaces, and be between 2 and 50 characters.',
  EMAIL_REQUIRED: 'Email address is required.',
  EMAIL_INVALID: 'Please provide a valid email address.',
  MOBILE_REQUIRED: 'Mobile number is required.',
  MOBILE_INVALID: 'Mobile number must be a valid 10-digit number (e.g. 9876543210 or +919876543210).',
  PASSWORD_REQUIRED: 'Password is required.',
  PASSWORD_INVALID: 'Password must be 8-128 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character (!@#$%^&*()_+-=[]{};:\'",.<>?/).',
  ROLE_REQUIRED: 'Role is required.',
  ROLE_INVALID: 'Role must be either "student" or "alumni".',

  // Student Specific Fields
  STUDENT_ID_REQUIRED: 'Student ID / Roll Number is required.',
  STUDENT_ID_INVALID: 'Student ID must be between 3 and 20 alphanumeric characters.',
  STUDENT_DEPT_REQUIRED: 'Department is required for student registration.',
  STUDENT_GRAD_YEAR_REQUIRED: 'Graduation year is required for student registration.',
  STUDENT_GRAD_YEAR_INVALID: 'Graduation year must be a 4-digit year (current or future year, e.g. 2024-2035).',

  // Alumni Specific Fields
  ALUMNI_GRAD_YEAR_REQUIRED: 'Graduation year is required for alumni registration.',
  ALUMNI_GRAD_YEAR_INVALID: 'Graduation year must be a valid past or current 4-digit year (e.g. 1950-2026).',
  ALUMNI_DEPT_REQUIRED: 'Department is required for alumni registration.',
  ALUMNI_COMPANY_REQUIRED: 'Current company name is required for alumni registration.',
  ALUMNI_DESIGNATION_REQUIRED: 'Designation / job title is required for alumni registration.',
  LINKEDIN_INVALID: 'LinkedIn profile must be a valid URL (e.g. https://www.linkedin.com/in/username).'
};

// Regex patterns
const REGEX_PATTERNS = {
  // Allow letters, spaces, dots, and hyphens (min 2, max 50 chars)
  FULL_NAME: /^[a-zA-Z\s.-]{2,50}$/,
  // Standard email validation RFC 5322 regex
  EMAIL: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  // Mobile validation: supporting Indian 10-digit mobile numbers starting with 6-9, or with international prefix +91/etc.
  MOBILE: /^(?:\+?\d{1,3}[- ]?)?[6-9]\d{9}$/,
  // Password: At least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,128}$/,
  // Student ID / Roll Number: Alphanumeric with hyphens/slashes
  STUDENT_ID: /^[a-zA-Z0-9/-]{3,20}$/,
  // LinkedIn profile URL pattern
  LINKEDIN_URL: /^https?:\/\/(www\.)?linkedin\.com\/in\/[a-zA-Z0-9_-]+\/?$/
};

module.exports = {
  ROLES,
  VALIDATION_MESSAGES,
  REGEX_PATTERNS
};
