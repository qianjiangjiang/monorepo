package com.dream.service.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WechatSession(
        String openid,
        @JsonProperty("session_key") String sessionKey,
        String unionid,
        Integer errcode,
        String errmsg
) {
}
