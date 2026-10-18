package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.RegistrationDAO;
import com.alumni.server.HttpServerApp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Random;

/**
 * SessionFeedbackIntegrationTest
 *
 * Team A & Team B: End-to-end integration testing for Session and Feedback modules.
 * Validates complete cross-module flow:
 * Mentorship Request -> Session Creation -> Session Completion -> Feedback Submission & Validation ->
 * Duplicate Prevention -> Feedback Aggregation and Metrics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SessionFeedbackIntegrationTest {

    private static HttpServerApp server;
    private static int httpPort = 5063;
    private static HttpClient httpClient;

    private static long studentUserId;
    private static String studentToken;
    private static String studentEmail;

    private static long mentorUserId;
    private static String mentorToken;
    private static String mentorEmail;

    private static long adminUserId;
    private static String adminToken;

    private static long requestId;
    private static long sessionId;
    private static long feedbackId;

    @BeforeAll
    public static void setup() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "Database connection must be established");

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);

        long timestamp = System.currentTimeMillis();

        // 1. Register student
        studentEmail = "sess_stu_" + timestamp + "@college.edu";
        JSONObject studentReg = new JSONObject();
        studentReg.put("fullName", "Session Student User");
        studentReg.put("email", studentEmail);
        studentReg.put("mobileNumber", "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        studentReg.put("password", "TestPass!2026");
        studentReg.put("studentId", "SF-STU-" + (timestamp % 1000000));
        studentReg.put("department", "Computer Engineering");
        studentReg.put("graduationYear", 2026);

        HttpResult sReg = sendHttp("POST", "/api/auth/register/student", studentReg.toString(), "application/json", null);
        Assertions.assertEquals(201, sReg.status);
        studentUserId = sReg.json.getLong("id");

        HttpResult sLogin = sendHttp("POST", "/api/auth/login",
                new JSONObject().put("email", studentEmail).put("password", "TestPass!2026").toString(),
                "application/json", null);
        studentToken = sLogin.json.getString("token");

        // 2. Register mentor (alumni)
        mentorEmail = "sess_mentor_" + timestamp + "@tech.com";
        JSONObject mentorReg = new JSONObject();
        mentorReg.put("fullName", "Session Mentor Expert");
        mentorReg.put("email", mentorEmail);
        mentorReg.put("mobileNumber", "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        mentorReg.put("password", "TestPass!2026");
        mentorReg.put("department", "Computer Engineering");
        mentorReg.put("graduationYear", 2020);
        mentorReg.put("company", "Cloud Systems Inc");
        mentorReg.put("designation", "Principal Architect");

        HttpResult mReg = sendHttp("POST", "/api/auth/register/alumni", mentorReg.toString(), "application/json", null);
        Assertions.assertEquals(201, mReg.status);
        mentorUserId = mReg.json.getLong("id");

        HttpResult mLogin = sendHttp("POST", "/api/auth/login",
                new JSONObject().put("email", mentorEmail).put("password", "TestPass!2026").toString(),
                "application/json", null);
        mentorToken = mLogin.json.getString("token");

        // 3. Admin token
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
        HttpResult admLogin = sendHttp("POST", "/api/auth/login",
                new JSONObject().put("email", "admin@college.edu").put("password", "Admin@123").toString(),
                "application/json", null);
        adminToken = admLogin.json.getString("token");
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupSessionData();
        cleanupUser(studentUserId);
        cleanupUser(mentorUserId);
    }

    // ==========================================
    // [1] Session Lifecycle Tests
    // ==========================================

    @Test
    @Order(1)
    public void testStep1_CreateMentorshipRequestAndAccept() throws Exception {
        // Student creates mentorship request
        JSONObject reqPayload = new JSONObject();
        reqPayload.put("studentId", studentUserId);
        reqPayload.put("mentorId", mentorUserId);
        reqPayload.put("sessionGoal", "Distributed Systems Design");
        reqPayload.put("message", "Looking for guidance on high throughput distributed messaging architectures.");

        HttpResult reqRes = sendHttp("POST", "/api/requests", reqPayload.toString(), "application/json", studentToken);
        Assertions.assertEquals(201, reqRes.status);

        // Fetch the created request from DB or GET
        HttpResult getReqs = sendHttp("GET", "/api/requests?studentId=" + studentUserId, null, null, studentToken);
        Assertions.assertEquals(200, getReqs.status);
        JSONArray arr = getReqs.json.getJSONArray("requests");
        Assertions.assertTrue(arr.length() >= 1);
        requestId = arr.getJSONObject(0).getLong("id");
        Assertions.assertTrue(requestId > 0);

        // Mentor accepts request via PUT /api/requests?id=
        JSONObject acceptPayload = new JSONObject();
        acceptPayload.put("status", "ACCEPTED");
        acceptPayload.put("mentorResponse", "Looking forward to our session!");

        HttpResult accRes = sendHttp("PUT", "/api/requests?id=" + requestId,
                acceptPayload.toString(), "application/json", mentorToken);
        Assertions.assertEquals(200, accRes.status);
    }

    @Test
    @Order(2)
    public void testStep2_StartSessionHistory() throws Exception {
        JSONObject sessPayload = new JSONObject();
        sessPayload.put("studentId", studentUserId);
        sessPayload.put("alumniId", mentorUserId);
        sessPayload.put("mentorshipRequestId", requestId);
        sessPayload.put("priority", "HIGH");

        HttpResult res = sendHttp("POST", "/api/session-history", sessPayload.toString(), "application/json", studentToken);
        Assertions.assertEquals(201, res.status, "Session creation must return 201 Created");
        Assertions.assertTrue(res.json.getBoolean("success"));
        sessionId = res.json.getLong("sessionId");
        Assertions.assertEquals("STARTED", res.json.getString("status"));
        Assertions.assertTrue(sessionId > 0);
    }

    // ==========================================
    // [2] Feedback Validation against Incomplete Sessions
    // ==========================================

    @Test
    @Order(3)
    public void testStep3_FeedbackRejectedForIncompleteSession() throws Exception {
        // Attempt to submit feedback while session is still STARTED
        JSONObject fbPayload = new JSONObject();
        fbPayload.put("sessionId", sessionId);
        fbPayload.put("rating", 5);
        fbPayload.put("comments", "Session was great!");

        HttpResult res = sendHttp("POST", "/api/feedback", fbPayload.toString(), "application/json", studentToken);
        Assertions.assertEquals(400, res.status, "Feedback must be rejected for sessions that are not COMPLETED");
    }

    @Test
    @Order(4)
    public void testStep4_CompleteSession() throws Exception {
        // Transition session to COMPLETED via PATCH
        JSONObject updatePayload = new JSONObject().put("status", "COMPLETED");
        HttpResult res = sendHttp("PATCH", "/api/session-history/" + sessionId + "/status",
                updatePayload.toString(), "application/json", studentToken);
        Assertions.assertEquals(200, res.status, "Session transition to COMPLETED must succeed");
    }

    // ==========================================
    // [3] Feedback Input Validation & Persistence
    // ==========================================

    @Test
    @Order(5)
    public void testStep5_FeedbackRatingBoundaryRejection() throws Exception {
        // Rating out of range (rating = 6)
        JSONObject fbTooHigh = new JSONObject()
                .put("sessionId", sessionId)
                .put("rating", 6)
                .put("comments", "Great session!");
        HttpResult resHigh = sendHttp("POST", "/api/feedback", fbTooHigh.toString(), "application/json", studentToken);
        Assertions.assertEquals(400, resHigh.status, "Rating > 5 must be rejected with 400");

        // Rating out of range (rating = 0)
        JSONObject fbTooLow = new JSONObject()
                .put("sessionId", sessionId)
                .put("rating", 0)
                .put("comments", "Bad session!");
        HttpResult resLow = sendHttp("POST", "/api/feedback", fbTooLow.toString(), "application/json", studentToken);
        Assertions.assertEquals(400, resLow.status, "Rating < 1 must be rejected with 400");
    }

    @Test
    @Order(6)
    public void testStep6_FeedbackEmptyCommentsRejection() throws Exception {
        JSONObject fbEmptyComments = new JSONObject()
                .put("sessionId", sessionId)
                .put("rating", 4)
                .put("comments", "   ");
        HttpResult res = sendHttp("POST", "/api/feedback", fbEmptyComments.toString(), "application/json", studentToken);
        Assertions.assertEquals(400, res.status, "Empty comments must be rejected with 400");
    }

    @Test
    @Order(7)
    public void testStep7_SaveValidFeedbackSuccess() throws Exception {
        JSONObject validFeedback = new JSONObject()
                .put("sessionId", sessionId)
                .put("rating", 5)
                .put("comments", "Outstanding mentorship! Mentor shared exceptional insights on scalable systems.");

        HttpResult res = sendHttp("POST", "/api/feedback", validFeedback.toString(), "application/json", studentToken);
        Assertions.assertEquals(201, res.status, "Valid feedback submission must return 201 Created");
        Assertions.assertTrue(res.json.getBoolean("success"));
        feedbackId = res.json.getLong("feedbackId");
        Assertions.assertTrue(feedbackId > 0);

        // Verify in DB directly
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT session_id, student_id, mentor_id, rating, comments FROM feedback WHERE id = ?")) {
            ps.setLong(1, feedbackId);
            try (ResultSet rs = ps.executeQuery()) {
                Assertions.assertTrue(rs.next(), "Feedback record must be persisted in database");
                Assertions.assertEquals(sessionId, rs.getLong("session_id"));
                Assertions.assertEquals(studentUserId, rs.getLong("student_id"));
                Assertions.assertEquals(mentorUserId, rs.getLong("mentor_id"));
                Assertions.assertEquals(5, rs.getInt("rating"));
                Assertions.assertEquals("Outstanding mentorship! Mentor shared exceptional insights on scalable systems.", rs.getString("comments"));
            }
        }
    }

    @Test
    @Order(8)
    public void testStep8_DuplicateFeedbackPrevented() throws Exception {
        // Attempt to submit second feedback for the same session
        JSONObject duplicateFeedback = new JSONObject()
                .put("sessionId", sessionId)
                .put("rating", 4)
                .put("comments", "Submitting a second review for the same session.");

        HttpResult res = sendHttp("POST", "/api/feedback", duplicateFeedback.toString(), "application/json", studentToken);
        Assertions.assertEquals(400, res.status, "Duplicate feedback for the same session must be rejected");
    }

    // ==========================================
    // [4] Feedback Retrieval & Summary Aggregation
    // ==========================================

    @Test
    @Order(9)
    public void testStep9_GetFeedbackBySession() throws Exception {
        HttpResult res = sendHttp("GET", "/api/feedback/session/" + sessionId, null, null, studentToken);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        JSONObject fb = res.json.getJSONObject("feedback");
        Assertions.assertEquals(feedbackId, fb.getLong("id"));
        Assertions.assertEquals(sessionId, fb.getLong("sessionId"));
        Assertions.assertEquals(studentUserId, fb.getLong("studentId"));
        Assertions.assertEquals(mentorUserId, fb.getLong("mentorId"));
        Assertions.assertEquals(5, fb.getInt("rating"));
        Assertions.assertEquals("Session Student User", fb.getString("studentName"));
        Assertions.assertEquals("Session Mentor Expert", fb.getString("mentorName"));
    }

    @Test
    @Order(10)
    public void testStep10_GetMentorFeedbackSummary() throws Exception {
        HttpResult res = sendHttp("GET", "/api/feedback/mentor/" + mentorUserId, null, null, null);
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        Assertions.assertEquals(1, res.json.getInt("totalReviews"));
        Assertions.assertEquals(5.0, res.json.getDouble("averageRating"));

        JSONObject dist = res.json.getJSONObject("ratingDistribution");
        Assertions.assertEquals(1, dist.getInt("5"));
        Assertions.assertEquals(0, dist.getInt("4"));

        JSONArray reviews = res.json.getJSONArray("reviews");
        Assertions.assertEquals(1, reviews.length());
        Assertions.assertEquals(5, reviews.getJSONObject(0).getInt("rating"));
        Assertions.assertEquals("Session Student User", reviews.getJSONObject(0).getString("studentName"));
    }

    @Test
    @Order(11)
    public void testStep11_AdminGetAllFeedback() throws Exception {
        HttpResult res = sendHttp("GET", "/api/feedback", null, null, adminToken);
        Assertions.assertEquals(200, res.status, "Admin should be able to view all feedback");
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getJSONArray("feedback").length() >= 1);
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
        URI uri = URI.create("http://127.0.0.1:" + httpPort + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher publisher = (body != null)
                ? HttpRequest.BodyPublishers.ofString(body)
                : HttpRequest.BodyPublishers.noBody();

        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        builder.method(method.toUpperCase(), publisher);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        HttpResult res = new HttpResult();
        res.status = response.statusCode();
        res.raw = response.body();

        try {
            res.json = new JSONObject(res.raw);
        } catch (Exception e) {
            res.json = new JSONObject();
        }
        return res;
    }

    private static void cleanupSessionData() {
        if (feedbackId > 0) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM feedback WHERE id = ?")) {
                stmt.setLong(1, feedbackId);
                stmt.executeUpdate();
            } catch (SQLException ignored) {}
        }
        if (sessionId > 0) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM session_history WHERE id = ?")) {
                stmt.setLong(1, sessionId);
                stmt.executeUpdate();
            } catch (SQLException ignored) {}
        }
        if (requestId > 0) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM mentorship_requests WHERE id = ?")) {
                stmt.setLong(1, requestId);
                stmt.executeUpdate();
            } catch (SQLException ignored) {}
        }
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
