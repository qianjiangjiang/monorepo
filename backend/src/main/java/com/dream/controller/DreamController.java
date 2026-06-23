package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.common.auth.CurrentUserContext;
import com.dream.common.ratelimit.RateLimited;
import com.dream.controller.dto.DreamInterpretRequest;
import com.dream.controller.dto.DreamInterpretResponse;
import com.dream.service.DreamInterpretationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dream")
public class DreamController {

    private final DreamInterpretationService dreamInterpretationService;

    public DreamController(DreamInterpretationService dreamInterpretationService) {
        this.dreamInterpretationService = dreamInterpretationService;
    }

    @RateLimited(keyPrefix = "rate:dream:interpret", limit = 20, windowSeconds = 60)
    @PostMapping("/interpret")
    public ApiResponse<DreamInterpretResponse> interpret(@Valid @RequestBody DreamInterpretRequest request) {
        Long userId = CurrentUserContext.require().userId();
        return ApiResponse.ok(dreamInterpretationService.interpret(userId, request));
    }
}
