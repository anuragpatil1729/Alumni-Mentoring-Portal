/**
 * Authentication Middleware for Input Validation
 */

const {
  validateStudentRegistration,
  validateAlumniRegistration,
  validateRegistrationInput
} = require('../utils/validation');

/**
 * Middleware to manually validate Student Registration payload.
 */
function validateStudentRegistrationMiddleware(req, res, next) {
  const result = validateStudentRegistration(req.body);
  if (!result.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Student registration validation failed',
      errors: result.errors
    });
  }
  next();
}

/**
 * Middleware to manually validate Alumni Registration payload.
 */
function validateAlumniRegistrationMiddleware(req, res, next) {
  const result = validateAlumniRegistration(req.body);
  if (!result.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Alumni registration validation failed',
      errors: result.errors
    });
  }
  next();
}

/**
 * Middleware to manually validate unified Registration payload based on role field.
 */
function validateRegistrationMiddleware(req, res, next) {
  const result = validateRegistrationInput(req.body);
  if (!result.isValid) {
    return res.status(400).json({
      success: false,
      message: 'Registration validation failed',
      errors: result.errors
    });
  }
  next();
}

module.exports = {
  validateStudentRegistrationMiddleware,
  validateAlumniRegistrationMiddleware,
  validateRegistrationMiddleware
};
