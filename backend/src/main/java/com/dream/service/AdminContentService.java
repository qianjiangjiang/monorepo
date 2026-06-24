package com.dream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.controller.dto.AdminDreamRecordResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.controller.dto.SensitiveWordRequest;
import com.dream.controller.dto.SensitiveWordResponse;
import com.dream.domain.DreamRecord;
import com.dream.domain.DreamResult;
import com.dream.domain.SensitiveWord;
import com.dream.mapper.DreamRecordMapper;
import com.dream.mapper.DreamResultMapper;
import com.dream.mapper.SensitiveWordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminContentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SensitiveWordMapper sensitiveWordMapper;
    private final DreamRecordMapper dreamRecordMapper;
    private final DreamResultMapper dreamResultMapper;
    private final SensitiveWordCache sensitiveWordCache;
    private final ObjectMapper objectMapper;

    public AdminContentService(
            SensitiveWordMapper sensitiveWordMapper,
            DreamRecordMapper dreamRecordMapper,
            DreamResultMapper dreamResultMapper,
            SensitiveWordCache sensitiveWordCache,
            ObjectMapper objectMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.dreamRecordMapper = dreamRecordMapper;
        this.dreamResultMapper = dreamResultMapper;
        this.sensitiveWordCache = sensitiveWordCache;
        this.objectMapper = objectMapper;
    }

    public List<SensitiveWordResponse> listSensitiveWords() {
        return sensitiveWordMapper.selectList(Wrappers.<SensitiveWord>lambdaQuery()
                        .orderByDesc(SensitiveWord::getCreatedAt)
                        .orderByDesc(SensitiveWord::getId))
                .stream()
                .map(SensitiveWordResponse::from)
                .toList();
    }

    @Transactional
    public SensitiveWordResponse saveSensitiveWord(SensitiveWordRequest request) {
        SensitiveWord sensitiveWord = request.id() == null
                ? new SensitiveWord()
                : getRequiredSensitiveWord(request.id());
        sensitiveWord.setWord(request.word().trim());
        sensitiveWord.setType(StringUtils.hasText(request.type()) ? request.type().trim() : "block");
        if (request.id() == null) {
            sensitiveWordMapper.insert(sensitiveWord);
        } else {
            sensitiveWordMapper.updateById(sensitiveWord);
        }
        sensitiveWordCache.refresh();
        return SensitiveWordResponse.from(sensitiveWord);
    }

    @Transactional
    public void deleteSensitiveWord(Long id) {
        sensitiveWordMapper.deleteById(id);
        sensitiveWordCache.refresh();
    }

    public PageResponse<AdminDreamRecordResponse> listDreamRecords(int page, int size) {
        IPage<DreamRecord> recordPage = dreamRecordMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)),
                Wrappers.<DreamRecord>lambdaQuery()
                        .orderByDesc(DreamRecord::getCreatedAt)
                        .orderByDesc(DreamRecord::getId));
        List<Long> recordIds = recordPage.getRecords().stream()
                .map(DreamRecord::getId)
                .toList();
        Map<Long, DreamResult> resultsByRecordId = latestResultsByRecordIds(recordIds);
        List<AdminDreamRecordResponse> records = recordPage.getRecords().stream()
                .map(record -> toDreamRecordResponse(record, resultsByRecordId.get(record.getId())))
                .toList();
        return new PageResponse<>(recordPage.getTotal(), records);
    }

    private SensitiveWord getRequiredSensitiveWord(Long id) {
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sensitive word not found");
        }
        return sensitiveWord;
    }

    private long normalizePage(int page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, DreamResult> latestResultsByRecordIds(List<Long> recordIds) {
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        List<DreamResult> results = dreamResultMapper.selectList(Wrappers.<DreamResult>lambdaQuery()
                .in(DreamResult::getDreamRecordId, recordIds)
                .orderByDesc(DreamResult::getCreatedAt)
                .orderByDesc(DreamResult::getId));
        Map<Long, DreamResult> latest = new LinkedHashMap<>();
        for (DreamResult result : results) {
            latest.putIfAbsent(result.getDreamRecordId(), result);
        }
        return latest;
    }

    private AdminDreamRecordResponse toDreamRecordResponse(DreamRecord record, DreamResult result) {
        JsonNode parsedResult = readResultJson(result == null ? null : result.getResultJson());
        return new AdminDreamRecordResponse(
                record.getId(),
                result == null ? null : result.getId(),
                record.getUserId(),
                record.getDreamText(),
                parseTags(record.getTags()),
                parsedResult == null ? "" : parsedResult.path("summary").asText(""),
                result == null ? null : result.getResultJson(),
                result == null ? null : result.getProvider(),
                result == null ? null : result.getModel(),
                result == null ? null : result.getPromptVersion(),
                result == null ? null : result.getTokenIn(),
                result == null ? null : result.getTokenOut(),
                result == null ? null : result.getStatus(),
                record.getCreatedAt(),
                result == null ? null : result.getCreatedAt());
    }

    private JsonNode readResultJson(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(resultJson);
        } catch (JsonProcessingException exception) {
            return null;
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
}
