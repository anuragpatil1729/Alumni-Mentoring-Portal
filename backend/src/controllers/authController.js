/**
 * Authentication Controller
 * Handles Student and Alumni registration endpoints.
 */

const {
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput
} = require('../utils/validation');

/**
 * Controller to handle Student Registration
 */
function registerStudent(req, res) {
  const validation = validateStudentRegistration(req.body);
  if (!validation.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Student registration validation failed',
      errors: validation.errors
    });
  }

  // Payload is valid
  const studentData = {
    fullName: req.body.fullName || req.body.name,
    email: req.body.email,
    mobileNumber: req.body.mobileNumber || req.body.phone,
    role: 'student',
    studentId: req.body.studentId || req.body.rollNumber,
    department: req.body.department || req.body.branch,
    graduationYear: Number(req.body.graduationYear || req.body.passoutYear)
  };

  return res.status(201).json({
    success: true,
    message: 'Student registered successfully',
    data: {
      fullName: studentData.fullName,
      email: studentData.email,
      mobileNumber: studentData.mobileNumber,
      role: studentData.role,
      studentId: studentData.studentId,
      department: studentData.department,
      graduationYear: studentData.graduationYear
    }
  });
}

/**
 * Controller to handle Alumni Registration
 */
function registerAlumni(req, res) {
  const validation = validateAlumniRegistration(req.body);
  if (!validation.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Alumni registration validation failed',
      errors: validation.errors
    });
  }

  // Payload is valid
  const alumniData = {
    fullName: req.body.fullName || req.body.name,
    email: req.body.email,
    mobileNumber: req.body.mobileNumber || req.body.phone,
    role: 'alumni',
    graduationYear: Number(req.body.graduationYear || req.body.passoutYear),
    department: req.body.department || req.body.branch,
    company: req.body.company || req.body.currentCompany,
    designation: req.body.designation || req.body.jobTitle,
    linkedInProfile: req.body.linkedInProfile || req.body.linkedin || null
  };

  return res.status(201).json({
    success: true,
    message: 'Alumni registered successfully',
    data: alumniData
  });
}

/**
 * Controller to handle Unified User Registration (by role)
 */
function registerUser(req, res) {
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

module.exports = {
  registerStudent,
  registerAlumni,
  registerUser
};
