package com.dream.service;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.service.wechat.WechatClient;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContentSafetyService {

    private static final int WECHAT_CHUNK_SIZE = 2000;

    private final SensitiveWordCache sensitiveWordCache;
    private final WechatClient wechatClient;

    public ContentSafetyService(SensitiveWordCache sensitiveWordCache, WechatClient wechatClient) {
        this.sensitiveWordCache = sensitiveWordCache;
        this.wechatClient = wechatClient;
    }

    public void checkInterpretRequest(String openid, String dreamText, List<String> tags, String school) {
        String content = buildContent(dreamText, tags, school);
        checkSensitiveWords(content);
        checkWechat(openid, content);
    }

    private void checkSensitiveWords(String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        String lowerContent = content.toLowerCase(Locale.ROOT);
        for (String word : sensitiveWordCache.getWords()) {
            if (lowerContent.contains(word)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "content contains sensitive word");
            }
        }
    }

    private void checkWechat(String openid, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + WECHAT_CHUNK_SIZE);
            wechatClient.msgSecCheck(openid, content.substring(start, end));
            start = end;
        }
    }

    private String buildContent(String dreamText, List<String> tags, String school) {
        StringBuilder content = new StringBuilder();
        if (StringUtils.hasText(dreamText)) {
            content.append(dreamText.trim());
        }
        if (tags != null && !tags.isEmpty()) {
            content.append('\n').append(String.join(",", tags));
        }
        if (StringUtils.hasText(school)) {
            content.append('\n').append(school.trim());
        }
        return content.toString();
    }
}
