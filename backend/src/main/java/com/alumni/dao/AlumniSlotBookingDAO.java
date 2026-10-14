package com.alumni.dao;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;

public class AlumniSlotBookingDAO {

    public long createBooking(
            long eventId,
            long alumniId,
            long bookedBy,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            String purpose,
            String notes) throws SQLException {

        if (slotDate == null || startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "A valid date and time range are required.");
        }

        if (!"JUDGE".equals(purpose)
                && !"GUEST_SESSION".equals(purpose)
                && !"SPONSOR".equals(purpose)) {
            throw new IllegalArgumentException("Invalid booking purpose.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                // Lock the alumni row so concurrent booking requests
                // for this alumni are serialized.
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT user_id FROM alumni "
                      + "WHERE user_id = ? FOR UPDATE")) {
                    lock.setLong(1, alumniId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Alumni not found.");
                        }
                    }
                }

                try (PreparedStatement event = connection.prepareStatement(
                        "SELECT id FROM events WHERE id = ?")) {
                    event.setLong(1, eventId);
                    try (ResultSet rs = event.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Event not found.");
                        }
                    }
                }

                // A BOOKED slot for the same alumni cannot overlap.
                String conflictSql =
                        "SELECT id FROM alumni_slot_bookings "
                      + "WHERE alumni_id = ? AND slot_date = ? "
                      + "AND status = 'BOOKED' "
                      + "AND start_time < ? AND end_time > ? "
                      + "FOR UPDATE";

                try (PreparedStatement conflict =
                        connection.prepareStatement(conflictSql)) {
                    conflict.setLong(1, alumniId);
                    conflict.setDate(2, Date.valueOf(slotDate));
                    conflict.setTime(3, Time.valueOf(endTime));
                    conflict.setTime(4, Time.valueOf(startTime));

                    try (ResultSet rs = conflict.executeQuery()) {
                        if (rs.next()) {
                            throw new BookingConflictException(
                                    "The alumni already has a booking "
                                  + "during this time.");
                        }
                    }
                }

                String insertSql =
                        "INSERT INTO alumni_slot_bookings "
                      + "(event_id, alumni_id, booked_by, slot_date, "
                      + "start_time, end_time, booking_purpose, "
                      + "status, notes) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?, 'BOOKED', ?)";

                long bookingId;

                try (PreparedStatement insert = connection.prepareStatement(
                        insertSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, eventId);
                    insert.setLong(2, alumniId);
                    insert.setLong(3, bookedBy);
                    insert.setDate(4, Date.valueOf(slotDate));
                    insert.setTime(5, Time.valueOf(startTime));
                    insert.setTime(6, Time.valueOf(endTime));
                    insert.setString(7, purpose);

                    if (notes == null || notes.isBlank()) {
                        insert.setNull(8, Types.VARCHAR);
                    } else {
                        insert.setString(8, notes);
                    }

                    insert.executeUpdate();

                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve booking ID.");
                        }
                        bookingId = keys.getLong(1);
                    }
                }

                connection.commit();
                return bookingId;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public JSONArray getBookings(long userId, boolean isAdmin)
            throws SQLException {

        String sql =
                "SELECT b.id, b.event_id, e.title AS event_title, "
              + "b.alumni_id, u.full_name AS alumni_name, "
              + "b.booked_by, b.slot_date, b.start_time, b.end_time, "
              + "b.booking_purpose, b.status, b.notes, b.created_at "
              + "FROM alumni_slot_bookings b "
              + "JOIN events e ON e.id = b.event_id "
              + "JOIN users u ON u.id = b.alumni_id "
              + (isAdmin ? "" : "WHERE b.booked_by = ? ")
              + "ORDER BY b.slot_date, b.start_time";

        JSONArray results = new JSONArray();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (!isAdmin) {
                statement.setLong(1, userId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("id", rs.getLong("id"));
                    item.put("eventId", rs.getLong("event_id"));
                    item.put("eventTitle", rs.getString("event_title"));
                    item.put("alumniId", rs.getLong("alumni_id"));
                    item.put("alumniName", rs.getString("alumni_name"));
                    item.put("bookedBy", rs.getLong("booked_by"));
                    item.put("slotDate", rs.getDate("slot_date").toString());
                    item.put("startTime", rs.getTime("start_time").toString());
                    item.put("endTime", rs.getTime("end_time").toString());
                    item.put("purpose", rs.getString("booking_purpose"));
                    item.put("status", rs.getString("status"));
                    item.put("notes", rs.getString("notes"));
                    item.put("createdAt",
                            rs.getTimestamp("created_at").toString());
                    results.put(item);
                }
            }
        }

        return results;
    }

    public boolean updateStatus(
            long bookingId, long userId, boolean isAdmin, String status)
            throws SQLException {

        if (!"CANCELLED".equals(status)
                && !"COMPLETED".equals(status)) {
            throw new IllegalArgumentException(
                    "Status must be CANCELLED or COMPLETED.");
        }

        String sql =
                "UPDATE alumni_slot_bookings SET status = ? "
              + "WHERE id = ? "
              + (isAdmin ? "" : "AND booked_by = ")
              + (isAdmin ? "" : "? ")
              + "AND status = 'BOOKED'";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setLong(2, bookingId);

            if (!isAdmin) {
                statement.setLong(3, userId);
            }

            return statement.executeUpdate() > 0;
        }
    }

    public static class BookingConflictException
            extends SQLException {
        public BookingConflictException(String message) {
            super(message, "45000");
        }
    }
}