package com.dream.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthInterceptorTest {

    private final JwtService jwtService = new JwtService(jwtProperties());
    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(jwtService);

    @Test
    void userTokenIsAuthenticatedForAdminApi() {
        MockHttpServletRequest request = request("/api/admin/ai/config", jwtService.issueToken(1L, "openid", "user"));

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(accepted).isTrue();
        assertThat(CurrentUserContext.require().isAdmin()).isFalse();
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
    }

    @Test
    void adminTokenIsAcceptedForAdminApi() {
        MockHttpServletRequest request = request("/api/admin/ai/config", jwtService.issueAdminToken("admin"));

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(accepted).isTrue();
        assertThat(CurrentUserContext.require().isAdmin()).isTrue();
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
    }

    private MockHttpServletRequest request(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-with-more-than-32-bytes");
        return properties;
    }
}
