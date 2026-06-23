package com.dream.controller.dto;

import com.dream.domain.AiProviderConfig;
import java.math.BigDecimal;

public record AiProviderConfigResponse(
        Long id,
        String name,
        String provider,
        String baseUrl,
        String apiKeyMasked,
        String model,
        BigDecimal temperature,
        Integer maxTokens,
        BigDecimal topP,
        Integer timeoutMs,
        String responseFormat,
        Boolean enabled,
        Integer priority,
        Integer weight
) {

    public static AiProviderConfigResponse from(AiProviderConfig config, String apiKeyMasked) {
        return new AiProviderConfigResponse(
                config.getId(),
                config.getName(),
                config.getProvider(),
                config.getBaseUrl(),
                apiKeyMasked,
                config.getModel(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getTopP(),
                config.getTimeoutMs(),
                config.getResponseFormat(),
                config.getEnabled(),
                config.getPriority(),
                config.getWeight());
    }
}
