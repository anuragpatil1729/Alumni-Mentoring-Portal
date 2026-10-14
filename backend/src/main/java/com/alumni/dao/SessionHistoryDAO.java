package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SessionHistoryDAO {

    /**
     * Creates a new session-history record.
     */
    public long createSession(
            long studentId,
            long alumniId,
            Long requestId,
            String priority) throws SQLException {

        String safePriority = priority == null
                ? "MEDIUM"
                : priority.trim().toUpperCase(Locale.ROOT);

        if (!safePriority.matches("LOW|MEDIUM|HIGH|URGENT")) {
            throw new IllegalArgumentException(
                    "Priority must be LOW, MEDIUM, HIGH, or URGENT.");
        }

        String sql =
                "INSERT INTO session_history "
                + "(student_id, alumni_id, mentorship_request_id, "
                + "started_at, status, priority) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'STARTED', ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setLong(1, studentId);
            p.setLong(2, alumniId);

            if (requestId == null) {
                p.setNull(3, Types.BIGINT);
            } else {
                p.setLong(3, requestId);
            }

            p.setString(4, safePriority);
            p.executeUpdate();

            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }

            throw new SQLException(
                    "Session history record was not created.");
        }
    }

    /**
     * Updates a session status after checking the permitted transition.
     *
     * The caller must determine admin from the authenticated server-side
     * identity. Never accept the admin argument directly from client input.
     */
    public boolean updateStatus(
            long id,
            long userId,
            String nextStatus,
            boolean admin) throws SQLException {

        if (nextStatus == null
                || !nextStatus.matches(
                        "STARTED|PAUSED|COMPLETED|FAILED")) {
            throw new IllegalArgumentException(
                    "Invalid session status.");
        }

        try (Connection c = DBConnection.getConnection()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            try {
                String selectSql =
                        "SELECT status FROM session_history WHERE id=?"
                        + (admin ? "" : " AND student_id=?")
                        + " FOR UPDATE";

                String currentStatus = null;

                try (PreparedStatement p =
                             c.prepareStatement(selectSql)) {

                    p.setLong(1, id);

                    if (!admin) {
                        p.setLong(2, userId);
                    }

                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            currentStatus = r.getString("status");
                        }
                    }
                }

                if (currentStatus == null) {
                    c.rollback();
                    return false;
                }

                boolean allowed =
                        ("STARTED".equals(currentStatus)
                                && ("PAUSED".equals(nextStatus)
                                || "COMPLETED".equals(nextStatus)
                                || "FAILED".equals(nextStatus)))
                        || ("PAUSED".equals(currentStatus)
                                && ("STARTED".equals(nextStatus)
                                || "COMPLETED".equals(nextStatus)
                                || "FAILED".equals(nextStatus)));

                if (!allowed) {
                    throw new IllegalArgumentException(
                            "Invalid status transition: "
                            + currentStatus + " -> " + nextStatus);
                }

                String updateSql =
                        "UPDATE session_history SET status=?, "
                        + "paused_at = CASE "
                        + "WHEN ?='PAUSED' THEN CURRENT_TIMESTAMP "
                        + "ELSE paused_at END, "
                        + "completed_at = CASE "
                        + "WHEN ? IN ('COMPLETED','FAILED') "
                        + "THEN CURRENT_TIMESTAMP "
                        + "ELSE completed_at END, "
                        + "duration_minutes = CASE "
                        + "WHEN ? IN ('PAUSED','COMPLETED','FAILED') "
                        + "AND started_at IS NOT NULL "
                        + "THEN GREATEST(COALESCE(duration_minutes, 0), "
                        + "TIMESTAMPDIFF(MINUTE, started_at, "
                        + "CURRENT_TIMESTAMP)) "
                        + "ELSE duration_minutes END "
                        + "WHERE id=?"
                        + (admin ? "" : " AND student_id=?");

                try (PreparedStatement p =
                             c.prepareStatement(updateSql)) {

                    p.setString(1, nextStatus);
                    p.setString(2, nextStatus);
                    p.setString(3, nextStatus);
                    p.setString(4, nextStatus);
                    p.setLong(5, id);

                    if (!admin) {
                        p.setLong(6, userId);
                    }

                    p.executeUpdate();
                }

                c.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;

            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        }
    }

    /**
     * Returns session history for a student, or all records for an
     * administrator. The admin flag must come from authenticated
     * server-side authorization.
     */
    public JSONArray getHistory(
            long userId,
            String sort,
            boolean admin) throws SQLException {

        String sql =
                "SELECT id, student_id, alumni_id, "
                + "mentorship_request_id, status, priority, "
                + "started_at, paused_at, completed_at, "
                + "duration_minutes, failure_reason, created_at "
                + "FROM session_history"
                + (admin ? "" : " WHERE student_id=? OR alumni_id=?")
                + " ORDER BY created_at DESC";

        List<JSONObject> items = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            if (!admin) {
                p.setLong(1, userId);
                p.setLong(2, userId);
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    JSONObject item = new JSONObject()
                            .put("id", r.getLong("id"))
                            .put("studentId", r.getLong("student_id"))
                            .put("alumniId", r.getLong("alumni_id"))
                            .put("status", r.getString("status"))
                            .put("priority", r.getString("priority"))
                            .put("durationMinutes",
                                    r.getInt("duration_minutes"))
                            .put("failureReason",
                                    r.getString("failure_reason"));

                    long requestId =
                            r.getLong("mentorship_request_id");

                    item.put(
                            "mentorshipRequestId",
                            r.wasNull() ? JSONObject.NULL : requestId);

                    Timestamp started = r.getTimestamp("started_at");
                    Timestamp paused = r.getTimestamp("paused_at");
                    Timestamp completed = r.getTimestamp("completed_at");
                    Timestamp created = r.getTimestamp("created_at");

                    item.put("startedAt",
                            started == null
                                    ? JSONObject.NULL : started.toString());

                    item.put("pausedAt",
                            paused == null
                                    ? JSONObject.NULL : paused.toString());

                    item.put("completedAt",
                            completed == null
                                    ? JSONObject.NULL : completed.toString());

                    item.put("createdAt",
                            created == null
                                    ? JSONObject.NULL : created.toString());

                    items.add(item);
                }
            }
        }

        String sortKey = sort == null
                ? "chronological"
                : sort.trim().toLowerCase(Locale.ROOT);

        if (!sortKey.equals("chronological") && !sortKey.equals("duration")
                && !sortKey.equals("priority") && !sortKey.equals("severity")) {
            throw new IllegalArgumentException(
                    "Sort must be chronological, duration, priority, or severity.");
        }

        // Team D: Execute pure manual Insertion Sort algorithm
        com.alumni.search.SessionSorter.SortTelemetry telemetry =
                com.alumni.search.SessionSorter.insertionSort(items, sortKey, true);

        JSONArray result = new JSONArray();
        for (JSONObject item : telemetry.getSortedList()) {
            result.put(item);
        }

        return result;
    }
}
