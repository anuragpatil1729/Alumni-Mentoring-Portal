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
import com.alumni.security.AuthService;
import com.alumni.dao.EventDAO;
import com.alumni.dao.OpportunityDAO;
import com.alumni.security.AuthException;
import com.alumni.security.AuthenticatedUser;
import com.alumni.dao.TeacherDAO;
import java.math.BigDecimal;
import com.alumni.dao.AlumniSlotBookingDAO;
import com.alumni.dao.PasswordResetDAO;
import com.alumni.dao.FeedbackDAO;
import com.alumni.service.UserDuplicateService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import java.time.format.DateTimeParseException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        server.createContext("/api/admin", new AdminHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/logout", new LogoutHandler());
        server.createContext("/api/auth/password-reset/request", new PasswordResetRequestHandler());
        server.createContext("/api/auth/password-reset/confirm", new PasswordResetConfirmHandler());
        server.createContext("/api/events", new EventHandler());
        server.createContext("/api/opportunities", new OpportunityHandler());
        server.createContext("/api/admin/events", new AdminEventsHandler());
        server.createContext("/api/teacher", new TeacherHandler());
        server.createContext("/api/session-history", new SessionHistoryHandler());
        server.createContext("/api/feedback", new FeedbackHandler());
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

                JSONObject user =
                        registrationDAO.authenticateUser(email, password);

                if (user == null) {
                    JSONObject error = new JSONObject();
                    error.put("success", false);
                    error.put("message", "Invalid email or password.");
                    HttpUtils.sendJsonResponse(exchange, 401, error.toString());
                    return;
                }

                try {
                    AuthService authService = new AuthService();
                    String token = authService.createToken(user.getLong("id"));

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Login successful!");
                    response.put("token", token);
                    response.put("user", user);

                    HttpUtils.sendJsonResponse(
                            exchange, 200, response.toString()
                    );

                } catch (Exception e) {
                    System.err.println(
                            "Token creation failed: " + e.getMessage()
                    );

                    HttpUtils.sendErrorResponse(
                            exchange, 500, "Unable to complete login."
                    );
                }

            } catch (Exception e) {
                System.err.println("Login request failed: " + e.getMessage());

                HttpUtils.sendErrorResponse(
                        exchange, 400, "Invalid login request."
                );
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

            AuthenticatedUser currentUser;
            try {
                currentUser = new AuthService().authenticate(exchange);
            } catch (AuthException e) {
                HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
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
                    if (!currentUser.hasRole("ADMIN") && reg.optLong("id", -1) != currentUser.getUserId()) {
                        HttpUtils.sendErrorResponse(exchange, 403, "Access denied: you can only view your own registration");
                        return;
                    }
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
    
    // Allow users to access their own profile; admins can access any profile.
    private void authorizeProfileAccess(
            HttpExchange exchange, long targetUserId)
            throws com.alumni.security.AuthException {

        com.alumni.security.AuthService authService =
                new com.alumni.security.AuthService();

        com.alumni.security.AuthenticatedUser currentUser =
                authService.authenticate(exchange);

        if (currentUser.getUserId() != targetUserId
                && !currentUser.hasRole("ADMIN")) {
            throw new com.alumni.security.AuthException(
                    403, "You can only access your own profile."
            );
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
                    authorizeProfileAccess(exchange, userId);
                    RegistrationService.ProcessResult result = registrationService.getProfile(userId);
                    HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());
                } catch (com.alumni.security.AuthException e) {
                     HttpUtils.sendErrorResponse(
                         exchange, e.getStatusCode(), e.getMessage()
                        );
                }catch (NumberFormatException e) {
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
                    authorizeProfileAccess(exchange, userId);
                    RegistrationService.ProcessResult result = registrationService.updateProfile(userId, body);
                    HttpUtils.sendJsonResponse(exchange, result.getStatus(), result.getBody().toString());

                } catch (com.alumni.security.AuthException e) {
                    HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
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

                    try {
                        AuthenticatedUser current = new AuthService().authenticate(exchange);
                        if (current.getUserId() != userId && !current.hasRole("ADMIN")) {
                            HttpUtils.sendErrorResponse(exchange, 403, "You can only manage your own avatar.");
                            return;
                        }
                    } catch (AuthException e) {
                        HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
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
                    AuthenticatedUser current = new AuthService().authenticate(exchange);
                    if (current.getUserId() != userId && !current.hasRole("ADMIN")) {
                        HttpUtils.sendErrorResponse(exchange, 403, "You can only manage your own avatar.");
                        return;
                    }
                } catch (AuthException e) {
                    HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
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
            AuthenticatedUser currentUser;
            try { currentUser = new AuthService().authenticate(exchange); }
            catch (AuthException e) { HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage()); return; }

            if ("GET".equals(method)) {
                String studentIdStr = qp.get("studentId");
                if (studentIdStr == null || studentIdStr.isEmpty()) studentIdStr = qp.get("student_id");

                String mentorIdStr = qp.get("mentorId");
                if (mentorIdStr == null || mentorIdStr.isEmpty()) mentorIdStr = qp.get("mentor_id");

                String idStr = qp.get("id");

                try {
                    if (studentIdStr != null && !studentIdStr.isEmpty()) {
                        long studentId = Long.parseLong(studentIdStr.trim());
                        if (currentUser.getUserId() != studentId && !currentUser.hasRole("ADMIN")) { HttpUtils.sendErrorResponse(exchange, 403, "You cannot view another user's requests."); return; }
                        MentorshipService.ProcessResult res = mentorshipService.getRequestsByStudent(studentId);
                        HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                    } else if (mentorIdStr != null && !mentorIdStr.isEmpty()) {
                        long mentorId = Long.parseLong(mentorIdStr.trim());
                        if (currentUser.getUserId() != mentorId && !currentUser.hasRole("ADMIN")) { HttpUtils.sendErrorResponse(exchange, 403, "You cannot view another user's requests."); return; }
                        MentorshipService.ProcessResult res = mentorshipService.getRequestsByMentor(mentorId);
                        HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                    } else if (idStr != null && !idStr.isEmpty()) {
                        long reqId = Long.parseLong(idStr.trim());
                        com.alumni.model.MentorshipRequest req = new com.alumni.dao.MentorshipRequestDAO().getRequestById(reqId);
                        if (req != null && currentUser.getUserId() != req.getMentorId() && !currentUser.hasRole("ADMIN")) { HttpUtils.sendErrorResponse(exchange, 403, "You cannot view this request."); return; }
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
                    if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("STUDENT")) { HttpUtils.sendErrorResponse(exchange, 403, "Only students or admins can create mentorship requests."); return; }
                    if (!currentUser.hasRole("ADMIN") && body.optLong("studentId", body.optLong("student_id", -1L)) != currentUser.getUserId()) { HttpUtils.sendErrorResponse(exchange, 403, "You may only create a request for your own account."); return; }
                    MentorshipService.ProcessResult res = mentorshipService.createRequest(body);
                    HttpUtils.sendJsonResponse(exchange, res.getStatus(), res.getBody().toString());
                } catch (Exception e) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Invalid request payload: " + e.getMessage());
                }

            } else if ("PUT".equals(method)) {
                if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("ALUMNI")) { HttpUtils.sendErrorResponse(exchange, 403, "Alumni or admin role required to update request status."); return; }
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
                    if (!currentUser.hasRole("ADMIN")) {
                        com.alumni.model.MentorshipRequest existing = new com.alumni.dao.MentorshipRequestDAO().getRequestById(requestId);
                        if (existing == null) { HttpUtils.sendErrorResponse(exchange, 404, "Mentorship request not found."); return; }
                        if (existing.getMentorId() != currentUser.getUserId()) { HttpUtils.sendErrorResponse(exchange, 403, "You can only update requests assigned to you."); return; }
                    }
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
    
    /** Password reset request: POST /api/auth/password-reset/request {"email":"..."}. */
    private static class PasswordResetRequestHandler implements HttpHandler {
        private final PasswordResetDAO dao = new PasswordResetDAO();
        @Override public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { HttpUtils.sendErrorResponse(exchange, 405, "Method not allowed."); return; }
            try {
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                String email = body.optString("email", "").trim();
                if (!InputValidator.isValidEmail(email)) { HttpUtils.sendErrorResponse(exchange, 400, "A valid email is required."); return; }
                String rawToken = dao.createResetToken(email);
                if (rawToken != null) {
                    // Delivery is delegated to a configured email provider. Never return reset tokens in API responses.
                    try { com.alumni.service.PasswordResetMailer.send(email, rawToken); }
                    catch (Exception mailError) { System.err.println("Password reset email could not be sent; configure SMTP: " + mailError.getMessage()); }
                }
                HttpUtils.sendJsonResponse(exchange, 200, new JSONObject().put("success", true)
                    .put("message", "If an account exists for that email, password reset instructions will be sent.").toString());
            } catch (Exception e) { HttpUtils.sendErrorResponse(exchange, 500, "Unable to process password reset request."); }
        }
    }

    /** Password reset confirmation: POST /api/auth/password-reset/confirm {token,newPassword}. */
    private static class PasswordResetConfirmHandler implements HttpHandler {
        private final PasswordResetDAO dao = new PasswordResetDAO();
        @Override public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { HttpUtils.sendErrorResponse(exchange, 405, "Method not allowed."); return; }
            try {
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                String token = body.optString("token", "").trim();
                String password = body.optString("newPassword", "");
                if (!token.matches("^[a-fA-F0-9]{64}$")) { HttpUtils.sendErrorResponse(exchange, 400, "Invalid or expired reset token."); return; }
                if (!InputValidator.isValidPassword(password)) { HttpUtils.sendErrorResponse(exchange, 422, "Password does not meet the required strength rules."); return; }
                String recoveredEmail = dao.consumeTokenAndChangePasswordWithEmail(token, RegistrationDAO.hashPassword(password));
                if (recoveredEmail == null) { HttpUtils.sendErrorResponse(exchange, 400, "Invalid, expired, or already used reset token."); return; }
                if (recoveredEmail != null && !recoveredEmail.isBlank()) {
                    try { com.alumni.service.PasswordResetMailer.sendRecoveryNotification(recoveredEmail); }
                    catch (Exception mailError) { System.err.println("Account recovery notification email note: " + mailError.getMessage()); }
                }
                HttpUtils.sendJsonResponse(exchange, 200, new JSONObject().put("success", true).put("message", "Password reset successful. Please log in again.").toString());
            } catch (Exception e) { HttpUtils.sendErrorResponse(exchange, 500, "Unable to reset password."); }
        }
    }

    // User Logout: POST /api/auth/logout
    private class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendErrorResponse(
                        exchange, 405, "Method Not Allowed"
                );
                return;
            }

            String header = exchange.getRequestHeaders()
                    .getFirst("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                HttpUtils.sendErrorResponse(
                        exchange, 401, "Authentication required."
                );
                return;
            }

            String token = header.substring(7).trim();

            if (!token.matches("^[a-fA-F0-9]{64}$")) {
                HttpUtils.sendErrorResponse(
                        exchange, 401, "Invalid token."
                );
                return;
            }

            try {
                AuthService authService = new AuthService();
                authService.authenticate(exchange);

                authService.revokeToken(token);

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Logout successful.");

                HttpUtils.sendJsonResponse(
                        exchange, 200, response.toString()
                );

            } catch (com.alumni.security.AuthException e) {
                HttpUtils.sendErrorResponse(
                        exchange, e.getStatusCode(), e.getMessage()
                );
            } catch (Exception e) {
                System.err.println("Logout failed: " + e.getMessage());

                HttpUtils.sendErrorResponse(
                        exchange, 500, "Unable to complete logout."
                );
            }
        }
    }

    
    private class AdminHandler implements HttpHandler {

        private final com.alumni.dao.AdminDAO adminDAO =
                new com.alumni.dao.AdminDAO();

        private final AuthService authService = new AuthService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.addCorsHeaders(exchange);

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                authService.requireRole(exchange, "admin");

                // GET /api/admin/users?role=teacher&active=true
                if ("/api/admin/users".equals(path)
                        && "GET".equalsIgnoreCase(method)) {

                    Map<String, String> qp =
                            HttpUtils.parseQueryParams(exchange);

                    String role = qp.get("role");
                    String active = qp.get("active");
                    String approval = qp.get("approval");

                    if (role != null && !role.isBlank()
                            && !role.matches(
                                "(?i)student|alumni|teacher|admin")) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400, "Invalid role filter.");
                        return;
                    }

                    if (active != null
                            && !active.equalsIgnoreCase("true")
                            && !active.equalsIgnoreCase("false")) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400,
                                "active must be true or false.");
                        return;
                    }

                    if (approval != null && !approval.isBlank()
                            && !approval.matches("(?i)PENDING|APPROVED|REJECTED")) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400, "Invalid approval status filter.");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("users",
                            adminDAO.getUsers(role, active, approval));

                    HttpUtils.sendJsonResponse(
                            exchange, 200, response.toString());
                    return;
                }

                // GET /api/admin/duplicates
                if ("/api/admin/duplicates".equals(path) && "GET".equalsIgnoreCase(method)) {
                    UserDuplicateService dupService = new UserDuplicateService();
                    JSONArray duplicates = dupService.scanAllDuplicates();
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("duplicates", duplicates);
                    response.put("totalClusters", duplicates.length());
                    HttpUtils.sendJsonResponse(exchange, 200, response.toString());
                    return;
                }

                // POST /api/admin/duplicates/check
                if ("/api/admin/duplicates/check".equals(path) && "POST".equalsIgnoreCase(method)) {
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    UserDuplicateService dupService = new UserDuplicateService();
                    String name = body.optString("name", "");
                    String email = body.optString("email", "");
                    String mobile = body.optString("mobileNumber", body.optString("mobile", ""));
                    String studentId = body.optString("studentId", "");
                    JSONObject checkResult = dupService.checkManualDuplicate(name, email, mobile, studentId);
                    checkResult.put("success", true);
                    HttpUtils.sendJsonResponse(exchange, 200, checkResult.toString());
                    return;
                }

                // POST /api/admin/users/{id}/approve or PATCH /api/admin/users/{id}/approve
                // Body: {"approved": true}
                if (path.matches("^/api/admin/users/\\d+/approve$")
                        && ("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {

                    long userId = Long.parseLong(path.split("/")[4]);
                    JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                    boolean approved = body.optBoolean("approved", true);

                    if (!adminDAO.approveUser(userId, approved)) {
                        HttpUtils.sendErrorResponse(exchange, 404, "User not found.");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", approved ? "User registration approved." : "User registration rejected.");
                    HttpUtils.sendJsonResponse(exchange, 200, response.toString());
                    return;
                }

                // DELETE /api/admin/users/{id}
                if (path.matches("^/api/admin/users/\\d+$") && "DELETE".equalsIgnoreCase(method)) {
                    long userId = Long.parseLong(path.split("/")[4]);

                    com.alumni.security.AuthenticatedUser actor =
                            authService.authenticate(exchange);

                    if (actor.getUserId() == userId) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400,
                                "You cannot delete your own account.");
                        return;
                    }

                    if (!adminDAO.deleteUser(userId)) {
                        HttpUtils.sendErrorResponse(
                                exchange, 404, "User not found.");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "User deleted successfully.");
                    HttpUtils.sendJsonResponse(exchange, 200, response.toString());
                    return;
                }

                // PATCH /api/admin/users/{id}/status
                // Body: {"active": false}
                if (path.matches(
                        "^/api/admin/users/\\d+/status$")
                        && "PATCH".equalsIgnoreCase(method)) {

                    long userId = Long.parseLong(
                        path.split("/")[4]);

                    JSONObject body =
                            HttpUtils.parseBodyAsJson(exchange);

                    if (!body.has("active")
                            || !(body.opt("active") instanceof Boolean)) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400,
                                "Boolean field 'active' is required.");
                        return;
                    }

                    boolean active = body.getBoolean("active");

                    com.alumni.security.AuthenticatedUser actor =
                            authService.authenticate(exchange);

                    if (actor.getUserId() == userId && !active) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400,
                                "You cannot deactivate your own account.");
                        return;
                    }

                    if (!adminDAO.setUserActive(userId, active)) {
                        HttpUtils.sendErrorResponse(
                                exchange, 404, "User not found.");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message",
                            active ? "User activated."
                                : "User deactivated.");

                    HttpUtils.sendJsonResponse(
                            exchange, 200, response.toString());
                    return;
                }

                // PATCH /api/admin/events/{id}/status
                // Body: {"status": "APPROVED"} or {"status": "REJECTED"}
                if (path.matches(
                        "^/api/admin/events/\\d+/status$")
                        && "PATCH".equalsIgnoreCase(method)) {

                    long eventId = Long.parseLong(
                            path.split("/")[4]);

                    JSONObject body =
                            HttpUtils.parseBodyAsJson(exchange);

                    String status = body.optString("status", "")
                            .trim().toUpperCase();

                    if (!status.equals("APPROVED")
                            && !status.equals("REJECTED")) {
                        HttpUtils.sendErrorResponse(
                                exchange, 400,
                                "Status must be APPROVED or REJECTED.");
                        return;
                    }

                    if (!adminDAO.updateEventStatus(eventId, status)) {
                        HttpUtils.sendErrorResponse(
                                exchange, 404, "Event not found.");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message",
                            "Event status updated to " + status + ".");

                    HttpUtils.sendJsonResponse(
                            exchange, 200, response.toString());
                    return;
                }

                HttpUtils.sendErrorResponse(
                        exchange, 404, "Admin endpoint not found.");

            } catch (com.alumni.security.AuthException e) {
                HttpUtils.sendErrorResponse(
                        exchange, e.getStatusCode(), e.getMessage());

            } catch (NumberFormatException e) {
                HttpUtils.sendErrorResponse(
                        exchange, 400, "Invalid ID.");

            } catch (Exception e) {
                System.err.println("Admin API error: " + e.getMessage());
                HttpUtils.sendErrorResponse(
                        exchange, 500, "Admin operation failed.");
            }
        }
    }


private class EventHandler implements HttpHandler {
    private final EventDAO dao = new EventDAO();
    private final AuthService auth = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(method) &&
                    "/api/events".equals(path)) {
                HttpUtils.sendJsonResponse(
                    exchange, 200, dao.getApprovedEvents().toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method) &&
                    "/api/events".equals(path)) {
                AuthenticatedUser user =
                    auth.requireRole(exchange, "alumni");

                JSONObject body = HttpUtils.parseBodyAsJson(exchange);

                String title = body.optString("title", "").trim();
                String description =
                    body.optString("description", "").trim();
                String date = body.optString("date", "").trim();
                String start = body.optString("start_time", "").trim();
                String end = body.optString("end_time", "").trim();
                String venue = body.optString("venue", "").trim();
                String type = body.optString("event_type", "GENERAL")
                    .trim().toUpperCase();

                if (title.isEmpty() || description.isEmpty()
                        || date.isEmpty() || start.isEmpty()
                        || end.isEmpty() || venue.isEmpty()) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400, "All required event fields must be provided.");
                    return;
                }

                LocalDate eventDate = LocalDate.parse(date);
                LocalTime startTime = LocalTime.parse(start);
                LocalTime endTime = LocalTime.parse(end);

                if (eventDate.isBefore(LocalDate.now())) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400, "Event date cannot be in the past.");
                    return;
                }

                if (!endTime.isAfter(startTime)) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400, "End time must be after start time.");
                    return;
                }

                if (!type.matches(
                        "GENERAL|JUDGE|GUEST_SESSION|SPONSOR")) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400, "Invalid event type.");
                    return;
                }

                long id = dao.createEvent(body, user.getUserId());

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("id", id);
                response.put("status", "PENDING");

                HttpUtils.sendJsonResponse(
                    exchange, 201, response.toString());
                return;
            }

            HttpUtils.sendErrorResponse(
                exchange, 404, "Event endpoint not found.");

        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(
                exchange, e.getStatusCode(), e.getMessage());
        } catch (DateTimeParseException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid date or time format. Use YYYY-MM-DD and HH:mm.");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(
                exchange, 500, "Event database operation failed.");
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid event request.");
        }
    }
}

private class OpportunityHandler implements HttpHandler {
    private final OpportunityDAO dao = new OpportunityDAO();
    private final AuthService auth = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(method) &&
                    "/api/opportunities".equals(path)) {
                HttpUtils.sendJsonResponse(
                    exchange, 200, dao.getOpenOpportunities().toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method) &&
                    "/api/opportunities".equals(path)) {
                AuthenticatedUser user =
                    auth.requireRole(exchange, "alumni");

                JSONObject body = HttpUtils.parseBodyAsJson(exchange);

                String[] required = {
                    "title", "description", "organization",
                    "opportunity_type"
                };

                for (String field : required) {
                    if (body.optString(field, "").trim().isEmpty()) {
                        HttpUtils.sendErrorResponse(
                            exchange, 400, "Missing required field: " + field);
                        return;
                    }
                }

                String deadline = body.optString("deadline", "").trim();
                if (!deadline.isEmpty()) {
                    LocalDate.parse(deadline);
                }

                String link =
                    body.optString("application_link", "").trim();

                if (!link.isEmpty()
                        && !link.matches("https?://.+")) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400, "Application link must start with http:// or https://.");
                    return;
                }

                long id = dao.createOpportunity(
                    body, user.getUserId());

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("id", id);
                response.put("status", "OPEN");

                HttpUtils.sendJsonResponse(
                    exchange, 201, response.toString());
                return;
            }

            String prefix = "/api/opportunities/";
            String suffix = "/status";

            if ("PATCH".equalsIgnoreCase(method)
                    && path.startsWith(prefix)
                    && path.endsWith(suffix)) {

                AuthenticatedUser user =
                    auth.requireRole(exchange, "alumni");

                String idText = path.substring(
                    prefix.length(), path.length() - suffix.length());

                long id = Long.parseLong(idText);

                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                String status = body.optString("status", "")
                    .trim().toUpperCase();

                boolean updated = dao.updateStatus(
                    id, user.getUserId(), status);

                if (!updated) {
                    HttpUtils.sendErrorResponse(
                        exchange, 404,
                        "Opportunity not found or not owned by this alumnus.");
                    return;
                }

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("status", status);
                HttpUtils.sendJsonResponse(
                    exchange, 200, response.toString());
                return;
            }

            HttpUtils.sendErrorResponse(
                exchange, 404, "Opportunity endpoint not found.");

        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(
                exchange, e.getStatusCode(), e.getMessage());
        } catch (DateTimeParseException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid deadline. Use YYYY-MM-DD.");
        } catch (NumberFormatException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid opportunity ID.");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(
                exchange, 500, "Opportunity database operation failed.");
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid opportunity request.");
        }
    }
}

private class AdminEventsHandler implements HttpHandler {
    private final EventDAO dao = new EventDAO();
    private final AuthService auth = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            auth.requireRole(exchange, "admin");

            if ("GET".equalsIgnoreCase(method)
                    && "/api/admin/events".equals(path)) {
                HttpUtils.sendJsonResponse(
                    exchange, 200, dao.getAllEvents().toString());
                return;
            }

            String prefix = "/api/admin/events/";
            String suffix = "/status";

            if ("PATCH".equalsIgnoreCase(method)
                    && path.startsWith(prefix)
                    && path.endsWith(suffix)) {

                String idText = path.substring(
                    prefix.length(), path.length() - suffix.length());

                long id = Long.parseLong(idText);
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                String status = body.optString("status", "")
                    .trim().toUpperCase();

                if (!"APPROVED".equals(status)
                        && !"REJECTED".equals(status)) {
                    HttpUtils.sendErrorResponse(
                        exchange, 400,
                        "Status must be APPROVED or REJECTED.");
                    return;
                }

                if (!dao.updateStatus(id, status)) {
                    HttpUtils.sendErrorResponse(
                        exchange, 409,
                        "Event not found or is no longer pending.");
                    return;
                }

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("id", id);
                response.put("status", status);
                HttpUtils.sendJsonResponse(
                    exchange, 200, response.toString());
                return;
            }

            HttpUtils.sendErrorResponse(
                exchange, 404, "Admin event endpoint not found.");

        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(
                exchange, e.getStatusCode(), e.getMessage());
        } catch (NumberFormatException e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid event ID.");
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(
                exchange, 500, "Event database operation failed.");
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(
                exchange, 400, "Invalid admin event request.");
        }
    }
}

private static class SessionHistoryHandler implements HttpHandler {
    private final com.alumni.dao.SessionHistoryDAO dao = new com.alumni.dao.SessionHistoryDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        try {
            AuthenticatedUser user = new AuthService().authenticate(exchange);
            if (!user.hasRole("STUDENT") && !user.hasRole("ADMIN") && !user.hasRole("ALUMNI")) {
                HttpUtils.sendErrorResponse(exchange, 403, "Student, alumni, or admin role required.");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/session-history".equals(path)) {
                if (!user.hasRole("ADMIN") && !user.hasRole("STUDENT")) { HttpUtils.sendErrorResponse(exchange, 403, "Student or admin role required."); return; }
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                if (user.hasRole("ADMIN") && !body.has("studentId")) { HttpUtils.sendErrorResponse(exchange, 400, "studentId is required when an admin creates a session."); return; }
                long studentId = body.optLong("studentId", user.getUserId());
                if (!user.hasRole("ADMIN") && studentId != user.getUserId()) { HttpUtils.sendErrorResponse(exchange, 403, "You can only start a session for yourself."); return; }
                long alumniId = body.getLong("alumniId");
                Long requestId = body.has("mentorshipRequestId") && !body.isNull("mentorshipRequestId") ? body.getLong("mentorshipRequestId") : null;
                long id = dao.createSession(studentId, alumniId, requestId, body.optString("priority", "MEDIUM"));
                HttpUtils.sendJsonResponse(exchange, 201, new JSONObject().put("success", true).put("sessionId", id).put("status", "STARTED").toString());
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/api/session-history".equals(path)) {
                Map<String, String> query = HttpUtils.parseQueryParams(exchange);
                String sort = query.getOrDefault("sort", "chronological");
                HttpUtils.sendJsonResponse(exchange, 200,
                    dao.getHistory(user.getUserId(), sort, user.hasRole("ADMIN")).toString());
                return;
            }
            if ("PATCH".equalsIgnoreCase(method) && path.matches("/api/session-history/\\d+/status")) {
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                boolean changed = dao.updateStatus(Long.parseLong(path.split("/")[3]),
                    user.getUserId(), body.getString("status").trim().toUpperCase(), user.hasRole("ADMIN"));
                if (!changed) {
                    HttpUtils.sendErrorResponse(exchange, 404, "Session not found.");
                    return;
                }
                HttpUtils.sendJsonResponse(exchange, 200, "{\"success\":true}");
                return;
            }
            HttpUtils.sendErrorResponse(exchange, 404, "Session history endpoint not found.");
        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            HttpUtils.sendErrorResponse(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Session history database error.");
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 400, "Invalid session history request.");
        }
    }
}

private static class FeedbackHandler implements HttpHandler {
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // POST /api/feedback: student or admin submits feedback for a completed session
            if ("POST".equalsIgnoreCase(method) && "/api/feedback".equals(path)) {
                AuthenticatedUser user = authService.authenticate(exchange);
                if (!user.hasRole("STUDENT") && !user.hasRole("ADMIN")) {
                    HttpUtils.sendErrorResponse(exchange, 403, "Student or admin role required to submit feedback.");
                    return;
                }

                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                if (!body.has("sessionId") || !body.has("rating") || !body.has("comments")) {
                    HttpUtils.sendErrorResponse(exchange, 400, "sessionId, rating, and comments are required.");
                    return;
                }

                long sessionId = body.getLong("sessionId");
                int rating = body.getInt("rating");
                String comments = body.getString("comments");

                long feedbackId = feedbackDAO.saveFeedback(sessionId, user.getUserId(), rating, comments, user.hasRole("ADMIN"));

                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("message", "Feedback submitted successfully.");
                resp.put("feedbackId", feedbackId);
                HttpUtils.sendJsonResponse(exchange, 201, resp.toString());
                return;
            }

            // GET /api/feedback/session/{id}
            if ("GET".equalsIgnoreCase(method) && path.matches("^/api/feedback/session/\\d+$")) {
                authService.authenticate(exchange);
                long sessionId = Long.parseLong(path.split("/")[4]);
                JSONObject feedback = feedbackDAO.getFeedbackBySession(sessionId);
                if (feedback == null) {
                    HttpUtils.sendErrorResponse(exchange, 404, "No feedback found for session #" + sessionId + ".");
                    return;
                }
                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("feedback", feedback);
                HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                return;
            }

            // GET /api/feedback/mentor/{id}
            if ("GET".equalsIgnoreCase(method) && path.matches("^/api/feedback/mentor/\\d+$")) {
                long mentorId = Long.parseLong(path.split("/")[4]);
                JSONObject summary = feedbackDAO.getMentorFeedbackSummary(mentorId);
                summary.put("success", true);
                HttpUtils.sendJsonResponse(exchange, 200, summary.toString());
                return;
            }

            // GET /api/feedback (admin only)
            if ("GET".equalsIgnoreCase(method) && "/api/feedback".equals(path)) {
                authService.requireRole(exchange, "admin");
                JSONArray all = feedbackDAO.getAllFeedback();
                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("feedback", all);
                resp.put("total", all.length());
                HttpUtils.sendJsonResponse(exchange, 200, resp.toString());
                return;
            }

            HttpUtils.sendErrorResponse(exchange, 404, "Feedback endpoint not found.");

        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpUtils.sendErrorResponse(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Feedback database error: " + e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 400, "Invalid feedback request: " + e.getMessage());
        }
    }
}

static class TeacherHandler implements HttpHandler {
    private final TeacherDAO dao = new TeacherDAO();
    private final AlumniSlotBookingDAO bookingDAO = new AlumniSlotBookingDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        final String base = "/api/teacher/events";

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                    "GET, POST, PUT, PATCH, OPTIONS");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            AuthService auth = new AuthService();
            AuthenticatedUser user = auth.requireAnyRole(exchange, "TEACHER", "ADMIN");
            boolean admin = user.hasRole("ADMIN");

            // Slot bookings are restricted to authenticated teachers/admins.
            if ("POST".equalsIgnoreCase(method) && path.equals("/api/teacher/bookings")) {
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                long bookingId = bookingDAO.createBooking(
                    body.getLong("eventId"), body.getLong("alumniId"), user.getUserId(),
                    LocalDate.parse(body.getString("slotDate")),
                    LocalTime.parse(body.getString("startTime")),
                    LocalTime.parse(body.getString("endTime")),
                    body.getString("purpose").trim().toUpperCase(),
                    body.optString("notes", null));
                JSONObject out = new JSONObject().put("success", true)
                    .put("bookingId", bookingId).put("message", "Alumni slot booked successfully.");
                HttpUtils.sendJsonResponse(exchange, 201, out.toString());
                return;
            }
            if ("GET".equalsIgnoreCase(method) && path.equals("/api/teacher/bookings")) {
                HttpUtils.sendJsonResponse(exchange, 200,
                    bookingDAO.getBookings(user.getUserId(), admin).toString());
                return;
            }
            if ("PATCH".equalsIgnoreCase(method) && path.matches("/api/teacher/bookings/\\d+/status")) {
                long bookingId = Long.parseLong(path.split("/")[4]);
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                String status = body.getString("status").trim().toUpperCase();
                boolean updated = bookingDAO.updateStatus(bookingId, user.getUserId(), admin, status);
                if (!updated) {
                    HttpUtils.sendErrorResponse(exchange, 404,
                        "Booking not found, not authorized, or no longer BOOKED.");
                    return;
                }
                HttpUtils.sendJsonResponse(exchange, 200,
                    "{\"success\":true,\"message\":\"Booking status updated.\"}");
                return;
            }

            // Teachers can only access events assigned to them. Admins can inspect
            // event details, but must have an assignment to enter/update marks.
            if ("POST".equalsIgnoreCase(method) && path.matches("/api/teacher/events/\\d+/marks")) {
                String[] parts = path.substring(base.length() + 1).split("/");
                long eventId = Long.parseLong(parts[0]);
                if (dao.getAssignedEvent(eventId, user.getUserId()) == null) {
                    HttpUtils.sendErrorResponse(exchange, 403, "You are not assigned to this event.");
                    return;
                }
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                dao.createInternalMark(eventId, body.getLong("studentId"), user.getUserId(),
                    body.getBigDecimal("marks"), body.has("maxMarks") ? body.getBigDecimal("maxMarks") : new BigDecimal("100.00"),
                    body.optString("remarks", null));
                HttpUtils.sendJsonResponse(exchange, 201,
                    "{\"success\":true,\"message\":\"Marks created.\"}");
                return;
            }
            if ("PUT".equalsIgnoreCase(method) && path.matches("/api/teacher/events/\\d+/marks/\\d+")) {
                String[] parts = path.substring(base.length() + 1).split("/");
                long eventId = Long.parseLong(parts[0]);
                long studentId = Long.parseLong(parts[2]);
                if (dao.getAssignedEvent(eventId, user.getUserId()) == null) {
                    HttpUtils.sendErrorResponse(exchange, 403, "You are not assigned to this event.");
                    return;
                }
                JSONObject body = HttpUtils.parseBodyAsJson(exchange);
                boolean updated = dao.updateInternalMark(eventId, studentId, user.getUserId(),
                    body.getBigDecimal("marks"), body.has("maxMarks") ? body.getBigDecimal("maxMarks") : new BigDecimal("100.00"),
                    body.optString("remarks", null));
                if (!updated) {
                    HttpUtils.sendErrorResponse(exchange, 404, "Marks not found or event not assigned.");
                    return;
                }
                HttpUtils.sendJsonResponse(exchange, 200,
                    "{\"success\":true,\"message\":\"Marks updated.\"}");
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                HttpUtils.sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (path.equals(base) || path.equals(base + "/")) {
                HttpUtils.sendJsonResponse(exchange, 200,
                    dao.getScheduledEvents(user.getUserId()).toString());
                return;
            }
            String remainder = path.startsWith(base) ? path.substring(base.length()) : "";
            if (!remainder.matches("/\\d+(/marks)?")) {
                HttpUtils.sendErrorResponse(exchange, 404, "Endpoint not found");
                return;
            }
            String[] parts = remainder.substring(1).split("/");
            long eventId = Long.parseLong(parts[0]);
            JSONObject event = dao.getAssignedEvent(eventId, user.getUserId());
            if (event == null) {
                HttpUtils.sendErrorResponse(exchange, 404, "Assigned event not found");
                return;
            }
            if (parts.length == 2) {
                HttpUtils.sendJsonResponse(exchange, 200,
                    dao.getEventMarks(eventId, user.getUserId()).toString());
            } else {
                HttpUtils.sendJsonResponse(exchange, 200, event.toString());
            }
        } catch (AuthException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (AlumniSlotBookingDAO.BookingConflictException e) {
            HttpUtils.sendErrorResponse(exchange, 409, e.getMessage());
        } catch (NumberFormatException e) {
            HttpUtils.sendErrorResponse(exchange, 400, "Invalid ID or numeric value.");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendErrorResponse(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                HttpUtils.sendErrorResponse(exchange, 409, "A conflicting or duplicate record exists.");
            } else {
                HttpUtils.sendErrorResponse(exchange, 500, "Database operation failed.");
            }
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 400, "Invalid request.");
        }
    }
}

}
