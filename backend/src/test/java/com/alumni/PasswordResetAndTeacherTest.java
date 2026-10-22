package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.PasswordResetDAO;
import com.alumni.dao.TeacherDAO;
import com.alumni.service.PasswordResetMailer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class PasswordResetAndTeacherTest {

    private static final String TEST_EMAIL = "reset_test_unique_99@college.edu";
    private static final String ADMIN_ORIGINAL_HASH = "sha256$ca0fc3423a78e231952620ff1f065eba$1b684c010f2eb76c5f082287f0802d283aa35ef96643871ff9801c98633bb7a8";

    @BeforeAll
    public static void setUp() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "Database connection must be established");

        // Ensure admin user has correct password in case previous run touched it
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE id = 1")) {
            ps.setString(1, ADMIN_ORIGINAL_HASH);
            ps.executeUpdate();
        }

        // Create dedicated user for reset test
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO users (full_name, email, mobile_number, password_hash, role, is_active) " +
                     "VALUES ('Reset Test User', ?, '9000000099', 'initial_hash', 'student', TRUE) " +
                     "ON DUPLICATE KEY UPDATE password_hash = 'initial_hash', is_active = TRUE")) {
            ps.setString(1, TEST_EMAIL);
            ps.executeUpdate();
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM password_resets WHERE user_id IN (SELECT id FROM users WHERE email = ?)")) {
                ps.setString(1, TEST_EMAIL);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE email = ?")) {
                ps.setString(1, TEST_EMAIL);
                ps.executeUpdate();
            }
            // Ensure admin password remains pristine
            try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password_hash = ? WHERE id = 1")) {
                ps.setString(1, ADMIN_ORIGINAL_HASH);
                ps.executeUpdate();
            }
        }
    }

    @Test
    public void testPasswordResetDAO_TokenLifecycleAndEmailReturn() throws Exception {
        PasswordResetDAO dao = new PasswordResetDAO();

        // Create reset token for dedicated test user
        String token = dao.createResetToken(TEST_EMAIL);
        Assertions.assertNotNull(token, "Reset token should be generated");
        Assertions.assertEquals(64, token.length(), "Token should be 64-char hex string");

        // Hash a dummy new password
        String newPasswordHash = "$2a$10$abcdefghijklmnopqrstuvwxyz12345678901234567890123456";

        // Consume token and verify the returned email matches
        String returnedEmail = dao.consumeTokenAndChangePasswordWithEmail(token, newPasswordHash);
        Assertions.assertNotNull(returnedEmail, "Returned email should not be null upon successful reset");
        Assertions.assertTrue(TEST_EMAIL.equalsIgnoreCase(returnedEmail), "Returned email should match test user email");

        // Attempting to consume again should fail (one-time use)
        String secondAttempt = dao.consumeTokenAndChangePasswordWithEmail(token, newPasswordHash);
        Assertions.assertNull(secondAttempt, "Token must be single-use and cannot be consumed twice");
    }

    @Test
    public void testPasswordResetMailer_EnvironmentCheck() {
        // When SMTP env vars are not set, mailer should throw descriptive IllegalStateException
        IllegalStateException ex1 = Assertions.assertThrows(IllegalStateException.class, () -> {
            PasswordResetMailer.send("test@example.com", "fake-token");
        });
        Assertions.assertTrue(ex1.getMessage().contains("Missing environment variable: SMTP_HOST"));

        IllegalStateException ex2 = Assertions.assertThrows(IllegalStateException.class, () -> {
            PasswordResetMailer.sendRecoveryNotification("test@example.com");
        });
        Assertions.assertTrue(ex2.getMessage().contains("Missing environment variable: SMTP_HOST"));
    }

    @Test
    public void testTeacherDAO_MarksValidation() {
        TeacherDAO dao = new TeacherDAO();

        // Marks > maxMarks should throw IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dao.createInternalMark(1L, 1L, 1L, new BigDecimal("105.00"), new BigDecimal("100.00"), "Invalid marks");
        });

        // Negative marks should throw IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dao.createInternalMark(1L, 1L, 1L, new BigDecimal("-5.00"), new BigDecimal("100.00"), "Invalid marks");
        });

        // Scale > 2 should throw IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dao.createInternalMark(1L, 1L, 1L, new BigDecimal("85.123"), new BigDecimal("100.00"), "Invalid scale");
        });
    }

    @Test
    public void testTeacherDAO_ScheduledEventsQuery() throws Exception {
        TeacherDAO dao = new TeacherDAO();
        // Querying for a non-existent teacher returns empty array without throwing
        JSONArray events = dao.getScheduledEvents(999999L);
        Assertions.assertNotNull(events);
        Assertions.assertEquals(0, events.length());
    }
}
