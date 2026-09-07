package com.alumni.server;

import com.alumni.dao.AlumniDAO;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.search.SearchAlgorithms;
import com.alumni.search.SearchResult;
import com.alumni.service.RegistrationService;
import com.alumni.validation.InputValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpServerApp {

    private final int port;
    private HttpServer server;
    private final RegistrationService registrationService;
    private final RegistrationDAO registrationDAO;
    private final AlumniDAO alumniDAO;

    public HttpServerApp(int port) {
        this.port = port;
        this.registrationDAO = new RegistrationDAO();
        this.alumniDAO = new AlumniDAO();
        this.registrationService = new RegistrationService(this.registrationDAO);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Root & Health Check
        server.createContext("/", new RootHandler());
        server.createContext("/api/health", new HealthHandler());

        // Auth & Registrations
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/password-strength", new PasswordStrengthHandler());
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
}
