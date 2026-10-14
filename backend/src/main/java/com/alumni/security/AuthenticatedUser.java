
package com.alumni.security;

public class AuthenticatedUser {
    private final long userId;
    private final String role;

    public AuthenticatedUser(long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String expectedRole) {
        return role != null && role.equalsIgnoreCase(expectedRole);
    }
}
