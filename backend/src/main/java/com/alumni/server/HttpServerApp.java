package com.alumni.server;

import com.alumni.dao.AlumniDAO;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.search.SearchAlgorithms;
import com.alumni.search.SearchResult;
import com.alumni.service.MentorshipService;
import com.alumni.service.RegistrationService;
import com.alumni.validation.InputValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class HttpServerApp {

    private final int port;
    private HttpServer server;
    private final RegistrationService registrationService;
    private final RegistrationDAO registrationDAO;
    private final AlumniDAO alumniDAO;
    private final MentorshipService mentorshipService;

    public HttpServerApp(int port) {
        this.port = port;
        this.registrationDAO = new RegistrationDAO();
        this.alumniDAO = new AlumniDAO();
        this.registrationService = new RegistrationService(this.registrationDAO);
        this.mentorshipService = new MentorshipService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Root & Health Check
        server.createContext("/", new RootHandler());
        server.createContext("/api/health", new HealthHandler());

        // Auth & Registrations
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/password-strength", new PasswordStrengthHandler());
        server.createContext("/api/auth/check-availability", new AvailabilityCheckHandler());
        server.createContext("/api/auth/register/student", new StudentRegisterHandler());
        server.createContext("/api/auth/register/alumni", new AlumniRegisterHandler());
        server.createContext("/api/auth/register", new UnifiedRegisterHandler());
        server.createContext("/api/auth/registrations", new RegistrationLookupHandler());

        // Servlet & CGI style endpoints
        server.createContext("/api/servlet/register", new UnifiedRegisterHandler());
        server.createContext("/cgi-bin/register", new UnifiedRegisterHandler());

        // Mentors & Search
        server.createContext("/api/mentors/search", new MentorSearchHandler());
        server.createContext("/api/mentors", new MentorsHandler());

        // User Profile View & Edit
        server.createContext("/api/profile", new ProfileHandler());

        // Avatar Image Streaming & Upload (MySQL MEDIUMBLOB - Approach 2)
        AvatarHandler avatarHandler = new AvatarHandler();
        server.createContext("/api/users/avatar", avatarHandler);
        server.createContext("/api/users", avatarHandler);

        // Mentorship Requests (Week 5)
        MentorshipRequestHandler requestHandler = new MentorshipRequestHandler();
        server.createContext("/api/requests", requestHandler);

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int getPort() {
        return (server != null) ? server.getAddress().getPort() : port;
    }

    // Root handler
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"/".equals(exchange.getRequestURI().getPath())) {
                // Ignore 404 for unhandled root prefixes
                HttpUtils.sendErrorResponse(exchange, 404, "Not Found");
                return;
            }

            JSONObject json = new JSONObject();
            json.put("message", "Alumni Mentoring Portal Backend API is running (Java OpenJDK JDBC Engine)");
            json.put("status", "OK");
            json.put("frontendUrl", "http://localhost:5173");
            json.put("healthCheck", "/api/health");

            HttpUtils.sendJsonResponse(exchange, 200, json.toString());
        }
    }

    // Health check handler
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            JSONObject json = new JSONObject();
            json.put("status", "OK");
            json.put("timestamp", Instant.now().toString());

            HttpUtils.sendJsonResponse(exchange, 200, json.toString());
        }
    }

    // User Login: POST /api/auth/login
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                JSONObject payload = HttpUtils.parseBodyAsJson(exchange);
                String email = payload.optString("email", "").trim();
                String password = payload.optString("password", "");

                if (email.isEmpty() || password.isEmpty()) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Email and password are required.");
                    HttpUtils.sendJsonResponse(exchange, 400, err.toString());
                    return;
                }

                JSONObject user = registrationDAO.authenticateUser(email, password);
                if (user != null) {
                    JSONObject resp = new JSONObject();
                    resp.put("success", true);
                    resp.put("message", "Login successful!");
                    resp.put("user", user);
                    HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                } else {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Invalid email or password.");
                    HttpUtils.sendJsonResponse(exchange, 401, err.toString());
                }
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Authentication failed: " + e.getMessage());
            }
        }
    }

    // Password Strength Evaluator: POST or GET /api/auth/password-strength
    private class PasswordStrengthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String password = "";
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    password = body.optString("password", "");
                } catch (Exception e) {
                    password = "";
                }
            } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> qp = HttpUtils.parseQueryParams(exchange);
                password = qp.getOrDefault("password", "");
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            InputValidator.PasswordStrength ps = InputValidator.evaluatePasswordStrength(password);
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("valid", InputValidator.isValidPassword(password));
            resp.put("strength", ps.toJsonObject());

            HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
        }
    }

    // Availability Check for Email and Mobile: GET or POST /api/auth/check-availability
    private class AvailabilityCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String email = null;
                String mobile = null;

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=", 2);
                            if (pair.length == 2) {
                                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8).trim();
                                String val = URLDecoder.decode(pair[1], StandardCharsets.UTF_8).trim();
                                if ("email".equalsIgnoreCase(key)) email = val;
                                if ("mobileNumber".equalsIgnoreCase(key) || "mobile".equalsIgnoreCase(key) || "phone".equalsIgnoreCase(key)) mobile = val;
                            }
                        }
                    }
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    email = body.optString("email", null);
                    mobile = body.optString("mobileNumber", body.optString("mobile", body.optString("phone", null)));
                } else {
                    HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                JSONObject resp = new JSONObject();
                resp.put("success", true);

                if (email != null && !email.trim().isEmpty()) {
                    boolean emailInUse = registrationDAO.existsByEmail(email.trim());
                    resp.put("email", email.trim());
                    resp.put("emailAvailable", !emailInUse);
                    resp.put("emailMessage", emailInUse ? "This email address is already in use." : "Email is available.");
                }

                if (mobile != null && !mobile.trim().isEmpty()) {
                    boolean mobileInUse = registrationDAO.existsByMobileNumber(mobile.trim());
                    resp.put("mobileNumber", mobile.trim());
                    resp.put("mobileAvailable", !mobileInUse);
                    resp.put("mobileMessage", mobileInUse ? "This mobile number is already in use." : "Mobile number is available.");
                }

                HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
            } catch (Exception e) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Error checking availability: " + e.getMessage());
                HttpUtils.sendJsonResponse(exchange, 500, err.toString());
            }
        }
    }

    // Student Registration
    private class StudentRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                JSONObject payload = HttpUtils.parseBodyAsJson(exchange);
                RegistrationService.ProcessResult result = registrationService.processStudentRegistration(payload);
                HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 400, "Invalid JSON payload: " + e.getMessage());
            }
        }
    }

    // Alumni Registration
    private class AlumniRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                JSONObject payload = HttpUtils.parseBodyAsJson(exchange);
                RegistrationService.ProcessResult result = registrationService.processAlumniRegistration(payload);
                HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 400, "Invalid JSON payload: " + e.getMessage());
            }
        }
    }

    // Unified Registration (also handles /api/servlet/register and /cgi-bin/register)
    private class UnifiedRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                JSONObject payload = HttpUtils.parseBodyAsJson(exchange);
                RegistrationService.ProcessResult result = registrationService.processUnifiedRegistration(payload);
                HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 400, "Invalid registration payload: " + e.getMessage());
            }
        }
    }

    // Registration Lookup: GET /api/auth/registrations/:email
    private class RegistrationLookupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String prefix = "/api/auth/registrations/";
            if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
                HttpUtils.sendErrorResponse(exchange, 400, "Email parameter missing");
                return;
            }

            String email = URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8).trim();
            try {
                JSONObject reg = registrationDAO.findRegistrationByEmail(email);
                if (reg != null) {
                    JSONObject resp = new JSONObject();
                    resp.put("success", true);
                    resp.put("registration", reg);
                    HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                } else {
                    HttpUtils.sendErrorResponse(exchange, 404, "Registration not found");
                }
            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Database lookup failed: " + e.getMessage());
            }
        }
    }

    // Mentor Search Handler: GET /api/mentors/search?query=...&algorithm=linear|binary&key=...
    private class MentorSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, String> qp = HttpUtils.parseQueryParams(exchange);
                String query = qp.getOrDefault("query", "");
                String algorithm = qp.getOrDefault("algorithm", "linear").toLowerCase().trim();
                String key = qp.getOrDefault("key", "fullName");
                boolean exact = "true".equalsIgnoreCase(qp.get("exact")) || "1".equals(qp.get("exact"));

                String deptFilter = qp.get("department");
                String indFilter = qp.get("industry");
                String compFilter = qp.get("company");
                String minExpStr = qp.get("minExperience");
                String maxExpStr = qp.get("maxExperience");
                String gradYrStr = qp.get("graduationYear");

                List<Alumni> dataset = alumniDAO.getAllAlumni();

                SearchResult sr;
                if ("binary".equals(algorithm)) {
                    sr = SearchAlgorithms.manualBinarySearch(dataset, query, key, exact);
                } else {
                    sr = SearchAlgorithms.manualLinearSearch(dataset, query, new String[] {
                            "fullName", "company", "designation", "skills", "department", "industry"
                    });
                }

                List<Alumni> filtered = new ArrayList<>();
                for (Alumni a : sr.getResults()) {
                    boolean keep = true;

                    if (deptFilter != null && !deptFilter.isEmpty() && !"All".equalsIgnoreCase(deptFilter)) {
                        if (a.getDepartment() == null || !a.getDepartment().toLowerCase().contains(deptFilter.toLowerCase())) {
                            keep = false;
                        }
                    }

                    if (indFilter != null && !indFilter.isEmpty() && !"All".equalsIgnoreCase(indFilter)) {
                        if (a.getIndustry() == null || !a.getIndustry().toLowerCase().contains(indFilter.toLowerCase())) {
                            keep = false;
                        }
                    }

                    if (compFilter != null && !compFilter.isEmpty() && !"All".equalsIgnoreCase(compFilter)) {
                        if (a.getCompany() == null || !a.getCompany().toLowerCase().contains(compFilter.toLowerCase())) {
                            keep = false;
                        }
                    }

                    if (minExpStr != null && !minExpStr.isEmpty()) {
                        try {
                            int minE = Integer.parseInt(minExpStr.trim());
                            int exp = (a.getExperienceYears() != null) ? a.getExperienceYears() : 0;
                            if (exp < minE) keep = false;
                        } catch (NumberFormatException ignored) {}
                    }

                    if (maxExpStr != null && !maxExpStr.isEmpty()) {
                        try {
                            int maxE = Integer.parseInt(maxExpStr.trim());
                            int exp = (a.getExperienceYears() != null) ? a.getExperienceYears() : 0;
                            if (exp > maxE) keep = false;
                        } catch (NumberFormatException ignored) {}
                    }

                    if (gradYrStr != null && !gradYrStr.isEmpty() && !"All".equalsIgnoreCase(gradYrStr)) {
                        try {
                            int gy = Integer.parseInt(gradYrStr.trim());
                            if (a.getGraduationYear() == null || a.getGraduationYear() != gy) {
                                keep = false;
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    if (keep) {
                        filtered.add(a);
                    }
                }

                JSONArray arr = new JSONArray();
                for (Alumni a : filtered) {
                    JSONObject item = new JSONObject();
                    item.put("id", a.getUserId());
                    item.put("fullName", a.getFullName());
                    item.put("email", a.getEmail());
                    item.put("mobileNumber", a.getMobileNumber());
                    item.put("role", "alumni");
                    item.put("department", a.getDepartment());
                    item.put("graduationYear", a.getGraduationYear());
                    item.put("company", a.getCompany());
                    item.put("designation", a.getDesignation());
                    item.put("linkedin_profile", a.getLinkedInProfile());
                    item.put("linkedInProfile", a.getLinkedInProfile());
                    item.put("experience_years", a.getExperienceYears());
                    item.put("experienceYears", a.getExperienceYears());
                    item.put("industry", a.getIndustry());
                    item.put("skills", a.getSkills());
                    item.put("bio", a.getBio());
                    item.put("max_mentees", a.getMaxMentees());
                    item.put("maxMentees", a.getMaxMentees());
                    item.put("avatarUrl", a.getAvatarUrl() != null ? a.getAvatarUrl() : JSONObject.NULL);
                    arr.put(item);
                }

                JSONObject metrics = new JSONObject();
                metrics.put("totalRecords", dataset.size());
                metrics.put("resultsCount", filtered.size());
                metrics.put("comparisons", sr.getComparisons());
                metrics.put("executionTimeMs", Math.round(sr.getExecutionTimeMs() * 10000.0) / 10000.0);

                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("query", query);
                resp.put("algorithm", "binary".equals(algorithm) ? "binary" : "linear");
                resp.put("searchKey", "binary".equals(algorithm) ? key : "multi-field");
                resp.put("metrics", metrics);
                resp.put("data", arr);

                HttpUtils.sendJsonResponse(exchange, 200, resp.toString());

            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Search error: " + e.getMessage());
            }
        }
    }

    // Mentors list or single mentor by ID
    private class MentorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String prefix = "/api/mentors";

            if (path.length() > prefix.length() && path.charAt(prefix.length()) == '/') {
                String idStr = path.substring(prefix.length() + 1).trim();
                try {
                    long id = Long.parseLong(idStr);
                    Alumni m = alumniDAO.findById(id);
                    if (m != null) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", m.getUserId());
                        obj.put("fullName", m.getFullName());
                        obj.put("email", m.getEmail());
                        obj.put("company", m.getCompany());
                        obj.put("designation", m.getDesignation());
                        obj.put("department", m.getDepartment());
                        obj.put("graduationYear", m.getGraduationYear());
                        obj.put("experienceYears", m.getExperienceYears());
                        obj.put("skills", m.getSkills());
                        obj.put("industry", m.getIndustry());
                        obj.put("bio", m.getBio());
                        obj.put("linkedInProfile", m.getLinkedInProfile());
                        obj.put("avatarUrl", m.getAvatarUrl() != null ? m.getAvatarUrl() : JSONObject.NULL);

                        JSONObject resp = new JSONObject();
                        resp.put("success", true);
                        resp.put("data", obj);
                        HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                    } else {
                        HttpUtils.sendErrorResponse(exchange, 404, "Mentor with ID " + idStr + " not found.");
                    }
                } catch (NumberFormatException e) {
                    HttpUtils.sendErrorResponse(exchange, 404, "Invalid mentor ID");
                } catch (SQLException e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Database error: " + e.getMessage());
                }
                return;
            }

            try {
                List<Alumni> list = alumniDAO.getAllAlumni();
                JSONArray arr = new JSONArray();
                for (Alumni a : list) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", a.getUserId());
                    obj.put("fullName", a.getFullName());
                    obj.put("email", a.getEmail());
                    obj.put("company", a.getCompany());
                    obj.put("designation", a.getDesignation());
                    obj.put("department", a.getDepartment());
                    obj.put("graduationYear", a.getGraduationYear());
                    obj.put("experienceYears", a.getExperienceYears());
                    obj.put("skills", a.getSkills());
                    obj.put("industry", a.getIndustry());
                    obj.put("bio", a.getBio());
                    obj.put("linkedInProfile", a.getLinkedInProfile());
                    obj.put("avatarUrl", a.getAvatarUrl() != null ? a.getAvatarUrl() : JSONObject.NULL);
                    arr.put(obj);
                }

                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("count", list.size());
                resp.put("data", arr);
                HttpUtils.sendJsonResponse(exchange, 200, resp.toString());

            } catch (Exception e) {
                HttpUtils.sendErrorResponse(exchange, 500, "Database error: " + e.getMessage());
            }
        }
    }

    // Profile Handler: GET /api/profile?id=... or PUT/POST /api/profile
    private class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String method = exchange.getRequestMethod().toUpperCase();
            Map<String, String> qp = HttpUtils.parseQueryParams(exchange);

            if ("GET".equals(method)) {
                String idStr = qp.get("id");
                if (idStr == null || idStr.isEmpty()) {
                    idStr = qp.get("userId");
                }
                if (idStr == null || idStr.isEmpty()) {
                    String path = exchange.getRequestURI().getPath();
                    String prefix = "/api/profile/";
                    if (path.startsWith(prefix) && path.length() > prefix.length()) {
                        idStr = path.substring(prefix.length()).trim();
                    }
                }

                if (idStr == null || idStr.isEmpty()) {
                    HttpUtils.sendErrorResponse(exchange, 400, "User ID query parameter 'id' is required.");
                    return;
                }

                try {
                    long userId = Long.parseLong(idStr.trim());
                    RegistrationService.ProcessResult result = registrationService.getProfile(userId);
                    HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());
                } catch (NumberFormatException e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid user ID format.");
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Error fetching profile: " + e.getMessage());
                }

            } else if ("PUT".equals(method) || "POST".equals(method)) {
                try {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    String idStr = qp.get("id");
                    if (idStr == null || idStr.isEmpty()) {
                        idStr = qp.get("userId");
                    }
                    if (idStr == null || idStr.isEmpty()) {
                        idStr = body.optString("id", body.optString("userId", null));
                    }

                    if (idStr == null || idStr.trim().isEmpty()) {
                        HttpUtils.sendErrorResponse(exchange, 400, "User ID is required for profile update.");
                        return;
                    }

                    long userId = Long.parseLong(idStr.trim());
                    RegistrationService.ProcessResult result = registrationService.updateProfile(userId, body);
                    HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());

                } catch (NumberFormatException e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid user ID format.");
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid profile payload: " + e.getMessage());
                }
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Avatar handler for MySQL MEDIUMBLOB storage (Approach 2)
    private class AvatarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            Map<String, String> qp = HttpUtils.parseQueryParams(exchange);

            // Extract userId
            Long userId = null;
            // Case 1: /api/users/{id}/avatar
            if (path.matches("^/api/users/\\d+/avatar/?$")) {
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    try {
                        userId = Long.parseLong(parts[3]);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Case 2: query param ?userId=... or ?id=...
            if (userId == null) {
                String idStr = qp.get("userId");
                if (idStr == null || idStr.isEmpty()) {
                    idStr = qp.get("id");
                }
                if (idStr != null && !idStr.isEmpty()) {
                    try {
                        userId = Long.parseLong(idStr.trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            if ("GET".equals(method) || "HEAD".equals(method)) {
                if (userId == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "User ID is required to fetch avatar.");
                    return;
                }

                try {
                    RegistrationDAO.AvatarData avatar = registrationDAO.getUserAvatar(userId);
                    if (avatar == null || avatar.getBytes() == null || avatar.getBytes().length == 0) {
                        HttpUtils.sendErrorResponse(exchange, 404, "Avatar not found for user ID " + userId);
                        return;
                    }

                    exchange.getResponseHeaders().set("Content-Type", avatar.getMimeType());
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                    byte[] bytes = avatar.getBytes();
                    if ("HEAD".equals(method)) {
                        exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                        exchange.sendResponseHeaders(200, -1);
                        return;
                    }
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                        os.flush();
                    }
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Error retrieving avatar: " + e.getMessage());
                }
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                try {
                    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    byte[] imageBytes = null;
                    String mimeType = "image/jpeg";

                    if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                        JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                        if (userId == null) {
                            if (body.has("userId")) userId = body.getLong("userId");
                            else if (body.has("id")) userId = body.getLong("id");
                        }

                        String rawData = body.optString("imageData", body.optString("image", ""));
                        if (rawData.isEmpty()) {
                            HttpUtils.sendErrorResponse(exchange, 400, "Missing imageData parameter.");
                            return;
                        }

                        if (rawData.contains(",")) {
                            // Extract mime type if present in data URL
                            if (rawData.startsWith("data:")) {
                                int semiIdx = rawData.indexOf(';');
                                if (semiIdx > 5) {
                                    mimeType = rawData.substring(5, semiIdx);
                                }
                            }
                            rawData = rawData.substring(rawData.indexOf(",") + 1);
                        }

                        if (body.has("mimeType") && !body.getString("mimeType").trim().isEmpty()) {
                            mimeType = body.getString("mimeType").trim();
                        }

                        imageBytes = Base64.getDecoder().decode(rawData.trim());
                    } else {
                        // Binary stream upload
                        if (contentType != null && !contentType.isEmpty()) {
                            mimeType = contentType.split(";")[0].trim();
                        }
                        try (java.io.InputStream is = exchange.getRequestBody()) {
                            imageBytes = is.readAllBytes();
                        }
                    }

                    if (userId == null) {
                        HttpUtils.sendErrorResponse(exchange, 400, "User ID is required for avatar upload.");
                        return;
                    }

                    if (imageBytes == null || imageBytes.length == 0) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Image data cannot be empty.");
                        return;
                    }

                    // Validate max size 5MB
                    if (imageBytes.length > 5 * 1024 * 1024) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Avatar image exceeds maximum allowed size (5MB).");
                        return;
                    }

                    boolean updated = registrationDAO.updateUserAvatar(userId, imageBytes, mimeType);
                    if (!updated) {
                        HttpUtils.sendErrorResponse(exchange, 404, "User not found or avatar update failed.");
                        return;
                    }

                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("message", "Avatar updated successfully in MySQL BLOB.");
                    res.put("userId", userId);
                    res.put("avatarUrl", "/api/users/" + userId + "/avatar");
                    res.put("sizeBytes", imageBytes.length);
                    res.put("mimeType", mimeType);
                    HttpUtils.sendJsonResponse(exchange, 200, res.toString());

                } catch (IllegalArgumentException e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid base64 image data: " + e.getMessage());
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Error updating avatar: " + e.getMessage());
                }
            } else if ("DELETE".equals(method)) {
                if (userId == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "User ID is required to remove avatar.");
                    return;
                }

                try {
                    boolean deleted = registrationDAO.deleteUserAvatar(userId);
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("deleted", deleted);
                    res.put("message", deleted ? "Avatar removed successfully." : "No avatar found to remove.");
                    HttpUtils.sendJsonResponse(exchange, 200, res.toString());
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Error deleting avatar: " + e.getMessage());
                }
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Mentorship Request Handler (Week 5)
    private class MentorshipRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> qp = HttpUtils.parseQueryParams(exchange);
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                String studentIdStr = qp.get("studentId");
                if (studentIdStr == null || studentIdStr.isEmpty()) studentIdStr = qp.get("student_id");

                String mentorIdStr = qp.get("mentorId");
                if (mentorIdStr == null || mentorIdStr.isEmpty()) mentorIdStr = qp.get("mentor_id");

                String idStr = qp.get("id");

                try {
                    if (studentIdStr != null && !studentIdStr.isEmpty()) {
                        long studentId = Long.parseLong(studentIdStr.trim());
                        MentorshipService.ProcessResult res = mentorshipService.getRequestsByStudent(studentId);
                        HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                    } else if (mentorIdStr != null && !mentorIdStr.isEmpty()) {
                        long mentorId = Long.parseLong(mentorIdStr.trim());
                        MentorshipService.ProcessResult res = mentorshipService.getRequestsByMentor(mentorId);
                        HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                    } else if (idStr != null && !idStr.isEmpty()) {
                        long reqId = Long.parseLong(idStr.trim());
                        com.alumni.model.MentorshipRequest req = new com.alumni.dao.MentorshipRequestDAO().getRequestById(reqId);
                        if (req != null) {
                            JSONObject resp = new JSONObject();
                            resp.put("success", true);
                            resp.put("request", req.toJson());
                            HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                        } else {
                            HttpUtils.sendErrorResponse(exchange, 404, "Mentorship request not found.");
                        }
                    } else {
                        HttpUtils.sendErrorResponse(exchange, 400, "Query parameter 'studentId', 'mentorId', or 'id' is required.");
                    }
                } catch (NumberFormatException e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid ID parameter format.");
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 500, "Error retrieving requests: " + e.getMessage());
                }

            } else if ("POST".equals(method)) {
                try {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    MentorshipService.ProcessResult res = mentorshipService.createRequest(body);
                    HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid request payload: " + e.getMessage());
                }

            } else if ("PUT".equals(method)) {
                try {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    String idStr = qp.get("id");
                    if (idStr == null || idStr.isEmpty()) {
                        idStr = qp.get("requestId");
                    }
                    if (idStr == null || idStr.isEmpty()) {
                        idStr = body.optString("id", body.optString("requestId", null));
                    }
                    if (idStr == null || idStr.trim().isEmpty()) {
                        String prefix = "/api/requests/";
                        if (path.startsWith(prefix) && path.length() > prefix.length()) {
                            idStr = path.substring(prefix.length()).trim();
                        }
                    }

                    if (idStr == null || idStr.trim().isEmpty()) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Request ID is required to update status.");
                        return;
                    }

                    long requestId = Long.parseLong(idStr.trim());
                    MentorshipService.ProcessResult res = mentorshipService.updateRequestStatus(requestId, body);
                    HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                } catch (NumberFormatException e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid request ID format.");
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid status update payload: " + e.getMessage());
                }

            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }
}



