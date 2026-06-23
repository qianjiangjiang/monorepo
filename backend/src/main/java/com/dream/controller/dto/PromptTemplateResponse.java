package com.dream.controller.dto;

import com.dream.domain.PromptTemplate;

public record PromptTemplateResponse(
        Long id,
        String sceneCode,
        String version,
        String systemPrompt,
        String userPromptTemplate,
        String schemaJson,
        Boolean enabled,
        String remark
) {

    public static PromptTemplateResponse from(PromptTemplate template) {
        return new PromptTemplateResponse(
                template.getId(),
                template.getSceneCode(),
                template.getVersion(),
                template.getSystemPrompt(),
                template.getUserPromptTemplate(),
                template.getSchemaJson(),
                template.getEnabled(),
                template.getRemark());
    }
}
