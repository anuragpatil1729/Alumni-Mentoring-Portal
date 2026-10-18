package com.alumni.model;

import org.json.JSONObject;
import java.sql.Timestamp;

/**
 * Feedback
 *
 * Represents mentoring feedback provided by a student upon completion of a session.
 */
public class Feedback {

    private long id;
    private long sessionId;
    private long studentId;
    private long mentorId;
    private int rating;
    private String comments;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Optional metadata joined from users
    private String studentName;
    private String mentorName;

    public Feedback() {}

    public Feedback(long sessionId, long studentId, long mentorId, int rating, String comments) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.mentorId = mentorId;
        this.rating = rating;
        this.comments = comments;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public long getMentorId() { return mentorId; }
    public void setMentorId(long mentorId) { this.mentorId = mentorId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("sessionId", sessionId);
        json.put("studentId", studentId);
        json.put("mentorId", mentorId);
        json.put("rating", rating);
        json.put("comments", comments != null ? comments : "");
        json.put("createdAt", createdAt != null ? createdAt.toString() : JSONObject.NULL);
        json.put("updatedAt", updatedAt != null ? updatedAt.toString() : JSONObject.NULL);
        if (studentName != null) json.put("studentName", studentName);
        if (mentorName != null) json.put("mentorName", mentorName);
        return json;
    }
}
