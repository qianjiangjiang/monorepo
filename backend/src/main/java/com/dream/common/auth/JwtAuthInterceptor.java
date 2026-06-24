package com.dream.common.auth;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final JwtService jwtService;

    public JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        UserPrincipal principal = jwtService.parseToken(token);
        if (isAdminPath(request) && !principal.isAdmin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        CurrentUserContext.set(principal);
        return true;
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String path = contextPath == null || contextPath.isEmpty()
                ? requestUri
                : requestUri.substring(contextPath.length());
        return path.startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
