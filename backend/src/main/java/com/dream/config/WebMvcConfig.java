package com.dream.config;

import com.dream.common.auth.AdminAuthInterceptor;
import com.dream.common.auth.JwtAuthInterceptor;
import com.dream.common.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(
            JwtAuthInterceptor jwtAuthInterceptor,
            AdminAuthInterceptor adminAuthInterceptor,
            RateLimitInterceptor rateLimitInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.*.*:*",
                        "http://10.*.*.*:*",
                        "http://172.*.*.*:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/wxLogin", "/api/auth/adminLogin", "/api/health");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
