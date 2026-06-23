package com.dream.controller.dto;

public record AiProviderTestResponse(
        boolean success,
        String provider,
        String model,
        String message
) {
}
