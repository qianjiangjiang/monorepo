package com.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream.domain.SensitiveWord;
import com.dream.mapper.SensitiveWordMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensitiveWordCacheTest {

    @Mock
    private SensitiveWordMapper sensitiveWordMapper;

    @Test
    void reusesLoadedWordsUntilRefresh() {
        when(sensitiveWordMapper.selectList(any())).thenReturn(List.of(sensitiveWord(" 禁词 ")));
        SensitiveWordCache cache = new SensitiveWordCache(sensitiveWordMapper);

        assertThat(cache.getWords()).containsExactly("禁词");
        assertThat(cache.getWords()).containsExactly("禁词");

        verify(sensitiveWordMapper, times(1)).selectList(any());
    }

    @Test
    void refreshReloadsWordsFromMapper() {
        when(sensitiveWordMapper.selectList(any()))
                .thenReturn(List.of(sensitiveWord("first")))
                .thenReturn(List.of(sensitiveWord("second")));
        SensitiveWordCache cache = new SensitiveWordCache(sensitiveWordMapper);

        assertThat(cache.getWords()).containsExactly("first");
        cache.refresh();

        assertThat(cache.getWords()).containsExactly("second");
        verify(sensitiveWordMapper, times(2)).selectList(any());
    }

    private SensitiveWord sensitiveWord(String word) {
        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setWord(word);
        return sensitiveWord;
    }
}
