package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TeacherDAO {

    // View events assigned to a teacher
    public JSONArray getScheduledEvents(long teacherId) throws SQLException {
        String sql =
            "SELECT e.id, e.title, e.description, e.date, " +
            "e.start_time, e.end_time, e.venue, e.event_type, e.status " +
            "FROM events e " +
            "JOIN teacher_event_assignments tea ON tea.event_id = e.id " +
            "WHERE tea.teacher_id = ? " +
            "ORDER BY e.date ASC, e.start_time ASC";

        JSONArray events = new JSONArray();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teacherId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject event = new JSONObject();
                    event.put("id", rs.getLong("id"));
                    event.put("title", rs.getString("title"));
                    event.put("description", rs.getString("description"));
                    event.put("date", rs.getString("date"));
                    event.put("startTime", rs.getString("start_time"));
                    event.put("endTime", rs.getString("end_time"));
                    event.put("venue", rs.getString("venue"));
                    event.put("eventType", rs.getString("event_type"));
                    event.put("status", rs.getString("status"));
                    events.put(event);
                }
            }
        }

        return events;
    }

    // View event details only if the teacher is assigned to it
    public JSONObject getAssignedEvent(long eventId, long teacherId)
            throws SQLException {

        String sql =
            "SELECT e.id, e.title, e.description, e.date, " +
            "e.start_time, e.end_time, e.venue, e.event_type, e.status " +
            "FROM events e " +
            "JOIN teacher_event_assignments tea ON tea.event_id = e.id " +
            "WHERE e.id = ? AND tea.teacher_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, eventId);
            ps.setLong(2, teacherId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                JSONObject event = new JSONObject();
                event.put("id", rs.getLong("id"));
                event.put("title", rs.getString("title"));
                event.put("description", rs.getString("description"));
                event.put("date", rs.getString("date"));
                event.put("startTime", rs.getString("start_time"));
                event.put("endTime", rs.getString("end_time"));
                event.put("venue", rs.getString("venue"));
                event.put("eventType", rs.getString("event_type"));
                event.put("status", rs.getString("status"));

                return event;
            }
        }
    }

    // View students and their marks for an assigned event
    public JSONArray getEventMarks(long eventId, long teacherId)
            throws SQLException {

        String sql =
            "SELECT s.user_id AS student_id, u.full_name, " +
            "im.id AS mark_id, im.marks, im.remarks " +
            "FROM teacher_event_assignments tea " +
            "JOIN students s ON 1 = 1 " +
            "JOIN users u ON u.id = s.user_id " +
            "LEFT JOIN internal_marks im " +
            "ON im.student_id = s.user_id AND im.event_id = tea.event_id " +
            "WHERE tea.event_id = ? AND tea.teacher_id = ? " +
            "ORDER BY u.full_name";

        JSONArray students = new JSONArray();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, eventId);
            ps.setLong(2, teacherId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject student = new JSONObject();
                    student.put("studentId", rs.getLong("student_id"));
                    student.put("fullName", rs.getString("full_name"));

                    long markId = rs.getLong("mark_id");
                    student.put("markId",
                            rs.wasNull() ? JSONObject.NULL : markId);

                    double marks = rs.getDouble("marks");
                    student.put("marks",
                            rs.wasNull() ? JSONObject.NULL : marks);

                    student.put("remarks", rs.getString("remarks"));
                    students.put(student);
                }
            }
        }

        return students;
    }
    // Create marks for a student in an assigned event
public void createInternalMark(
        long eventId,
        long studentId,
        long teacherId,
        BigDecimal marks,
        BigDecimal maxMarks,
        String remarks) throws SQLException {

    validateMarks(marks, maxMarks);
    verifyAssignmentAndStudent(eventId, studentId, teacherId);

    String sql =
        "INSERT INTO internal_marks " +
        "(student_id, event_id, teacher_id, marks, max_marks, remarks) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, studentId);
        ps.setLong(2, eventId);
        ps.setLong(3, teacherId);
        ps.setBigDecimal(4, marks);
        ps.setBigDecimal(5, maxMarks);
        ps.setString(6, remarks);

        ps.executeUpdate();
    }
}

// Update marks for an existing student/event record
public boolean updateInternalMark(
        long eventId,
        long studentId,
        long teacherId,
        BigDecimal marks,
        BigDecimal maxMarks,
        String remarks) throws SQLException {

    validateMarks(marks, maxMarks);

    String sql =
        "UPDATE internal_marks im " +
        "JOIN teacher_event_assignments tea " +
        "ON tea.event_id = im.event_id AND tea.teacher_id = ? " +
        "SET im.marks = ?, im.max_marks = ?, im.remarks = ?, " +
        "im.teacher_id = ? " +
        "WHERE im.event_id = ? AND im.student_id = ?";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, teacherId);
        ps.setBigDecimal(2, marks);
        ps.setBigDecimal(3, maxMarks);
        ps.setString(4, remarks);
        ps.setLong(5, teacherId);
        ps.setLong(6, eventId);
        ps.setLong(7, studentId);

        return ps.executeUpdate() > 0;
    }
}

private void verifyAssignmentAndStudent(
        long eventId, long studentId, long teacherId)
        throws SQLException {

    String sql =
        "SELECT 1 FROM teacher_event_assignments tea " +
        "JOIN students s ON s.user_id = ? " +
        "WHERE tea.event_id = ? AND tea.teacher_id = ?";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setLong(1, studentId);
        ps.setLong(2, eventId);
        ps.setLong(3, teacherId);

        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalArgumentException(
                    "Student or teacher-event assignment not found.");
            }
        }
    }
}

private void validateMarks(BigDecimal marks, BigDecimal maxMarks) {
    if (marks == null || maxMarks == null
            || maxMarks.compareTo(BigDecimal.ZERO) <= 0
            || marks.compareTo(BigDecimal.ZERO) < 0
            || marks.compareTo(maxMarks) > 0
            || marks.precision() > 5
            || maxMarks.precision() > 5
            || marks.scale() > 2
            || maxMarks.scale() > 2) {
        throw new IllegalArgumentException(
            "Marks must be between zero and maximum marks, " +
            "with at most two decimal places.");
    }
}
}