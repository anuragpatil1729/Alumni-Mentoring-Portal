
package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EventDAO {

    public long createEvent(JSONObject data, long alumniId)
            throws SQLException {

        String sql = """
            INSERT INTO events
                (title, description, date, start_time, end_time,
                 venue, organizer_alumni_id, event_type, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, data.getString("title").trim());
            ps.setString(2, data.getString("description").trim());
            ps.setString(3, data.getString("date"));
            ps.setString(4, data.getString("start_time"));
            ps.setString(5, data.getString("end_time"));
            ps.setString(6, data.getString("venue").trim());
            ps.setLong(7, alumniId);
            ps.setString(8, data.optString("event_type", "GENERAL")
                .toUpperCase());

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Event could not be created.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new SQLException("Event ID was not generated.");
    }

    public JSONArray getApprovedEvents() throws SQLException {
        return queryEvents(
            "SELECT * FROM events WHERE status = 'APPROVED' " +
            "ORDER BY date ASC, start_time ASC", null);
    }

    public JSONArray getAllEvents() throws SQLException {
        return queryEvents(
            "SELECT * FROM events ORDER BY created_at DESC", null);
    }

    public boolean updateStatus(long eventId, String status)
            throws SQLException {

        if (!"APPROVED".equals(status) &&
                !"REJECTED".equals(status)) {
            throw new IllegalArgumentException(
                "Status must be APPROVED or REJECTED.");
        }

        String sql = """
            UPDATE events
            SET status = ?
            WHERE id = ? AND status = 'PENDING'
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, eventId);
            return ps.executeUpdate() == 1;
        }
    }

    private JSONArray queryEvents(String sql, Long organizerId)
            throws SQLException {

        JSONArray result = new JSONArray();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (organizerId != null) {
                ps.setLong(1, organizerId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject event = new JSONObject();
                    event.put("id", rs.getLong("id"));
                    event.put("title", rs.getString("title"));
                    event.put("description", rs.getString("description"));
                    event.put("date", rs.getDate("date").toString());
                    event.put("start_time",
                        rs.getTime("start_time").toString());
                    event.put("end_time",
                        rs.getTime("end_time").toString());
                    event.put("venue", rs.getString("venue"));
                    event.put("organizer_alumni_id",
                        rs.getLong("organizer_alumni_id"));
                    event.put("event_type", rs.getString("event_type"));
                    event.put("status", rs.getString("status"));
                    event.put("created_at",
                        rs.getTimestamp("created_at").toString());
                    event.put("updated_at",
                        rs.getTimestamp("updated_at").toString());
                    result.put(event);
                }
            }
        }

        return result;
    }
}
