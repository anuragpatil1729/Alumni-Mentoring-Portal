package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.MentorshipRequestDAO;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.model.MentorshipRequest;
import com.alumni.model.Student;
import com.alumni.model.User;
import com.alumni.server.HttpServerApp;
import com.alumni.service.MentorshipService;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MentorshipRequestTest {

    private static HttpServerApp server;
    private static int httpPort;
    private static RegistrationDAO registrationDAO;
    private static MentorshipRequestDAO requestDAO;
    private static MentorshipService mentorshipService;

    private static long studentId1;
    private static long studentId2;
    private static long mentorId1;
    private static long mentorIdCapacity;
    private static long nonAlumniUserId;

    private static long createdRequestId;

    @BeforeAll
    public static void setup() throws Exception {
        registrationDAO = new RegistrationDAO();
        requestDAO = new MentorshipRequestDAO();
        mentorshipService = new MentorshipService(requestDAO, registrationDAO);

        // 1. Student 1
        User u1 = new User();
        u1.setFullName("Req Student One");
        u1.setEmail("req_student1_" + System.currentTimeMillis() + "@test.com");
        u1.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        u1.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));
        Student s1 = new Student();
        s1.setStudentId("STU_R1_" + System.currentTimeMillis());
        s1.setDepartment("Computer Engineering");
        s1.setGraduationYear(2025);
        studentId1 = registrationDAO.registerStudent(u1, s1);

        // 2. Student 2
        User u2 = new User();
        u2.setFullName("Req Student Two");
        u2.setEmail("req_student2_" + System.currentTimeMillis() + "@test.com");
        u2.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        u2.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));
        Student s2 = new Student();
        s2.setStudentId("STU_R2_" + System.currentTimeMillis());
        s2.setDepartment("Information Technology");
        s2.setGraduationYear(2026);
        studentId2 = registrationDAO.registerStudent(u2, s2);

        // 3. Mentor 1 (normal capacity)
        User m1 = new User();
        m1.setFullName("Req Mentor One");
        m1.setEmail("req_mentor1_" + System.currentTimeMillis() + "@test.com");
        m1.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        m1.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));
        Alumni a1 = new Alumni();
        a1.setDepartment("Computer Engineering");
        a1.setGraduationYear(2018);
        a1.setCompany("Google");
        a1.setDesignation("Staff Engineer");
        a1.setIndustry("Information Technology");
        a1.setSkills("Java, Cloud, Architecture");
        a1.setMaxMentees(5);
        mentorId1 = registrationDAO.registerAlumni(m1, a1);

        // 4. Mentor 2 (capacity limit = 1)
        User m2 = new User();
        m2.setFullName("Req Mentor Capacity");
        m2.setEmail("req_mentor_cap_" + System.currentTimeMillis() + "@test.com");
        m2.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        m2.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));
        Alumni a2 = new Alumni();
        a2.setDepartment("Data Science & AI");
        a2.setGraduationYear(2019);
        a2.setCompany("Amazon");
        a2.setDesignation("Senior Scientist");
        a2.setIndustry("Information Technology");
        a2.setSkills("AI, ML");
        a2.setMaxMentees(1);
        mentorIdCapacity = registrationDAO.registerAlumni(m2, a2);

        nonAlumniUserId = studentId2;

        // 5. Start HTTP test server
        httpPort = 5059;
        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupUser(studentId1);
        cleanupUser(studentId2);
        cleanupUser(mentorId1);
        cleanupUser(mentorIdCapacity);
    }

    private static void cleanupUser(long userId) {
        if (userId <= 0) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // =========================================================================
    // Test 1: Successful request creation
    // =========================================================================
    @Test
    @Order(1)
    public void testCreateValidRequest() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "Hello mentor, I would love career advice regarding cloud engineering.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(201, result.getStatus());
        Assertions.assertTrue(result.getBody().getBoolean("success"));
        Assertions.assertTrue(result.getBody().has("request"));

        createdRequestId = result.getBody().getJSONObject("request").getLong("id");
        Assertions.assertTrue(createdRequestId > 0);
    }

    // =========================================================================
    // Test 2: Prevent duplicate active request (409 Conflict)
    // =========================================================================
    @Test
    @Order(2)
    public void testPreventDuplicateActiveRequest() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "tech");
        payload.put("message", "Second request attempt which should be rejected as duplicate.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(409, result.getStatus());
        Assertions.assertFalse(result.getBody().getBoolean("success"));
        Assertions.assertTrue(result.getBody().getString("message").contains("active mentorship request"));
    }

    // =========================================================================
    // Test 3: Self request prevention
    // =========================================================================
    @Test
    @Order(3)
    public void testPreventSelfRequest() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", studentId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "Trying to request mentorship from myself.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertFalse(result.getBody().getBoolean("success"));
        Assertions.assertTrue(result.getBody().getString("message").contains("yourself"));
    }

    // =========================================================================
    // Test 4: Prevent request to non-alumni user
    // =========================================================================
    @Test
    @Order(4)
    public void testPreventRequestToNonAlumni() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", nonAlumniUserId);
        payload.put("sessionGoal", "career");
        payload.put("message", "Trying to request mentorship from another student.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getString("message").contains("alumni mentor"));
    }

    // =========================================================================
    // Test 5: Prevent request from non-student user
    // =========================================================================
    @Test
    @Order(5)
    public void testPreventRequestFromNonStudent() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", mentorId1); // Alumni trying to request as student
        payload.put("mentorId", mentorIdCapacity);
        payload.put("sessionGoal", "career");
        payload.put("message", "Alumni trying to request as student.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getString("message").contains("registered students"));
    }

    // =========================================================================
    // Test 6: Non-existent student rejection
    // =========================================================================
    @Test
    @Order(6)
    public void testNonExistentStudentRejection() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", 9999999L);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "Non existent student message.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(404, result.getStatus());
        Assertions.assertTrue(result.getBody().getString("message").contains("Student user not found"));
    }

    // =========================================================================
    // Test 7: Non-existent mentor rejection
    // =========================================================================
    @Test
    @Order(7)
    public void testNonExistentMentorRejection() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", 9999999L);
        payload.put("sessionGoal", "career");
        payload.put("message", "Non existent mentor message.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(404, result.getStatus());
        Assertions.assertTrue(result.getBody().getString("message").contains("Mentor user not found"));
    }

    // =========================================================================
    // Test 8: Empty message validation
    // =========================================================================
    @Test
    @Order(8)
    public void testValidateEmptyMessage() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "   ");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getJSONObject("errors").has("message"));
    }

    // =========================================================================
    // Test 9: Short message validation (< 10 chars)
    // =========================================================================
    @Test
    @Order(9)
    public void testValidateShortMessage() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "Short");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getJSONObject("errors").has("message"));
    }

    // =========================================================================
    // Test 10: Long message validation (> 1000 chars)
    // =========================================================================
    @Test
    @Order(10)
    public void testValidateLongMessage() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "career");
        payload.put("message", "A".repeat(1005));

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getJSONObject("errors").has("message"));
    }

    // =========================================================================
    // Test 11: Missing session goal validation
    // =========================================================================
    @Test
    @Order(11)
    public void testValidateEmptySessionGoal() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "");
        payload.put("message", "Valid message length for testing.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getJSONObject("errors").has("sessionGoal"));
    }

    // =========================================================================
    // Test 12: Long session goal validation (> 100 chars)
    // =========================================================================
    @Test
    @Order(12)
    public void testValidateLongSessionGoal() {
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "G".repeat(105));
        payload.put("message", "Valid message length for testing.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertTrue(result.getBody().getJSONObject("errors").has("sessionGoal"));
    }

    // =========================================================================
    // Test 13: Get requests by student ID
    // =========================================================================
    @Test
    @Order(13)
    public void testGetRequestsByStudent() {
        MentorshipService.ProcessResult result = mentorshipService.getRequestsByStudent(studentId1);
        Assertions.assertEquals(200, result.getStatus());
        JSONArray arr = result.getBody().getJSONArray("requests");
        Assertions.assertTrue(arr.length() >= 1);

        JSONObject first = arr.getJSONObject(0);
        Assertions.assertEquals(studentId1, first.getLong("studentId"));
        Assertions.assertTrue(first.has("mentor"));
        Assertions.assertEquals("Google", first.getJSONObject("mentor").getString("company"));
    }

    // =========================================================================
    // Test 14: Get requests by mentor ID
    // =========================================================================
    @Test
    @Order(14)
    public void testGetRequestsByMentor() {
        MentorshipService.ProcessResult result = mentorshipService.getRequestsByMentor(mentorId1);
        Assertions.assertEquals(200, result.getStatus());
        JSONArray arr = result.getBody().getJSONArray("requests");
        Assertions.assertTrue(arr.length() >= 1);

        JSONObject first = arr.getJSONObject(0);
        Assertions.assertEquals(mentorId1, first.getLong("mentorId"));
        Assertions.assertTrue(first.has("student"));
        Assertions.assertEquals("Computer Engineering", first.getJSONObject("student").getString("department"));
    }

    // =========================================================================
    // Test 15: Mentor accepts request successfully
    // =========================================================================
    @Test
    @Order(15)
    public void testMentorAcceptRequest() {
        JSONObject updateData = new JSONObject();
        updateData.put("mentorId", mentorId1);
        updateData.put("status", "ACCEPTED");
        updateData.put("mentorResponse", "Glad to connect! Let's schedule a call.");

        MentorshipService.ProcessResult result = mentorshipService.updateRequestStatus(createdRequestId, updateData);
        Assertions.assertEquals(200, result.getStatus());
        Assertions.assertEquals("ACCEPTED", result.getBody().getJSONObject("request").getString("status"));
        Assertions.assertEquals("Glad to connect! Let's schedule a call.", result.getBody().getJSONObject("request").getString("mentorResponse"));
    }

    // =========================================================================
    // Test 16: Unauthorized user trying to accept/reject request (403)
    // =========================================================================
    @Test
    @Order(16)
    public void testUnauthorizedStatusUpdate() {
        JSONObject updateData = new JSONObject();
        updateData.put("mentorId", mentorIdCapacity); // Not the target mentor!
        updateData.put("status", "REJECTED");

        MentorshipService.ProcessResult result = mentorshipService.updateRequestStatus(createdRequestId, updateData);
        Assertions.assertEquals(403, result.getStatus());
        Assertions.assertTrue(result.getBody().getString("message").contains("not authorized"));
    }

    // =========================================================================
    // Test 17: Invalid status transition validation
    // =========================================================================
    @Test
    @Order(17)
    public void testInvalidStatusTransition() {
        JSONObject updateData = new JSONObject();
        updateData.put("mentorId", mentorId1);
        updateData.put("status", "INVALID_STATUS_STRING");

        MentorshipService.ProcessResult result = mentorshipService.updateRequestStatus(createdRequestId, updateData);
        Assertions.assertEquals(400, result.getStatus());
    }

    // =========================================================================
    // Test 18: Mentor rejects request with note
    // =========================================================================
    @Test
    @Order(18)
    public void testMentorRejectRequest() {
        JSONObject updateData = new JSONObject();
        updateData.put("mentorId", mentorId1);
        updateData.put("status", "REJECTED");
        updateData.put("mentorResponse", "Currently unavailable for new mentees.");

        MentorshipService.ProcessResult result = mentorshipService.updateRequestStatus(createdRequestId, updateData);
        Assertions.assertEquals(200, result.getStatus());
        Assertions.assertEquals("REJECTED", result.getBody().getJSONObject("request").getString("status"));
    }

    // =========================================================================
    // Test 19: Allow new request after previous was rejected
    // =========================================================================
    @Test
    @Order(19)
    public void testAllowNewRequestAfterRejection() {
        // Since createdRequestId is now REJECTED, studentId1 can send a new request to mentorId1!
        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId1);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "tech");
        payload.put("message", "Follow-up request after initial rejection.");

        MentorshipService.ProcessResult result = mentorshipService.createRequest(payload);
        Assertions.assertEquals(201, result.getStatus());
        Assertions.assertTrue(result.getBody().getBoolean("success"));
    }

    // =========================================================================
    // Test 20: Max mentee capacity enforcement (mentorIdCapacity max = 1)
    // =========================================================================
    @Test
    @Order(20)
    public void testMaxMenteeCapacityEnforcement() {
        // 1. Student 1 requests mentorIdCapacity
        JSONObject req1 = new JSONObject();
        req1.put("studentId", studentId1);
        req1.put("mentorId", mentorIdCapacity);
        req1.put("sessionGoal", "career");
        req1.put("message", "First mentee request to capacity mentor.");
        MentorshipService.ProcessResult res1 = mentorshipService.createRequest(req1);
        Assertions.assertEquals(201, res1.getStatus());
        long capReqId = res1.getBody().getJSONObject("request").getLong("id");

        // 2. Mentor accepts Student 1 (Capacity 1/1 reached!)
        JSONObject acceptPayload = new JSONObject();
        acceptPayload.put("mentorId", mentorIdCapacity);
        acceptPayload.put("status", "ACCEPTED");
        MentorshipService.ProcessResult acceptRes = mentorshipService.updateRequestStatus(capReqId, acceptPayload);
        Assertions.assertEquals(200, acceptRes.getStatus());

        // 3. Student 2 tries to request mentorIdCapacity -> must be rejected due to capacity limit
        JSONObject req2 = new JSONObject();
        req2.put("studentId", studentId2);
        req2.put("mentorId", mentorIdCapacity);
        req2.put("sessionGoal", "career");
        req2.put("message", "Second student trying to request full mentor.");
        MentorshipService.ProcessResult res2 = mentorshipService.createRequest(req2);
        Assertions.assertEquals(409, res2.getStatus());
        Assertions.assertTrue(res2.getBody().getString("message").contains("maximum mentee capacity"));
    }

    // =========================================================================
    // Test 21: HTTP POST /api/requests
    // =========================================================================
    @Test
    @Order(21)
    public void testHttpCreateRequestEndpoint() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/requests").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("studentId", studentId2);
        payload.put("mentorId", mentorId1);
        payload.put("sessionGoal", "resume");
        payload.put("message", "HTTP REST test request for resume review.");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        Assertions.assertEquals(201, conn.getResponseCode());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            Assertions.assertTrue(json.getBoolean("success"));
            Assertions.assertEquals("Mentorship request sent successfully!", json.getString("message"));
        }
    }

    // =========================================================================
    // Test 22: HTTP GET /api/requests?studentId=...
    // =========================================================================
    @Test
    @Order(22)
    public void testHttpGetRequestsEndpoint() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/requests?studentId=" + studentId2).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        Assertions.assertEquals(200, conn.getResponseCode());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            Assertions.assertTrue(json.getBoolean("success"));
            Assertions.assertTrue(json.getJSONArray("requests").length() >= 1);
        }
    }

    // =========================================================================
    // Test 23: HTTP PUT /api/requests?id=...
    // =========================================================================
    @Test
    @Order(23)
    public void testHttpPutRequestStatusEndpoint() throws Exception {
        // Fetch student 2's request ID
        List<MentorshipRequest> list = requestDAO.getRequestsByStudent(studentId2);
        Assertions.assertFalse(list.isEmpty());
        long reqId = list.get(0).getId();

        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/requests?id=" + reqId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("mentorId", mentorId1);
        payload.put("status", "ACCEPTED");
        payload.put("mentorResponse", "Accepted via HTTP REST endpoint test.");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        Assertions.assertEquals(200, conn.getResponseCode());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            Assertions.assertTrue(json.getBoolean("success"));
            Assertions.assertEquals("ACCEPTED", json.getJSONObject("request").getString("status"));
        }
    }
}
