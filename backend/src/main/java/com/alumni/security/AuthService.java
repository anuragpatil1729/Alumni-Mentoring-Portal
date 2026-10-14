
package com.alumni.security;

import com.alumni.config.DBConnection;
import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_LIFETIME_HOURS = 12;

    /**
     * Generate a random token for an already authenticated user.
     * Store only the token hash in the database.
     * Return the raw token once to the caller.
     */
    public String createToken(long userId) throws Exception {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = sha256(rawToken);

        String sql = """
            INSERT INTO auth_tokens
                (user_id, token_hash, expires_at)
            SELECT id, ?, DATE_ADD(NOW(), INTERVAL ? HOUR)
            FROM users
            WHERE id = ? AND is_active = TRUE
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);
            statement.setInt(2, TOKEN_LIFETIME_HOURS);
            statement.setLong(3, userId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                    "Cannot issue token: user does not exist or is inactive."
                );
            }
        }

        return rawToken;
    }

    /**
     * Validate the Authorization: Bearer header.
     * The role is always read from the database.
     */
    public AuthenticatedUser authenticate(HttpExchange exchange)
            throws AuthException {

        String header = exchange.getRequestHeaders()
                                .getFirst("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthException(401, "Authentication required.");
        }

        String rawToken = header.substring(7).trim();

        // Our tokens are exactly 64 hexadecimal characters.
        if (!rawToken.matches("^[a-fA-F0-9]{64}$")) {
            throw new AuthException(401, "Invalid or expired token.");
        }

        final String tokenHash;

        try {
            tokenHash = sha256(rawToken);
        } catch (Exception e) {
            throw new AuthException(500, "Authentication could not be completed.");
        }

        String sql = """
            SELECT u.id, u.role
            FROM auth_tokens t
            JOIN users u ON u.id = t.user_id
            WHERE t.token_hash = ?
              AND t.revoked_at IS NULL
              AND t.expires_at > NOW()
              AND u.is_active = TRUE
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException(401, "Invalid or expired token.");
                }

                return new AuthenticatedUser(
                    rs.getLong("id"),
                    rs.getString("role")
                );
            }

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(500, "Authentication service unavailable.");
        }
    }

    public AuthenticatedUser requireRole(
            HttpExchange exchange, String requiredRole)
            throws AuthException {

        AuthenticatedUser user = authenticate(exchange);

        if (!user.hasRole(requiredRole)) {
            throw new AuthException(403, "You do not have permission to perform this action.");
        }

        return user;
    }

    public AuthenticatedUser requireAnyRole(
            HttpExchange exchange, String... allowedRoles)
            throws AuthException {

        AuthenticatedUser user = authenticate(exchange);

        for (String role : allowedRoles) {
            if (user.hasRole(role)) {
                return user;
            }
        }

        throw new AuthException(403, "You do not have permission to perform this action.");
    }

    /**
     * Revoke a token when logging out.
     */
    public void revokeToken(String rawToken) throws Exception {
        if (rawToken == null ||
                !rawToken.matches("^[a-fA-F0-9]{64}$")) {
            return;
        }

        String sql = """
            UPDATE auth_tokens
            SET revoked_at = NOW()
            WHERE token_hash = ? AND revoked_at IS NULL
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, sha256(rawToken));
            statement.executeUpdate();
        }
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
