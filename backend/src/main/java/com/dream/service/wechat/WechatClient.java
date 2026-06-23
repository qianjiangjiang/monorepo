package com.dream.service.wechat;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WechatClient {

    private static final String MOCK_CODE_PREFIX = "mock";
    private static final long ACCESS_TOKEN_EXPIRY_SKEW_SECONDS = 60;

    private final WechatProperties properties;
    private final WebClient webClient;
    private volatile String cachedAccessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

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

    public void msgSecCheck(String openid, String content) {
        if (!properties.isContentSafetyEnabled() || properties.isMockEnabled() || !StringUtils.hasText(content)) {
            return;
        }
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        URI uri = UriComponentsBuilder.fromUriString(properties.getMsgSecCheckUrl())
                .queryParam("access_token", accessToken())
                .build()
                .toUri();

        MsgSecCheckResponse response = webClient.post()
                .uri(uri)
                .bodyValue(new MsgSecCheckRequest(content, 2, 1, openid))
                .retrieve()
                .bodyToMono(MsgSecCheckResponse.class)
                .block(properties.getTimeout());

        if (response == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat msgSecCheck returned empty response");
        }
        if (response.errcode() != null && response.errcode() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat msgSecCheck failed: " + response.errmsg());
        }
        if (response.result() != null && !"pass".equalsIgnoreCase(response.result().suggest())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content did not pass wechat security check");
        }
    }

    private String accessToken() {
        Instant now = Instant.now();
        if (StringUtils.hasText(cachedAccessToken) && now.isBefore(accessTokenExpiresAt)) {
            return cachedAccessToken;
        }
        synchronized (this) {
            now = Instant.now();
            if (StringUtils.hasText(cachedAccessToken) && now.isBefore(accessTokenExpiresAt)) {
                return cachedAccessToken;
            }
            if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat app config is missing");
            }
            URI uri = UriComponentsBuilder.fromUriString(properties.getAccessTokenUrl())
                    .queryParam("grant_type", "client_credential")
                    .queryParam("appid", properties.getAppId())
                    .queryParam("secret", properties.getAppSecret())
                    .build()
                    .toUri();
            AccessTokenResponse response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block(properties.getTimeout());
            if (response == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat access_token returned empty response");
            }
            if (response.errcode() != null && response.errcode() != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat access_token failed: " + response.errmsg());
            }
            if (!StringUtils.hasText(response.accessToken())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat access_token is missing");
            }
            long expiresIn = response.expiresIn() == null ? 7200 : response.expiresIn();
            cachedAccessToken = response.accessToken();
            accessTokenExpiresAt = now.plusSeconds(Math.max(60, expiresIn - ACCESS_TOKEN_EXPIRY_SKEW_SECONDS));
            return cachedAccessToken;
        }
    }

    private WechatSession mockSession(String code) {
        String suffix = code.replaceFirst("^mock:?", "");
        if (!StringUtils.hasText(suffix)) {
            suffix = "local";
        }
        String openid = "mock-openid-" + suffix.replaceAll("[^A-Za-z0-9_-]", "-");
        return new WechatSession(openid, null, null, null, null);
    }

    private record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Integer expiresIn,
            Integer errcode,
            String errmsg
    ) {
    }

    private record MsgSecCheckRequest(
            String content,
            Integer version,
            Integer scene,
            String openid
    ) {
    }

    private record MsgSecCheckResponse(
            Integer errcode,
            String errmsg,
            MsgSecCheckResult result
    ) {
    }

    private record MsgSecCheckResult(
            String suggest,
            Integer label
    ) {
    }
}
