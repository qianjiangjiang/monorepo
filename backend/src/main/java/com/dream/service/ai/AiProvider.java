package com.dream.service.ai;

import com.dream.domain.AiProviderConfig;

public interface AiProvider {

    boolean supports(String provider);

    AiCompletionResponse complete(AiProviderConfig config, String apiKey, AiCompletionRequest request);
}
