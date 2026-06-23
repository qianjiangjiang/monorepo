package com.dream.service.ai;

public record AiCompletionResponse(
        String content,
        int tokenIn,
        int tokenOut
) {
}
