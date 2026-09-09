package com.alumni.dao;

import com.alumni.config.DBConnection;
import com.alumni.model.MentorshipRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MentorshipRequestDAO {

    public long createRequest(long studentId, long mentorId, String sessionGoal, String message) throws SQLException {
        String sql = "INSERT INTO mentorship_requests (student_id, mentor_id, session_goal, message, status) " +
                "VALUES (?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, studentId);
            stmt.setLong(2, mentorId);
            stmt.setString(3, sessionGoal);
            stmt.setString(4, message);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public boolean hasActiveRequest(long studentId, long mentorId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM mentorship_requests " +
                "WHERE student_id = ? AND mentor_id = ? AND status = 'PENDING'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            stmt.setLong(2, mentorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<MentorshipRequest> getRequestsByStudent(long studentId) throws SQLException {
        String sql = "SELECT mr.*, " +
                "       mu.full_name AS mentor_name, mu.email AS mentor_email, " +
                "       (mu.avatar_image IS NOT NULL) AS mentor_has_avatar, " +
                "       a.company AS mentor_company, a.designation AS mentor_designation, a.department AS mentor_department " +
                "  FROM mentorship_requests mr " +
                "  JOIN users mu ON mu.id = mr.mentor_id " +
                "  LEFT JOIN alumni a ON a.user_id = mr.mentor_id " +
                " WHERE mr.student_id = ? " +
                " ORDER BY mr.created_at DESC";

        List<MentorshipRequest> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MentorshipRequest req = mapResultSetToRequest(rs);
                    req.setMentorName(rs.getString("mentor_name"));
                    req.setMentorEmail(rs.getString("mentor_email"));
                    req.setMentorCompany(rs.getString("mentor_company"));
                    req.setMentorDesignation(rs.getString("mentor_designation"));
                    req.setMentorDepartment(rs.getString("mentor_department"));
                    if (rs.getBoolean("mentor_has_avatar")) {
                        req.setMentorAvatarUrl("/api/users/" + req.getMentorId() + "/avatar");
                    }
                    list.add(req);
                }
            }
        }
        return list;
    }

    public List<MentorshipRequest> getRequestsByMentor(long mentorId) throws SQLException {
        String sql = "SELECT mr.*, " +
                "       su.full_name AS student_name, su.email AS student_email, " +
                "       (su.avatar_image IS NOT NULL) AS student_has_avatar, " +
                "       s.student_id AS student_roll_number, s.department AS student_department, " +
                "       s.graduation_year AS student_graduation_year " +
                "  FROM mentorship_requests mr " +
                "  JOIN users su ON su.id = mr.student_id " +
                "  LEFT JOIN students s ON s.user_id = mr.student_id " +
                " WHERE mr.mentor_id = ? " +
                " ORDER BY mr.created_at DESC";

        List<MentorshipRequest> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mentorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MentorshipRequest req = mapResultSetToRequest(rs);
                    req.setStudentName(rs.getString("student_name"));
                    req.setStudentEmail(rs.getString("student_email"));
                    req.setStudentRollNumber(rs.getString("student_roll_number"));
                    req.setStudentDepartment(rs.getString("student_department"));
                    int gradYr = rs.getInt("student_graduation_year");
                    req.setStudentGraduationYear(rs.wasNull() ? null : gradYr);
                    if (rs.getBoolean("student_has_avatar")) {
                        req.setStudentAvatarUrl("/api/users/" + req.getStudentId() + "/avatar");
                    }
                    list.add(req);
                }
            }
        }
        return list;
    }

    public MentorshipRequest getRequestById(long requestId) throws SQLException {
        String sql = "SELECT mr.*, " +
                "       su.full_name AS student_name, su.email AS student_email, " +
                "       (su.avatar_image IS NOT NULL) AS student_has_avatar, " +
                "       s.student_id AS student_roll_number, s.department AS student_department, s.graduation_year AS student_graduation_year, " +
                "       mu.full_name AS mentor_name, mu.email AS mentor_email, " +
                "       (mu.avatar_image IS NOT NULL) AS mentor_has_avatar, " +
                "       a.company AS mentor_company, a.designation AS mentor_designation, a.department AS mentor_department " +
                "  FROM mentorship_requests mr " +
                "  JOIN users su ON su.id = mr.student_id " +
                "  LEFT JOIN students s ON s.user_id = mr.student_id " +
                "  JOIN users mu ON mu.id = mr.mentor_id " +
                "  LEFT JOIN alumni a ON a.user_id = mr.mentor_id " +
                " WHERE mr.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    MentorshipRequest req = mapResultSetToRequest(rs);
                    req.setStudentName(rs.getString("student_name"));
                    req.setStudentEmail(rs.getString("student_email"));
                    req.setStudentRollNumber(rs.getString("student_roll_number"));
                    req.setStudentDepartment(rs.getString("student_department"));
                    int gradYr = rs.getInt("student_graduation_year");
                    req.setStudentGraduationYear(rs.wasNull() ? null : gradYr);
                    if (rs.getBoolean("student_has_avatar")) {
                        req.setStudentAvatarUrl("/api/users/" + req.getStudentId() + "/avatar");
                    }

                    req.setMentorName(rs.getString("mentor_name"));
                    req.setMentorEmail(rs.getString("mentor_email"));
                    req.setMentorCompany(rs.getString("mentor_company"));
                    req.setMentorDesignation(rs.getString("mentor_designation"));
                    req.setMentorDepartment(rs.getString("mentor_department"));
                    if (rs.getBoolean("mentor_has_avatar")) {
                        req.setMentorAvatarUrl("/api/users/" + req.getMentorId() + "/avatar");
                    }
                    return req;
                }
            }
        }
        return null;
    }

    public boolean updateRequestStatus(long requestId, long mentorId, String newStatus, String mentorResponse) throws SQLException {
        String sql = "UPDATE mentorship_requests SET status = ?, mentor_response = ? " +
                "WHERE id = ? AND mentor_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, mentorResponse != null ? mentorResponse.trim() : null);
            stmt.setLong(3, requestId);
            stmt.setLong(4, mentorId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateRequestStatus(long requestId, String newStatus, String mentorResponse) throws SQLException {
        String sql = "UPDATE mentorship_requests SET status = ?, mentor_response = ? " +
                "WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, mentorResponse != null ? mentorResponse.trim() : null);
            stmt.setLong(3, requestId);

            return stmt.executeUpdate() > 0;
        }
    }

    public int countAcceptedMentees(long mentorId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM mentorship_requests " +
                "WHERE mentor_id = ? AND status = 'ACCEPTED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, mentorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean deleteRequest(long requestId) throws SQLException {
        String sql = "DELETE FROM mentorship_requests WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requestId);
            return stmt.executeUpdate() > 0;
        }
    }

    private MentorshipRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        MentorshipRequest req = new MentorshipRequest();
        req.setId(rs.getLong("id"));
        req.setStudentId(rs.getLong("student_id"));
        req.setMentorId(rs.getLong("mentor_id"));
        req.setSessionGoal(rs.getString("session_goal"));
        req.setMessage(rs.getString("message"));
        req.setStatus(rs.getString("status"));
        req.setMentorResponse(rs.getString("mentor_response"));
        req.setCreatedAt(rs.getTimestamp("created_at"));
        req.setUpdatedAt(rs.getTimestamp("updated_at"));
        return req;
    }
}
