package com.dream.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record SensitiveWordRequest(
        Long id,
        @NotBlank(message = "word is required") String word,
        String type
) {
}
