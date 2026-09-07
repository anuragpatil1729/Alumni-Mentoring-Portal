package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.AlumniDAO;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.model.Student;
import com.alumni.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class JDBCTest {

    private static final RegistrationDAO registrationDAO = new RegistrationDAO();
    private static final AlumniDAO alumniDAO = new AlumniDAO();

    @BeforeAll
    public static void setup() {
        Assertions.assertTrue(DBConnection.testConnection(), "MySQL JDBC connection should be successful");
    }

    @Test
    public void testDatabaseConnection() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Assertions.assertNotNull(conn);
            Assertions.assertFalse(conn.isClosed());
            Assertions.assertTrue(conn.isValid(2));
        }
    }

    @Test
    public void testQueryAlumni() throws SQLException {
        List<Alumni> list = alumniDAO.getAllAlumni();
        Assertions.assertNotNull(list);
        Assertions.assertFalse(list.isEmpty(), "Alumni table should contain records");

        Alumni first = list.get(0);
        Assertions.assertNotNull(first.getFullName());
        Assertions.assertNotNull(first.getCompany());
    }

    @Test
    public void testTransactionalStudentRegistration() throws SQLException {
        String testEmail = "test_student_" + System.currentTimeMillis() + "@example.com";

        User user = new User();
        user.setFullName("JDBC Test Student");
        user.setEmail(testEmail);
        user.setMobileNumber("9123456780");
        user.setPasswordHash("test_hash_123");

        Student student = new Student();
        student.setStudentId("STU_" + System.currentTimeMillis());
        student.setDepartment("Computer Engineering");
        student.setGraduationYear(2026);

        long userId = registrationDAO.registerStudent(user, student);
        Assertions.assertTrue(userId > 0, "Generated user ID should be positive");
        Assertions.assertTrue(registrationDAO.existsByEmail(testEmail));

        // Clean up test record
        cleanupUser(userId);
    }

    @Test
    public void testTransactionalAlumniRegistration() throws SQLException {
        String testEmail = "test_alumni_" + System.currentTimeMillis() + "@example.com";

        User user = new User();
        user.setFullName("JDBC Test Mentor");
        user.setEmail(testEmail);
        user.setMobileNumber("9876543210");
        user.setPasswordHash("test_hash_456");

        Alumni alumni = new Alumni();
        alumni.setDepartment("Computer Engineering");
        alumni.setGraduationYear(2021);
        alumni.setCompany("JDBC Tech Labs");
        alumni.setDesignation("Lead Architect");
        alumni.setExperienceYears(5);
        alumni.setIndustry("Information Technology");
        alumni.setSkills("Java, JDBC, MySQL, Spring");
        alumni.setBio("Experienced in Java database programming.");
        alumni.setMaxMentees(3);

        long userId = registrationDAO.registerAlumni(user, alumni);
        Assertions.assertTrue(userId > 0, "Generated user ID should be positive");

        Alumni retrieved = alumniDAO.findById(userId);
        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals("JDBC Tech Labs", retrieved.getCompany());
        Assertions.assertEquals("Lead Architect", retrieved.getDesignation());

        // Clean up test record
        cleanupUser(userId);
    }

    private void cleanupUser(long userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }
}
