package com.dream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.domain.SensitiveWord;
import com.dream.mapper.SensitiveWordMapper;
import com.dream.service.wechat.WechatClient;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContentSafetyService {

    private static final int WECHAT_CHUNK_SIZE = 2000;

    private final SensitiveWordMapper sensitiveWordMapper;
    private final WechatClient wechatClient;

    public ContentSafetyService(SensitiveWordMapper sensitiveWordMapper, WechatClient wechatClient) {
        this.sensitiveWordMapper = sensitiveWordMapper;
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
        String lowerContent = content.toLowerCase();
        List<SensitiveWord> words = sensitiveWordMapper.selectList(Wrappers.<SensitiveWord>query()
                .select("word"));
        for (SensitiveWord sensitiveWord : words) {
            String word = sensitiveWord.getWord();
            if (StringUtils.hasText(word) && lowerContent.contains(word.trim().toLowerCase())) {
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
