package com.dream.common.ratelimit;

import com.dream.common.auth.CurrentUserContext;
import com.dream.common.auth.UserPrincipal;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter rateLimiter;

    public RateLimitInterceptor(RedisRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimited rateLimited = findRateLimited(handlerMethod);
        if (rateLimited == null) {
            return true;
        }

        String key = buildKey(rateLimited, request);
        boolean allowed = rateLimiter.tryAcquire(key, rateLimited.limit(), Duration.ofSeconds(rateLimited.windowSeconds()));
        if (!allowed) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        return true;
    }

    private RateLimited findRateLimited(HandlerMethod handlerMethod) {
        RateLimited methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RateLimited.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimited.class);
    }

    private String buildKey(RateLimited rateLimited, HttpServletRequest request) {
        if (rateLimited.byUser()) {
            UserPrincipal principal = CurrentUserContext.get();
            if (principal != null && principal.userId() != null) {
                return rateLimited.keyPrefix() + ":user:" + principal.userId();
            }
        }
        if (!rateLimited.includeIp()) {
            return rateLimited.keyPrefix() + ":global";
        }
        return rateLimited.keyPrefix() + ":" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
