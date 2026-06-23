package com.dream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.domain.SensitiveWord;
import com.dream.mapper.SensitiveWordMapper;
import com.dream.service.wechat.WechatClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentSafetyServiceTest {

    @Mock
    private SensitiveWordMapper sensitiveWordMapper;

    @Mock
    private WechatClient wechatClient;

    @InjectMocks
    private ContentSafetyService contentSafetyService;

    @Test
    void blocksSensitiveWordsBeforeWechatCheck() {
        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setWord("禁词");
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of(sensitiveWord));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> contentSafetyService.checkInterpretRequest("openid", "梦到禁词", List.of(), ""));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        verifyNoInteractions(wechatClient);
    }

    @Test
    void delegatesCleanContentToWechatCheck() {
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of());

        contentSafetyService.checkInterpretRequest("openid", "梦见月光", List.of("反复出现"), "心理学");

        verify(wechatClient).msgSecCheck(eq("openid"), contains("梦见月光"));
    }
}
