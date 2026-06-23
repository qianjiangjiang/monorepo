package com.dream.controller.dto;

import com.dream.domain.SensitiveWord;
import java.time.LocalDateTime;

public record SensitiveWordResponse(
        Long id,
        String word,
        String type,
        LocalDateTime createdAt
) {

    public static SensitiveWordResponse from(SensitiveWord sensitiveWord) {
        return new SensitiveWordResponse(
                sensitiveWord.getId(),
                sensitiveWord.getWord(),
                sensitiveWord.getType(),
                sensitiveWord.getCreatedAt());
    }
}
