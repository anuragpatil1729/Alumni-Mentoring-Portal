package com.alumni.model;

import org.json.JSONObject;
import java.sql.Timestamp;

public class MentorshipRequest {
    private long id;
    private long studentId;
    private long mentorId;
    private String sessionGoal;
    private String message;
    private String status; // PENDING, ACCEPTED, REJECTED, CANCELLED
    private String mentorResponse;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Enriched details
    private String studentName;
    private String studentEmail;
    private String studentRollNumber;
    private String studentDepartment;
    private Integer studentGraduationYear;
    private String studentAvatarUrl;

    private String mentorName;
    private String mentorEmail;
    private String mentorCompany;
    private String mentorDesignation;
    private String mentorDepartment;
    private String mentorAvatarUrl;

    public MentorshipRequest() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public long getMentorId() { return mentorId; }
    public void setMentorId(long mentorId) { this.mentorId = mentorId; }

    public String getSessionGoal() { return sessionGoal; }
    public void setSessionGoal(String sessionGoal) { this.sessionGoal = sessionGoal; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMentorResponse() { return mentorResponse; }
    public void setMentorResponse(String mentorResponse) { this.mentorResponse = mentorResponse; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentRollNumber() { return studentRollNumber; }
    public void setStudentRollNumber(String studentRollNumber) { this.studentRollNumber = studentRollNumber; }

    public String getStudentDepartment() { return studentDepartment; }
    public void setStudentDepartment(String studentDepartment) { this.studentDepartment = studentDepartment; }

    public Integer getStudentGraduationYear() { return studentGraduationYear; }
    public void setStudentGraduationYear(Integer studentGraduationYear) { this.studentGraduationYear = studentGraduationYear; }

    public String getStudentAvatarUrl() { return studentAvatarUrl; }
    public void setStudentAvatarUrl(String studentAvatarUrl) { this.studentAvatarUrl = studentAvatarUrl; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getMentorEmail() { return mentorEmail; }
    public void setMentorEmail(String mentorEmail) { this.mentorEmail = mentorEmail; }

    public String getMentorCompany() { return mentorCompany; }
    public void setMentorCompany(String mentorCompany) { this.mentorCompany = mentorCompany; }

    public String getMentorDesignation() { return mentorDesignation; }
    public void setMentorDesignation(String mentorDesignation) { this.mentorDesignation = mentorDesignation; }

    public String getMentorDepartment() { return mentorDepartment; }
    public void setMentorDepartment(String mentorDepartment) { this.mentorDepartment = mentorDepartment; }

    public String getMentorAvatarUrl() { return mentorAvatarUrl; }
    public void setMentorAvatarUrl(String mentorAvatarUrl) { this.mentorAvatarUrl = mentorAvatarUrl; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("studentId", studentId);
        json.put("mentorId", mentorId);
        json.put("sessionGoal", sessionGoal);
        json.put("message", message);
        json.put("status", status);
        json.put("mentorResponse", mentorResponse != null ? mentorResponse : JSONObject.NULL);
        json.put("createdAt", createdAt != null ? createdAt.toString() : "");
        json.put("updatedAt", updatedAt != null ? updatedAt.toString() : "");

        JSONObject sObj = new JSONObject();
        sObj.put("id", studentId);
        sObj.put("name", studentName != null ? studentName : "");
        sObj.put("email", studentEmail != null ? studentEmail : "");
        sObj.put("rollNumber", studentRollNumber != null ? studentRollNumber : "");
        sObj.put("department", studentDepartment != null ? studentDepartment : "");
        if (studentGraduationYear != null) sObj.put("graduationYear", studentGraduationYear);
        if (studentAvatarUrl != null) sObj.put("avatarUrl", studentAvatarUrl);
        json.put("student", sObj);

        JSONObject mObj = new JSONObject();
        mObj.put("id", mentorId);
        mObj.put("name", mentorName != null ? mentorName : "");
        mObj.put("email", mentorEmail != null ? mentorEmail : "");
        mObj.put("company", mentorCompany != null ? mentorCompany : "");
        mObj.put("designation", mentorDesignation != null ? mentorDesignation : "");
        mObj.put("department", mentorDepartment != null ? mentorDepartment : "");
        if (mentorAvatarUrl != null) mObj.put("avatarUrl", mentorAvatarUrl);
        json.put("mentor", mObj);

        json.put("studentName", studentName != null ? studentName : "");
        json.put("studentEmail", studentEmail != null ? studentEmail : "");
        json.put("studentDepartment", studentDepartment != null ? studentDepartment : "");
        if (studentGraduationYear != null) json.put("studentGraduationYear", studentGraduationYear);
        if (studentAvatarUrl != null) json.put("studentAvatar", studentAvatarUrl);

        json.put("mentorName", mentorName != null ? mentorName : "");
        json.put("mentorEmail", mentorEmail != null ? mentorEmail : "");
        json.put("mentorCompany", mentorCompany != null ? mentorCompany : "");
        json.put("mentorDesignation", mentorDesignation != null ? mentorDesignation : "");
        json.put("mentorDepartment", mentorDepartment != null ? mentorDepartment : "");
        if (mentorAvatarUrl != null) json.put("mentorAvatar", mentorAvatarUrl);

        return json;
    }
}
