package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.common.ratelimit.RateLimited;
import com.dream.controller.dto.AdminLoginRequest;
import com.dream.controller.dto.AdminLoginResponse;
import com.dream.controller.dto.WxLoginRequest;
import com.dream.controller.dto.WxLoginResponse;
import com.dream.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @RateLimited(keyPrefix = "rate:auth:wx-login", limit = 30, windowSeconds = 60)
    @PostMapping("/wxLogin")
    public ApiResponse<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.ok(authService.wxLogin(request.code()));
    }

    @RateLimited(keyPrefix = "rate:auth:admin-login", limit = 10, windowSeconds = 60)
    @PostMapping("/adminLogin")
    public ApiResponse<AdminLoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.adminLogin(request));
    }
}
