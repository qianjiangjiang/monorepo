package com.dream.common.auth;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;

public final class CurrentUserContext {

    private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UserPrincipal principal) {
        CURRENT.set(principal);
    }

    public static UserPrincipal require() {
        UserPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }

    public static UserPrincipal get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
