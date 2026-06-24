package com.dream.service;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.common.auth.JwtService;
import com.dream.config.AdminProperties;
import com.dream.controller.dto.AdminLoginRequest;
import com.dream.controller.dto.AdminLoginResponse;
import com.dream.controller.dto.UserResponse;
import com.dream.controller.dto.WxLoginResponse;
import com.dream.domain.User;
import com.dream.service.wechat.WechatClient;
import com.dream.service.wechat.WechatSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final WechatClient wechatClient;
    private final UserService userService;
    private final JwtService jwtService;
    private final AdminProperties adminProperties;

    public AuthService(
            WechatClient wechatClient,
            UserService userService,
            JwtService jwtService,
            AdminProperties adminProperties) {
        this.wechatClient = wechatClient;
        this.userService = userService;
        this.jwtService = jwtService;
        this.adminProperties = adminProperties;
    }

    @Transactional
    public WxLoginResponse wxLogin(String code) {
        WechatSession session = wechatClient.code2Session(code);
        User user = userService.upsertByOpenid(session.openid());
        String token = jwtService.issueToken(user.getId(), user.getOpenid());
        return new WxLoginResponse(token, UserResponse.from(user));
    }

    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        if (!StringUtils.hasText(adminProperties.getUsername())
                || !StringUtils.hasText(adminProperties.getPassword())
                || !safeEquals(adminProperties.getUsername(), request.username().trim())
                || !safeEquals(adminProperties.getPassword(), request.password())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new AdminLoginResponse(jwtService.issueAdminToken(request.username().trim()), "admin");
    }

    private boolean safeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
