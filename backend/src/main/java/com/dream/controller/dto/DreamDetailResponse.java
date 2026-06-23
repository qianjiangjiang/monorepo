package com.dream.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DreamDetailResponse(
        DreamHistoryItemResponse dreamRecord,
        JsonNode result
) {
}
