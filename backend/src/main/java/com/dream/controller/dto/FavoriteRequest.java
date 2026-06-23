package com.dream.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FavoriteRequest(
        @NotNull(message = "dreamResultId is required")
        @Positive(message = "dreamResultId must be positive")
        Long dreamResultId,
        @NotBlank(message = "action is required")
        String action
) {
}
