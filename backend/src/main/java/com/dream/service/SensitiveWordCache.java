package com.dream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dream.domain.SensitiveWord;
import com.dream.mapper.SensitiveWordMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SensitiveWordCache {

    private static final String WORDS_KEY = "words";

    private final SensitiveWordMapper sensitiveWordMapper;
    private final Cache<String, List<String>> wordsCache = Caffeine.newBuilder().build();

    public SensitiveWordCache(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct
    public void warmup() {
        refresh();
    }

    public void refresh() {
        wordsCache.invalidateAll();
        wordsCache.put(WORDS_KEY, loadWords());
    }

    public List<String> getWords() {
        return wordsCache.get(WORDS_KEY, key -> loadWords());
    }

    private List<String> loadWords() {
        return sensitiveWordMapper.selectList(Wrappers.<SensitiveWord>query()
                        .select("word"))
                .stream()
                .map(SensitiveWord::getWord)
                .filter(StringUtils::hasText)
                .map(word -> word.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
