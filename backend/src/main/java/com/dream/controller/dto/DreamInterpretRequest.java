package com.dream.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DreamInterpretRequest(
        @NotBlank(message = "dreamText is required")
        @Size(max = 4000, message = "dreamText must be at most 4000 characters")
        String dreamText,
        List<String> tags,
        String school
) {
}
