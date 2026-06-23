package com.dream.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDreamRecordResponse(
        Long dreamRecordId,
        Long dreamResultId,
        Long userId,
        String dreamText,
        List<String> tags,
        String summary,
        String resultJson,
        String provider,
        String model,
        String promptVersion,
        Integer tokenIn,
        Integer tokenOut,
        String status,
        LocalDateTime createdAt,
        LocalDateTime resultCreatedAt
) {
}
