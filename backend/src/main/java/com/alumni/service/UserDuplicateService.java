package com.alumni.service;

import com.alumni.config.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * UserDuplicateService
 *
 * Implements duplicate checking algorithms for users:
 * 1. Normalized mobile number collisions.
 * 2. Duplicate student IDs in the students table.
 * 3. Near-duplicate full names using Levenshtein distance & token normalization.
 * 4. Email prefix / alias collisions across domains.
 *
 * Supports both batch system-wide duplicate audits and manual single-user verification.
 */
public class UserDuplicateService {

    public static class DuplicateGroup {
        private final String duplicateType;
        private final String confidence;
        private final String reason;
        private final List<JSONObject> users = new ArrayList<>();

        public DuplicateGroup(String duplicateType, String confidence, String reason) {
            this.duplicateType = duplicateType;
            this.confidence = confidence;
            this.reason = reason;
        }

        public void addUser(JSONObject user) {
            this.users.add(user);
        }

        public List<JSONObject> getUsers() {
            return users;
        }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("duplicateType", duplicateType);
            obj.put("confidence", confidence);
            obj.put("reason", reason);
            JSONArray arr = new JSONArray();
            for (JSONObject u : users) arr.put(u);
            obj.put("users", arr);
            return obj;
        }
    }

    /**
     * Normalizes a mobile phone number by stripping all non-digit characters
     * and trimming country codes like '91' if 12 digits.
     */
    public static String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.replaceAll("\\D+", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    /**
     * Computes the Levenshtein distance between two strings.
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        String a = s1.trim().toLowerCase(Locale.ROOT);
        String b = s2.trim().toLowerCase(Locale.ROOT);

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * Performs a comprehensive batch scan of all registered users to identify
     * duplicate candidates.
     */
    public JSONArray scanAllDuplicates() throws SQLException {
        String sql = """
            SELECT u.id, u.full_name, u.email, u.mobile_number, u.role, u.is_active,
                   COALESCE(u.approval_status, 'APPROVED') AS approval_status,
                   u.created_at, s.student_id
            FROM users u
            LEFT JOIN students s ON s.user_id = u.id
            ORDER BY u.id ASC
        """;

        List<JSONObject> allUsers = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JSONObject u = new JSONObject();
                u.put("id", rs.getLong("id"));
                u.put("fullName", rs.getString("full_name"));
                u.put("email", rs.getString("email"));
                u.put("mobileNumber", rs.getString("mobile_number"));
                u.put("role", rs.getString("role"));
                u.put("active", rs.getBoolean("is_active"));
                u.put("approvalStatus", rs.getString("approval_status"));
                u.put("createdAt", rs.getTimestamp("created_at").toString());
                String sid = rs.getString("student_id");
                if (sid != null) u.put("studentId", sid);
                allUsers.add(u);
            }
        }

        List<DuplicateGroup> groups = new ArrayList<>();

        // 1. Check duplicate normalized mobile numbers
        Map<String, List<JSONObject>> mobileMap = new HashMap<>();
        for (JSONObject u : allUsers) {
            String normMobile = normalizeMobile(u.optString("mobileNumber"));
            if (normMobile.length() >= 7) {
                mobileMap.computeIfAbsent(normMobile, k -> new ArrayList<>()).add(u);
            }
        }
        for (Map.Entry<String, List<JSONObject>> entry : mobileMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                DuplicateGroup g = new DuplicateGroup(
                    "DUPLICATE_MOBILE",
                    "HIGH",
                    "Multiple accounts share identical normalized mobile number: " + entry.getKey()
                );
                for (JSONObject u : entry.getValue()) g.addUser(u);
                groups.add(g);
            }
        }

        // 2. Check duplicate Student IDs
        Map<String, List<JSONObject>> studentIdMap = new HashMap<>();
        for (JSONObject u : allUsers) {
            String sid = u.optString("studentId", "").trim().toUpperCase(Locale.ROOT);
            if (!sid.isEmpty()) {
                studentIdMap.computeIfAbsent(sid, k -> new ArrayList<>()).add(u);
            }
        }
        for (Map.Entry<String, List<JSONObject>> entry : studentIdMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                DuplicateGroup g = new DuplicateGroup(
                    "DUPLICATE_STUDENT_ID",
                    "HIGH",
                    "Multiple accounts share identical Student ID: " + entry.getKey()
                );
                for (JSONObject u : entry.getValue()) g.addUser(u);
                groups.add(g);
            }
        }

        // 3. Check near-duplicate names (Levenshtein distance <= 1 or same token set)
        Set<String> pairedNames = new HashSet<>();
        for (int i = 0; i < allUsers.size(); i++) {
            JSONObject u1 = allUsers.get(i);
            String name1 = u1.optString("fullName", "").trim();
            if (name1.length() < 3) continue;

            for (int j = i + 1; j < allUsers.size(); j++) {
                JSONObject u2 = allUsers.get(j);
                String name2 = u2.optString("fullName", "").trim();
                if (name2.length() < 3) continue;

                // Don't duplicate if already grouped by mobile
                if (normalizeMobile(u1.optString("mobileNumber")).equals(normalizeMobile(u2.optString("mobileNumber")))) {
                    continue;
                }

                String pairKey = u1.getLong("id") + "-" + u2.getLong("id");
                if (pairedNames.contains(pairKey)) continue;

                int dist = levenshteinDistance(name1, name2);
                boolean exactMatch = name1.equalsIgnoreCase(name2);

                if (exactMatch || (dist <= 1 && name1.length() > 6)) {
                    pairedNames.add(pairKey);
                    DuplicateGroup g = new DuplicateGroup(
                        exactMatch ? "EXACT_NAME_COLLISION" : "SIMILAR_NAME",
                        exactMatch ? "HIGH" : "MEDIUM",
                        "Identical or near-identical full names: '" + name1 + "' vs '" + name2 + "'"
                    );
                    g.addUser(u1);
                    g.addUser(u2);
                    groups.add(g);
                }
            }
        }

        // 4. Check email prefix match (e.g. anurag.patil@ vs anurag.patil@gmail.com)
        Map<String, List<JSONObject>> emailPrefixMap = new HashMap<>();
        for (JSONObject u : allUsers) {
            String email = u.optString("email", "").toLowerCase(Locale.ROOT);
            int atIdx = email.indexOf('@');
            if (atIdx > 0) {
                String prefix = email.substring(0, atIdx).replace(".", "").replace("-", "").replace("_", "");
                if (prefix.length() >= 4) {
                    emailPrefixMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(u);
                }
            }
        }
        for (Map.Entry<String, List<JSONObject>> entry : emailPrefixMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Ensure they aren't all already part of a mobile group
                boolean allSameMobile = true;
                String firstMobile = normalizeMobile(entry.getValue().get(0).optString("mobileNumber"));
                for (JSONObject u : entry.getValue()) {
                    if (!normalizeMobile(u.optString("mobileNumber")).equals(firstMobile)) {
                        allSameMobile = false;
                        break;
                    }
                }
                if (!allSameMobile) {
                    DuplicateGroup g = new DuplicateGroup(
                        "EMAIL_HANDLE_SIMILARITY",
                        "MEDIUM",
                        "Users have matching email handles: '" + entry.getKey() + "'"
                    );
                    for (JSONObject u : entry.getValue()) g.addUser(u);
                    groups.add(g);
                }
            }
        }

        JSONArray result = new JSONArray();
        for (DuplicateGroup g : groups) {
            result.put(g.toJsonObject());
        }
        return result;
    }

    /**
     * Performs manual duplicate check for a candidate input (e.g., during registration review).
     */
    public JSONObject checkManualDuplicate(String name, String email, String mobile, String studentId)
            throws SQLException {

        JSONObject result = new JSONObject();
        JSONArray matches = new JSONArray();

        String normMobile = normalizeMobile(mobile);
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        String cleanName = name != null ? name.trim() : "";
        String cleanStudentId = studentId != null ? studentId.trim().toUpperCase(Locale.ROOT) : "";

        String sql = """
            SELECT u.id, u.full_name, u.email, u.mobile_number, u.role, u.is_active,
                   COALESCE(u.approval_status, 'APPROVED') AS approval_status,
                   u.created_at, s.student_id
            FROM users u
            LEFT JOIN students s ON s.user_id = u.id
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long uid = rs.getLong("id");
                String uName = rs.getString("full_name");
                String uEmail = rs.getString("email");
                String uMobile = rs.getString("mobile_number");
                String uStudentId = rs.getString("student_id");

                List<String> matchReasons = new ArrayList<>();
                String confidence = "LOW";

                if (!cleanEmail.isEmpty() && uEmail != null && cleanEmail.equalsIgnoreCase(uEmail.trim())) {
                    matchReasons.add("Exact email match: " + uEmail);
                    confidence = "HIGH";
                }

                if (!normMobile.isEmpty() && normMobile.equals(normalizeMobile(uMobile))) {
                    matchReasons.add("Exact mobile match: " + uMobile);
                    confidence = "HIGH";
                }

                if (!cleanStudentId.isEmpty() && uStudentId != null && cleanStudentId.equalsIgnoreCase(uStudentId.trim())) {
                    matchReasons.add("Exact student ID match: " + uStudentId);
                    confidence = "HIGH";
                }

                if (!cleanName.isEmpty() && uName != null) {
                    if (cleanName.equalsIgnoreCase(uName.trim())) {
                        matchReasons.add("Exact full name match: " + uName);
                        if (!"HIGH".equals(confidence)) confidence = "MEDIUM";
                    } else if (levenshteinDistance(cleanName, uName) <= 1 && cleanName.length() > 5) {
                        matchReasons.add("Similar full name: " + uName);
                        if (!"HIGH".equals(confidence)) confidence = "MEDIUM";
                    }
                }

                if (!matchReasons.isEmpty()) {
                    JSONObject match = new JSONObject();
                    match.put("id", uid);
                    match.put("fullName", uName);
                    match.put("email", uEmail);
                    match.put("mobileNumber", uMobile);
                    match.put("role", rs.getString("role"));
                    match.put("active", rs.getBoolean("is_active"));
                    match.put("approvalStatus", rs.getString("approval_status"));
                    match.put("studentId", uStudentId != null ? uStudentId : JSONObject.NULL);
                    match.put("confidence", confidence);
                    match.put("matchReasons", new JSONArray(matchReasons));
                    matches.put(match);
                }
            }
        }

        result.put("query", new JSONObject()
            .put("name", cleanName)
            .put("email", cleanEmail)
            .put("mobile", normMobile)
            .put("studentId", cleanStudentId)
        );
        result.put("duplicateCount", matches.length());
        result.put("hasDuplicates", matches.length() > 0);
        result.put("matches", matches);

        return result;
    }
}
