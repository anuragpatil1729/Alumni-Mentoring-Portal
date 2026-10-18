package com.alumni.dao;

import com.alumni.config.DBConnection;
import com.alumni.model.Feedback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FeedbackDAO
 *
 * Implements JDBC operations for session feedback, rating summaries,
 * and constraint validations.
 */
public class FeedbackDAO {

    /**
     * Saves feedback for a completed session.
     * Validates that the session is COMPLETED and belongs to the given student (or bypass if admin).
     */
    public long saveFeedback(long sessionId, long studentId, int rating, String comments, boolean isAdmin)
            throws SQLException, IllegalArgumentException, IllegalStateException {

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be an integer between 1 and 5.");
        }
        if (comments == null || comments.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback comments cannot be empty.");
        }
        String cleanComments = comments.trim();

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Verify session history record
                String sessionSql = "SELECT student_id, alumni_id, status FROM session_history WHERE id = ? FOR UPDATE";
                long sessionStudentId;
                long mentorId;
                String status;

                try (PreparedStatement ps = con.prepareStatement(sessionSql)) {
                    ps.setLong(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Session #" + sessionId + " does not exist.");
                        }
                        sessionStudentId = rs.getLong("student_id");
                        mentorId = rs.getLong("alumni_id");
                        status = rs.getString("status");
                    }
                }

                // Verify ownership (unless admin)
                if (!isAdmin && sessionStudentId != studentId) {
                    throw new IllegalStateException("You are not authorized to submit feedback for this session.");
                }

                // Verify session status is COMPLETED
                if (!"COMPLETED".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Feedback can only be submitted for COMPLETED sessions. Current status: " + status);
                }

                // 2. Check if feedback already exists for this session
                String checkSql = "SELECT id FROM feedback WHERE session_id = ?";
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setLong(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException("Feedback has already been submitted for session #" + sessionId + ".");
                        }
                    }
                }

                // 3. Insert feedback record
                String insertSql = "INSERT INTO feedback (session_id, student_id, mentor_id, rating, comments) VALUES (?, ?, ?, ?, ?)";
                long generatedId = -1;

                try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, sessionId);
                    ps.setLong(2, sessionStudentId);
                    ps.setLong(3, mentorId);
                    ps.setInt(4, rating);
                    ps.setString(5, cleanComments);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedId = keys.getLong(1);
                        }
                    }
                }

                con.commit();
                return generatedId;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Checks if feedback exists for a given session.
     */
    public boolean hasFeedback(long sessionId) throws SQLException {
        String sql = "SELECT 1 FROM feedback WHERE session_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns feedback by session ID.
     */
    public JSONObject getFeedbackBySession(long sessionId) throws SQLException {
        String sql = """
            SELECT f.id, f.session_id, f.student_id, f.mentor_id, f.rating, f.comments, f.created_at,
                   su.full_name AS student_name, mu.full_name AS mentor_name
            FROM feedback f
            JOIN users su ON su.id = f.student_id
            JOIN users mu ON mu.id = f.mentor_id
            WHERE f.session_id = ?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", rs.getLong("id"));
                    obj.put("sessionId", rs.getLong("session_id"));
                    obj.put("studentId", rs.getLong("student_id"));
                    obj.put("mentorId", rs.getLong("mentor_id"));
                    obj.put("rating", rs.getInt("rating"));
                    obj.put("comments", rs.getString("comments"));
                    obj.put("studentName", rs.getString("student_name"));
                    obj.put("mentorName", rs.getString("mentor_name"));
                    obj.put("createdAt", rs.getTimestamp("created_at").toString());
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Returns feedback reviews and rating aggregate for a mentor.
     */
    public JSONObject getMentorFeedbackSummary(long mentorId) throws SQLException {
        String countSql = """
            SELECT COUNT(*) AS total_reviews,
                   COALESCE(AVG(rating), 0.0) AS avg_rating,
                   SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) AS r5,
                   SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) AS r4,
                   SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) AS r3,
                   SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) AS r2,
                   SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) AS r1
            FROM feedback
            WHERE mentor_id = ?
        """;

        JSONObject result = new JSONObject();
        result.put("mentorId", mentorId);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(countSql)) {
            ps.setLong(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_reviews");
                    double avg = rs.getDouble("avg_rating");
                    result.put("totalReviews", total);
                    result.put("averageRating", Math.round(avg * 10.0) / 10.0);

                    JSONObject dist = new JSONObject();
                    dist.put("5", rs.getInt("r5"));
                    dist.put("4", rs.getInt("r4"));
                    dist.put("3", rs.getInt("r3"));
                    dist.put("2", rs.getInt("r2"));
                    dist.put("1", rs.getInt("r1"));
                    result.put("ratingDistribution", dist);
                }
            }
        }

        // Fetch recent reviews
        String reviewsSql = """
            SELECT f.id, f.session_id, f.student_id, f.rating, f.comments, f.created_at,
                   su.full_name AS student_name
            FROM feedback f
            JOIN users su ON su.id = f.student_id
            WHERE f.mentor_id = ?
            ORDER BY f.created_at DESC
            LIMIT 50
        """;

        JSONArray reviews = new JSONArray();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(reviewsSql)) {
            ps.setLong(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject rev = new JSONObject();
                    rev.put("id", rs.getLong("id"));
                    rev.put("sessionId", rs.getLong("session_id"));
                    rev.put("studentId", rs.getLong("student_id"));
                    rev.put("studentName", rs.getString("student_name"));
                    rev.put("rating", rs.getInt("rating"));
                    rev.put("comments", rs.getString("comments"));
                    rev.put("createdAt", rs.getTimestamp("created_at").toString());
                    reviews.put(rev);
                }
            }
        }
        result.put("reviews", reviews);

        return result;
    }

    /**
     * Returns all feedback records (e.g. for Admin monitoring).
     */
    public JSONArray getAllFeedback() throws SQLException {
        String sql = """
            SELECT f.id, f.session_id, f.student_id, f.mentor_id, f.rating, f.comments, f.created_at,
                   su.full_name AS student_name, mu.full_name AS mentor_name
            FROM feedback f
            JOIN users su ON su.id = f.student_id
            JOIN users mu ON mu.id = f.mentor_id
            ORDER BY f.created_at DESC
        """;

        JSONArray list = new JSONArray();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getLong("id"));
                obj.put("sessionId", rs.getLong("session_id"));
                obj.put("studentId", rs.getLong("student_id"));
                obj.put("mentorId", rs.getLong("mentor_id"));
                obj.put("rating", rs.getInt("rating"));
                obj.put("comments", rs.getString("comments"));
                obj.put("studentName", rs.getString("student_name"));
                obj.put("mentorName", rs.getString("mentor_name"));
                obj.put("createdAt", rs.getTimestamp("created_at").toString());
                list.put(obj);
            }
        }
        return list;
    }
}
