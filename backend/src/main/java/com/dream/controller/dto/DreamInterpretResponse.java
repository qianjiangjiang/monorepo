package com.dream.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DreamInterpretResponse(
        Long dreamRecordId,
        Long dreamResultId,
        String school,
        JsonNode result
) {
}
