package com.dream.service.ai;

public record AiCompletionRequest(
        String systemPrompt,
        String userPrompt,
        String schemaJson,
        boolean repair
) {
}
