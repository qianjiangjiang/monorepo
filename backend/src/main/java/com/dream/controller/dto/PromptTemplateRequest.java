package com.dream.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptTemplateRequest(
        Long id,
        @NotBlank(message = "sceneCode is required") String sceneCode,
        @NotBlank(message = "version is required") String version,
        @NotBlank(message = "systemPrompt is required") String systemPrompt,
        @NotBlank(message = "userPromptTemplate is required") String userPromptTemplate,
        String schemaJson,
        Boolean enabled,
        String remark
) {
}
