package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.model.Student;
import com.alumni.model.User;
import com.alumni.server.HttpServerApp;
import com.alumni.service.RegistrationService;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProfileTest {

    private static HttpServerApp server;
    private static int httpPort;
    private static RegistrationDAO registrationDAO;
    private static RegistrationService registrationService;

    private static long studentUserId;
    private static long alumniUserId;
    private static long secondStudentUserId;

    @BeforeAll
    public static void setup() throws Exception {
        registrationDAO = new RegistrationDAO();
        registrationService = new RegistrationService(registrationDAO);

        // 1. Create a test student
        User sUser = new User();
        sUser.setFullName("Original Student Name");
        sUser.setEmail("profile_student_" + System.currentTimeMillis() + "@example.com");
        sUser.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        sUser.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));

        Student student = new Student();
        student.setStudentId("STU_P_" + System.currentTimeMillis());
        student.setDepartment("Computer Engineering");
        student.setGraduationYear(2025);

        studentUserId = registrationDAO.registerStudent(sUser, student);

        // 2. Create a second test student (for mobile collision test)
        User sUser2 = new User();
        sUser2.setFullName("Second Student");
        sUser2.setEmail("profile_student2_" + System.currentTimeMillis() + "@example.com");
        sUser2.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        sUser2.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));

        Student student2 = new Student();
        student2.setStudentId("STU_P2_" + System.currentTimeMillis());
        student2.setDepartment("Information Technology");
        student2.setGraduationYear(2026);

        secondStudentUserId = registrationDAO.registerStudent(sUser2, student2);

        // 3. Create a test alumni
        User aUser = new User();
        aUser.setFullName("Original Alumni Name");
        aUser.setEmail("profile_alumni_" + System.currentTimeMillis() + "@example.com");
        aUser.setMobileNumber("9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000))));
        aUser.setPasswordHash(RegistrationDAO.hashPassword("Password@123"));

        Alumni alumni = new Alumni();
        alumni.setDepartment("Computer Engineering");
        alumni.setGraduationYear(2020);
        alumni.setCompany("Original Corp");
        alumni.setDesignation("Software Engineer");
        alumni.setExperienceYears(3);
        alumni.setIndustry("Information Technology");
        alumni.setSkills("Java, SQL");
        alumni.setBio("Original bio.");
        alumni.setMaxMentees(2);

        alumniUserId = registrationDAO.registerAlumni(aUser, alumni);

        // 4. Start HTTP Server for Profile Endpoint tests
        httpPort = 5058;
        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
        cleanupUser(studentUserId);
        cleanupUser(secondStudentUserId);
        cleanupUser(alumniUserId);
    }

    private static void cleanupUser(long userId) {
        if (userId <= 0) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Test
    @Order(1)
    public void testGetStudentProfile() throws SQLException {
        JSONObject profile = registrationDAO.getUserProfile(studentUserId);
        Assertions.assertNotNull(profile);
        Assertions.assertEquals("Original Student Name", profile.getString("fullName"));
        Assertions.assertEquals("student", profile.getString("role"));
        Assertions.assertEquals("Computer Engineering", profile.getString("department"));
        Assertions.assertEquals(2025, profile.getInt("graduationYear"));
        Assertions.assertTrue(profile.has("studentId"));
    }

    @Test
    @Order(2)
    public void testGetAlumniProfile() throws SQLException {
        JSONObject profile = registrationDAO.getUserProfile(alumniUserId);
        Assertions.assertNotNull(profile);
        Assertions.assertEquals("Original Alumni Name", profile.getString("fullName"));
        Assertions.assertEquals("alumni", profile.getString("role"));
        Assertions.assertEquals("Original Corp", profile.getString("company"));
        Assertions.assertEquals("Software Engineer", profile.getString("designation"));
        Assertions.assertEquals(3, profile.getInt("experienceYears"));
        Assertions.assertEquals("Java, SQL", profile.getString("skills"));
    }

    @Test
    @Order(3)
    public void testUpdateStudentProfileSuccess() {
        String newMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));
        JSONObject updateData = new JSONObject();
        updateData.put("fullName", "Updated Student Name");
        updateData.put("mobileNumber", newMobile);
        updateData.put("department", "Data Science & AI");
        updateData.put("graduationYear", 2026);

        RegistrationService.ProcessResult result = registrationService.updateProfile(studentUserId, updateData);
        Assertions.assertEquals(200, result.getStatus());
        Assertions.assertTrue(result.getBody().getBoolean("success"));

        JSONObject updated = result.getBody().getJSONObject("profile");
        Assertions.assertEquals("Updated Student Name", updated.getString("fullName"));
        Assertions.assertEquals(newMobile, updated.getString("mobileNumber"));
        Assertions.assertEquals("Data Science & AI", updated.getString("department"));
        Assertions.assertEquals(2026, updated.getInt("graduationYear"));
    }

    @Test
    @Order(4)
    public void testUpdateAlumniProfileSuccess() {
        String newMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));
        JSONObject updateData = new JSONObject();
        updateData.put("fullName", "Updated Alumni Leader");
        updateData.put("mobileNumber", newMobile);
        updateData.put("department", "Electronics & Telecom");
        updateData.put("graduationYear", 2019);
        updateData.put("company", "Google");
        updateData.put("designation", "Staff Software Engineer");
        updateData.put("experienceYears", 6);
        updateData.put("industry", "Information Technology");
        updateData.put("skills", "Go, Distributed Systems, Cloud Architecture");
        updateData.put("bio", "Passionate about mentoring students in cloud infra and backend systems.");
        updateData.put("maxMentees", 5);
        updateData.put("linkedInProfile", "https://linkedin.com/in/updatedalumnileader");

        RegistrationService.ProcessResult result = registrationService.updateProfile(alumniUserId, updateData);
        Assertions.assertEquals(200, result.getStatus());
        Assertions.assertTrue(result.getBody().getBoolean("success"));

        JSONObject updated = result.getBody().getJSONObject("profile");
        Assertions.assertEquals("Updated Alumni Leader", updated.getString("fullName"));
        Assertions.assertEquals(newMobile, updated.getString("mobileNumber"));
        Assertions.assertEquals("Google", updated.getString("company"));
        Assertions.assertEquals("Staff Software Engineer", updated.getString("designation"));
        Assertions.assertEquals(6, updated.getInt("experienceYears"));
        Assertions.assertEquals(5, updated.getInt("maxMentees"));
        Assertions.assertEquals("https://linkedin.com/in/updatedalumnileader", updated.getString("linkedInProfile"));
    }

    @Test
    @Order(5)
    public void testMobileNumberCollisionFails() throws SQLException {
        // Fetch mobile of student 2
        JSONObject s2 = registrationDAO.getUserProfile(secondStudentUserId);
        String s2Mobile = s2.getString("mobileNumber");

        // Attempt to update student 1's mobile number to student 2's mobile number
        JSONObject updateData = new JSONObject();
        updateData.put("fullName", "Updated Student Name");
        updateData.put("mobileNumber", s2Mobile);
        updateData.put("department", "Computer Engineering");
        updateData.put("graduationYear", 2026);

        RegistrationService.ProcessResult result = registrationService.updateProfile(studentUserId, updateData);
        Assertions.assertEquals(409, result.getStatus());
        Assertions.assertFalse(result.getBody().getBoolean("success"));
        Assertions.assertTrue(result.getBody().getString("message").contains("mobile number"));
    }

    @Test
    @Order(6)
    public void testKeepOwnMobileNumberSucceeds() throws Exception {
        JSONObject current = registrationDAO.getUserProfile(studentUserId);
        String ownMobile = current.getString("mobileNumber");

        JSONObject updateData = new JSONObject();
        updateData.put("fullName", "Still My Own Mobile");
        updateData.put("mobileNumber", ownMobile);
        updateData.put("department", "Computer Engineering");
        updateData.put("graduationYear", 2026);

        RegistrationService.ProcessResult result = registrationService.updateProfile(studentUserId, updateData);
        Assertions.assertEquals(200, result.getStatus());
        Assertions.assertTrue(result.getBody().getBoolean("success"));
    }

    @Test
    @Order(7)
    public void testValidationFailsOnInvalidName() {
        JSONObject updateData = new JSONObject();
        updateData.put("fullName", "Name123WithNumbers");
        updateData.put("mobileNumber", "9876543210");
        updateData.put("department", "Computer Engineering");
        updateData.put("graduationYear", 2026);

        RegistrationService.ProcessResult result = registrationService.updateProfile(studentUserId, updateData);
        Assertions.assertEquals(400, result.getStatus());
        Assertions.assertFalse(result.getBody().getBoolean("success"));
    }

    @Test
    @Order(8)
    public void testHttpEndpointGetProfile() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/profile?id=" + studentUserId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assertions.assertEquals(200, conn.getResponseCode());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            Assertions.assertTrue(json.getBoolean("success"));
            Assertions.assertEquals(studentUserId, json.getJSONObject("profile").getLong("id"));
        }
    }

    @Test
    @Order(9)
    public void testHttpEndpointPutProfile() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/profile?id=" + alumniUserId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String uniqueMobile = "9" + String.format("%09d", Math.abs(new Random().nextInt(1_000_000_000)));
        JSONObject payload = new JSONObject();
        payload.put("fullName", "REST Updated Mentor");
        payload.put("mobileNumber", uniqueMobile);
        payload.put("department", "Electronics");
        payload.put("graduationYear", 2019);
        payload.put("company", "Meta");
        payload.put("designation", "Director of Engineering");
        payload.put("experienceYears", 8);
        payload.put("industry", "Information Technology");
        payload.put("skills", "Leadership, AI Systems");
        payload.put("bio", "Executive mentor.");
        payload.put("maxMentees", 10);

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
            Assertions.assertEquals("Meta", json.getJSONObject("profile").getString("company"));
            Assertions.assertEquals("Director of Engineering", json.getJSONObject("profile").getString("designation"));
        }
    }

    @Test
    @Order(10)
    public void testHttpEndpointNotFound() throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/profile?id=9999999").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assertions.assertEquals(404, conn.getResponseCode());
    }

    @Test
    @Order(11)
    public void testAvatarBlobDaoUpdateAndGet() throws Exception {
        byte[] testBytes = "FAKE_JPEG_BLOB_CONTENT_FOR_TESTING".getBytes(StandardCharsets.UTF_8);
        boolean saved = registrationDAO.updateUserAvatar(studentUserId, testBytes, "image/jpeg");
        Assertions.assertTrue(saved);

        RegistrationDAO.AvatarData avatar = registrationDAO.getUserAvatar(studentUserId);
        Assertions.assertNotNull(avatar);
        Assertions.assertArrayEquals(testBytes, avatar.getBytes());
        Assertions.assertEquals("image/jpeg", avatar.getMimeType());

        // Also verify profile includes avatarUrl
        JSONObject profile = registrationDAO.getUserProfile(studentUserId);
        Assertions.assertNotNull(profile);
        Assertions.assertEquals("/api/users/" + studentUserId + "/avatar", profile.getString("avatarUrl"));
    }

    @Test
    @Order(12)
    public void testHttpAvatarUploadAndStream() throws Exception {
        // Upload via POST /api/users/avatar using Base64 JSON
        byte[] rawImage = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }; // PNG header
        String b64 = java.util.Base64.getEncoder().encodeToString(rawImage);

        URL uploadUrl = URI.create("http://127.0.0.1:" + httpPort + "/api/users/avatar").toURL();
        HttpURLConnection upConn = (HttpURLConnection) uploadUrl.openConnection();
        upConn.setRequestMethod("POST");
        upConn.setDoOutput(true);
        upConn.setRequestProperty("Content-Type", "application/json");

        JSONObject uploadBody = new JSONObject();
        uploadBody.put("userId", alumniUserId);
        uploadBody.put("imageData", "data:image/png;base64," + b64);
        uploadBody.put("mimeType", "image/png");

        try (OutputStream os = upConn.getOutputStream()) {
            os.write(uploadBody.toString().getBytes(StandardCharsets.UTF_8));
        }
        Assertions.assertEquals(200, upConn.getResponseCode());

        // Stream via GET /api/users/{id}/avatar
        URL streamUrl = URI.create("http://127.0.0.1:" + httpPort + "/api/users/" + alumniUserId + "/avatar").toURL();
        HttpURLConnection streamConn = (HttpURLConnection) streamUrl.openConnection();
        streamConn.setRequestMethod("GET");
        Assertions.assertEquals(200, streamConn.getResponseCode());
        Assertions.assertEquals("image/png", streamConn.getContentType());

        byte[] receivedBytes = streamConn.getInputStream().readAllBytes();
        Assertions.assertArrayEquals(rawImage, receivedBytes);
    }

    @Test
    @Order(13)
    public void testHttpAvatarDelete() throws Exception {
        URL delUrl = URI.create("http://127.0.0.1:" + httpPort + "/api/users/avatar?userId=" + alumniUserId).toURL();
        HttpURLConnection delConn = (HttpURLConnection) delUrl.openConnection();
        delConn.setRequestMethod("DELETE");
        Assertions.assertEquals(200, delConn.getResponseCode());

        // Next GET should return 404
        URL streamUrl = URI.create("http://127.0.0.1:" + httpPort + "/api/users/" + alumniUserId + "/avatar").toURL();
        HttpURLConnection streamConn = (HttpURLConnection) streamUrl.openConnection();
        streamConn.setRequestMethod("GET");
        Assertions.assertEquals(404, streamConn.getResponseCode());
    }
}

