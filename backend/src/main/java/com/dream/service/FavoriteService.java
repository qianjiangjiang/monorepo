package com.dream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.controller.dto.DreamHistoryItemResponse;
import com.dream.controller.dto.FavoriteRequest;
import com.dream.controller.dto.FavoriteResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.domain.DreamRecord;
import com.dream.domain.DreamResult;
import com.dream.domain.Favorite;
import com.dream.mapper.DreamRecordMapper;
import com.dream.mapper.DreamResultMapper;
import com.dream.mapper.FavoriteMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FavoriteService {

    private static final String ACTION_ADD = "add";
    private static final String ACTION_REMOVE = "remove";

    private final FavoriteMapper favoriteMapper;
    private final DreamResultMapper dreamResultMapper;
    private final DreamRecordMapper dreamRecordMapper;
    private final DreamResultSanitizer dreamResultSanitizer;
    private final ObjectMapper objectMapper;

    public FavoriteService(
            FavoriteMapper favoriteMapper,
            DreamResultMapper dreamResultMapper,
            DreamRecordMapper dreamRecordMapper,
            DreamResultSanitizer dreamResultSanitizer,
            ObjectMapper objectMapper) {
        this.favoriteMapper = favoriteMapper;
        this.dreamResultMapper = dreamResultMapper;
        this.dreamRecordMapper = dreamRecordMapper;
        this.dreamResultSanitizer = dreamResultSanitizer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FavoriteResponse favorite(Long userId, FavoriteRequest request) {
        DreamResult result = requireOwnedResult(userId, request.dreamResultId());
        String action = request.action().trim().toLowerCase();
        if (ACTION_ADD.equals(action)) {
            Favorite existing = favoriteMapper.selectOne(Wrappers.<Favorite>lambdaQuery()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getDreamResultId, result.getId()));
            if (existing == null) {
                Favorite favorite = new Favorite();
                favorite.setUserId(userId);
                favorite.setDreamResultId(result.getId());
                try {
                    favoriteMapper.insert(favorite);
                } catch (DuplicateKeyException ignored) {
                    // Concurrent add is still an idempotent success for the caller.
                }
            }
            return new FavoriteResponse(true);
        }
        if (ACTION_REMOVE.equals(action)) {
            favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getDreamResultId, result.getId()));
            return new FavoriteResponse(false);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "favorite action must be add or remove");
    }

    public PageResponse<DreamHistoryItemResponse> list(Long userId, int page, int size) {
        IPage<Favorite> favoritePage = favoriteMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)),
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt)
                        .orderByDesc(Favorite::getId));
        List<Long> resultIds = favoritePage.getRecords().stream()
                .map(Favorite::getDreamResultId)
                .toList();
        Map<Long, DreamResult> resultsById = resultsById(resultIds);
        Map<Long, DreamRecord> recordsById = recordsById(resultsById.values().stream()
                .map(DreamResult::getDreamRecordId)
                .toList());

        List<DreamHistoryItemResponse> items = favoritePage.getRecords().stream()
                .map(Favorite::getDreamResultId)
                .map(resultsById::get)
                .filter(result -> result != null && recordsById.containsKey(result.getDreamRecordId()))
                .map(result -> toHistoryItem(recordsById.get(result.getDreamRecordId()), result))
                .toList();
        return new PageResponse<>(favoritePage.getTotal(), items);
    }

    private DreamResult requireOwnedResult(Long userId, Long dreamResultId) {
        DreamResult result = dreamResultMapper.selectById(dreamResultId);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "dream result not found");
        }
        DreamRecord record = dreamRecordMapper.selectOne(Wrappers.<DreamRecord>lambdaQuery()
                .eq(DreamRecord::getId, result.getDreamRecordId())
                .eq(DreamRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "dream result not found");
        }
        return result;
    }

    private Map<Long, DreamResult> resultsById(List<Long> resultIds) {
        if (resultIds.isEmpty()) {
            return Map.of();
        }
        List<DreamResult> results = dreamResultMapper.selectList(Wrappers.<DreamResult>lambdaQuery()
                .in(DreamResult::getId, resultIds));
        Map<Long, DreamResult> byId = new LinkedHashMap<>();
        for (DreamResult result : results) {
            byId.put(result.getId(), result);
        }
        return byId;
    }

    private Map<Long, DreamRecord> recordsById(List<Long> recordIds) {
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        List<DreamRecord> records = dreamRecordMapper.selectList(Wrappers.<DreamRecord>lambdaQuery()
                .in(DreamRecord::getId, recordIds));
        Map<Long, DreamRecord> byId = new LinkedHashMap<>();
        for (DreamRecord record : records) {
            byId.put(record.getId(), record);
        }
        return byId;
    }

    private DreamHistoryItemResponse toHistoryItem(DreamRecord record, DreamResult result) {
        JsonNode parsedResult = readResultJson(result.getResultJson());
        return new DreamHistoryItemResponse(
                record.getId(),
                result.getId(),
                record.getDreamText(),
                parsedResult.path("summary").asText(""),
                record.getCreatedAt(),
                parseTags(record.getTags()),
                responseSchool(result),
                true,
                parsedResult);
    }

    private String responseSchool(DreamResult result) {
        return StringUtils.hasText(result.getSchool()) ? result.getSchool() : "";
    }

    private JsonNode readResultJson(String resultJson) {
        try {
            return dreamResultSanitizer.withDisclaimer(objectMapper.readTree(resultJson));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "dream result json is invalid");
        }
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String tag : tags.split(",")) {
            if (StringUtils.hasText(tag)) {
                parsed.add(tag.trim());
            }
        }
        return parsed;
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 50);
    }
}
