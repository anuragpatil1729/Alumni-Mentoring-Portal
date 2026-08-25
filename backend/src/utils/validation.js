/**
 * Server-Side Manual Input Validation Utilities
 * Validates registration fields for Student and Alumni users.
 */

const { ROLES, VALIDATION_MESSAGES, REGEX_PATTERNS } = require('./constants');

/**
 * Checks if a value is empty (null, undefined, non-string/non-number, or whitespace string).
 * @param {*} value 
 * @returns {boolean}
 */
function isEmpty(value) {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  if (typeof value === 'number') return isNaN(value);
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value).length === 0;
  return false;
}

/**
 * Sanitizes input string by trimming whitespace.
 * @param {*} value 
 * @returns {string}
 */
function sanitizeString(value) {
  return typeof value === 'string' ? value.trim() : '';
}

/**
 * Validates email format.
 * @param {string} email 
 * @returns {boolean}
 */
function isValidEmail(email) {
  if (isEmpty(email)) return false;
  const sanitized = sanitizeString(email);
  if (sanitized.length > 254) return false;
  return REGEX_PATTERNS.EMAIL.test(sanitized);
}

/**
 * Validates mobile number format.
 * @param {string} mobile 
 * @returns {boolean}
 */
function isValidMobile(mobile) {
  if (isEmpty(mobile)) return false;
  const sanitized = sanitizeString(mobile);
  // Strip spaces, dashes, or parentheses before regex checking
  const cleaned = sanitized.replace(/[\s()-]/g, '');
  return REGEX_PATTERNS.MOBILE.test(cleaned);
}

/**
 * Validates password strength & format criteria.
 * Criteria: 8-128 chars, 1 uppercase, 1 lowercase, 1 number, 1 special character.
 * @param {string} password 
 * @returns {boolean}
 */
function isValidPassword(password) {
  if (typeof password !== 'string') return false;
  return REGEX_PATTERNS.PASSWORD.test(password);
}

/**
 * Validates full name.
 * @param {string} fullName 
 * @returns {boolean}
 */
function isValidFullName(fullName) {
  if (isEmpty(fullName)) return false;
  const sanitized = sanitizeString(fullName);
  return REGEX_PATTERNS.FULL_NAME.test(sanitized);
}

/**
 * Validates Student ID / Roll Number format.
 * @param {string} studentId 
 * @returns {boolean}
 */
function isValidStudentId(studentId) {
  if (isEmpty(studentId)) return false;
  const sanitized = sanitizeString(studentId);
  return REGEX_PATTERNS.STUDENT_ID.test(sanitized);
}

/**
 * Validates graduation year for students (current or near-future year).
 * @param {number|string} year 
 * @returns {boolean}
 */
function isValidStudentGradYear(year) {
  if (isEmpty(year)) return false;
  const parsedYear = Number(year);
  if (!Number.isInteger(parsedYear)) return false;
  const currentYear = new Date().getFullYear();
  // Valid student graduation year: current year - 1 up to 10 years into the future
  return parsedYear >= currentYear - 1 && parsedYear <= currentYear + 10;
}

/**
 * Validates graduation year for alumni (past or current year).
 * @param {number|string} year 
 * @returns {boolean}
 */
function isValidAlumniGradYear(year) {
  if (isEmpty(year)) return false;
  const parsedYear = Number(year);
  if (!Number.isInteger(parsedYear)) return false;
  const currentYear = new Date().getFullYear();
  // Valid alumni graduation year: 1950 to current year
  return parsedYear >= 1950 && parsedYear <= currentYear;
}

/**
 * Validates LinkedIn profile URL if provided.
 * @param {string} url 
 * @returns {boolean}
 */
function isValidLinkedInUrl(url) {
  if (isEmpty(url)) return true; // Optional field
  const sanitized = sanitizeString(url);
  return REGEX_PATTERNS.LINKEDIN_URL.test(sanitized);
}

/**
 * Validates Common Registration Fields (fullName, email, mobileNumber, password, role).
 * @param {object} data 
 * @returns {object} errors object containing key-value pairs of field error messages
 */
function validateCommonFields(data) {
  const errors = {};

  // Full Name Validation
  const fullName = data.fullName || data.name;
  if (isEmpty(fullName)) {
    errors.fullName = VALIDATION_MESSAGES.FULL_NAME_REQUIRED;
  } else if (!isValidFullName(fullName)) {
    errors.fullName = VALIDATION_MESSAGES.FULL_NAME_INVALID;
  }

  // Email Validation
  const email = data.email;
  if (isEmpty(email)) {
    errors.email = VALIDATION_MESSAGES.EMAIL_REQUIRED;
  } else if (!isValidEmail(email)) {
    errors.email = VALIDATION_MESSAGES.EMAIL_INVALID;
  }

  // Mobile Number Validation
  const mobileNumber = data.mobileNumber || data.phone || data.mobile;
  if (isEmpty(mobileNumber)) {
    errors.mobileNumber = VALIDATION_MESSAGES.MOBILE_REQUIRED;
  } else if (!isValidMobile(mobileNumber)) {
    errors.mobileNumber = VALIDATION_MESSAGES.MOBILE_INVALID;
  }

  // Password Validation
  const password = data.password;
  if (isEmpty(password)) {
    errors.password = VALIDATION_MESSAGES.PASSWORD_REQUIRED;
  } else if (!isValidPassword(password)) {
    errors.password = VALIDATION_MESSAGES.PASSWORD_INVALID;
  }

  return errors;
}

/**
 * Server-Side Validation for Student Registration Fields
 * @param {object} data 
 * @returns {{ isValid: boolean, errors: object }}
 */
function validateStudentRegistration(data = {}) {
  const errors = validateCommonFields(data);

  // Student ID / Roll Number
  const studentId = data.studentId || data.rollNumber;
  if (isEmpty(studentId)) {
    errors.studentId = VALIDATION_MESSAGES.STUDENT_ID_REQUIRED;
  } else if (!isValidStudentId(studentId)) {
    errors.studentId = VALIDATION_MESSAGES.STUDENT_ID_INVALID;
  }

  // Department / Branch
  const department = data.department || data.branch;
  if (isEmpty(department)) {
    errors.department = VALIDATION_MESSAGES.STUDENT_DEPT_REQUIRED;
  }

  // Graduation Year
  const graduationYear = data.graduationYear || data.passoutYear;
  if (isEmpty(graduationYear)) {
    errors.graduationYear = VALIDATION_MESSAGES.STUDENT_GRAD_YEAR_REQUIRED;
  } else if (!isValidStudentGradYear(graduationYear)) {
    errors.graduationYear = VALIDATION_MESSAGES.STUDENT_GRAD_YEAR_INVALID;
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Server-Side Validation for Alumni Registration Fields
 * @param {object} data 
 * @returns {{ isValid: boolean, errors: object }}
 */
function validateAlumniRegistration(data = {}) {
  const errors = validateCommonFields(data);

  // Graduation Year
  const graduationYear = data.graduationYear || data.passoutYear;
  if (isEmpty(graduationYear)) {
    errors.graduationYear = VALIDATION_MESSAGES.ALUMNI_GRAD_YEAR_REQUIRED;
  } else if (!isValidAlumniGradYear(graduationYear)) {
    errors.graduationYear = VALIDATION_MESSAGES.ALUMNI_GRAD_YEAR_INVALID;
  }

  // Department
  const department = data.department || data.branch;
  if (isEmpty(department)) {
    errors.department = VALIDATION_MESSAGES.ALUMNI_DEPT_REQUIRED;
  }

  // Company
  const company = data.company || data.currentCompany;
  if (isEmpty(company)) {
    errors.company = VALIDATION_MESSAGES.ALUMNI_COMPANY_REQUIRED;
  }

  // Designation
  const designation = data.designation || data.jobTitle;
  if (isEmpty(designation)) {
    errors.designation = VALIDATION_MESSAGES.ALUMNI_DESIGNATION_REQUIRED;
  }

  // LinkedIn Profile (Optional)
  const linkedInProfile = data.linkedInProfile || data.linkedin;
  if (!isEmpty(linkedInProfile) && !isValidLinkedInUrl(linkedInProfile)) {
    errors.linkedInProfile = VALIDATION_MESSAGES.LINKEDIN_INVALID;
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Main Dispatcher for Registration Input Validation based on Role
 * @param {object} data 
 * @returns {{ isValid: boolean, errors: object }}
 */
function validateRegistrationInput(data = {}) {
  const role = sanitizeString(data.role).toLowerCase();

  if (isEmpty(role)) {
    return {
      isValid: false,
      errors: { role: VALIDATION_MESSAGES.ROLE_REQUIRED }
    };
  }

  if (role === ROLES.STUDENT) {
    return validateStudentRegistration(data);
  } else if (role === ROLES.ALUMNI) {
    return validateAlumniRegistration(data);
  } else {
    return {
      isValid: false,
      errors: { role: VALIDATION_MESSAGES.ROLE_INVALID }
    };
  }
}

module.exports = {
  isEmpty,
  sanitizeString,
  isValidEmail,
  isValidMobile,
  isValidPassword,
  isValidFullName,
  isValidStudentId,
  isValidStudentGradYear,
  isValidAlumniGradYear,
  isValidLinkedInUrl,
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput
};
