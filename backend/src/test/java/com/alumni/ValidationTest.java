package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.server.HttpServerApp;
import com.alumni.service.SocketServer;
import com.alumni.validation.InputValidator;
import com.alumni.validation.ValidationResult;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ValidationTest {

    private static HttpServerApp server;
    private static SocketServer socketServer;
    private static int httpPort;
    private static int socketPort;

    @BeforeAll
    public static void setup() throws Exception {
        Assertions.assertTrue(DBConnection.testConnection(), "MySQL connection must be established");

        // Start HTTP test server on dynamic/available port
        httpPort = 5055;
        server = new HttpServerApp(httpPort);
        server.start();

        socketPort = 5056;
        socketServer = new SocketServer(socketPort);
        socketServer.start();

        Thread.sleep(100);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) server.stop();
        if (socketServer != null) socketServer.stop();
    }

    // ==========================================
    // [1] Email Format Validation Tests
    // ==========================================
    @Test
    @Order(1)
    public void testValidEmails() {
        Assertions.assertTrue(InputValidator.isValidEmail("student@university.edu"));
        Assertions.assertTrue(InputValidator.isValidEmail("john.doe@company.com"));
        Assertions.assertTrue(InputValidator.isValidEmail("user+tag@domain.co.in"));
    }

    @Test
    @Order(2)
    public void testInvalidEmails() {
        Assertions.assertFalse(InputValidator.isValidEmail(""));
        Assertions.assertFalse(InputValidator.isValidEmail("not-an-email"));
        Assertions.assertFalse(InputValidator.isValidEmail("missing@domain"));
        Assertions.assertFalse(InputValidator.isValidEmail("@nodomain.com"));
    }

    // ==========================================
    // [2] Mobile Number Validation Tests
    // ==========================================
    @Test
    @Order(3)
    public void testValidMobileNumbers() {
        Assertions.assertTrue(InputValidator.isValidMobile("9876543210"));
        Assertions.assertTrue(InputValidator.isValidMobile("+919876543210"));
        Assertions.assertTrue(InputValidator.isValidMobile("8123456789"));
        Assertions.assertTrue(InputValidator.isValidMobile("7012345678"));
    }

    @Test
    @Order(4)
    public void testInvalidMobileNumbers() {
        Assertions.assertFalse(InputValidator.isValidMobile(""));
        Assertions.assertFalse(InputValidator.isValidMobile("12345"));
        Assertions.assertFalse(InputValidator.isValidMobile("5123456789")); // starts with 5
        Assertions.assertFalse(InputValidator.isValidMobile("abcdefghij"));
    }

    // ==========================================
    // [3] Password Criteria Validation Tests
    // ==========================================
    @Test
    @Order(5)
    public void testValidPasswords() {
        Assertions.assertTrue(InputValidator.isValidPassword("StrongP@ssw0rd"));
        Assertions.assertTrue(InputValidator.isValidPassword("C0mpl3x#P@ss!"));
        Assertions.assertTrue(InputValidator.isValidPassword("Secure123$Auth"));
    }

    @Test
    @Order(6)
    public void testWeakPasswords() {
        Assertions.assertFalse(InputValidator.isValidPassword("short")); // < 8 chars
        Assertions.assertFalse(InputValidator.isValidPassword("alllowercase1!"));
        Assertions.assertFalse(InputValidator.isValidPassword("ALLUPPERCASE1!"));
        Assertions.assertFalse(InputValidator.isValidPassword("NoSpecialChars123"));
        Assertions.assertFalse(InputValidator.isValidPassword("NoNumbers!Pass"));
    }

    // ==========================================
    // [4] Student Registration Field Validation Tests
    // ==========================================
    @Test
    @Order(7)
    public void testValidStudentRegistrationPayload() {
        JSONObject data = new JSONObject();
        data.put("fullName", "Rohan Verma");
        data.put("email", "rohan.verma@college.edu");
        data.put("mobileNumber", "9876543210");
        data.put("password", "Passw0rd!123");
        data.put("studentId", "CS2026001");
        data.put("department", "Computer Engineering");
        data.put("graduationYear", 2026);

        ValidationResult res = InputValidator.validateStudentRegistration(data);
        Assertions.assertTrue(res.isValid());
    }

    @Test
    @Order(8)
    public void testStudentRegistrationMissingFields() {
        JSONObject data = new JSONObject();
        ValidationResult res = InputValidator.validateStudentRegistration(data);

        Assertions.assertFalse(res.isValid());
        Assertions.assertTrue(res.getErrors().containsKey("fullName"));
        Assertions.assertTrue(res.getErrors().containsKey("email"));
        Assertions.assertTrue(res.getErrors().containsKey("mobileNumber"));
        Assertions.assertTrue(res.getErrors().containsKey("password"));
        Assertions.assertTrue(res.getErrors().containsKey("studentId"));
        Assertions.assertTrue(res.getErrors().containsKey("department"));
        Assertions.assertTrue(res.getErrors().containsKey("graduationYear"));
    }

    @Test
    @Order(9)
    public void testStudentRegistrationInvalidFormats() {
        JSONObject data = new JSONObject();
        data.put("fullName", "1"); // too short / numbers
        data.put("email", "invalid-email");
        data.put("mobileNumber", "123");
        data.put("password", "weak");
        data.put("studentId", "ab"); // too short (<3)
        data.put("department", "Computer");
        data.put("graduationYear", 1990); // too far in past for student

        ValidationResult res = InputValidator.validateStudentRegistration(data);
        Assertions.assertFalse(res.isValid());
        Assertions.assertEquals("Please provide a valid email address.", res.getErrors().get("email"));
        Assertions.assertTrue(res.getErrors().containsKey("password"));
        Assertions.assertTrue(res.getErrors().containsKey("studentId"));
    }

    // ==========================================
    // [5] Alumni Registration Field Validation Tests
    // ==========================================
    @Test
    @Order(10)
    public void testValidAlumniRegistrationPayload() {
        JSONObject data = new JSONObject();
        data.put("fullName", "Kavita Rao");
        data.put("email", "kavita.rao@techcorp.com");
        data.put("mobileNumber", "9876543210");
        data.put("password", "Passw0rd!123");
        data.put("department", "Computer Engineering");
        data.put("graduationYear", 2020);
        data.put("company", "TechCorp");
        data.put("designation", "Staff Engineer");
        data.put("linkedInProfile", "https://linkedin.com/in/kavita-rao");

        ValidationResult res = InputValidator.validateAlumniRegistration(data);
        Assertions.assertTrue(res.isValid());
    }

    @Test
    @Order(11)
    public void testAlumniRegistrationMissingFields() {
        JSONObject data = new JSONObject();
        ValidationResult res = InputValidator.validateAlumniRegistration(data);

        Assertions.assertFalse(res.isValid());
        Assertions.assertTrue(res.getErrors().containsKey("fullName"));
        Assertions.assertTrue(res.getErrors().containsKey("email"));
        Assertions.assertTrue(res.getErrors().containsKey("company"));
        Assertions.assertTrue(res.getErrors().containsKey("designation"));
    }

    @Test
    @Order(12)
    public void testAlumniRegistrationInvalidFields() {
        JSONObject data = new JSONObject();
        data.put("fullName", "Kavita Rao");
        data.put("email", "kavita.rao@techcorp.com");
        data.put("mobileNumber", "9876543210");
        data.put("password", "Passw0rd!123");
        data.put("department", "Computer Engineering");
        data.put("graduationYear", 2045); // Future year invalid for alumni
        data.put("company", "TechCorp");
        data.put("designation", "Staff Engineer");
        data.put("linkedInProfile", "not-a-linkedin-url");

        ValidationResult res = InputValidator.validateAlumniRegistration(data);
        Assertions.assertFalse(res.isValid());
        Assertions.assertTrue(res.getErrors().containsKey("graduationYear"));
        Assertions.assertTrue(res.getErrors().containsKey("linkedInProfile"));
    }

    // ==========================================
    // [6] HTTP API Endpoint Server Integration Tests
    // ==========================================
    @Test
    @Order(13)
    public void testPostRegisterStudentSuccess() throws Exception {
        String testEmail = "valid_student_" + System.currentTimeMillis() + "@college.edu";
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Aman Gupta");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543210");
        payload.put("password", "StrongP@ss1");
        payload.put("studentId", "STU" + System.currentTimeMillis());
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("id"));

        cleanupUser(res.json.getLong("id"));
    }

    @Test
    @Order(14)
    public void testPostRegisterStudentInvalid() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "invalid-email");

        HttpResult res = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("errors"));
    }

    @Test
    @Order(15)
    public void testPostRegisterAlumniSuccess() throws Exception {
        String testEmail = "valid_alumni_" + System.currentTimeMillis() + "@tech.com";
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Ritu Roy");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543211");
        payload.put("password", "StrongP@ss2");
        payload.put("department", "Information Technology");
        payload.put("graduationYear", 2021);
        payload.put("company", "Stripe");
        payload.put("designation", "Software Engineer");

        HttpResult res = sendHttp("POST", "/api/auth/register/alumni", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("id"));

        cleanupUser(res.json.getLong("id"));
    }

    @Test
    @Order(16)
    public void testPostRegisterAlumniInvalid() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Test User");

        HttpResult res = sendHttp("POST", "/api/auth/register/alumni", payload.toString(), "application/json");
        Assertions.assertEquals(400, res.status);
        Assertions.assertFalse(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.has("errors"));
    }

    @Test
    @Order(17)
    public void testUnifiedRegistrationRouting() throws Exception {
        String testEmail = "unified_student_" + System.currentTimeMillis() + "@college.edu";
        JSONObject payload = new JSONObject();
        payload.put("role", "student");
        payload.put("fullName", "Deepak Shah");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543212");
        payload.put("password", "StrongP@ss3");
        payload.put("studentId", "STU-U-" + (System.currentTimeMillis() % 1000000000L));
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/auth/register", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status);
        Assertions.assertEquals("student", res.json.getString("role"));

        cleanupUser(res.json.getLong("id"));
    }

    @Test
    @Order(18)
    public void testServletStyleRegistration() throws Exception {
        String testEmail = "servlet_test_" + System.currentTimeMillis() + "@college.edu";
        JSONObject payload = new JSONObject();
        payload.put("role", "student");
        payload.put("fullName", "Servlet User");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543213");
        payload.put("password", "ServletP@ss1");
        payload.put("studentId", "SRV" + System.currentTimeMillis());
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult res = sendHttp("POST", "/api/servlet/register", payload.toString(), "application/json");
        Assertions.assertEquals(201, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        cleanupUser(res.json.getLong("id"));
    }

    @Test
    @Order(19)
    public void testCgiStyleFormRegistration() throws Exception {
        String testEmail = "cgi_test_" + System.currentTimeMillis() + "@college.edu";
        String formBody = "role=student&fullName=CGI+Tester&email=" + testEmail +
                "&mobileNumber=9876543214&password=CgiStrongP%40ss1&studentId=CGI" + System.currentTimeMillis() +
                "&department=Computer+Engineering&graduationYear=2026";

        HttpResult res = sendHttp("POST", "/cgi-bin/register", formBody, "application/x-www-form-urlencoded");
        Assertions.assertEquals(201, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));

        cleanupUser(res.json.getLong("id"));
    }

    @Test
    @Order(20)
    public void testTcpSocketRegistration() throws Exception {
        String testEmail = "socket_test_" + System.currentTimeMillis() + "@college.edu";
        JSONObject payload = new JSONObject();
        payload.put("role", "student");
        payload.put("fullName", "Socket User");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543215");
        payload.put("password", "SocketP@ss1");
        payload.put("studentId", "SOCK" + System.currentTimeMillis());
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        try (Socket socket = new Socket("127.0.0.1", socketPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            writer.println(payload.toString());
            String responseLine = reader.readLine();
            Assertions.assertNotNull(responseLine);

            JSONObject resp = new JSONObject(responseLine);
            Assertions.assertTrue(resp.getBoolean("success"));
            Assertions.assertTrue(resp.has("id"));

            cleanupUser(resp.getLong("id"));
        }
    }

    @Test
    @Order(21)
    public void testGetRegistrationByEmail() throws Exception {
        String testEmail = "lookup_test_" + System.currentTimeMillis() + "@company.com";
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Maya Sen");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543216");
        payload.put("password", "StrongP@ss4");
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2020);
        payload.put("company", "Salesforce");
        payload.put("designation", "Lead Architect");

        HttpResult regRes = sendHttp("POST", "/api/auth/register/alumni", payload.toString(), "application/json");
        Assertions.assertEquals(201, regRes.status);
        long userId = regRes.json.getLong("id");

        HttpResult getRes = sendHttp("GET", "/api/auth/registrations/" + testEmail, null, null);
        Assertions.assertEquals(200, getRes.status);
        Assertions.assertTrue(getRes.json.getBoolean("success"));
        JSONObject regData = getRes.json.getJSONObject("registration");
        Assertions.assertEquals("Maya Sen", regData.getString("fullName"));
        Assertions.assertEquals("Salesforce", regData.getString("company"));
        Assertions.assertFalse(regData.has("passwordHash"), "Password hash should never be exposed");

        cleanupUser(userId);
    }

    @Test
    @Order(22)
    public void testLoginSuccessAndInvalid() throws Exception {
        // 1. Invalid login
        JSONObject badCreds = new JSONObject();
        badCreds.put("email", "nonexistent.user@example.com");
        badCreds.put("password", "WrongPass@123");
        HttpResult badRes = sendHttp("POST", "/api/auth/login", badCreds.toString(), "application/json");
        Assertions.assertEquals(401, badRes.status);
        Assertions.assertFalse(badRes.json.getBoolean("success"));

        // 2. Register a student and login
        String testEmail = "login_student_" + System.currentTimeMillis() + "@college.edu";
        JSONObject payload = new JSONObject();
        payload.put("fullName", "Login Tester");
        payload.put("email", testEmail);
        payload.put("mobileNumber", "9876543210");
        payload.put("password", "StrongP@ss1");
        payload.put("studentId", "STU" + System.currentTimeMillis());
        payload.put("department", "Computer Engineering");
        payload.put("graduationYear", 2026);

        HttpResult regRes = sendHttp("POST", "/api/auth/register/student", payload.toString(), "application/json");
        Assertions.assertEquals(201, regRes.status);
        long userId = regRes.json.getLong("id");

        // 3. Login with correct credentials
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("email", testEmail);
        loginPayload.put("password", "StrongP@ss1");
        HttpResult loginRes = sendHttp("POST", "/api/auth/login", loginPayload.toString(), "application/json");
        Assertions.assertEquals(200, loginRes.status);
        Assertions.assertTrue(loginRes.json.getBoolean("success"));
        Assertions.assertEquals("student", loginRes.json.getJSONObject("user").getString("role"));
        Assertions.assertEquals("Login Tester", loginRes.json.getJSONObject("user").getString("fullName"));

        cleanupUser(userId);
    }

    @Test
    @Order(23)
    public void testFullNameValidationCases() {
        // Valid names
        Assertions.assertTrue(InputValidator.isValidFullName("Aarav Mehta"));
        Assertions.assertTrue(InputValidator.isValidFullName("Priya Sharma"));
        Assertions.assertTrue(InputValidator.isValidFullName("J. Doe"));
        Assertions.assertTrue(InputValidator.isValidFullName("Mary-Jane Watson"));
        Assertions.assertTrue(InputValidator.isValidFullName("Dr. Bruce Banner"));

        // Invalid names
        Assertions.assertFalse(InputValidator.isValidFullName(null));
        Assertions.assertFalse(InputValidator.isValidFullName(""));
        Assertions.assertFalse(InputValidator.isValidFullName("   "));
        Assertions.assertFalse(InputValidator.isValidFullName("A")); // Too short
        Assertions.assertFalse(InputValidator.isValidFullName("12345")); // Numeric
        Assertions.assertFalse(InputValidator.isValidFullName("Aarav123")); // Contains digits
        Assertions.assertFalse(InputValidator.isValidFullName("@Alex!")); // Special chars
        Assertions.assertFalse(InputValidator.isValidFullName("Aarav   Mehta")); // Double spaces
        Assertions.assertFalse(InputValidator.isValidFullName("A".repeat(55))); // Too long
    }

    @Test
    @Order(24)
    public void testPasswordStrengthEvaluator() {
        // Empty
        InputValidator.PasswordStrength psEmpty = InputValidator.evaluatePasswordStrength("");
        Assertions.assertEquals(0, psEmpty.getScore());
        Assertions.assertEquals("Very Weak", psEmpty.getLevel());
        Assertions.assertFalse(psEmpty.isHasMinLength());

        // Weak (too short, missing criteria)
        InputValidator.PasswordStrength psShort = InputValidator.evaluatePasswordStrength("Ab1!");
        Assertions.assertEquals(1, psShort.getScore());
        Assertions.assertFalse(psShort.isHasMinLength());

        // Missing uppercase
        InputValidator.PasswordStrength psNoUpper = InputValidator.evaluatePasswordStrength("password123!");
        Assertions.assertFalse(psNoUpper.isHasUpperCase());
        Assertions.assertTrue(psNoUpper.isHasMinLength());
        Assertions.assertTrue(psNoUpper.isHasLowerCase());
        Assertions.assertTrue(psNoUpper.isHasDigit());
        Assertions.assertTrue(psNoUpper.isHasSpecialChar());
        Assertions.assertEquals(2, psNoUpper.getScore()); // Fair

        // Missing special char
        InputValidator.PasswordStrength psNoSpecial = InputValidator.evaluatePasswordStrength("Password123");
        Assertions.assertFalse(psNoSpecial.isHasSpecialChar());
        Assertions.assertFalse(InputValidator.isValidPassword("Password123"));

        // Good (all 5 criteria, 8-11 chars)
        InputValidator.PasswordStrength psGood = InputValidator.evaluatePasswordStrength("Passw0rd!");
        Assertions.assertTrue(psGood.isHasMinLength());
        Assertions.assertTrue(psGood.isHasUpperCase());
        Assertions.assertTrue(psGood.isHasLowerCase());
        Assertions.assertTrue(psGood.isHasDigit());
        Assertions.assertTrue(psGood.isHasSpecialChar());
        Assertions.assertEquals(3, psGood.getScore());
        Assertions.assertEquals("Good", psGood.getLevel());
        Assertions.assertTrue(InputValidator.isValidPassword("Passw0rd!"));

        // Strong (all 5 criteria, >= 12 chars)
        InputValidator.PasswordStrength psStrong = InputValidator.evaluatePasswordStrength("Sup3r$ecureP@ssword2026");
        Assertions.assertEquals(4, psStrong.getScore());
        Assertions.assertEquals("Strong", psStrong.getLevel());
        Assertions.assertTrue(InputValidator.isValidPassword("Sup3r$ecureP@ssword2026"));
    }

    @Test
    @Order(25)
    public void testHttpPasswordStrengthEndpoint() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("password", "Sup3r$tr0ngP@ss1");

        HttpResult res = sendHttp("POST", "/api/auth/password-strength", payload.toString(), "application/json");
        Assertions.assertEquals(200, res.status);
        Assertions.assertTrue(res.json.getBoolean("success"));
        Assertions.assertTrue(res.json.getBoolean("valid"));
        Assertions.assertEquals("Strong", res.json.getJSONObject("strength").getString("level"));
        Assertions.assertEquals(4, res.json.getJSONObject("strength").getInt("score"));
    }

    private static class HttpResult {
        int status;
        JSONObject json;
        String raw;
    }

    private HttpResult sendHttp(String method, String path, String body, String contentType) throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
