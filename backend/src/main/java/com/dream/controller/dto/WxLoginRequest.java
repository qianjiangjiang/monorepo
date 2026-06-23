package com.dream.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record WxLoginRequest(
        @NotBlank(message = "code is required") String code
) {
}
