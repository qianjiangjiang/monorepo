package com.dream.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dream.common.auth.AdminAuthInterceptor;
import com.dream.common.auth.JwtAuthInterceptor;
import com.dream.common.auth.JwtProperties;
import com.dream.common.auth.JwtService;
import com.dream.common.auth.UserPrincipal;
import com.dream.common.exception.GlobalExceptionHandler;
import com.dream.service.AdminContentService;
import com.dream.service.AiAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminAiControllerAuthTest {

    private static final String JWT_SECRET = "jwt-secret-0123456789abcdef012345";

    private AiAdminService aiAdminService;
    private MockMvc mockMvc;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        aiAdminService = org.mockito.Mockito.mock(AiAdminService.class);
        AdminContentService adminContentService = org.mockito.Mockito.mock(AdminContentService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(JWT_SECRET);
        jwtProperties.afterPropertiesSet();
        jwtService = new JwtService(jwtProperties);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminAiController(aiAdminService, adminContentService))
                .addInterceptors(new JwtAuthInterceptor(jwtService), new AdminAuthInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsNormalUserJwtOnAdminEndpoint() throws Exception {
        String token = jwtService.issueToken(1L, "openid-user", UserPrincipal.ROLE_USER);

        mockMvc.perform(get("/api/admin/ai/config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(aiAdminService);
    }

    @Test
    void allowsAdminJwtOnAdminEndpoint() throws Exception {
        when(aiAdminService.listProviderConfigs()).thenReturn(List.of());
        String token = jwtService.issueToken(2L, "openid-admin", UserPrincipal.ROLE_ADMIN);

        mockMvc.perform(get("/api/admin/ai/config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(aiAdminService).listProviderConfigs();
    }
}
