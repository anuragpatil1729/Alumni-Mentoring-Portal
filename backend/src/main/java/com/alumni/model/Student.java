package com.alumni.model;

public class Student {
    private Long userId;
    private String studentId;
    private String department;
    private Integer graduationYear;

    public Student() {}

    public Student(Long userId, String studentId, String department, Integer graduationYear) {
        this.userId = userId;
        this.studentId = studentId;
        this.department = department;
        this.graduationYear = graduationYear;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
}
