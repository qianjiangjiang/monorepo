package com.dream.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream.common.auth.CurrentUserContext;
import com.dream.common.auth.UserPrincipal;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RedisRateLimiter rateLimiter;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimiter);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void usesCurrentUserWhenUserScopedLimitIsConfigured() throws Exception {
        CurrentUserContext.set(new UserPrincipal(42L, "openid", "user"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handler("userLimited"));

        assertThat(allowed).isTrue();
        verify(rateLimiter).tryAcquire(eq("rate:test:user:42"), eq(7), eq(Duration.ofSeconds(60)));
    }

    @Test
    void fallsBackToClientIpWhenUserScopedLimitHasNoCurrentUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.2");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handler("userLimited"));

        assertThat(allowed).isTrue();
        verify(rateLimiter).tryAcquire(eq("rate:test:203.0.113.7"), eq(7), eq(Duration.ofSeconds(60)));
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {

        @RateLimited(keyPrefix = "rate:test", limit = 7, windowSeconds = 60, byUser = true)
        public void userLimited() {
        }
    }
}
