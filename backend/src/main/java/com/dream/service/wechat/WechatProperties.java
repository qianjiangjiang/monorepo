package com.dream.service.wechat;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dream.wx")
public class WechatProperties {

    private String appId;
    private String appSecret;
    private boolean mockEnabled;
    private boolean contentSafetyEnabled = true;
    private String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
    private String accessTokenUrl = "https://api.weixin.qq.com/cgi-bin/token";
    private String msgSecCheckUrl = "https://api.weixin.qq.com/wxa/msg_sec_check";
    private Duration timeout = Duration.ofSeconds(5);

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public boolean isContentSafetyEnabled() {
        return contentSafetyEnabled;
    }

    public void setContentSafetyEnabled(boolean contentSafetyEnabled) {
        this.contentSafetyEnabled = contentSafetyEnabled;
    }

    public String getCode2SessionUrl() {
        return code2SessionUrl;
    }

    public void setCode2SessionUrl(String code2SessionUrl) {
        this.code2SessionUrl = code2SessionUrl;
    }

    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }

    public String getMsgSecCheckUrl() {
        return msgSecCheckUrl;
    }

    public void setMsgSecCheckUrl(String msgSecCheckUrl) {
        this.msgSecCheckUrl = msgSecCheckUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
