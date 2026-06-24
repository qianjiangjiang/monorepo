package com.dream.service;

import com.dream.common.auth.JwtService;
import com.dream.controller.dto.UserResponse;
import com.dream.controller.dto.WxLoginResponse;
import com.dream.domain.User;
import com.dream.service.wechat.WechatClient;
import com.dream.service.wechat.WechatSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final WechatClient wechatClient;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(WechatClient wechatClient, UserService userService, JwtService jwtService) {
        this.wechatClient = wechatClient;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Transactional
    public WxLoginResponse wxLogin(String code) {
        WechatSession session = wechatClient.code2Session(code);
        User user = userService.upsertByOpenid(session.openid());
        String token = jwtService.issueToken(user.getId(), user.getOpenid(), user.getRole());
        return new WxLoginResponse(token, UserResponse.from(user));
    }
}
