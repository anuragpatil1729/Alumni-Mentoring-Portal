/**
 * Authentication Controller
 * Handles Student and Alumni registration endpoints.
 */

const {
  processRegistration,
  processStudentRegistration,
  processAlumniRegistration,
  getRegistration
} = require('../services/registrationService');

async function sendProcessedRegistration(res, resultPromise) {
  const result = await resultPromise;
  return res.status(result.status).json(result.body);
}

async function registerStudent(req, res) {
  return sendProcessedRegistration(res, processStudentRegistration(req.body));
}

async function registerAlumni(req, res) {
  return sendProcessedRegistration(res, processAlumniRegistration(req.body));
}

async function registerUser(req, res) {
  return sendProcessedRegistration(res, processRegistration(req.body));
}

async function getRegistrationByEmail(req, res) {
  return sendProcessedRegistration(res, getRegistration(req.params.email));
}

module.exports = {
  registerStudent,
  registerAlumni,
  registerUser,
  getRegistrationByEmail
};
