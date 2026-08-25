/**
 * Authentication Routes
 */

const express = require('express');
const router = express.Router();

const {
  registerStudent,
  registerAlumni,
  registerUser
} = require('../controllers/authController');

const {
  validateStudentRegistrationMiddleware,
  validateAlumniRegistrationMiddleware,
  validateRegistrationMiddleware
} = require('../middleware/authMiddleware');

// Unified registration route (role in payload)
router.post('/register', validateRegistrationMiddleware, registerUser);

// Student registration route
router.post('/register/student', validateStudentRegistrationMiddleware, registerStudent);

// Alumni registration route
router.post('/register/alumni', validateAlumniRegistrationMiddleware, registerAlumni);

module.exports = router;
