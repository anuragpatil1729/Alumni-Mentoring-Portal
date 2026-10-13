
package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OpportunityDAO {

    public long createOpportunity(JSONObject data, long alumniId)
            throws SQLException {

        String sql = """
            INSERT INTO opportunities
                (alumni_id, title, description, organization,
                 opportunity_type, location, eligibility, deadline,
                 application_link, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, alumniId);
            ps.setString(2, data.getString("title").trim());
            ps.setString(3, data.getString("description").trim());
            ps.setString(4, data.getString("organization").trim());
            ps.setString(5, data.getString("opportunity_type").trim());
            ps.setString(6, data.optString("location", ""));
            ps.setString(7, data.optString("eligibility", ""));

            String deadline = data.optString("deadline", "");
            if (deadline.isBlank()) {
                ps.setNull(8, java.sql.Types.DATE);
            } else {
                ps.setString(8, deadline);
            }

            String link = data.optString("application_link", "");
            if (link.isBlank()) {
                ps.setNull(9, java.sql.Types.VARCHAR);
            } else {
                ps.setString(9, link);
            }

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                    "Opportunity could not be created.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new SQLException("Opportunity ID was not generated.");
    }

    public JSONArray getOpenOpportunities() throws SQLException {
        JSONArray result = new JSONArray();

        String sql = """
            SELECT * FROM opportunities
            WHERE status = 'OPEN'
              AND (deadline IS NULL OR deadline >= CURRENT_DATE)
            ORDER BY created_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("id", rs.getLong("id"));
                item.put("alumni_id", rs.getLong("alumni_id"));
                item.put("title", rs.getString("title"));
                item.put("description", rs.getString("description"));
                item.put("organization", rs.getString("organization"));
                item.put("opportunity_type",
                    rs.getString("opportunity_type"));
                item.put("location", rs.getString("location"));
                item.put("eligibility", rs.getString("eligibility"));

                java.sql.Date deadline = rs.getDate("deadline");
                item.put("deadline",
                    deadline == null ? JSONObject.NULL : deadline.toString());

                item.put("application_link",
                    rs.getString("application_link"));
                item.put("status", rs.getString("status"));
                result.put(item);
            }
        }

        return result;
    }

    public boolean updateStatus(long opportunityId, long alumniId,
                                String status) throws SQLException {

        if (!"OPEN".equals(status) &&
                !"CLOSED".equals(status) &&
                !"CANCELLED".equals(status)) {
            throw new IllegalArgumentException(
                "Invalid opportunity status.");
        }

        String sql = """
            UPDATE opportunities
            SET status = ?
            WHERE id = ? AND alumni_id = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, opportunityId);
            ps.setLong(3, alumniId);
            return ps.executeUpdate() == 1;
        }
    }
}
