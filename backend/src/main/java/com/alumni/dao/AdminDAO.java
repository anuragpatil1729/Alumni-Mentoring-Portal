package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    public JSONArray getUsers(String role, String activeFilter)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT id, full_name, email, mobile_number, role, " +
            "is_active, created_at FROM users WHERE 1=1"
        );

        if (role != null && !role.isBlank()) {
            sql.append(" AND role = ?");
        }

        if ("true".equalsIgnoreCase(activeFilter)) {
            sql.append(" AND is_active = TRUE");
        } else if ("false".equalsIgnoreCase(activeFilter)) {
            sql.append(" AND is_active = FALSE");
        }

        sql.append(" ORDER BY created_at DESC");

        JSONArray users = new JSONArray();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int index = 1;
            if (role != null && !role.isBlank()) {
                ps.setString(index++, role.toLowerCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject user = new JSONObject();
                    user.put("id", rs.getLong("id"));
                    user.put("fullName", rs.getString("full_name"));
                    user.put("email", rs.getString("email"));
                    user.put("mobileNumber",
                            rs.getString("mobile_number"));
                    user.put("role", rs.getString("role"));
                    user.put("active", rs.getBoolean("is_active"));
                    user.put("createdAt",
                            rs.getTimestamp("created_at").toString());
                    users.put(user);
                }
            }
        }

        return users;
    }

    public boolean setUserActive(long userId, boolean active)
            throws SQLException {

        String sql =
            "UPDATE users SET is_active = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setLong(2, userId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateEventStatus(long eventId, String status)
            throws SQLException {

        String sql =
            "UPDATE events SET status = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, eventId);

            return ps.executeUpdate() == 1;
        }
    }
}
