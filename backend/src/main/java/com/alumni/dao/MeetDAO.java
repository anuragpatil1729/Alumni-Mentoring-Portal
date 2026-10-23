package com.alumni.dao;

import com.alumni.config.DBConnection;
import com.alumni.model.ScheduledMeet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MeetDAO {

    public static class ConflictException extends Exception {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class CapacityException extends Exception {
        public CapacityException(String message) {
            super(message);
        }
    }

    /**
     * Check for scheduling conflicts for the mentor or student
     */
    public boolean hasTimeConflict(long mentorId, Long studentId, String meetDate, String startTime, String endTime, Long excludeMeetId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(1) FROM scheduled_meets WHERE meet_date = ? AND status = 'SCHEDULED' " +
            "AND ((start_time < ? AND end_time > ?) OR (start_time >= ? AND start_time < ?)) " +
            "AND (mentor_id = ? "
        );
        if (studentId != null) {
            sql.append("OR student_id = ?");
        }
        sql.append(")");
        if (excludeMeetId != null) {
            sql.append(" AND id != ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, meetDate);
            stmt.setString(idx++, endTime);
            stmt.setString(idx++, startTime);
            stmt.setString(idx++, startTime);
            stmt.setString(idx++, endTime);
            stmt.setLong(idx++, mentorId);
            if (studentId != null) {
                stmt.setLong(idx++, studentId);
            }
            if (excludeMeetId != null) {
                stmt.setLong(idx++, excludeMeetId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Create a new scheduled meet (1-on-1 or Public)
     */
    public long createMeet(ScheduledMeet meet) throws SQLException, ConflictException {
        // Validate conflict
        if (hasTimeConflict(meet.getMentorId(), meet.getStudentId(), meet.getMeetDate(), meet.getStartTime(), meet.getEndTime(), null)) {
            throw new ConflictException("Scheduling conflict: Mentor or student already has a session during this time window.");
        }

        String sql = "INSERT INTO scheduled_meets (title, description, meet_type, mentor_id, student_id, " +
                "created_by_teacher_id, meet_date, start_time, end_time, meeting_link, venue, max_participants, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, meet.getTitle());
            stmt.setString(2, meet.getDescription());
            stmt.setString(3, meet.getMeetType());
            stmt.setLong(4, meet.getMentorId());
            if (meet.getStudentId() != null) {
                stmt.setLong(5, meet.getStudentId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            stmt.setLong(6, meet.getCreatedByTeacherId());
            stmt.setString(7, meet.getMeetDate());
            stmt.setString(8, meet.getStartTime());
            stmt.setString(9, meet.getEndTime());
            stmt.setString(10, meet.getMeetingLink());
            stmt.setString(11, meet.getVenue());
            if (meet.getMaxParticipants() != null) {
                stmt.setInt(12, meet.getMaxParticipants());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated meet ID.");
    }

    /**
     * Retrieve all meets matching criteria
     */
    public List<ScheduledMeet> getMeets(Long teacherId, Long mentorId, Long studentId, String meetType, String status, Long currentUserId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT sm.*, " +
            "u_mentor.full_name AS mentor_name, u_mentor.email AS mentor_email, " +
            "a.company AS mentor_company, a.designation AS mentor_designation, " +
            "u_student.full_name AS student_name, u_student.email AS student_email, " +
            "s.department AS student_dept, " +
            "u_teacher.full_name AS teacher_name, " +
            "(SELECT COUNT(1) FROM meet_attendees ma WHERE ma.meet_id = sm.id) AS attendee_count "
        );
        if (currentUserId != null) {
            sql.append(", (SELECT COUNT(1) FROM meet_attendees ma2 WHERE ma2.meet_id = sm.id AND ma2.student_id = ?) AS is_enrolled ");
        }
        sql.append("FROM scheduled_meets sm ");
        sql.append("LEFT JOIN users u_mentor ON sm.mentor_id = u_mentor.id ");
        sql.append("LEFT JOIN alumni a ON sm.mentor_id = a.user_id ");
        sql.append("LEFT JOIN users u_student ON sm.student_id = u_student.id ");
        sql.append("LEFT JOIN students s ON sm.student_id = s.user_id ");
        sql.append("LEFT JOIN users u_teacher ON sm.created_by_teacher_id = u_teacher.id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (currentUserId != null) {
            params.add(currentUserId);
        }

        if (teacherId != null) {
            sql.append("AND sm.created_by_teacher_id = ? ");
            params.add(teacherId);
        }
        if (mentorId != null) {
            sql.append("AND sm.mentor_id = ? ");
            params.add(mentorId);
        }
        if (studentId != null) {
            sql.append("AND (sm.student_id = ? OR sm.id IN (SELECT meet_id FROM meet_attendees WHERE student_id = ?)) ");
            params.add(studentId);
            params.add(studentId);
        }
        if (meetType != null && !meetType.trim().isEmpty() && !"ALL".equalsIgnoreCase(meetType)) {
            sql.append("AND sm.meet_type = ? ");
            params.add(meetType.toUpperCase());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND sm.status = ? ");
            params.add(status.toUpperCase());
        }

        sql.append("ORDER BY sm.meet_date ASC, sm.start_time ASC");

        List<ScheduledMeet> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ScheduledMeet m = mapRow(rs, currentUserId != null);
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * Get a single meet by ID
     */
    public ScheduledMeet getMeetById(long meetId, Long currentUserId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT sm.*, " +
            "u_mentor.full_name AS mentor_name, u_mentor.email AS mentor_email, " +
            "a.company AS mentor_company, a.designation AS mentor_designation, " +
            "u_student.full_name AS student_name, u_student.email AS student_email, " +
            "s.department AS student_dept, " +
            "u_teacher.full_name AS teacher_name, " +
            "(SELECT COUNT(1) FROM meet_attendees ma WHERE ma.meet_id = sm.id) AS attendee_count "
        );
        if (currentUserId != null) {
            sql.append(", (SELECT COUNT(1) FROM meet_attendees ma2 WHERE ma2.meet_id = sm.id AND ma2.student_id = ?) AS is_enrolled ");
        }
        sql.append("FROM scheduled_meets sm ");
        sql.append("LEFT JOIN users u_mentor ON sm.mentor_id = u_mentor.id ");
        sql.append("LEFT JOIN alumni a ON sm.mentor_id = a.user_id ");
        sql.append("LEFT JOIN users u_student ON sm.student_id = u_student.id ");
        sql.append("LEFT JOIN students s ON sm.student_id = s.user_id ");
        sql.append("LEFT JOIN users u_teacher ON sm.created_by_teacher_id = u_teacher.id ");
        sql.append("WHERE sm.id = ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (currentUserId != null) {
                stmt.setLong(idx++, currentUserId);
            }
            stmt.setLong(idx, meetId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs, currentUserId != null);
                }
            }
        }
        return null;
    }

    /**
     * Update meet status ('SCHEDULED', 'COMPLETED', 'CANCELLED')
     */
    public boolean updateStatus(long meetId, String status) throws SQLException {
        String sql = "UPDATE scheduled_meets SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toUpperCase());
            stmt.setLong(2, meetId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Student registers / joins a public meet
     */
    public boolean registerStudent(long meetId, long studentId) throws SQLException, CapacityException {
        ScheduledMeet meet = getMeetById(meetId, studentId);
        if (meet == null) {
            throw new SQLException("Meet not found.");
        }
        if (!"PUBLIC".equalsIgnoreCase(meet.getMeetType())) {
            throw new SQLException("Only public group sessions allow open registration.");
        }
        if ("CANCELLED".equalsIgnoreCase(meet.getStatus())) {
            throw new SQLException("Cannot join a cancelled session.");
        }
        if (meet.getMaxParticipants() != null && meet.getAttendeeCount() >= meet.getMaxParticipants()) {
            throw new CapacityException("This public session has reached maximum capacity (" + meet.getMaxParticipants() + " attendees).");
        }

        String sql = "INSERT IGNORE INTO meet_attendees (meet_id, student_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, meetId);
            stmt.setLong(2, studentId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Student cancels / leaves a public meet
     */
    public boolean unregisterStudent(long meetId, long studentId) throws SQLException {
        String sql = "DELETE FROM meet_attendees WHERE meet_id = ? AND student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, meetId);
            stmt.setLong(2, studentId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Get attendee list for a public meet
     */
    public JSONArray getMeetAttendees(long meetId) throws SQLException {
        String sql = "SELECT ma.registered_at, u.id AS student_id, u.full_name, u.email, u.mobile_number, " +
                "s.student_id AS roll_number, s.department, s.graduation_year " +
                "FROM meet_attendees ma " +
                "JOIN users u ON ma.student_id = u.id " +
                "LEFT JOIN students s ON u.id = s.user_id " +
                "WHERE ma.meet_id = ? ORDER BY ma.registered_at ASC";

        JSONArray array = new JSONArray();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, meetId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("studentId", rs.getLong("student_id"));
                    obj.put("fullName", rs.getString("full_name"));
                    obj.put("email", rs.getString("email"));
                    obj.put("mobileNumber", rs.getString("mobile_number"));
                    obj.put("rollNumber", rs.getString("roll_number") != null ? rs.getString("roll_number") : "");
                    obj.put("department", rs.getString("department") != null ? rs.getString("department") : "");
                    obj.put("graduationYear", rs.getInt("graduation_year"));
                    obj.put("registeredAt", rs.getTimestamp("registered_at").toString());
                    array.put(obj);
                }
            }
        }
        return array;
    }

    /**
     * Get all active students for dropdown selection
     */
    public JSONArray getAllStudents() throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.email, s.student_id AS roll_number, s.department " +
                "FROM users u " +
                "JOIN students s ON u.id = s.user_id " +
                "WHERE u.is_active = TRUE ORDER BY u.full_name ASC";

        JSONArray array = new JSONArray();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getLong("id"));
                obj.put("fullName", rs.getString("full_name"));
                obj.put("email", rs.getString("email"));
                obj.put("rollNumber", rs.getString("roll_number") != null ? rs.getString("roll_number") : "");
                obj.put("department", rs.getString("department") != null ? rs.getString("department") : "");
                array.put(obj);
            }
        }
        return array;
    }

    /**
     * Get all mentors for dropdown selection
     */
    public JSONArray getAllMentors() throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.email, a.company, a.designation, a.department " +
                "FROM users u " +
                "JOIN alumni a ON u.id = a.user_id " +
                "WHERE u.is_active = TRUE ORDER BY u.full_name ASC";

        JSONArray array = new JSONArray();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getLong("id"));
                obj.put("fullName", rs.getString("full_name"));
                obj.put("email", rs.getString("email"));
                obj.put("company", rs.getString("company") != null ? rs.getString("company") : "");
                obj.put("designation", rs.getString("designation") != null ? rs.getString("designation") : "");
                obj.put("department", rs.getString("department") != null ? rs.getString("department") : "");
                array.put(obj);
            }
        }
        return array;
    }

    private ScheduledMeet mapRow(ResultSet rs, boolean hasEnrolledCheck) throws SQLException {
        ScheduledMeet m = new ScheduledMeet();
        m.setId(rs.getLong("id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setMeetType(rs.getString("meet_type"));
        m.setMentorId(rs.getLong("mentor_id"));
        long sId = rs.getLong("student_id");
        if (!rs.wasNull()) {
            m.setStudentId(sId);
        }
        m.setCreatedByTeacherId(rs.getLong("created_by_teacher_id"));
        m.setMeetDate(rs.getString("meet_date"));
        m.setStartTime(rs.getString("start_time"));
        m.setEndTime(rs.getString("end_time"));
        m.setMeetingLink(rs.getString("meeting_link"));
        m.setVenue(rs.getString("venue"));
        int max = rs.getInt("max_participants");
        if (!rs.wasNull()) {
            m.setMaxParticipants(max);
        }
        m.setStatus(rs.getString("status"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setUpdatedAt(rs.getTimestamp("updated_at"));

        m.setMentorName(rs.getString("mentor_name"));
        m.setMentorEmail(rs.getString("mentor_email"));
        m.setMentorCompany(rs.getString("mentor_company"));
        m.setMentorDesignation(rs.getString("mentor_designation"));

        m.setStudentName(rs.getString("student_name"));
        m.setStudentEmail(rs.getString("student_email"));
        m.setStudentDepartment(rs.getString("student_dept"));

        m.setTeacherName(rs.getString("teacher_name"));
        m.setAttendeeCount(rs.getInt("attendee_count"));

        if (hasEnrolledCheck) {
            m.setRegistered(rs.getInt("is_enrolled") > 0);
        }
        return m;
    }
}
