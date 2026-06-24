package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.common.auth.CurrentUserContext;
import com.dream.common.auth.UserPrincipal;
import com.dream.common.ratelimit.RateLimited;
import com.dream.controller.dto.DreamDetailResponse;
import com.dream.controller.dto.DreamHistoryItemResponse;
import com.dream.controller.dto.DreamInterpretRequest;
import com.dream.controller.dto.DreamInterpretResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.service.DreamInterpretationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dream")
public class DreamController {

    private final DreamInterpretationService dreamInterpretationService;

    public DreamController(DreamInterpretationService dreamInterpretationService) {
        this.dreamInterpretationService = dreamInterpretationService;
    }

    @RateLimited(keyPrefix = "rate:dream:interpret", limit = 20, windowSeconds = 60, byUser = true)
    @PostMapping("/interpret")
    public ApiResponse<DreamInterpretResponse> interpret(@Valid @RequestBody DreamInterpretRequest request) {
        UserPrincipal principal = CurrentUserContext.require();
        return ApiResponse.ok(dreamInterpretationService.interpret(principal, request));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<DreamHistoryItemResponse>> history(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        Long userId = CurrentUserContext.require().userId();
        return ApiResponse.ok(dreamInterpretationService.history(userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DreamDetailResponse> detail(@PathVariable @Positive Long id) {
        Long userId = CurrentUserContext.require().userId();
        return ApiResponse.ok(dreamInterpretationService.detail(userId, id));
    }
}
