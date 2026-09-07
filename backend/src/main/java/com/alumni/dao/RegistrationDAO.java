package com.alumni.dao;

import com.alumni.config.DBConnection;
import com.alumni.model.Alumni;
import com.alumni.model.Student;
import com.alumni.model.User;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.HexFormat;

/**
 * RegistrationDAO
 *
 * Implements transactional JDBC storage operations for Student and Alumni registrations
 * in local MySQL.
 */
public class RegistrationDAO {

    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return "sha256$" + HexFormat.of().formatHex(salt) + "$" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "plain$" + password;
        }
    }

    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) return false;
        if (storedHash.startsWith("sha256$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length == 3) {
                try {
                    byte[] salt = HexFormat.of().parseHex(parts[1]);
                    String expectedHashHex = parts[2];
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(salt);
                    byte[] hash = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                    String computedHashHex = HexFormat.of().formatHex(hash);
                    return MessageDigest.isEqual(computedHashHex.getBytes(StandardCharsets.UTF_8),
                                                 expectedHashHex.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    public long registerStudent(User user, Student student) throws SQLException {
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new SQLException("User password hash cannot be empty.");
        }
        String insertUserSql = "INSERT INTO users (full_name, email, mobile_number, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        String insertStudentSql = "INSERT INTO students (user_id, student_id, department, graduation_year) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            long generatedUserId;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, user.getFullName());
                userStmt.setString(2, user.getEmail());
                userStmt.setString(3, user.getMobileNumber());
                userStmt.setString(4, user.getPasswordHash());
                userStmt.setString(5, "student");

                int affected = userStmt.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedUserId = generatedKeys.getLong(1);
                        user.setId(generatedUserId);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement studentStmt = conn.prepareStatement(insertStudentSql)) {
                studentStmt.setLong(1, generatedUserId);
                studentStmt.setString(2, student.getStudentId());
                studentStmt.setString(3, student.getDepartment());
                studentStmt.setInt(4, student.getGraduationYear());

                studentStmt.executeUpdate();
            }

            conn.commit();
            student.setUserId(generatedUserId);
            return generatedUserId;

        } catch (SQLException e) {
            System.err.println("❌ Transaction failed in registerStudent: " + e.getMessage());
            throw e;
        }
    }

    public long registerAlumni(User user, Alumni alumni) throws SQLException {
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new SQLException("User password hash cannot be empty.");
        }
        String insertUserSql = "INSERT INTO users (full_name, email, mobile_number, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        String insertAlumniSql = "INSERT INTO alumni (user_id, department, graduation_year, company, designation, linkedin_profile, experience_years, industry, skills, bio, max_mentees) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            long generatedUserId;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, user.getFullName());
                userStmt.setString(2, user.getEmail());
                userStmt.setString(3, user.getMobileNumber());
                userStmt.setString(4, user.getPasswordHash());
                userStmt.setString(5, "alumni");

                int affected = userStmt.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedUserId = generatedKeys.getLong(1);
                        user.setId(generatedUserId);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement alumniStmt = conn.prepareStatement(insertAlumniSql)) {
                alumniStmt.setLong(1, generatedUserId);
                alumniStmt.setString(2, alumni.getDepartment());
                alumniStmt.setInt(3, alumni.getGraduationYear());
                alumniStmt.setString(4, alumni.getCompany());
                alumniStmt.setString(5, alumni.getDesignation());
                alumniStmt.setString(6, alumni.getLinkedInProfile());

                if (alumni.getExperienceYears() != null) {
                    alumniStmt.setInt(7, alumni.getExperienceYears());
                } else {
                    alumniStmt.setNull(7, Types.INTEGER);
                }

                alumniStmt.setString(8, alumni.getIndustry());
                alumniStmt.setString(9, alumni.getSkills());
                alumniStmt.setString(10, alumni.getBio());

                if (alumni.getMaxMentees() != null) {
                    alumniStmt.setInt(11, alumni.getMaxMentees());
                } else {
                    alumniStmt.setNull(11, Types.INTEGER);
                }

                alumniStmt.executeUpdate();
            }

            conn.commit();
            alumni.setUserId(generatedUserId);
            return generatedUserId;

        } catch (SQLException e) {
            System.err.println("❌ Transaction failed in registerAlumni: " + e.getMessage());
            throw e;
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(1) FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existsByMobileNumber(String mobileNumber) throws SQLException {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) return false;
        String digits = mobileNumber.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return false;
        String last10 = digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
        String sql = "SELECT COUNT(1) FROM users WHERE RIGHT(REPLACE(REPLACE(REPLACE(mobile_number, '+', ''), ' ', ''), '-', ''), 10) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, last10);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Retrieves safe registration profile without password for GET /api/auth/registrations/:email
     */
    public JSONObject findRegistrationByEmail(String email) throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.email, u.mobile_number, u.role, " +
                "       s.student_id, COALESCE(s.department, a.department) AS department, " +
                "       COALESCE(s.graduation_year, a.graduation_year) AS graduation_year, " +
                "       a.company, a.designation, a.linkedin_profile, a.experience_years, " +
                "       a.industry, a.skills, a.bio, a.max_mentees " +
                "  FROM users u " +
                "  LEFT JOIN students s ON s.user_id = u.id " +
                "  LEFT JOIN alumni a ON a.user_id = u.id " +
                " WHERE u.email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getLong("id"));
                    obj.put("fullName", rs.getString("full_name"));
                    obj.put("email", rs.getString("email"));
                    obj.put("mobileNumber", rs.getString("mobile_number"));
                    obj.put("role", rs.getString("role"));

                    String role = rs.getString("role");
                    if ("student".equalsIgnoreCase(role)) {
                        obj.put("studentId", rs.getString("student_id"));
                        obj.put("department", rs.getString("department"));
                        obj.put("graduationYear", rs.getInt("graduation_year"));
                    } else {
                        obj.put("department", rs.getString("department"));
                        obj.put("graduationYear", rs.getInt("graduation_year"));
                        obj.put("company", rs.getString("company"));
                        obj.put("designation", rs.getString("designation"));
                        obj.put("linkedInProfile", rs.getString("linkedin_profile"));
                        int exp = rs.getInt("experience_years");
                        obj.put("experienceYears", rs.wasNull() ? null : exp);
                        obj.put("industry", rs.getString("industry"));
                        obj.put("skills", rs.getString("skills"));
                        obj.put("bio", rs.getString("bio"));
                        int max = rs.getInt("max_mentees");
                        obj.put("maxMentees", rs.wasNull() ? null : max);
                    }
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Authenticates user by email and password, returning user profile object on success.
     */
    public JSONObject authenticateUser(String email, String password) throws SQLException {
        if (email == null || password == null) return null;

        String sql = "SELECT u.id, u.full_name, u.email, u.mobile_number, u.password_hash, u.role, " +
                "       s.student_id, COALESCE(s.department, a.department) AS department, " +
                "       COALESCE(s.graduation_year, a.graduation_year) AS graduation_year, " +
                "       a.company, a.designation, a.linkedin_profile " +
                "  FROM users u " +
                "  LEFT JOIN students s ON s.user_id = u.id " +
                "  LEFT JOIN alumni a ON a.user_id = u.id " +
                " WHERE LOWER(u.email) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (verifyPassword(password, storedHash)) {
                        JSONObject user = new JSONObject();
                        user.put("id", rs.getLong("id"));
                        user.put("fullName", rs.getString("full_name"));
                        user.put("email", rs.getString("email"));
                        user.put("mobileNumber", rs.getString("mobile_number"));
                        user.put("role", rs.getString("role"));
                        user.put("department", rs.getString("department"));
                        user.put("graduationYear", rs.getInt("graduation_year"));

                        if ("student".equalsIgnoreCase(rs.getString("role"))) {
                            user.put("studentId", rs.getString("student_id"));
                        } else {
                            user.put("company", rs.getString("company"));
                            user.put("designation", rs.getString("designation"));
                            user.put("linkedinProfile", rs.getString("linkedin_profile"));
                        }
                        return user;
                    }
                }
            }
        }
        return null;
    }
}
