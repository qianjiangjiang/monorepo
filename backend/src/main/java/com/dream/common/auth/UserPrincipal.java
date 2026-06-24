package com.dream.common.auth;

import java.util.Locale;

public record UserPrincipal(Long userId, String openid, String role) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    public UserPrincipal {
        role = normalizeRole(role);
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
