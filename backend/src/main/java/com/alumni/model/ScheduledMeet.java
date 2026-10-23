package com.alumni.model;

import org.json.JSONObject;
import java.sql.Timestamp;

/**
 * ScheduledMeet
 *
 * Represents a session or meeting scheduled by a teacher/faculty member.
 * Can be 1-on-1 (specific student + mentor) or PUBLIC (1 mentor + N students).
 */
public class ScheduledMeet {

    private long id;
    private String title;
    private String description;
    private String meetType; // "ONE_ON_ONE" or "PUBLIC"
    private long mentorId;
    private Long studentId;
    private long createdByTeacherId;
    private String meetDate;
    private String startTime;
    private String endTime;
    private String meetingLink;
    private String venue;
    private Integer maxParticipants;
    private String status; // "SCHEDULED", "COMPLETED", "CANCELLED"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Joined metadata
    private String mentorName;
    private String mentorEmail;
    private String mentorCompany;
    private String mentorDesignation;
    private String studentName;
    private String studentEmail;
    private String studentDepartment;
    private String teacherName;
    private int attendeeCount;
    private boolean isRegistered; // whether current requesting student is registered

    public ScheduledMeet() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMeetType() { return meetType; }
    public void setMeetType(String meetType) { this.meetType = meetType; }

    public long getMentorId() { return mentorId; }
    public void setMentorId(long mentorId) { this.mentorId = mentorId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public long getCreatedByTeacherId() { return createdByTeacherId; }
    public void setCreatedByTeacherId(long createdByTeacherId) { this.createdByTeacherId = createdByTeacherId; }

    public String getMeetDate() { return meetDate; }
    public void setMeetDate(String meetDate) { this.meetDate = meetDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getMentorEmail() { return mentorEmail; }
    public void setMentorEmail(String mentorEmail) { this.mentorEmail = mentorEmail; }

    public String getMentorCompany() { return mentorCompany; }
    public void setMentorCompany(String mentorCompany) { this.mentorCompany = mentorCompany; }

    public String getMentorDesignation() { return mentorDesignation; }
    public void setMentorDesignation(String mentorDesignation) { this.mentorDesignation = mentorDesignation; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentDepartment() { return studentDepartment; }
    public void setStudentDepartment(String studentDepartment) { this.studentDepartment = studentDepartment; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public int getAttendeeCount() { return attendeeCount; }
    public void setAttendeeCount(int attendeeCount) { this.attendeeCount = attendeeCount; }

    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("description", description != null ? description : "");
        json.put("meetType", meetType);
        json.put("mentorId", mentorId);
        json.put("mentorName", mentorName != null ? mentorName : "");
        json.put("mentorEmail", mentorEmail != null ? mentorEmail : "");
        json.put("mentorCompany", mentorCompany != null ? mentorCompany : "");
        json.put("mentorDesignation", mentorDesignation != null ? mentorDesignation : "");
        json.put("studentId", studentId != null ? studentId : JSONObject.NULL);
        json.put("studentName", studentName != null ? studentName : "");
        json.put("studentEmail", studentEmail != null ? studentEmail : "");
        json.put("studentDepartment", studentDepartment != null ? studentDepartment : "");
        json.put("createdByTeacherId", createdByTeacherId);
        json.put("teacherName", teacherName != null ? teacherName : "");
        json.put("meetDate", meetDate);
        json.put("startTime", startTime);
        json.put("endTime", endTime);
        json.put("meetingLink", meetingLink != null ? meetingLink : "");
        json.put("venue", venue != null ? venue : "");
        json.put("maxParticipants", maxParticipants != null ? maxParticipants : JSONObject.NULL);
        json.put("attendeeCount", attendeeCount);
        json.put("isRegistered", isRegistered);
        json.put("status", status);
        json.put("createdAt", createdAt != null ? createdAt.toString() : "");
        json.put("updatedAt", updatedAt != null ? updatedAt.toString() : "");
        return json;
    }
}
