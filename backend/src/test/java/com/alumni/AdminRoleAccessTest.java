package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.RegistrationDAO;
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
 * AdminRoleAccessTest
 *
 * Tests:
 * 1. Admin Authentication (Team D)
 * 2. Manual duplicate checking logic for users (Team D)
 * 3. Role-based access testing for Admin vs Student/Alumni/Unauthenticated (Team B)
 * 4. Admin User Approval and Deletion workflows (Team A)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminRoleAccessTest {

    private static HttpServerApp server;
    private static int httpPort = 5062;

    private static long adminUserId;
    private static String adminToken;

    private static long studentUserId;
    private static String studentToken;
    private static String studentEmail;

    private static long alumniUserId;
    private static String alumniToken;
    private static String alumniEmail;

    private static long dummyUserId;

    @BeforeAll
    public static void setup() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "Database connection must be established");

        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);

        long timestamp = System.currentTimeMillis();

        // 1. Ensure seed admin account exists in DB
        try (Connection con = DBConnection.getConnection()) {
            String checkAdmin = "SELECT id FROM users WHERE email = 'admin@college.edu'";
            try (PreparedStatement ps = con.prepareStatement(checkAdmin);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    adminUserId = rs.getLong("id");
                } else {
                    String hash = RegistrationDAO.hashPassword("Admin@123");
                    String insert = "INSERT INTO users (full_name, email, mobile_number, password_hash, role) " +
                            "VALUES ('System Administrator', 'admin@college.edu', '9999999999', ?, 'admin')";
                    try (PreparedStatement ins = con.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        ins.setString(1, hash);
                        ins.executeUpdate();
                        try (ResultSet k = ins.getGeneratedKeys()) {
                            if (k.next()) adminUserId = k.getLong(1);
                        }
                    }
                }
            }
        }

        // 2. Create student user for role permission tests
        studentEmail = "role_student_" + timestamp + "@college.edu";
        JSONObject studentReg = new JSONObject();
        studentReg.put("fullName", "Role Test Student");
        studentReg.put("email", studentEmail);
        studentReg.put("mobileNumber", "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        studentReg.put("password", "TestPass!2026");
        studentReg.put("studentId", "ROLE-STU-" + (timestamp % 1000000));
        studentReg.put("department", "Computer Engineering");
        studentReg.put("graduationYear", 2026);

        HttpResult sReg = sendHttp("POST", "/api/auth/register/student", studentReg.toString(), "application/json", null);
        Assertions.assertEquals(201, sReg.status);
        studentUserId = sReg.json.getLong("id");

        HttpResult sLogin = sendHttp("POST", "/api/auth/login",
                new JSONObject().put("email", studentEmail).put("password", "TestPass!2026").toString(),
                "application/json", null);
        Assertions.assertEquals(200, sLogin.status);
        studentToken = sLogin.json.getString("token");

        // 3. Create alumni user for role permission tests
        alumniEmail = "role_alumni_" + timestamp + "@corporate.com";
        JSONObject alumniReg = new JSONObject();
        alumniReg.put("fullName", "Role Test Alumni");
        alumniReg.put("email", alumniEmail);
        alumniReg.put("mobileNumber", "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        alumniReg.put("password", "TestPass!2026");
        alumniReg.put("department", "Computer Engineering");
        alumniReg.put("graduationYear", 2022);
        alumniReg.put("company", "Tech Corp");
        alumniReg.put("designation", "Software Architect");

        HttpResult aReg = sendHttp("POST", "/api/auth/register/alumni", alumniReg.toString(), "application/json", null);
        Assertions.assertEquals(201, aReg.status);
        alumniUserId = aReg.json.getLong("id");

        HttpResult aLogin = sendHttp("POST", "/api/auth/login",
                new JSONObject().put("email", alumniEmail).put("password", "TestPass!2026").toString(),
                "application/json", null);
        Assertions.assertEquals(200, aLogin.status);
        alumniToken = aLogin.json.getString("token");
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupUser(studentUserId);
        cleanupUser(alumniUserId);
        cleanupUser(dummyUserId);
    }

    // ==========================================
    // [1] Team D: Admin Authentication Tests
    // ==========================================

    @Test
    @Order(1)
    public void testAdminAuth_SuccessfulLogin() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "admin@college.edu");
        payload.put("password", "Admin@123");

        HttpResult res = sendHttp("POST", "/api/auth/login", payload.toString(), "application/json", null);
        Assertions.assertEquals(200, res.status, "Admin login must succeed with 200 OK");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("token"));
        adminToken = res.json.getString("token");

        JSONObject user = res.json.getJSONObject("user");
        Assertions.assertEquals("admin", user.getString("role"));
        Assertions.assertEquals(adminUserId, user.getLong("id"));
    }

    @Test
    @Order(2)
    public void testAdminAuth_InvalidCredentialsFails() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "admin@college.edu");
        payload.put("password", "WrongAdminPass!99");

        HttpResult res = sendHttp("POST", "/api/auth/login", payload.toString(), "application/json", null);
        Assertions.assertEquals(401, res.status, "Admin login with bad credentials must return 401 Unauthorized");
        Assertions.assertFalse(res.json.getBoolean("success"));
    }

    // ==========================================
    // [2] Team D: Manual Duplicate Checking Tests
    // ==========================================

    @Test
    @Order(3)
    public void testDuplicateChecking_BatchScan() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/duplicates", null, null, adminToken);
        Assertions.assertEquals(200, res.status, "Admin scan for duplicates must return 200 OK");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("duplicates"));
        Assertions.assertTrue(res.json.has("totalClusters"));
    }

    @Test
    @Order(4)
    public void testDuplicateChecking_ManualSingleCandidateCheck() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("name", "Role Test Student");
        payload.put("email", studentEmail);
        payload.put("mobileNumber", "9876543210");

        HttpResult res = sendHttp("POST", "/api/admin/duplicates/check", payload.toString(), "application/json", adminToken);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getBoolean("hasDuplicates"), "Should detect existing student email as duplicate");
        Assertions.assertTrue(res.json.getInt("duplicateCount") >= 1);

        JSONArray matches = res.json.getJSONArray("matches");
        boolean foundStudent = false;
        for (int i = 0; i < matches.length(); i++) {
            if (matches.getJSONObject(i).getLong("id") == studentUserId) {
                foundStudent = true;
                break;
            }
        }
        Assertions.assertTrue(foundStudent, "Matches must contain the existing student record");
    }

    // ==========================================
    // [3] Team B: Role-Based Access Control (RBAC) Tests
    // ==========================================

    @Test
    @Order(5)
    public void testRoleRBAC_AdminCanAccessUserDirectory() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/users", null, null, adminToken);
        Assertions.assertEquals(200, res.status, "Admin must be authorized to access /api/admin/users");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getJSONArray("users").length() > 0);
    }

    @Test
    @Order(6)
    public void testRoleRBAC_StudentForbiddenFromAdminUsers() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/users", null, null, studentToken);
        Assertions.assertEquals(403, res.status, "Student role must receive 403 Forbidden for admin users endpoint");
    }

    @Test
    @Order(7)
    public void testRoleRBAC_AlumniForbiddenFromAdminUsers() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/users", null, null, alumniToken);
        Assertions.assertEquals(403, res.status, "Alumni role must receive 403 Forbidden for admin users endpoint");
    }

    @Test
    @Order(8)
    public void testRoleRBAC_UnauthenticatedForbiddenFromAdminEndpoints() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/users", null, null, null);
        Assertions.assertEquals(401, res.status, "Unauthenticated request must receive 401 Unauthorized");
    }

    @Test
    @Order(9)
    public void testRoleRBAC_StudentForbiddenFromDuplicatesScan() throws Exception {
        HttpResult res = sendHttp("GET", "/api/admin/duplicates", null, null, studentToken);
        Assertions.assertEquals(403, res.status, "Student role must receive 403 Forbidden on duplicates scan");
    }

    // ==========================================
    // [4] Team A: Admin Approval & Delete User Actions
    // ==========================================

    @Test
    @Order(10)
    public void testAdminActions_CreateAndApproveUser() throws Exception {
        long timestamp = System.currentTimeMillis();
        String dummyEmail = "candidate_" + timestamp + "@college.edu";
        JSONObject candidateReg = new JSONObject();
        candidateReg.put("fullName", "Candidate User");
        candidateReg.put("email", dummyEmail);
        candidateReg.put("mobileNumber", "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        candidateReg.put("password", "TestPass!2026");
        candidateReg.put("studentId", "CAND-" + (timestamp % 1000000));
        candidateReg.put("department", "Information Technology");
        candidateReg.put("graduationYear", 2026);

        HttpResult regRes = sendHttp("POST", "/api/auth/register/student", candidateReg.toString(), "application/json", null);
        Assertions.assertEquals(201, regRes.status);
        dummyUserId = regRes.json.getLong("id");

        // Admin approves user
        JSONObject approvePayload = new JSONObject().put("approved", true);
        HttpResult appRes = sendHttp("POST", "/api/admin/users/" + dummyUserId + "/approve",
                approvePayload.toString(), "application/json", adminToken);
        Assertions.assertEquals(200, appRes.status, "Admin user approval must return 200 OK");
        Assertions.assertTrue(appRes.json.getBoolean("success"));

        // Verify in DB
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT approval_status, is_active FROM users WHERE id = ?")) {
            ps.setLong(1, dummyUserId);
            try (ResultSet rs = ps.executeQuery()) {
                Assertions.assertTrue(rs.next());
                Assertions.assertEquals("APPROVED", rs.getString("approval_status"));
                Assertions.assertTrue(rs.getBoolean("is_active"));
            }
        }
    }

    @Test
    @Order(11)
    public void testAdminActions_SelfDeleteForbidden() throws Exception {
        HttpResult res = sendHttp("DELETE", "/api/admin/users/" + adminUserId, null, null, adminToken);
        Assertions.assertEquals(400, res.status, "Admin must be blocked from deleting their own account");
    }

    @Test
    @Order(12)
    public void testAdminActions_StudentForbiddenFromDeletingUser() throws Exception {
        HttpResult res = sendHttp("DELETE", "/api/admin/users/" + dummyUserId, null, null, studentToken);
        Assertions.assertEquals(403, res.status, "Student must receive 403 Forbidden when attempting user deletion");
    }

    @Test
    @Order(13)
    public void testAdminActions_AdminSuccessfullyDeletesUser() throws Exception {
        HttpResult res = sendHttp("DELETE", "/api/admin/users/" + dummyUserId, null, null, adminToken);
        Assertions.assertEquals(200, res.status, "Admin deleting user must return 200 OK");
        Assertions.assertTrue(res.json.getBoolean("success"));

        // Verify user deleted in DB
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE id = ?")) {
            ps.setLong(1, dummyUserId);
            try (ResultSet rs = ps.executeQuery()) {
                Assertions.assertFalse(rs.next(), "Deleted user must no longer exist in database");
            }
        }
    }

    // ==========================================
    // HTTP Helpers
    // ==========================================

    private static class HttpResult {
        int status;
        JSONObject json;
        String raw;
    }

    private static HttpResult sendHttp(String method, String path, String body, String contentType, String token) throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

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
