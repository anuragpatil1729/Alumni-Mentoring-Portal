/**
 * Authentication Controller
 * Handles Student and Alumni registration endpoints.
 */

const {
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput
} = require('../utils/validation');
const { createRegistration, findRegistrationByEmail } = require('../models/registrationRepository');
const { scrypt, randomBytes } = require('crypto');
const { promisify } = require('util');

const scryptAsync = promisify(scrypt);

async function hashPassword(password) {
  const salt = randomBytes(16).toString('hex');
  const derivedKey = await scryptAsync(password, salt, 64);
  return `scrypt$${salt}$${derivedKey.toString('hex')}`;
}

function normalizeStudent(data) {
  return {
    fullName: (data.fullName || data.name).trim(), email: data.email.trim().toLowerCase(),
    mobileNumber: (data.mobileNumber || data.phone).trim(), role: 'student',
    studentId: (data.studentId || data.rollNumber).trim(), department: (data.department || data.branch).trim(),
    graduationYear: Number(data.graduationYear || data.passoutYear)
  };
}

function normalizeAlumni(data) {
  return {
    fullName: (data.fullName || data.name).trim(), email: data.email.trim().toLowerCase(),
    mobileNumber: (data.mobileNumber || data.phone).trim(), role: 'alumni',
    graduationYear: Number(data.graduationYear || data.passoutYear), department: (data.department || data.branch).trim(),
    company: (data.company || data.currentCompany).trim(), designation: (data.designation || data.jobTitle).trim(),
    linkedInProfile: data.linkedInProfile || data.linkedin || null
  };
}

async function persistRegistration(res, registration, password) {
  try {
    const saved = await createRegistration({ ...registration, passwordHash: await hashPassword(password) });
    delete saved.passwordHash;
    return res.status(201).json({ success: true, message: `${registration.role === 'student' ? 'Student' : 'Alumni'} registered successfully`, data: saved });
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ success: false, message: 'An account with this email or student ID already exists.' });
    }
    return res.status(503).json({ success: false, message: 'Registration could not be saved. Please try again later.' });
  }
}

/**
 * Controller to handle Student Registration
 */
async function registerStudent(req, res) {
  const validation = validateStudentRegistration(req.body);
  if (!validation.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Student registration validation failed',
      errors: validation.errors
    });
  }

  // Payload is valid
  return persistRegistration(res, normalizeStudent(req.body), req.body.password);
}

/**
 * Controller to handle Alumni Registration
 */
async function registerAlumni(req, res) {
  const validation = validateAlumniRegistration(req.body);
  if (!validation.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Alumni registration validation failed',
      errors: validation.errors
    });
  }

  // Payload is valid
  return persistRegistration(res, normalizeAlumni(req.body), req.body.password);
}

/**
 * Controller to handle Unified User Registration (by role)
 */
async function registerUser(req, res) {
  const validation = validateRegistrationInput(req.body);
  if (!validation.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Registration validation failed',
      errors: validation.errors
    });
  }

  const role = req.body.role.toLowerCase();
  if (role === 'student') {
    return registerStudent(req, res);
  } else if (role === 'alumni') {
    return registerAlumni(req, res);
  }
}

async function getRegistrationByEmail(req, res) {
  try {
    const registration = await findRegistrationByEmail(req.params.email.trim().toLowerCase());
    if (!registration) return res.status(404).json({ success: false, message: 'Registration not found.' });
    return res.status(200).json({ success: true, data: registration });
  } catch (error) {
    return res.status(503).json({ success: false, message: 'Registration data is currently unavailable.' });
  }
}

module.exports = {
  registerStudent,
  registerAlumni,
  registerUser,
  getRegistrationByEmail
};
