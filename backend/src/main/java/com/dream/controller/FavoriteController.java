package com.dream.controller;

import com.dream.common.ApiResponse;
import com.dream.common.auth.CurrentUserContext;
import com.dream.controller.dto.DreamHistoryItemResponse;
import com.dream.controller.dto.FavoriteRequest;
import com.dream.controller.dto.FavoriteResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.service.FavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ApiResponse<FavoriteResponse> favorite(@Valid @RequestBody FavoriteRequest request) {
        Long userId = CurrentUserContext.require().userId();
        return ApiResponse.ok(favoriteService.favorite(userId, request));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<DreamHistoryItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        Long userId = CurrentUserContext.require().userId();
        return ApiResponse.ok(favoriteService.list(userId, page, size));
    }
}
