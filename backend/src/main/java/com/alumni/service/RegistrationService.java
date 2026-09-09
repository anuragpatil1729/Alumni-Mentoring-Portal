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
        String mobile = InputValidator.getField(data, "mobileNumber", "phone");
        try {
            boolean emailExists = registrationDAO.existsByEmail(email);
            boolean mobileExists = registrationDAO.existsByMobileNumber(mobile);

            if (emailExists || mobileExists) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                JSONObject fieldErrors = new JSONObject();
                if (emailExists) {
                    fieldErrors.put("email", "This email address is already in use.");
                }
                if (mobileExists) {
                    fieldErrors.put("mobileNumber", "This mobile number is already in use.");
                }
                err.put("errors", fieldErrors);
                if (emailExists && mobileExists) {
                    err.put("message", "Email and mobile number are already registered.");
                } else if (emailExists) {
                    err.put("message", "This email address is already in use.");
                } else {
                    err.put("message", "This mobile number is already in use.");
                }
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

            saveOptionalAvatar(id, data, resp);

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
        String mobile = InputValidator.getField(data, "mobileNumber", "phone");
        try {
            boolean emailExists = registrationDAO.existsByEmail(email);
            boolean mobileExists = registrationDAO.existsByMobileNumber(mobile);

            if (emailExists || mobileExists) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                JSONObject fieldErrors = new JSONObject();
                if (emailExists) {
                    fieldErrors.put("email", "This email address is already in use.");
                }
                if (mobileExists) {
                    fieldErrors.put("mobileNumber", "This mobile number is already in use.");
                }
                err.put("errors", fieldErrors);
                if (emailExists && mobileExists) {
                    err.put("message", "Email and mobile number are already registered.");
                } else if (emailExists) {
                    err.put("message", "This email address is already in use.");
                } else {
                    err.put("message", "This mobile number is already in use.");
                }
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

            saveOptionalAvatar(id, data, resp);

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

    public ProcessResult getProfile(long userId) {
        try {
            JSONObject profile = registrationDAO.getUserProfile(userId);
            if (profile != null) {
                JSONObject resp = new JSONObject();
                resp.put("success", true);
                resp.put("profile", profile);
                return new ProcessResult(200, resp);
            } else {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "User profile not found.");
                return new ProcessResult(404, err);
            }
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    public ProcessResult updateProfile(long userId, JSONObject data) {
        try {
            JSONObject current = registrationDAO.getUserProfile(userId);
            if (current == null) {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "User profile not found.");
                return new ProcessResult(404, err);
            }

            String role = current.getString("role");
            String newMobile = InputValidator.getField(data, "mobileNumber", "phone");

            if ("student".equalsIgnoreCase(role)) {
                ValidationResult val = InputValidator.validateStudentProfileUpdate(data);
                if (!val.isValid()) {
                    return new ProcessResult(400, val.toJsonObject());
                }

                if (newMobile != null && registrationDAO.existsByMobileNumberExcludingUser(newMobile, userId)) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    JSONObject fieldErrors = new JSONObject();
                    fieldErrors.put("mobileNumber", "This mobile number is already in use.");
                    err.put("errors", fieldErrors);
                    err.put("message", "This mobile number is already in use.");
                    return new ProcessResult(409, err);
                }

                String fullName = InputValidator.getField(data, "fullName", "name");
                String department = InputValidator.getField(data, "department", "branch");
                int graduationYear = Integer.parseInt(String.valueOf(InputValidator.getFieldObj(data, "graduationYear", "passoutYear")));

                boolean ok = registrationDAO.updateStudentProfile(userId, fullName, newMobile, department, graduationYear);
                if (ok) {
                    JSONObject updated = registrationDAO.getUserProfile(userId);
                    JSONObject resp = new JSONObject();
                    resp.put("success", true);
                    resp.put("message", "Profile updated successfully!");
                    resp.put("profile", updated);
                    return new ProcessResult(200, resp);
                } else {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Profile update failed.");
                    return new ProcessResult(500, err);
                }

            } else if ("alumni".equalsIgnoreCase(role)) {
                ValidationResult val = InputValidator.validateAlumniProfileUpdate(data);
                if (!val.isValid()) {
                    return new ProcessResult(400, val.toJsonObject());
                }

                if (newMobile != null && registrationDAO.existsByMobileNumberExcludingUser(newMobile, userId)) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    JSONObject fieldErrors = new JSONObject();
                    fieldErrors.put("mobileNumber", "This mobile number is already in use.");
                    err.put("errors", fieldErrors);
                    err.put("message", "This mobile number is already in use.");
                    return new ProcessResult(409, err);
                }

                String fullName = InputValidator.getField(data, "fullName", "name");
                String department = InputValidator.getField(data, "department", "branch");
                int graduationYear = Integer.parseInt(String.valueOf(InputValidator.getFieldObj(data, "graduationYear", "passoutYear")));
                String company = InputValidator.getField(data, "company", "currentCompany");
                String designation = InputValidator.getField(data, "designation", "jobTitle");
                String linkedIn = InputValidator.getField(data, "linkedInProfile", "linkedin");

                Integer expYears = null;
                Object expObj = InputValidator.getFieldObj(data, "experienceYears", "experience");
                if (expObj != null && !expObj.toString().trim().isEmpty()) {
                    try {
                        expYears = Integer.parseInt(expObj.toString().trim());
                    } catch (NumberFormatException ignored) {}
                }

                String industry = InputValidator.getField(data, "industry");
                String skills = InputValidator.getField(data, "skills");
                String bio = InputValidator.getField(data, "bio");

                Integer maxMentees = null;
                Object maxObj = InputValidator.getFieldObj(data, "maxMentees", "mentees");
                if (maxObj != null && !maxObj.toString().trim().isEmpty()) {
                    try {
                        maxMentees = Integer.parseInt(maxObj.toString().trim());
                    } catch (NumberFormatException ignored) {}
                }

                boolean ok = registrationDAO.updateAlumniProfile(userId, fullName, newMobile, department, graduationYear,
                        company, designation, linkedIn, expYears, industry, skills, bio, maxMentees);
                if (ok) {
                    JSONObject updated = registrationDAO.getUserProfile(userId);
                    JSONObject resp = new JSONObject();
                    resp.put("success", true);
                    resp.put("message", "Profile updated successfully!");
                    resp.put("profile", updated);
                    return new ProcessResult(200, resp);
                } else {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Profile update failed.");
                    return new ProcessResult(500, err);
                }
            } else {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("message", "Unsupported user role: " + role);
                return new ProcessResult(400, err);
            }

        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("message", "Database error: " + e.getMessage());
            return new ProcessResult(500, err);
        }
    }

    private void saveOptionalAvatar(long userId, JSONObject data, JSONObject resp) {
        String rawAvatar = InputValidator.getField(data, "avatarData", "imageData", "avatarImage", "avatar");
        if (rawAvatar != null && !rawAvatar.trim().isEmpty()) {
            try {
                String mimeType = "image/jpeg";
                if (rawAvatar.contains(",")) {
                    if (rawAvatar.startsWith("data:")) {
                        int semi = rawAvatar.indexOf(';');
                        if (semi > 5) mimeType = rawAvatar.substring(5, semi);
                    }
                    rawAvatar = rawAvatar.substring(rawAvatar.indexOf(",") + 1);
                }
                if (data.has("mimeType") && !data.getString("mimeType").trim().isEmpty()) {
                    mimeType = data.getString("mimeType").trim();
                }
                byte[] bytes = java.util.Base64.getDecoder().decode(rawAvatar.trim());
                if (bytes != null && bytes.length > 0 && bytes.length <= 5 * 1024 * 1024) {
                    registrationDAO.updateUserAvatar(userId, bytes, mimeType);
                    resp.put("avatarUrl", "/api/users/" + userId + "/avatar");
                }
            } catch (Exception ex) {
                System.err.println("Could not save initial avatar during registration: " + ex.getMessage());
            }
        }
    }
}


