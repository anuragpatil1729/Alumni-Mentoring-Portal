const { getDatabasePool } = require('../config/database');

async function createRegistration(registration) {
  const connection = await getDatabasePool().getConnection();
  try {
    await connection.beginTransaction();
    const [userResult] = await connection.execute(
      `INSERT INTO users (full_name, email, mobile_number, password_hash, role)
       VALUES (?, ?, ?, ?, ?)`,
      [registration.fullName, registration.email, registration.mobileNumber, registration.passwordHash, registration.role]
    );

    if (registration.role === 'student') {
      await connection.execute(
        `INSERT INTO students (user_id, student_id, department, graduation_year)
         VALUES (?, ?, ?, ?)`,
        [userResult.insertId, registration.studentId, registration.department, registration.graduationYear]
      );
    } else {
      await connection.execute(
        `INSERT INTO alumni (user_id, department, graduation_year, company, designation, linkedin_profile, experience_years, industry, skills, bio, max_mentees)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          userResult.insertId,
          registration.department,
          registration.graduationYear,
          registration.company,
          registration.designation,
          registration.linkedInProfile,
          registration.experienceYears,
          registration.industry,
          registration.skills,
          registration.bio,
          registration.maxMentees
        ]
      );
    }
    await connection.commit();
    return { id: userResult.insertId, ...registration };
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

async function findRegistrationByEmail(email) {
  const [rows] = await getDatabasePool().execute(
    `SELECT u.id, u.full_name AS fullName, u.email, u.mobile_number AS mobileNumber, u.role,
            s.student_id AS studentId, COALESCE(s.department, a.department) AS department,
            COALESCE(s.graduation_year, a.graduation_year) AS graduationYear,
            a.company, a.designation, a.linkedin_profile AS linkedInProfile,
            a.experience_years AS experienceYears, a.industry, a.skills, a.bio, a.max_mentees AS maxMentees
       FROM users u
       LEFT JOIN students s ON s.user_id = u.id
       LEFT JOIN alumni a ON a.user_id = u.id
      WHERE u.email = ?`,
    [email]
  );
  return rows[0] || null;
}

module.exports = { createRegistration, findRegistrationByEmail };
