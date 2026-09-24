package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Student;
import com.alumni.model.User;
import com.alumni.server.HttpServerApp;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

/**
 * LoginBoundaryTest
 *
 * Comprehensive Boundary Value Analysis (BVA) & Equivalence Partitioning (EP)
 * for the User Authentication endpoint: POST /api/auth/login
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginBoundaryTest {

    private static HttpServerApp server;
    private static int httpPort;
    private static RegistrationDAO registrationDAO;

    // Test accounts
    private static long boundaryStudentId;
    private static final String STUDENT_EMAIL = "login_boundary_student@college.edu";
    private static final String STUDENT_PASSWORD = "Password@123";

    // Long password user for upper length boundary testing
    private static long longPasswordUserId;
    private static final String LONG_PASS_EMAIL = "long_pass_user@college.edu";
    // 128 characters password (upper limit of valid password length)
    private static final String EXACT_128_CHAR_PASSWORD = "A1!" + "a".repeat(125);

    @BeforeAll
    public static void setup() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "Database connection must be established");

        registrationDAO = new RegistrationDAO();

        // 1. Create a known test student user
        cleanupUserByEmail(STUDENT_EMAIL);
        cleanupUserByEmail(LONG_PASS_EMAIL);

        User sUser = new User();
        sUser.setFullName("Boundary Test Student");
        sUser.setEmail(STUDENT_EMAIL);
        sUser.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        sUser.setPasswordHash(RegistrationDAO.hashPassword(STUDENT_PASSWORD));

        Student s = new Student();
        s.setStudentId("STU_BND_" + (System.currentTimeMillis() % 1000000));
        s.setDepartment("Computer Engineering");
        s.setGraduationYear(2026);
        boundaryStudentId = registrationDAO.registerStudent(sUser, s);

        // 2. Create user with exactly 128-char password
        User lpUser = new User();
        lpUser.setFullName("Long Pass User");
        lpUser.setEmail(LONG_PASS_EMAIL);
        lpUser.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        lpUser.setPasswordHash(RegistrationDAO.hashPassword(EXACT_128_CHAR_PASSWORD));

        Student lpStudent = new Student();
        lpStudent.setStudentId("STU_LP_" + (System.currentTimeMillis() % 1000000));
        lpStudent.setDepartment("Information Technology");
        lpStudent.setGraduationYear(2025);
        longPasswordUserId = registrationDAO.registerStudent(lpUser, lpStudent);

        // 3. Start test HTTP server on port 5060
        httpPort = 5060;
        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupUser(boundaryStudentId);
        cleanupUser(longPasswordUserId);
    }

    // ==========================================
    // [1] Baseline Valid Login Tests
    // ==========================================

    @Test
    @Order(1)
    public void testValidLoginSuccess() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status, "Valid login must return 200 OK");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertEquals("Login successful!", res.json.getString("message"));
        Assertions.assertTrue(res.json.has("user"));

        JSONObject user = res.json.getJSONObject("user");
        Assertions.assertEquals(boundaryStudentId, user.getLong("id"));
        Assertions.assertEquals("student", user.getString("role"));
        Assertions.assertEquals("Computer Engineering", user.getString("department"));
    }

    @Test
    @Order(2)
    public void testValidAlumniSeedLoginSuccess() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "anurag.patil@microsoft.com");
        payload.put("password", "Alumni@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        JSONObject user = res.json.getJSONObject("user");
        Assertions.assertEquals("alumni", user.getString("role"));
        Assertions.assertEquals("Microsoft", user.getString("company"));
    }

    // ==========================================
    // [2] Empty and Missing Input Boundaries
    // ==========================================

    @Test
    @Order(3)
    public void testEmptyPayloadBoundary() throws Exception {
        HttpResult res = sendLogin("{}");
        Assertions.assertEquals(400, res.status, "Empty JSON must return 400 Bad Request");
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertEquals("Email and password are required.", res.json.getString("message"));
    }

    @Test
    @Order(4)
    public void testEmptyEmailBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "");
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(5)
    public void testWhitespaceOnlyEmailBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "   ");
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(6)
    public void testEmptyPasswordBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", "");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(7)
    public void testMissingEmailField() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(8)
    public void testMissingPasswordField() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    // ==========================================
    // [3] Email Whitespace & Case-Insensitivity Boundaries
    // ==========================================

    @Test
    @Order(9)
    public void testEmailWithLeadingAndTrailingWhitespace() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "   " + STUDENT_EMAIL + "   ");
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status, "Email with outer whitespace should be trimmed and accepted");
        Assertions.assertTrue(res.json.getBoolean("success"));
    }

    @Test
    @Order(10)
    public void testEmailAllUppercaseBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL.toUpperCase());
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status, "Email authentication must be case-insensitive");
        Assertions.assertTrue(res.json.getBoolean("success"));
    }

    @Test
    @Order(11)
    public void testEmailMixedCaseBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "LoGiN_BoUnDaRy_StUdEnT@CoLlEgE.EdU");
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
    }

    // ==========================================
    // [4] Password Exactness & Case-Sensitivity Boundaries
    // ==========================================

    @Test
    @Order(12)
    public void testPasswordCaseSensitivity() throws Exception {
        // Correct is "Password@123", testing lowercase "password@123"
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", "password@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "Password must be strictly case-sensitive");
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertEquals("Invalid email or password.", res.json.getString("message"));
    }

    @Test
    @Order(13)
    public void testPasswordSingleCharacterOff() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", "Password@124"); // last char changed

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    // ==========================================
    // [5] Length Boundaries (Email & Password)
    // ==========================================

    @Test
    @Order(14)
    public void testSingleCharacterEmailBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "a");
        payload.put("password", STUDENT_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "Non-existent single-character email should return 401 Unauthorized");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(15)
    public void testMaxValidRfcEmailLengthBoundary() throws Exception {
        // Exactly 254 characters email (RFC 5321 maximum valid email length)
        String localPart = "a".repeat(64);
        String domainPart = "b".repeat(185) + ".com"; // 64 + 1 + 185 + 4 = 254 characters
        String maxEmail = localPart + "@" + domainPart;
        Assertions.assertEquals(254, maxEmail.length(), "Email length must be exactly 254 chars");

        JSONObject payload = new JSONObject();
        payload.put("email", maxEmail);
        payload.put("password", "ValidPass@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "Valid RFC length non-existent email safely rejected without 500");
    }

    @Test
    @Order(16)
    public void testOverflowEmailLengthBoundary() throws Exception {
        // Very large email string (1,000+ characters) to test buffer overflow / DoS resilience
        String overflowEmail = "user_" + "x".repeat(1000) + "@example.com";

        JSONObject payload = new JSONObject();
        payload.put("email", overflowEmail);
        payload.put("password", "ValidPass@123");

        HttpResult res = sendLogin(payload.toString());
        // Must handle cleanly (either 401 or 400), but never crash or return 500
        Assertions.assertTrue(res.status == 401 || res.status == 400, "Buffer overflow input must not crash server");
    }

    @Test
    @Order(17)
    public void testPasswordUpperLimit128CharsBoundary() throws Exception {
        // Exact 128-character password succeeds for registered user
        JSONObject payload = new JSONObject();
        payload.put("email", LONG_PASS_EMAIL);
        payload.put("password", EXACT_128_CHAR_PASSWORD);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(200, res.status, "128-character upper boundary password must authenticate successfully");
        Assertions.assertTrue(res.json.getBoolean("success"));
    }

    @Test
    @Order(18)
    public void testExtremeLengthPasswordBoundary() throws Exception {
        // 4,000+ characters password payload to test server resilience against large string hashing attacks
        String hugePassword = "P@" + "9".repeat(4000);

        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", hugePassword);

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "Extremely long password must be safely rejected with 401");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    // ==========================================
    // [6] Security Boundaries: SQL Injection, XSS, & Special Characters
    // ==========================================

    @Test
    @Order(19)
    public void testSqlInjectionTautologyInEmail() throws Exception {
        // Classic SQL injection tautology: ' OR '1'='1
        JSONObject payload = new JSONObject();
        payload.put("email", "' OR '1'='1");
        payload.put("password", "anything");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "SQL injection payload must return 401 Unauthorized, never bypass auth");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(20)
    public void testSqlInjectionCommentInEmail() throws Exception {
        // Admin comment bypass: admin'--
        JSONObject payload = new JSONObject();
        payload.put("email", "admin'--");
        payload.put("password", "anything");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(21)
    public void testSqlInjectionUnionSelectInEmail() throws Exception {
        // Union select injection
        JSONObject payload = new JSONObject();
        payload.put("email", "test' UNION SELECT 1, 'admin', 'a@a.com', '9876543210', 'hash', 'student', null, null, null, null, null, null, null--");
        payload.put("password", "hash");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "PreparedStatement must safely parameterize UNION SELECT injection");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(22)
    public void testSqlInjectionInPassword() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", STUDENT_EMAIL);
        payload.put("password", "' OR '1'='1");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(23)
    public void testXssScriptTagInEmail() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "<script>alert('xss')</script>@test.com");
        payload.put("password", "Password@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(24)
    public void testUnicodeAndEmojiBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "student🚀@college.edu");
        payload.put("password", "Password🔒@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status, "Unicode/emoji inputs safely handled without server failure");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    @Test
    @Order(25)
    public void testNullByteBoundary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "student\0@college.edu");
        payload.put("password", "Password\0@123");

        HttpResult res = sendLogin(payload.toString());
        Assertions.assertEquals(401, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    // ==========================================
    // [7] HTTP Method Boundary
    // ==========================================

    @Test
    @Order(26)
    public void testInvalidHttpMethodOnLogin() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/auth/login").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        Assertions.assertEquals(405, responseCode, "GET method on /api/auth/login must return 405 Method Not Allowed");
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private static class HttpResult {
        int status;
        JSONObject json;
        String raw;
    }

    private HttpResult sendLogin(String jsonBody) throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/auth/login").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        HttpResult res = new HttpResult();
        res.status = conn.getResponseCode();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                res.status >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            res.raw = sb.toString();
            try {
                res.json = new JSONObject(res.raw);
            } catch (Exception e) {
                res.json = new JSONObject();
            }
        }
        return res;
    }

    private static void cleanupUser(long userId) {
        if (userId <= 0) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private static void cleanupUserByEmail(String email) {
        if (email == null) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
