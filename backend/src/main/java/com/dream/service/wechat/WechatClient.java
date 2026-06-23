package com.dream.service.wechat;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WechatClient {

    private static final String MOCK_CODE_PREFIX = "mock";

    private final WechatProperties properties;
    private final WebClient webClient;

    public WechatClient(WechatProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    public WechatSession code2Session(String code) {
        if (properties.isMockEnabled() && code.startsWith(MOCK_CODE_PREFIX)) {
            return mockSession(code);
        }

        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat app config is missing");
        }

        URI uri = UriComponentsBuilder.fromUriString(properties.getCode2SessionUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUri();

        WechatSession session = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WechatSession.class)
                .block(properties.getTimeout());

        if (session == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat code2session returned empty response");
        }
        if (session.errcode() != null && session.errcode() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "wechat code2session failed: " + session.errmsg());
        }
        if (!StringUtils.hasText(session.openid())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat openid is missing");
        }
        return session;
    }

    private WechatSession mockSession(String code) {
        String suffix = code.replaceFirst("^mock:?", "");
        if (!StringUtils.hasText(suffix)) {
            suffix = "local";
        }
        String openid = "mock-openid-" + suffix.replaceAll("[^A-Za-z0-9_-]", "-");
        return new WechatSession(openid, null, null, null, null);
    }
}
