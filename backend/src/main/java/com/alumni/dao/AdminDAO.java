package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    public JSONArray getUsers(String role, String activeFilter) throws SQLException {
        return getUsers(role, activeFilter, null);
    }

    public JSONArray getUsers(String role, String activeFilter, String approvalFilter)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT id, full_name, email, mobile_number, role, " +
            "is_active, COALESCE(approval_status, 'APPROVED') AS approval_status, created_at FROM users WHERE 1=1"
        );

        if (role != null && !role.isBlank()) {
            sql.append(" AND role = ?");
        }

        if ("true".equalsIgnoreCase(activeFilter)) {
            sql.append(" AND is_active = TRUE");
        } else if ("false".equalsIgnoreCase(activeFilter)) {
            sql.append(" AND is_active = FALSE");
        }

        if (approvalFilter != null && !approvalFilter.isBlank()) {
            sql.append(" AND approval_status = ?");
        }

        sql.append(" ORDER BY created_at DESC");

        JSONArray users = new JSONArray();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int index = 1;
            if (role != null && !role.isBlank()) {
                ps.setString(index++, role.toLowerCase());
            }

            if (approvalFilter != null && !approvalFilter.isBlank()) {
                ps.setString(index++, approvalFilter.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject user = new JSONObject();
                    user.put("id", rs.getLong("id"));
                    user.put("fullName", rs.getString("full_name"));
                    user.put("email", rs.getString("email"));
                    user.put("mobileNumber", rs.getString("mobile_number"));
                    user.put("role", rs.getString("role"));
                    user.put("active", rs.getBoolean("is_active"));
                    user.put("approvalStatus", rs.getString("approval_status"));
                    user.put("createdAt", rs.getTimestamp("created_at").toString());
                    users.put(user);
                }
            }
        }

        return users;
    }

    public boolean setUserActive(long userId, boolean active)
            throws SQLException {

        String sql = "UPDATE users SET is_active = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setLong(2, userId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean approveUser(long userId, boolean approved) throws SQLException {
        String status = approved ? "APPROVED" : "REJECTED";
        String sql = "UPDATE users SET approval_status = ?, is_active = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setBoolean(2, approved);
            ps.setLong(3, userId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUser(long userId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Delete dependent child table entries where ON DELETE CASCADE is missing or restrictive
                // Feedback
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM feedback WHERE student_id = ? OR mentor_id = ?")) {
                    ps.setLong(1, userId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                // Session history
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM session_history WHERE student_id = ? OR alumni_id = ?")) {
                    ps.setLong(1, userId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                // Mentorship requests
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM mentorship_requests WHERE student_id = ? OR mentor_id = ?")) {
                    ps.setLong(1, userId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                // Auth tokens
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM auth_tokens WHERE user_id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                // Password resets
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM password_resets WHERE user_id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                // Students profile
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM students WHERE user_id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                // Alumni profile
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM alumni WHERE user_id = ?")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                // Users record
                boolean deleted;
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    deleted = (ps.executeUpdate() == 1);
                }

                con.commit();
                return deleted;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean updateEventStatus(long eventId, String status)
            throws SQLException {

        String sql = "UPDATE events SET status = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, eventId);

            return ps.executeUpdate() == 1;
        }
    }
}
