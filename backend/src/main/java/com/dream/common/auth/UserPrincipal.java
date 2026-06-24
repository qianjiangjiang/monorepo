package com.dream.common.auth;

public record UserPrincipal(Long userId, String openid, String role) {

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
