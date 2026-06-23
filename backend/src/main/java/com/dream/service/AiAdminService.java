package com.dream.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dream.common.crypto.ApiKeyCipher;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.controller.dto.AiProviderConfigRequest;
import com.dream.controller.dto.AiProviderConfigResponse;
import com.dream.controller.dto.AiProviderTestResponse;
import com.dream.controller.dto.PromptTemplateRequest;
import com.dream.controller.dto.PromptTemplateResponse;
import com.dream.domain.AiProviderConfig;
import com.dream.domain.PromptTemplate;
import com.dream.mapper.AiProviderConfigMapper;
import com.dream.mapper.PromptTemplateMapper;
import com.dream.service.ai.AiCompletionRequest;
import com.dream.service.ai.AiProvider;
import com.dream.service.ai.AiProviderException;
import com.dream.service.ai.AiProviderRegistry;
import com.dream.service.ai.AiConfigurationCache;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiAdminService {

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final ApiKeyCipher apiKeyCipher;
    private final AiConfigurationCache aiConfigurationCache;
    private final AiProviderRegistry aiProviderRegistry;

    public AiAdminService(
            AiProviderConfigMapper aiProviderConfigMapper,
            PromptTemplateMapper promptTemplateMapper,
            ApiKeyCipher apiKeyCipher,
            AiConfigurationCache aiConfigurationCache,
            AiProviderRegistry aiProviderRegistry) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.promptTemplateMapper = promptTemplateMapper;
        this.apiKeyCipher = apiKeyCipher;
        this.aiConfigurationCache = aiConfigurationCache;
        this.aiProviderRegistry = aiProviderRegistry;
    }

    public List<AiProviderConfigResponse> listProviderConfigs() {
        return aiProviderConfigMapper.selectList(Wrappers.<AiProviderConfig>lambdaQuery()
                        .orderByAsc(AiProviderConfig::getPriority)
                        .orderByAsc(AiProviderConfig::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AiProviderConfigResponse saveProviderConfig(AiProviderConfigRequest request) {
        AiProviderConfig config = request.id() == null ? new AiProviderConfig() : getRequiredConfig(request.id());
        fillProviderConfig(config, request);
        if (request.id() == null) {
            aiProviderConfigMapper.insert(config);
        } else {
            aiProviderConfigMapper.updateById(config);
        }
        aiConfigurationCache.refresh();
        return toResponse(config);
    }

    @Transactional
    public void deleteProviderConfig(Long id) {
        aiProviderConfigMapper.deleteById(id);
        aiConfigurationCache.refresh();
    }

    public AiProviderTestResponse testProviderConfig(Long id) {
        AiProviderConfig config = getRequiredConfig(id);
        AiProvider provider = aiProviderRegistry.find(config.getProvider())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "unsupported provider"));
        try {
            provider.complete(config, apiKeyCipher.decrypt(config.getApiKey()), new AiCompletionRequest(
                    "You are a connectivity checker. Return JSON only.",
                    "Return {\"ok\":true} as a JSON object.",
                    null,
                    false));
            return new AiProviderTestResponse(true, config.getProvider(), config.getModel(), "ok");
        } catch (AiProviderException exception) {
            return new AiProviderTestResponse(false, config.getProvider(), config.getModel(), exception.getMessage());
        }
    }

    public List<PromptTemplateResponse> listPromptTemplates() {
        return promptTemplateMapper.selectList(Wrappers.<PromptTemplate>lambdaQuery()
                        .orderByAsc(PromptTemplate::getSceneCode)
                        .orderByDesc(PromptTemplate::getId))
                .stream()
                .map(PromptTemplateResponse::from)
                .toList();
    }

    @Transactional
    public PromptTemplateResponse savePromptTemplate(PromptTemplateRequest request) {
        PromptTemplate template = request.id() == null ? new PromptTemplate() : getRequiredTemplate(request.id());
        template.setSceneCode(request.sceneCode().trim());
        template.setVersion(request.version().trim());
        template.setSystemPrompt(request.systemPrompt());
        template.setUserPromptTemplate(request.userPromptTemplate());
        template.setSchemaJson(request.schemaJson());
        template.setEnabled(request.enabled() == null || request.enabled());
        template.setRemark(request.remark());
        if (request.id() == null) {
            promptTemplateMapper.insert(template);
        } else {
            promptTemplateMapper.updateById(template);
        }
        aiConfigurationCache.refresh();
        return PromptTemplateResponse.from(template);
    }

    @Transactional
    public void deletePromptTemplate(Long id) {
        promptTemplateMapper.deleteById(id);
        aiConfigurationCache.refresh();
    }

    public void refreshCache() {
        aiConfigurationCache.refresh();
    }

    private void fillProviderConfig(AiProviderConfig config, AiProviderConfigRequest request) {
        config.setName(request.name().trim());
        config.setProvider(request.provider().trim().toLowerCase(Locale.ROOT));
        config.setBaseUrl(request.baseUrl().trim());
        config.setModel(request.model().trim());
        config.setTemperature(defaultBigDecimal(request.temperature(), "0.70"));
        config.setMaxTokens(request.maxTokens() == null ? 1024 : request.maxTokens());
        config.setTopP(defaultBigDecimal(request.topP(), "1.00"));
        config.setTimeoutMs(request.timeoutMs() == null ? 30000 : request.timeoutMs());
        config.setResponseFormat(StringUtils.hasText(request.responseFormat()) ? request.responseFormat() : "json_object");
        config.setEnabled(request.enabled() == null || request.enabled());
        config.setPriority(request.priority() == null ? 100 : request.priority());
        config.setWeight(request.weight() == null ? 100 : request.weight());

        if (StringUtils.hasText(request.apiKey()) && !apiKeyCipher.looksMasked(request.apiKey())) {
            config.setApiKey(apiKeyCipher.encrypt(request.apiKey()));
        } else if (config.getId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "apiKey is required");
        }
    }

    private BigDecimal defaultBigDecimal(BigDecimal value, String defaultValue) {
        return value == null ? new BigDecimal(defaultValue) : value;
    }

    private AiProviderConfig getRequiredConfig(Long id) {
        AiProviderConfig config = aiProviderConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ai provider config not found");
        }
        return config;
    }

    private PromptTemplate getRequiredTemplate(Long id) {
        PromptTemplate template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "prompt template not found");
        }
        return template;
    }

    private AiProviderConfigResponse toResponse(AiProviderConfig config) {
        return AiProviderConfigResponse.from(config, apiKeyCipher.mask(apiKeyCipher.decrypt(config.getApiKey())));
    }
}
