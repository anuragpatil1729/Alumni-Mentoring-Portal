package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.server.HttpServerApp;
import org.json.JSONArray;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * AuthProfileIntegrationTest
 *
 * Full integration test suite validating the complete cross-module workflow:
 * Registration ➔ Authentication (Login) ➔ Profile View & Update ➔ Avatar Media ➔ Search Discovery.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthProfileIntegrationTest {

    private static HttpServerApp server;
    private static int httpPort;

    // Student lifecycle state
    private static long studentUserId;
    private static String studentEmail;
    private static String studentMobile;
    private static final String STUDENT_PASS = "StudentP@ssw0rd!2026";
    private static String studentIdStr;

    // Alumni lifecycle state
    private static long alumniUserId;
    private static String alumniEmail;
    private static String alumniMobile;
    private static final String ALUMNI_PASS = "AlumniP@ssw0rd!2026";

    // 1x1 transparent PNG in base64
    private static final String BASE64_PNG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @BeforeAll
    public static void setup() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "Database connection must be established");

        httpPort = 5061;
        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);

        long timestamp = System.currentTimeMillis();
        studentEmail = "integ_student_" + timestamp + "@university.edu";
        studentMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));
        studentIdStr = "STU-INT-" + (timestamp % 10000000);

        alumniEmail = "integ_alumni_" + timestamp + "@corporate.com";
        alumniMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupUser(studentUserId);
        cleanupUser(alumniUserId);
    }

    // ==========================================
    // [1] Student Lifecycle Integration Flow
    // ==========================================

    @Test
    @Order(1)
    public void testStudentStep1_PreRegistrationAvailabilityCheck() throws Exception {
        // Verify credentials are initially reported available
        HttpResult res = sendHttp("GET", "/api/auth/check-availability?email=" + studentEmail + "&mobileNumber=" + studentMobile, null, null);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getBoolean("emailAvailable"), "Email should be available before registration");
        Assertions.assertTrue(res.json.getBoolean("mobileAvailable"), "Mobile should be available before registration");
    }

    @Test
    @Order(2)
    public void testStudentStep2_RegistrationSuccess() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Integration Test Student");
        payload.put("email", studentEmail);
        payload.put("mobileNumber", studentMobile);
        payload.put("password", STUDENT_PASS);
        payload.put("studentId", studentIdStr);
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status, "Student registration must return 201 Created");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("id"));

        studentUserId = res.json.getLong("id");
        Assertions.assertTrue(studentUserId > 0);
        Assertions.assertEquals("student", res.json.getString("role"));

        // Verify DB state across users and students tables
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT u.full_name, u.email, u.password_hash, u.role, s.student_id, s.department " +
                     "FROM users u JOIN students s ON s.user_id = u.id WHERE u.id = ?")) {
            stmt.setLong(1, studentUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                Assertions.assertTrue(rs.next(), "Database must contain student record joined with user");
                Assertions.assertEquals("Integration Test Student", rs.getString("full_name"));
                Assertions.assertEquals(studentEmail, rs.getString("email"));
                Assertions.assertEquals("student", rs.getString("role"));
                Assertions.assertEquals(studentIdStr, rs.getString("student_id"));
                Assertions.assertTrue(rs.getString("password_hash").startsWith("sha256$"), "Password must be cryptographically hashed");
            }
        }
    }

    @Test
    @Order(3)
    public void testStudentStep3_AvailabilityCheckNowReportsInUse() throws Exception {
        // Email and mobile should now be marked in-use (available = false)
        HttpResult res = sendHttp("GET", "/api/auth/check-availability?email=" + studentEmail + "&mobileNumber=" + studentMobile, null, null);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertFalse(res.json.getBoolean("emailAvailable"), "Email must now be reported in-use");
        Assertions.assertFalse(res.json.getBoolean("mobileAvailable"), "Mobile must now be reported in-use");
    }

    @Test
    @Order(4)
    public void testStudentStep4_LoginWithRegisteredCredentials() throws Exception {
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", studentEmail);
        loginPayload.put("password", STUDENT_PASS);

        HttpResult res = sendHttp("POST", "/api/auth/login", loginPayload.toString(), "application/json");
        Assertions.assertEquals(200, res.status, "Login with new credentials must succeed");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertEquals("Login successful!", res.json.getString("message"));

        JSONObject user = res.json.getJSONObject("user");
        Assertions.assertEquals(studentUserId, user.getLong("id"));
        Assertions.assertEquals("Integration Test Student", user.getString("fullName"));
        Assertions.assertEquals("student", user.getString("role"));
        Assertions.assertEquals("Computer Engineering", user.getString("department"));
        Assertions.assertEquals(studentIdStr, user.getString("studentId"));
        Assertions.assertEquals(2026, user.getInt("graduationYear"));
    }

    @Test
    @Order(5)
    public void testStudentStep5_GetProfileMatchesRegistration() throws Exception {
        HttpResult res = sendHttp("GET", "/api/profile?id=" + studentUserId, null, null);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        JSONObject data = res.json.getJSONObject("profile");
        Assertions.assertEquals(studentUserId, data.getLong("id"));
        Assertions.assertEquals("Integration Test Student", data.getString("fullName"));
        Assertions.assertEquals(studentEmail, data.getString("email"));
        Assertions.assertEquals(studentMobile, data.getString("mobileNumber"));
        Assertions.assertEquals("student", data.getString("role"));
        Assertions.assertEquals("Computer Engineering", data.getString("department"));
        Assertions.assertEquals(studentIdStr, data.getString("studentId"));
        Assertions.assertEquals(2026, data.getInt("graduationYear"));
    }

    @Test
    @Order(6)
    public void testStudentStep6_UpdateProfilePersistsChanges() throws Exception {
        String newMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));

        JSONObject updatePayload = new JSONObject();
        updatePayload.put("fullName", "Integration Student Updated");
        updatePayload.put("mobileNumber", newMobile);
        updatePayload.put("department", "Information Technology");
        updatePayload.put("graduationYear", 2027);

        HttpResult res = sendHttp("PUT", "/api/profile?id=" + studentUserId, updatePayload.toString(), "application/json");
        Assertions.assertEquals(200, res.status, "Profile update must return 200 OK");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getString("message").contains("Profile updated successfully"));

        // Re-fetch profile to verify database persistence
        HttpResult getRes = sendHttp("GET", "/api/profile?id=" + studentUserId, null, null);
        Assertions.assertEquals(200, getRes.status);
        JSONObject updated = getRes.json.getJSONObject("profile");
        Assertions.assertEquals("Integration Student Updated", updated.getString("fullName"));
        Assertions.assertEquals(newMobile, updated.getString("mobileNumber"));
        Assertions.assertEquals("Information Technology", updated.getString("department"));
        Assertions.assertEquals(2027, updated.getInt("graduationYear"));
        // Student ID should remain intact
        Assertions.assertEquals(studentIdStr, updated.getString("studentId"));
    }

    @Test
    @Order(7)
    public void testStudentStep7_AvatarUploadAndStreaming() throws Exception {
        // Upload base64 avatar
        JSONObject avatarPayload = new JSONObject();
        avatarPayload.put("userId", studentUserId);
        avatarPayload.put("imageData", BASE64_PNG);
        avatarPayload.put("mimeType", "image/png");

        HttpResult uploadRes = sendHttp("POST", "/api/users/" + studentUserId + "/avatar", avatarPayload.toString(), "application/json");
        Assertions.assertEquals(200, uploadRes.status);
        Assertions.assertTrue(uploadRes.json.getBoolean("success"));
        Assertions.assertEquals("/api/users/" + studentUserId + "/avatar", uploadRes.json.getString("avatarUrl"));

        // Download and verify stream
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/users/" + studentUserId + "/avatar").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assertions.assertEquals(200, conn.getResponseCode());
        Assertions.assertEquals("image/png", conn.getContentType());
        byte[] fetchedBytes;
        try (var is = conn.getInputStream()) {
            fetchedBytes = is.readAllBytes();
        }
        Assertions.assertTrue(fetchedBytes.length > 0, "Avatar bytes must be retrieved from database BLOB");

        // Verify login now returns avatarUrl
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", studentEmail);
        loginPayload.put("password", STUDENT_PASS);
        HttpResult loginRes = sendHttp("POST", "/api/auth/login", loginPayload.toString(), "application/json");
        Assertions.assertEquals(200, loginRes.status);
        JSONObject user = loginRes.json.getJSONObject("user");
        Assertions.assertTrue(user.has("avatarUrl"));
        Assertions.assertEquals("/api/users/" + studentUserId + "/avatar", user.getString("avatarUrl"));

        // Clean up avatar
        HttpResult delRes = sendHttp("DELETE", "/api/users/" + studentUserId + "/avatar", null, null);
        Assertions.assertEquals(200, delRes.status);
        Assertions.assertTrue(delRes.json.getBoolean("deleted"));
    }

    // ==========================================
    // [2] Alumni Mentor Lifecycle Integration Flow
    // ==========================================

    @Test
    @Order(8)
    public void testAlumniStep1_RegistrationSuccess() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Integration Mentor Senior");
        payload.put("email", alumniEmail);
        payload.put("mobileNumber", alumniMobile);
        payload.put("password", ALUMNI_PASS);
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2019);
        payload.put("company", "Integration Labs Corp");
        payload.put("designation", "Staff Platform Engineer");
        payload.put("experienceYears", 7);
        payload.put("industry", "Information Technology");
        payload.put("skills", "Java, Distributed Systems, Cloud Architecture");
        payload.put("bio", "Seasoned engineer mentoring distributed systems.");
        payload.put("maxMentees", 4);
        payload.put("linkedInProfile", "https://linkedin.com/in/integration-mentor");

        HttpResult res = sendHttp("POST", "/api/auth/register/alumni", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("id"));

        alumniUserId = res.json.getLong("id");
        Assertions.assertTrue(alumniUserId > 0);
        Assertions.assertEquals("alumni", res.json.getString("role"));
    }

    @Test
    @Order(9)
    public void testAlumniStep2_LoginSuccess() throws Exception {
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", alumniEmail);
        loginPayload.put("password", ALUMNI_PASS);

        HttpResult res = sendHttp("POST", "/api/auth/login", loginPayload.toString(), "application/json");
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        JSONObject user = res.json.getJSONObject("user");
        Assertions.assertEquals(alumniUserId, user.getLong("id"));
        Assertions.assertEquals("alumni", user.getString("role"));
        Assertions.assertEquals("Integration Labs Corp", user.getString("company"));
        Assertions.assertEquals("Staff Platform Engineer", user.getString("designation"));
    }

    @Test
    @Order(10)
    public void testAlumniStep3_GetProfile() throws Exception {
        HttpResult res = sendHttp("GET", "/api/profile?id=" + alumniUserId, null, null);
        Assertions.assertEquals(200, res.status);
        JSONObject data = res.json.getJSONObject("profile");
        Assertions.assertEquals("Integration Labs Corp", data.getString("company"));
        Assertions.assertEquals("Staff Platform Engineer", data.getString("designation"));
        Assertions.assertEquals(7, data.getInt("experienceYears"));
        Assertions.assertEquals(4, data.getInt("maxMentees"));
    }

    @Test
    @Order(11)
    public void testAlumniStep4_UpdateProfileAndReflectInDirectorySearch() throws Exception {
        // Update company name and designation
        JSONObject updatePayload = new JSONObject();
        updatePayload.put("fullName", "Integration Mentor Senior");
        updatePayload.put("mobileNumber", alumniMobile);
        updatePayload.put("department", "Computer Engineering");
        updatePayload.put("graduationYear", 2019);
        updatePayload.put("company", "Quantum AI Systems");
        updatePayload.put("designation", "VP of Engineering");
        updatePayload.put("experienceYears", 8);
        updatePayload.put("industry", "Information Technology");
        updatePayload.put("skills", "Quantum Computing, AI, Java");
        updatePayload.put("bio", "Leading quantum engineering pipelines.");
        updatePayload.put("maxMentees", 5);

        HttpResult updateRes = sendHttp("PUT", "/api/profile?id=" + alumniUserId, updatePayload.toString(), "application/json");
        Assertions.assertEquals(200, updateRes.status);
        Assertions.assertTrue(updateRes.json.getBoolean("success"));

        // Verify update is immediately searchable via Linear Search
        HttpResult searchRes = sendHttp("GET", "/api/mentors/search?query=Quantum+AI+Systems&algorithm=linear", null, null);
        Assertions.assertEquals(200, searchRes.status);
        Assertions.assertTrue(searchRes.json.getBoolean("success"));

        JSONArray arr = searchRes.json.getJSONArray("data");
        Assertions.assertTrue(arr.length() >= 1, "Mentor must be found in directory by new company name");

        boolean found = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject m = arr.getJSONObject(i);
            if (m.getLong("id") == alumniUserId) {
                found = true;
                Assertions.assertEquals("Quantum AI Systems", m.getString("company"));
                Assertions.assertEquals("VP of Engineering", m.getString("designation"));
                Assertions.assertEquals(8, m.getInt("experienceYears"));
                break;
            }
        }
        Assertions.assertTrue(found, "Updated mentor record must match searched entry");

        // Verify single mentor lookup endpoint /api/mentors/:id
        HttpResult mentorLookup = sendHttp("GET", "/api/mentors/" + alumniUserId, null, null);
        Assertions.assertEquals(200, mentorLookup.status);
        JSONObject mData = mentorLookup.json.getJSONObject("data");
        Assertions.assertEquals("Quantum AI Systems", mData.getString("company"));
    }

    // ==========================================
    // [3] Cross-Module Negative & Boundary Scenarios
    // ==========================================

    @Test
    @Order(12)
    public void testConflict_DuplicateEmailRegistrationFails() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Another Student");
        payload.put("email", studentEmail); // duplicate email
        payload.put("mobileNumber", "9876543999");
        payload.put("password", "StudentPass@123");
        payload.put("studentId", "STU-DUP-123");
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(409, res.status, "Duplicate email registration must return 409 Conflict");
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getJSONObject("errors").has("email"));
    }

    @Test
    @Order(13)
    public void testConflict_DuplicateMobileRegistrationFails() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Another Student");
        payload.put("email", "completely_unique_" + System.currentTimeMillis() + "@test.edu");
        payload.put("mobileNumber", alumniMobile); // duplicate mobile
        payload.put("password", "StudentPass@123");
        payload.put("studentId", "STU-DUP-MOB");
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(409, res.status, "Duplicate mobile registration must return 409 Conflict");
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getJSONObject("errors").has("mobileNumber"));
    }

    @Test
    @Order(14)
    public void testLoginFailure_WrongPasswordForExistingUser() throws Exception {
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", studentEmail);
        loginPayload.put("password", "WrongP@ssword999");

        HttpResult res = sendHttp("POST", "/api/auth/login", loginPayload.toString(), "application/json");
        Assertions.assertEquals(401, res.status, "Incorrect password must return 401 Unauthorized");
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertEquals("Invalid email or password.", res.json.getString("message"));
    }

    @Test
    @Order(15)
    public void testProfileUpdateFailure_InvalidMobileLeavesProfileUntouched() throws Exception {
        JSONObject badUpdate = new JSONObject();
        badUpdate.put("fullName", "Integration Student Updated");
        badUpdate.put("mobileNumber", "12345"); // invalid mobile
        badUpdate.put("department", "Information Technology");
        badUpdate.put("graduationYear", 2027);

        HttpResult res = sendHttp("PUT", "/api/profile?id=" + studentUserId, badUpdate.toString(), "application/json");
        Assertions.assertEquals(400, res.status, "Invalid update payload must return 400 Bad Request");
        Assertions.assertFalse(res.json.getBoolean("success"));

        // Verify profile in DB still has valid data
        HttpResult check = sendHttp("GET", "/api/profile?id=" + studentUserId, null, null);
        Assertions.assertEquals(200, check.status);
        JSONObject current = check.json.getJSONObject("profile");
        Assertions.assertNotEquals("12345", current.getString("mobileNumber"));
    }

    @Test
    @Order(16)
    public void testProfileNotFound_NonExistentUserId() throws Exception {
        HttpResult res = sendHttp("GET", "/api/profile?id=999999999", null, null);
        Assertions.assertEquals(404, res.status, "Non-existent profile ID must return 404 Not Found");
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private static class HttpResult {
        int status;
        JSONObject json;
        String raw;
    }

    private HttpResult sendHttp(String method, String path, String body, String contentType) throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);

        if (body != null) {
            conn.setDoOutput(true);
            if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
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
}
