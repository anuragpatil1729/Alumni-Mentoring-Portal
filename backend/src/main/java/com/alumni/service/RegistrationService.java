package com.alumni.service;

import com.alumni.dao.RegistrationDAO;
import com.alumni.model.Alumni;
import com.alumni.model.Student;
import com.alumni.model.User;
import com.alumni.validation.InputValidator;
import com.alumni.validation.ValidationResult;
import org.json.JSONObject;

import java.sql.SQLException;

public class RegistrationService {

    private final RegistrationDAO registrationDAO;

    public RegistrationService() {
        this.registrationDAO = new RegistrationDAO();
    }

    public RegistrationService(RegistrationDAO registrationDAO) {
        this.registrationDAO = registrationDAO;
    }

    public static class ProcessResult {
        private final int status;
        private final JSONObject body;

        public ProcessResult(int status, JSONObject body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() { return status; }
        public JSONObject getBody() { return body; }
    }

    public ProcessResult processStudentRegistration(JSONObject data) {
        ValidationResult val = InputValidator.validateStudentRegistration(data);
        if (!val.isValid()) {
            return new ProcessResult(400, val.toJsonObject());
        }

        String email = InputValidator.getField(data, "email");
        try {
            if (registrationDAO.existsByEmail(email)) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "An account with this email address already exists.");
                return new ProcessResult(409, err);
            }

            User user = new User();
            user.setFullName(InputValidator.getField(data, "fullName", "name"));
            user.setEmail(email.toLowerCase());
            user.setMobileNumber(InputValidator.getField(data, "mobileNumber", "phone"));
            user.setPasswordHash(RegistrationDAO.hashPassword(InputValidator.getField(data, "password")));
            user.setRole("student");

            Student student = new Student();
            student.setStudentId(InputValidator.getField(data, "studentId", "rollNumber"));
            student.setDepartment(InputValidator.getField(data, "department", "branch"));
            student.setGraduationYear(Integer.parseInt(String.valueOf(InputValidator.getFieldObj(data, "graduationYear", "passoutYear"))));

            long id = registrationDAO.registerStudent(user, student);

            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("message", "Student account created successfully!");
            resp.put("id", id);
            resp.put("email", email);
            resp.put("role", "student");

            return new ProcessResult(201, resp);

        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult processAlumniRegistration(JSONObject data) {
        ValidationResult val = InputValidator.validateAlumniRegistration(data);
        if (!val.isValid()) {
            return new ProcessResult(400, val.toJsonObject());
        }

        String email = InputValidator.getField(data, "email");
        try {
            if (registrationDAO.existsByEmail(email)) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "An account with this email address already exists.");
                return new ProcessResult(409, err);
            }

            User user = new User();
            user.setFullName(InputValidator.getField(data, "fullName", "name"));
            user.setEmail(email.toLowerCase());
            user.setMobileNumber(InputValidator.getField(data, "mobileNumber", "phone"));
            user.setPasswordHash(RegistrationDAO.hashPassword(InputValidator.getField(data, "password")));
            user.setRole("alumni");

            Alumni alumni = new Alumni();
            alumni.setDepartment(InputValidator.getField(data, "department", "branch"));
            alumni.setGraduationYear(Integer.parseInt(String.valueOf(InputValidator.getFieldObj(data, "graduationYear", "passoutYear"))));
            alumni.setCompany(InputValidator.getField(data, "company", "currentCompany"));
            alumni.setDesignation(InputValidator.getField(data, "designation", "jobTitle"));
            alumni.setLinkedInProfile(InputValidator.getField(data, "linkedInProfile", "linkedin"));

            Object expObj = InputValidator.getFieldObj(data, "experienceYears", "experience");
            if (expObj != null && !expObj.toString().trim().isEmpty()) {
                try {
                    alumni.setExperienceYears(Integer.parseInt(expObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }

            alumni.setIndustry(InputValidator.getField(data, "industry"));
            alumni.setSkills(InputValidator.getField(data, "skills"));
            alumni.setBio(InputValidator.getField(data, "bio"));

            Object maxMenteesObj = InputValidator.getFieldObj(data, "maxMentees", "mentees");
            if (maxMenteesObj != null && !maxMenteesObj.toString().trim().isEmpty()) {
                try {
                    alumni.setMaxMentees(Integer.parseInt(maxMenteesObj.toString().trim()));
                } catch (NumberFormatException ignored) {}
            }

            long id = registrationDAO.registerAlumni(user, alumni);

            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("message", "Alumni account created successfully!");
            resp.put("id", id);
            resp.put("email", email);
            resp.put("role", "alumni");

            return new ProcessResult(201, resp);

        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult processUnifiedRegistration(JSONObject data) {
        String role = InputValidator.getField(data, "role");
        if (InputValidator.isEmpty(role)) {
            ValidationResult r = InputValidator.validateRegistration(data);
            return new ProcessResult(400, r.toJsonObject());
        }

        if ("student".equalsIgnoreCase(role)) {
            return processStudentRegistration(data);
        } else if ("alumni".equalsIgnoreCase(role)) {
            return processAlumniRegistration(data);
        } else {
            ValidationResult r = InputValidator.validateRegistration(data);
            return new ProcessResult(400, r.toJsonObject());
        }
    }
}
