package com.alumni.validation;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationResult {
    private final boolean valid;
    private final String message;
    private final Map<String, String> errors;

    public ValidationResult(boolean valid, String message, Map<String, String> errors) {
        this.valid = valid;
        this.message = message;
        this.errors = errors != null ? errors : new LinkedHashMap<>();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult failure(String message, Map<String, String> errors) {
        return new ValidationResult(false, message, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("success", valid);
        if (message != null) {
            json.put("message", message);
        }
        if (!errors.isEmpty()) {
            json.put("errors", new JSONObject(errors));
        }
        return json;
    }
}
