package com.dream.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AiProviderConfigRequest(
        Long id,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "provider is required") String provider,
        @NotBlank(message = "baseUrl is required") String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required") String model,
        BigDecimal temperature,
        @Min(value = 1, message = "maxTokens must be positive") Integer maxTokens,
        BigDecimal topP,
        @Min(value = 1000, message = "timeoutMs must be at least 1000")
        @Max(value = 120000, message = "timeoutMs must be at most 120000")
        Integer timeoutMs,
        String responseFormat,
        Boolean enabled,
        Integer priority,
        @Min(value = 0, message = "weight must not be negative") Integer weight
) {
}
