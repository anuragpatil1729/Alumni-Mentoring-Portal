package com.alumni.validation;

import java.util.regex.Pattern;

public class ValidationConstants {

    public static final String FULL_NAME_REQUIRED = "Full name is required.";
    public static final String FULL_NAME_INVALID = "Full name must contain only letters and spaces, and be between 2 and 50 characters.";
    public static final String EMAIL_REQUIRED = "Email address is required.";
    public static final String EMAIL_INVALID = "Please provide a valid email address.";
    public static final String MOBILE_REQUIRED = "Mobile number is required.";
    public static final String MOBILE_INVALID = "Mobile number must be a valid 10-digit number (e.g. 9876543210 or +919876543210).";
    public static final String PASSWORD_REQUIRED = "Password is required.";
    public static final String PASSWORD_INVALID = "Password must be 8-128 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character (!@#$%^&*()_+-=[]{};:\'\",.<>?/).";
    public static final String ROLE_REQUIRED = "Role is required.";
    public static final String ROLE_INVALID = "Role must be either \"student\" or \"alumni\".";

    // Student Specific
    public static final String STUDENT_ID_REQUIRED = "Student ID / Roll Number is required.";
    public static final String STUDENT_ID_INVALID = "Student ID must be between 3 and 20 alphanumeric characters.";
    public static final String STUDENT_DEPT_REQUIRED = "Department is required for student registration.";
    public static final String STUDENT_GRAD_YEAR_REQUIRED = "Graduation year is required for student registration.";
    public static final String STUDENT_GRAD_YEAR_INVALID = "Graduation year must be a 4-digit year (current or future year, e.g. 2024-2035).";

    // Alumni Specific
    public static final String ALUMNI_GRAD_YEAR_REQUIRED = "Graduation year is required for alumni registration.";
    public static final String ALUMNI_GRAD_YEAR_INVALID = "Graduation year must be a valid past or current 4-digit year (e.g. 1950-2026).";
    public static final String ALUMNI_DEPT_REQUIRED = "Department is required for alumni registration.";
    public static final String ALUMNI_COMPANY_REQUIRED = "Current company name is required for alumni registration.";
    public static final String ALUMNI_DESIGNATION_REQUIRED = "Designation / job title is required for alumni registration.";
    public static final String LINKEDIN_INVALID = "LinkedIn profile must be a valid URL (e.g. https://www.linkedin.com/in/username).";

    // Regex Patterns
    public static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s.-]{2,50}$");
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    public static final Pattern MOBILE_PATTERN = Pattern.compile("^(?:\\+?\\d{1,3}[- ]?)?[6-9]\\d{9}$");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,128}$");
    public static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9/-]{3,20}$");
    public static final Pattern LINKEDIN_URL_PATTERN = Pattern.compile("^https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9_-]+/?$");
}
