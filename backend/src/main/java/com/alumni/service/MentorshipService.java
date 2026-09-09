package com.alumni.service;

import com.alumni.config.DBConnection;
import com.alumni.dao.MentorshipRequestDAO;
import com.alumni.dao.RegistrationDAO;
import com.alumni.model.MentorshipRequest;
import com.alumni.validation.InputValidator;
import com.alumni.validation.ValidationResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MentorshipService {

    private final MentorshipRequestDAO requestDAO;
    private final RegistrationDAO registrationDAO;

    public MentorshipService() {
        this(new MentorshipRequestDAO(), new RegistrationDAO());
    }

    public MentorshipService(MentorshipRequestDAO requestDAO, RegistrationDAO registrationDAO) {
        this.requestDAO = requestDAO;
        this.registrationDAO = registrationDAO;
    }

    public static class ProcessResult {
        private final int status;
        private final JSONObject body;

        public ProcessResult(int status, JSONObject body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() { return status; }
        public JSONObject getBody() { return body; }
    }

    public ProcessResult createRequest(JSONObject data) {
        ValidationResult val = InputValidator.validateMentorshipRequest(data);
        if (!val.isValid()) {
            return new ProcessResult(400, val.toJsonObject());
        }

        long studentId = Long.parseLong(InputValidator.getFieldObj(data, "studentId", "student_id").toString().trim());
        long mentorId = Long.parseLong(InputValidator.getFieldObj(data, "mentorId", "mentor_id").toString().trim());
        String sessionGoal = InputValidator.getField(data, "sessionGoal", "session_goal", "goal").trim();
        String message = InputValidator.getField(data, "message", "note", "introductoryNote").trim();

        if (studentId == mentorId) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "You cannot send a mentorship request to yourself.");
            return new ProcessResult(400, err);
        }

        try {
            // Verify student exists and has 'student' role
            String studentRole = getUserRole(studentId);
            if (studentRole == null) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Student user not found.");
                return new ProcessResult(404, err);
            }
            if (!"student".equalsIgnoreCase(studentRole)) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Only registered students can request mentorship.");
                return new ProcessResult(400, err);
            }

            // Verify mentor exists and has 'alumni' role
            String mentorRole = getUserRole(mentorId);
            if (mentorRole == null) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Mentor user not found.");
                return new ProcessResult(404, err);
            }
            if (!"alumni".equalsIgnoreCase(mentorRole)) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Target user is not a registered alumni mentor.");
                return new ProcessResult(400, err);
            }

            // Check duplicate active request
            if (requestDAO.hasActiveRequest(studentId, mentorId)) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "You already have an active mentorship request with this mentor.");
                return new ProcessResult(409, err);
            }

            // Check mentor capacity
            Integer maxMentees = getMentorMaxMentees(mentorId);
            if (maxMentees != null && maxMentees > 0) {
                int currentAccepted = requestDAO.countAcceptedMentees(mentorId);
                if (currentAccepted >= maxMentees) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "This mentor has reached their maximum mentee capacity (" + maxMentees + ").");
                    return new ProcessResult(409, err);
                }
            }

            // Create the request
            long reqId = requestDAO.createRequest(studentId, mentorId, sessionGoal, message);
            if (reqId <= 0) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Failed to create mentorship request.");
                return new ProcessResult(500, err);
            }

            MentorshipRequest created = requestDAO.getRequestById(reqId);
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("message", "Mentorship request sent successfully!");
            resp.put("request", created != null ? created.toJson() : new JSONObject().put("id", reqId));

            return new ProcessResult(201, resp);

        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult getRequestsByStudent(long studentId) {
        try {
            List<MentorshipRequest> list = requestDAO.getRequestsByStudent(studentId);
            JSONArray arr = new JSONArray();
            for (MentorshipRequest r : list) {
                arr.put(r.toJson());
            }
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("count", list.size());
            resp.put("requests", arr);
            return new ProcessResult(200, resp);
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Error fetching student requests: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult getRequestsByMentor(long mentorId) {
        try {
            List<MentorshipRequest> list = requestDAO.getRequestsByMentor(mentorId);
            JSONArray arr = new JSONArray();
            for (MentorshipRequest r : list) {
                arr.put(r.toJson());
            }
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("count", list.size());
            resp.put("requests", arr);
            return new ProcessResult(200, resp);
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Error fetching mentor requests: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult updateRequestStatus(long requestId, JSONObject data) {
        ValidationResult val = InputValidator.validateRequestStatusUpdate(data);
        if (!val.isValid()) {
            return new ProcessResult(400, val.toJsonObject());
        }

        String newStatus = InputValidator.getField(data, "status").trim().toUpperCase();
        String mentorResponse = InputValidator.getField(data, "mentorResponse", "response", "note");
        long mentorId = data.optLong("mentorId", data.optLong("mentor_id", data.optLong("userId", 0)));
        long studentId = data.optLong("studentId", data.optLong("student_id", data.optLong("userId", 0)));

        try {
            MentorshipRequest existing = requestDAO.getRequestById(requestId);
            if (existing == null) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Mentorship request not found.");
                return new ProcessResult(404, err);
            }

            if ("CANCELLED".equals(newStatus)) {
                // Allow student or mentor to cancel
                if (studentId > 0 && existing.getStudentId() != studentId && existing.getMentorId() != studentId) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "You are not authorized to cancel this mentorship request.");
                    return new ProcessResult(403, err);
                }
                boolean updated = requestDAO.updateRequestStatus(requestId, "CANCELLED", mentorResponse);
                if (!updated) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Failed to cancel request.");
                    return new ProcessResult(500, err);
                }
                MentorshipRequest refreshed = requestDAO.getRequestById(requestId);
                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("message", "Mentorship request cancelled successfully.");
                resp.put("request", refreshed != null ? refreshed.toJson() : new JSONObject());
                return new ProcessResult(200, resp);
            }

            // For ACCEPTED or REJECTED, authorize mentor
            if (mentorId <= 0) {
                mentorId = existing.getMentorId();
            }

            if (existing.getMentorId() != mentorId) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "You are not authorized to update this mentorship request.");
                return new ProcessResult(403, err);
            }

            // If accepting, verify capacity
            if ("ACCEPTED".equals(newStatus) && !"ACCEPTED".equals(existing.getStatus())) {
                Integer maxMentees = getMentorMaxMentees(mentorId);
                if (maxMentees != null && maxMentees > 0) {
                    int currentAccepted = requestDAO.countAcceptedMentees(mentorId);
                    if (currentAccepted >= maxMentees) {
                        JSONObject err = new JSONObject();
                        err.put("success", false);
                        err.put("message", "Cannot accept request. You have reached your maximum mentee capacity (" + maxMentees + ").");
                        return new ProcessResult(409, err);
                    }
                }
            }

            boolean updated = requestDAO.updateRequestStatus(requestId, mentorId, newStatus, mentorResponse);
            if (!updated) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Failed to update request status.");
                return new ProcessResult(500, err);
            }

            MentorshipRequest refreshed = requestDAO.getRequestById(requestId);
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("message", "Mentorship request status updated to " + newStatus);
            resp.put("request", refreshed != null ? refreshed.toJson() : new JSONObject());

            return new ProcessResult(200, resp);

        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    private String getUserRole(long userId) throws SQLException {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }
        return null;
    }

    private Integer getMentorMaxMentees(long mentorId) throws SQLException {
        String sql = "SELECT max_mentees FROM alumni WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mentorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int max = rs.getInt("max_mentees");
                    return rs.wasNull() ? null : max;
                }
            }
        }
        return null;
    }
}
