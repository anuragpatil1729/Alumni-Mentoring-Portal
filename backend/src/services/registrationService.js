const { scrypt, randomBytes } = require('crypto');
const { promisify } = require('util');
const {
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput
} = require('../utils/validation');
const { createRegistration, findRegistrationByEmail } = require('../models/registrationRepository');

const scryptAsync = promisify(scrypt);

async function hashPassword(password) {
  const salt = randomBytes(16).toString('hex');
  const derivedKey = await scryptAsync(password, salt, 64);
  return `scrypt$${salt}$${derivedKey.toString('hex')}`;
}

function normalizeStudent(data) {
  return {
    fullName: (data.fullName || data.name).trim(),
    email: data.email.trim().toLowerCase(),
    mobileNumber: (data.mobileNumber || data.phone).trim(),
    role: 'student',
    studentId: (data.studentId || data.rollNumber).trim(),
    department: (data.department || data.branch).trim(),
    graduationYear: Number(data.graduationYear || data.passoutYear)
  };
}

function normalizeAlumni(data) {
  return {
    fullName: (data.fullName || data.name).trim(),
    email: data.email.trim().toLowerCase(),
    mobileNumber: (data.mobileNumber || data.phone).trim(),
    role: 'alumni',
    graduationYear: Number(data.graduationYear || data.passoutYear),
    department: (data.department || data.branch).trim(),
    company: (data.company || data.currentCompany).trim(),
    designation: (data.designation || data.jobTitle).trim(),
    linkedInProfile: data.linkedInProfile || data.linkedin || null,
    experienceYears: data.experienceYears !== undefined && data.experienceYears !== '' ? Number(data.experienceYears) : (data.experience ? Number(data.experience) : null),
    industry: data.industry ? String(data.industry).trim() : null,
    skills: data.skills ? String(data.skills).trim() : null,
    bio: data.bio ? String(data.bio).trim() : null,
    maxMentees: data.maxMentees !== undefined && data.maxMentees !== '' ? Number(data.maxMentees) : (data.mentees ? Number(data.mentees) : null)
  };
}

function buildValidationError(message, errors) {
  return { status: 400, body: { success: false, message, errors } };
}

async function saveRegistration(registration, password) {
  try {
    const saved = await createRegistration({ ...registration, passwordHash: await hashPassword(password) });
    delete saved.passwordHash;
    return {
      status: 201,
      body: {
        success: true,
        message: `${registration.role === 'student' ? 'Student' : 'Alumni'} registered successfully`,
        data: saved
      }
    };
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return { status: 409, body: { success: false, message: 'An account with this email or student ID already exists.' } };
    }
    return { status: 503, body: { success: false, message: 'Registration could not be saved. Please try again later.' } };
  }
}

async function processStudentRegistration(payload) {
  const validation = validateStudentRegistration(payload);
  if (!validation.isValid) return buildValidationError('Student registration validation failed', validation.errors);
  return saveRegistration(normalizeStudent(payload), payload.password);
}

async function processAlumniRegistration(payload) {
  const validation = validateAlumniRegistration(payload);
  if (!validation.isValid) return buildValidationError('Alumni registration validation failed', validation.errors);
  return saveRegistration(normalizeAlumni(payload), payload.password);
}

async function processRegistration(payload) {
  const validation = validateRegistrationInput(payload);
  if (!validation.isValid) return buildValidationError('Registration validation failed', validation.errors);

  const role = payload.role.toLowerCase();
  if (role === 'student') return processStudentRegistration(payload);
  if (role === 'alumni') return processAlumniRegistration(payload);
  return buildValidationError('Registration validation failed', { role: 'Role must be either student or alumni.' });
}

async function getRegistration(email) {
  try {
    const registration = await findRegistrationByEmail(email.trim().toLowerCase());
    if (!registration) return { status: 404, body: { success: false, message: 'Registration not found.' } };
    return { status: 200, body: { success: true, data: registration } };
  } catch (error) {
    return { status: 503, body: { success: false, message: 'Registration data is currently unavailable.' } };
  }
}

module.exports = {
  processRegistration,
  processStudentRegistration,
  processAlumniRegistration,
  getRegistration,
  normalizeStudent,
  normalizeAlumni
};
