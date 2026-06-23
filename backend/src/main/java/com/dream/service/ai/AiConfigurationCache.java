package com.dream.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dream.domain.AiProviderConfig;
import com.dream.domain.PromptTemplate;
import com.dream.mapper.AiProviderConfigMapper;
import com.dream.mapper.PromptTemplateMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiConfigurationCache {

    private static final String ENABLED_PROVIDERS_KEY = "enabled-providers";

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final Cache<String, List<AiProviderConfig>> providersCache = Caffeine.newBuilder().build();
    private final Cache<String, Optional<PromptTemplate>> promptTemplatesCache = Caffeine.newBuilder().build();

    public AiConfigurationCache(
            AiProviderConfigMapper aiProviderConfigMapper,
            PromptTemplateMapper promptTemplateMapper) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @PostConstruct
    public void warmup() {
        refresh();
    }

    public void refresh() {
        providersCache.invalidateAll();
        promptTemplatesCache.invalidateAll();
        providersCache.put(ENABLED_PROVIDERS_KEY, loadEnabledProviderConfigs());
        promptTemplatesCache.put("interpret", loadPromptTemplate("interpret"));
    }

    public List<AiProviderConfig> getEnabledProviderConfigs() {
        return providersCache.get(ENABLED_PROVIDERS_KEY, key -> loadEnabledProviderConfigs());
    }

    public Optional<PromptTemplate> getPromptTemplate(String sceneCode) {
        return promptTemplatesCache.get(sceneCode, this::loadPromptTemplate);
    }

    private List<AiProviderConfig> loadEnabledProviderConfigs() {
        return aiProviderConfigMapper.selectList(Wrappers.<AiProviderConfig>lambdaQuery()
                .eq(AiProviderConfig::getEnabled, true)
                .orderByAsc(AiProviderConfig::getPriority)
                .orderByDesc(AiProviderConfig::getWeight)
                .orderByAsc(AiProviderConfig::getId));
    }

    private Optional<PromptTemplate> loadPromptTemplate(String sceneCode) {
        return Optional.ofNullable(promptTemplateMapper.selectOne(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getSceneCode, sceneCode)
                .eq(PromptTemplate::getEnabled, true)
                .orderByDesc(PromptTemplate::getId)
                .last("LIMIT 1")));
    }
}
