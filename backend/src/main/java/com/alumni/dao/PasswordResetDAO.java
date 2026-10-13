package com.alumni.dao;

import com.alumni.config.DBConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HexFormat;

/** Secure, one-use password reset token storage. Raw tokens are never persisted. */
public final class PasswordResetDAO {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String createResetToken(String email) throws Exception {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        String tokenHash = sha256(rawToken);
        String findUser = "SELECT id FROM users WHERE LOWER(email)=LOWER(?) AND is_active=TRUE";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement p = c.prepareStatement(findUser)) {
                    p.setString(1, email.trim());
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) { c.commit(); return null; }
                        userId = rs.getLong(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE password_resets SET used_at=NOW() WHERE user_id=? AND used_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO password_resets(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(NOW(), INTERVAL 30 MINUTE))")) {
                    p.setLong(1, userId); p.setString(2, tokenHash); p.executeUpdate();
                }
                c.commit();
                return rawToken;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public boolean consumeTokenAndChangePassword(String rawToken, String newPasswordHash) throws Exception {
        String tokenHash = sha256(rawToken);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT user_id FROM password_resets WHERE token_hash=? AND used_at IS NULL AND expires_at>NOW() FOR UPDATE")) {
                    p.setString(1, tokenHash);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) { c.rollback(); return false; }
                        userId = rs.getLong(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=? AND is_active=TRUE")) {
                    p.setString(1, newPasswordHash); p.setLong(2, userId);
                    if (p.executeUpdate() != 1) { c.rollback(); return false; }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE password_resets SET used_at=NOW() WHERE token_hash=? AND used_at IS NULL")) {
                    p.setString(1, tokenHash);
                    if (p.executeUpdate() != 1) { c.rollback(); return false; }
                }
                // Password change invalidates every active API token for this user.
                try (PreparedStatement p = c.prepareStatement("UPDATE auth_tokens SET revoked_at=NOW() WHERE user_id=? AND revoked_at IS NULL")) {
                    p.setLong(1, userId); p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
