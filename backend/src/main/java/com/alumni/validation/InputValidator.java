package com.alumni.validation;

import org.json.JSONObject;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.alumni.validation.ValidationConstants.*;

public class InputValidator {

    public static class PasswordStrength {
        private final int score; // 0..4
        private final String level; // "Weak", "Fair", "Good", "Strong"
        private final boolean hasMinLength;
        private final boolean hasUpperCase;
        private final boolean hasLowerCase;
        private final boolean hasDigit;
        private final boolean hasSpecialChar;
        private final List<String> suggestions;

        public PasswordStrength(int score, String level, boolean hasMinLength, boolean hasUpperCase,
                                boolean hasLowerCase, boolean hasDigit, boolean hasSpecialChar, List<String> suggestions) {
            this.score = score;
            this.level = level;
            this.hasMinLength = hasMinLength;
            this.hasUpperCase = hasUpperCase;
            this.hasLowerCase = hasLowerCase;
            this.hasDigit = hasDigit;
            this.hasSpecialChar = hasSpecialChar;
            this.suggestions = suggestions;
        }

        public int getScore() { return score; }
        public String getLevel() { return level; }
        public boolean isHasMinLength() { return hasMinLength; }
        public boolean isHasUpperCase() { return hasUpperCase; }
        public boolean isHasLowerCase() { return hasLowerCase; }
        public boolean isHasDigit() { return hasDigit; }
        public boolean isHasSpecialChar() { return hasSpecialChar; }
        public List<String> getSuggestions() { return suggestions; }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("score", score);
            obj.put("level", level);
            obj.put("hasMinLength", hasMinLength);
            obj.put("hasUpperCase", hasUpperCase);
            obj.put("hasLowerCase", hasLowerCase);
            obj.put("hasDigit", hasDigit);
            obj.put("hasSpecialChar", hasSpecialChar);
            obj.put("suggestions", suggestions);
            return obj;
        }
    }

    public static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).trim().isEmpty();
        return false;
    }

    public static String sanitizeString(Object value) {
        return (value instanceof String) ? ((String) value).trim() : "";
    }

    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) return false;
        String sanitized = email.trim();
        if (sanitized.length() > 254) return false;
        return EMAIL_PATTERN.matcher(sanitized).matches();
    }

    public static boolean isValidMobile(String mobile) {
        if (isEmpty(mobile)) return false;
        String cleaned = mobile.trim().replaceAll("[\\s()-]", "");
        return MOBILE_PATTERN.matcher(cleaned).matches();
    }

    public static PasswordStrength evaluatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            List<String> list = new ArrayList<>();
            list.add("Password cannot be empty.");
            return new PasswordStrength(0, "Very Weak", false, false, false, false, false, list);
        }

        boolean hasMinLength = password.length() >= 8;
        boolean hasUpperCase = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLowerCase = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("\\d").matcher(password).find();
        boolean hasSpecialChar = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]").matcher(password).find();

        int passedCount = 0;
        List<String> suggestions = new ArrayList<>();

        if (hasMinLength) passedCount++;
        else suggestions.add("Must be at least 8 characters.");

        if (hasUpperCase) passedCount++;
        else suggestions.add("Include at least one uppercase letter (A-Z).");

        if (hasLowerCase) passedCount++;
        else suggestions.add("Include at least one lowercase letter (a-z).");

        if (hasDigit) passedCount++;
        else suggestions.add("Include at least one number (0-9).");

        if (hasSpecialChar) passedCount++;
        else suggestions.add("Include at least one special character (!@#$%^&*).");

        int score;
        if (!hasMinLength) {
            score = 1; // Short passwords under 8 characters are always Weak
        } else if (passedCount <= 2) {
            score = 1; // Weak
        } else if (passedCount <= 4) {
            score = 2; // Fair
        } else {
            score = (password.length() >= 12) ? 4 : 3; // 4 = Strong, 3 = Good
        }

        String level;
        switch (score) {
            case 4: level = "Strong"; break;
            case 3: level = "Good"; break;
            case 2: level = "Fair"; break;
            default: level = "Weak"; break;
        }

        return new PasswordStrength(score, level, hasMinLength, hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar, suggestions);
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8 || password.length() > 128) return false;
        PasswordStrength ps = evaluatePasswordStrength(password);
        return ps.isHasMinLength() && ps.isHasUpperCase() && ps.isHasLowerCase() &&
               ps.isHasDigit() && ps.isHasSpecialChar();
    }

    public static boolean isValidFullName(String fullName) {
        if (isEmpty(fullName)) return false;
        String trimmed = fullName.trim();
        // Length requirement 2 to 50 characters
        if (trimmed.length() < 2 || trimmed.length() > 50) return false;
        // Prohibit double consecutive spaces
        if (trimmed.contains("  ")) return false;
        // Must match allowed characters: letters, spaces, hyphens, and periods
        if (!FULL_NAME_PATTERN.matcher(trimmed).matches()) return false;
        // Must contain at least two letters
        long letterCount = trimmed.chars().filter(Character::isLetter).count();
        return letterCount >= 2;
    }

    public static boolean isValidStudentId(String studentId) {
        if (isEmpty(studentId)) return false;
        return STUDENT_ID_PATTERN.matcher(studentId.trim()).matches();
    }

    public static boolean isValidStudentGraduationYear(Object yearObj) {
        if (isEmpty(yearObj)) return false;
        try {
            int year = Integer.parseInt(String.valueOf(yearObj).trim());
            int currentYear = Year.now().getValue();
            return year >= (currentYear - 2) && year <= (currentYear + 10);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidAlumniGraduationYear(Object yearObj) {
        if (isEmpty(yearObj)) return false;
        try {
            int year = Integer.parseInt(String.valueOf(yearObj).trim());
            int currentYear = Year.now().getValue();
            return year >= 1950 && year <= currentYear;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidLinkedIn(String url) {
        if (isEmpty(url)) return true; // Optional field
        return LINKEDIN_URL_PATTERN.matcher(url.trim()).matches();
    }

    public static ValidationResult validateStudentRegistration(JSONObject data) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Full Name
        String fullName = getField(data, "fullName", "name");
        if (isEmpty(fullName)) {
            errors.put("fullName", FULL_NAME_REQUIRED);
        } else if (!isValidFullName(fullName)) {
            errors.put("fullName", FULL_NAME_INVALID);
        }

        // Email
        String email = getField(data, "email");
        if (isEmpty(email)) {
            errors.put("email", EMAIL_REQUIRED);
        } else if (!isValidEmail(email)) {
            errors.put("email", EMAIL_INVALID);
        }

        // Mobile
        String mobile = getField(data, "mobileNumber", "phone");
        if (isEmpty(mobile)) {
            errors.put("mobileNumber", MOBILE_REQUIRED);
        } else if (!isValidMobile(mobile)) {
            errors.put("mobileNumber", MOBILE_INVALID);
        }

        // Password
        String password = getField(data, "password");
        if (isEmpty(password)) {
            errors.put("password", PASSWORD_REQUIRED);
        } else if (!isValidPassword(password)) {
            PasswordStrength ps = evaluatePasswordStrength(password);
            String detail = ps.getSuggestions().isEmpty() ? PASSWORD_INVALID : String.join(" ", ps.getSuggestions());
            errors.put("password", detail);
        }

        // Student ID
        String studentId = getField(data, "studentId", "rollNumber");
        if (isEmpty(studentId)) {
            errors.put("studentId", STUDENT_ID_REQUIRED);
        } else if (!isValidStudentId(studentId)) {
            errors.put("studentId", STUDENT_ID_INVALID);
        }

        // Department
        String department = getField(data, "department", "branch");
        if (isEmpty(department)) {
            errors.put("department", STUDENT_DEPT_REQUIRED);
        }

        // Graduation Year
        Object gradYear = getFieldObj(data, "graduationYear", "passoutYear");
        if (isEmpty(gradYear)) {
            errors.put("graduationYear", STUDENT_GRAD_YEAR_REQUIRED);
        } else if (!isValidStudentGraduationYear(gradYear)) {
            errors.put("graduationYear", STUDENT_GRAD_YEAR_INVALID);
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Student registration validation failed", errors);
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateAlumniRegistration(JSONObject data) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Full Name
        String fullName = getField(data, "fullName", "name");
        if (isEmpty(fullName)) {
            errors.put("fullName", FULL_NAME_REQUIRED);
        } else if (!isValidFullName(fullName)) {
            errors.put("fullName", FULL_NAME_INVALID);
        }

        // Email
        String email = getField(data, "email");
        if (isEmpty(email)) {
            errors.put("email", EMAIL_REQUIRED);
        } else if (!isValidEmail(email)) {
            errors.put("email", EMAIL_INVALID);
        }

        // Mobile
        String mobile = getField(data, "mobileNumber", "phone");
        if (isEmpty(mobile)) {
            errors.put("mobileNumber", MOBILE_REQUIRED);
        } else if (!isValidMobile(mobile)) {
            errors.put("mobileNumber", MOBILE_INVALID);
        }

        // Password
        String password = getField(data, "password");
        if (isEmpty(password)) {
            errors.put("password", PASSWORD_REQUIRED);
        } else if (!isValidPassword(password)) {
            PasswordStrength ps = evaluatePasswordStrength(password);
            String detail = ps.getSuggestions().isEmpty() ? PASSWORD_INVALID : String.join(" ", ps.getSuggestions());
            errors.put("password", detail);
        }

        // Department
        String department = getField(data, "department", "branch");
        if (isEmpty(department)) {
            errors.put("department", ALUMNI_DEPT_REQUIRED);
        }

        // Graduation Year
        Object gradYear = getFieldObj(data, "graduationYear", "passoutYear");
        if (isEmpty(gradYear)) {
            errors.put("graduationYear", ALUMNI_GRAD_YEAR_REQUIRED);
        } else if (!isValidAlumniGraduationYear(gradYear)) {
            errors.put("graduationYear", ALUMNI_GRAD_YEAR_INVALID);
        }

        // Company
        String company = getField(data, "company", "currentCompany");
        if (isEmpty(company)) {
            errors.put("company", ALUMNI_COMPANY_REQUIRED);
        }

        // Designation
        String designation = getField(data, "designation", "jobTitle");
        if (isEmpty(designation)) {
            errors.put("designation", ALUMNI_DESIGNATION_REQUIRED);
        }

        // LinkedIn Profile (Optional, but if present must be valid)
        String linkedIn = getField(data, "linkedInProfile", "linkedin");
        if (!isEmpty(linkedIn) && !isValidLinkedIn(linkedIn)) {
            errors.put("linkedInProfile", LINKEDIN_INVALID);
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Alumni registration validation failed", errors);
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateRegistration(JSONObject data) {
        String role = getField(data, "role");
        if (isEmpty(role)) {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("role", ROLE_REQUIRED);
            return ValidationResult.failure("Registration validation failed", err);
        }

        String lowerRole = role.toLowerCase();
        if ("student".equals(lowerRole)) {
            return validateStudentRegistration(data);
        } else if ("alumni".equals(lowerRole)) {
            return validateAlumniRegistration(data);
        } else {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("role", ROLE_INVALID);
            return ValidationResult.failure("Registration validation failed", err);
        }
    }

    public static String getField(JSONObject json, String... keys) {
        for (String k : keys) {
            if (json.has(k) && !json.isNull(k)) {
                return String.valueOf(json.get(k)).trim();
            }
        }
        return null;
    }

    public static Object getFieldObj(JSONObject json, String... keys) {
        for (String k : keys) {
            if (json.has(k) && !json.isNull(k)) {
                return json.get(k);
            }
        }
        return null;
    }

    public static ValidationResult validateStudentProfileUpdate(JSONObject data) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Full Name
        String fullName = getField(data, "fullName", "name");
        if (isEmpty(fullName)) {
            errors.put("fullName", FULL_NAME_REQUIRED);
        } else if (!isValidFullName(fullName)) {
            errors.put("fullName", FULL_NAME_INVALID);
        }

        // Mobile
        String mobile = getField(data, "mobileNumber", "phone");
        if (isEmpty(mobile)) {
            errors.put("mobileNumber", MOBILE_REQUIRED);
        } else if (!isValidMobile(mobile)) {
            errors.put("mobileNumber", MOBILE_INVALID);
        }

        // Department
        String department = getField(data, "department", "branch");
        if (isEmpty(department)) {
            errors.put("department", STUDENT_DEPT_REQUIRED);
        }

        // Graduation Year
        Object gradYear = getFieldObj(data, "graduationYear", "passoutYear");
        if (isEmpty(gradYear)) {
            errors.put("graduationYear", STUDENT_GRAD_YEAR_REQUIRED);
        } else if (!isValidStudentGraduationYear(gradYear)) {
            errors.put("graduationYear", STUDENT_GRAD_YEAR_INVALID);
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Profile validation failed", errors);
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateAlumniProfileUpdate(JSONObject data) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Full Name
        String fullName = getField(data, "fullName", "name");
        if (isEmpty(fullName)) {
            errors.put("fullName", FULL_NAME_REQUIRED);
        } else if (!isValidFullName(fullName)) {
            errors.put("fullName", FULL_NAME_INVALID);
        }

        // Mobile
        String mobile = getField(data, "mobileNumber", "phone");
        if (isEmpty(mobile)) {
            errors.put("mobileNumber", MOBILE_REQUIRED);
        } else if (!isValidMobile(mobile)) {
            errors.put("mobileNumber", MOBILE_INVALID);
        }

        // Department
        String department = getField(data, "department", "branch");
        if (isEmpty(department)) {
            errors.put("department", ALUMNI_DEPT_REQUIRED);
        }

        // Graduation Year
        Object gradYear = getFieldObj(data, "graduationYear", "passoutYear");
        if (isEmpty(gradYear)) {
            errors.put("graduationYear", ALUMNI_GRAD_YEAR_REQUIRED);
        } else if (!isValidAlumniGraduationYear(gradYear)) {
            errors.put("graduationYear", ALUMNI_GRAD_YEAR_INVALID);
        }

        // Company
        String company = getField(data, "company", "currentCompany");
        if (isEmpty(company)) {
            errors.put("company", ALUMNI_COMPANY_REQUIRED);
        }

        // Designation
        String designation = getField(data, "designation", "jobTitle");
        if (isEmpty(designation)) {
            errors.put("designation", ALUMNI_DESIGNATION_REQUIRED);
        }

        // LinkedIn Profile (Optional, but if present must be valid)
        String linkedIn = getField(data, "linkedInProfile", "linkedin");
        if (!isEmpty(linkedIn) && !isValidLinkedIn(linkedIn)) {
            errors.put("linkedInProfile", LINKEDIN_INVALID);
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Profile validation failed", errors);
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateMentorshipRequest(JSONObject data) {
        if (data == null) {
            return ValidationResult.failure("Payload cannot be null", Map.of("payload", "Request payload is required"));
        }

        Map<String, String> errors = new LinkedHashMap<>();

        // Student ID
        Object studentIdObj = getFieldObj(data, "studentId", "student_id");
        if (isEmpty(studentIdObj)) {
            errors.put("studentId", "Student ID is required.");
        } else {
            try {
                long sid = Long.parseLong(studentIdObj.toString().trim());
                if (sid <= 0) errors.put("studentId", "Student ID must be positive.");
            } catch (NumberFormatException e) {
                errors.put("studentId", "Invalid student ID format.");
            }
        }

        // Mentor ID
        Object mentorIdObj = getFieldObj(data, "mentorId", "mentor_id");
        if (isEmpty(mentorIdObj)) {
            errors.put("mentorId", "Mentor ID is required.");
        } else {
            try {
                long mid = Long.parseLong(mentorIdObj.toString().trim());
                if (mid <= 0) errors.put("mentorId", "Mentor ID must be positive.");
            } catch (NumberFormatException e) {
                errors.put("mentorId", "Invalid mentor ID format.");
            }
        }

        // Session Goal
        String sessionGoal = getField(data, "sessionGoal", "session_goal", "goal");
        if (isEmpty(sessionGoal)) {
            errors.put("sessionGoal", "Session goal is required.");
        } else if (sessionGoal.length() > 100) {
            errors.put("sessionGoal", "Session goal cannot exceed 100 characters.");
        }

        // Message
        String message = getField(data, "message", "note", "introductoryNote");
        if (isEmpty(message)) {
            errors.put("message", "Introductory note/message is required.");
        } else {
            String trimmed = message.trim();
            if (trimmed.length() < 10) {
                errors.put("message", "Message must be at least 10 characters.");
            } else if (trimmed.length() > 1000) {
                errors.put("message", "Message cannot exceed 1000 characters.");
            }
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Mentorship request validation failed", errors);
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateRequestStatusUpdate(JSONObject data) {
        if (data == null) {
            return ValidationResult.failure("Payload cannot be null", Map.of("payload", "Request payload is required"));
        }

        Map<String, String> errors = new LinkedHashMap<>();
        String status = getField(data, "status");
        if (isEmpty(status)) {
            errors.put("status", "Status is required.");
        } else {
            String upper = status.trim().toUpperCase();
            if (!"ACCEPTED".equals(upper) && !"REJECTED".equals(upper) && !"CANCELLED".equals(upper)) {
                errors.put("status", "Status must be ACCEPTED, REJECTED, or CANCELLED.");
            }
        }

        String mentorResponse = getField(data, "mentorResponse", "response", "note");
        if (mentorResponse != null && mentorResponse.length() > 1000) {
            errors.put("mentorResponse", "Mentor response note cannot exceed 1000 characters.");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.failure("Status update validation failed", errors);
        }
        return ValidationResult.success();
    }
}

