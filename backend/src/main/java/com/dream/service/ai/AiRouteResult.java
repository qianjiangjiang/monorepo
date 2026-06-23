package com.dream.service.ai;

import com.dream.domain.AiProviderConfig;

public record AiRouteResult(
        AiProviderConfig config,
        AiCompletionResponse response,
        boolean fallbackUsed,
        int attempts
) {
}
