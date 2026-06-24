package com.dream.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public record DreamHistoryItemResponse(
        Long dreamRecordId,
        Long dreamResultId,
        String dreamText,
        String summary,
        LocalDateTime createdAt,
        List<String> tags,
        String school,
        Boolean favorited,
        JsonNode result
) {
}
