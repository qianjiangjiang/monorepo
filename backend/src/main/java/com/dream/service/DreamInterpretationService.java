package com.dream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dream.common.auth.UserPrincipal;
import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import com.dream.controller.dto.DreamDetailResponse;
import com.dream.controller.dto.DreamHistoryItemResponse;
import com.dream.controller.dto.DreamInterpretRequest;
import com.dream.controller.dto.DreamInterpretResponse;
import com.dream.controller.dto.PageResponse;
import com.dream.domain.DreamRecord;
import com.dream.domain.DreamResult;
import com.dream.domain.Favorite;
import com.dream.domain.PromptTemplate;
import com.dream.mapper.DreamRecordMapper;
import com.dream.mapper.DreamResultMapper;
import com.dream.mapper.FavoriteMapper;
import com.dream.service.ai.AiCompletionRequest;
import com.dream.service.ai.AiConfigurationCache;
import com.dream.service.ai.AiProviderException;
import com.dream.service.ai.AiProviderRouter;
import com.dream.service.ai.AiRouteResult;
import com.dream.service.ai.DreamResultSchemaValidator;
import com.dream.service.ai.DreamValidationResult;
import com.dream.service.ai.PromptRenderer;
import com.dream.service.ai.SafeDreamResultFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class DreamInterpretationService {

    private static final Logger log = LoggerFactory.getLogger(DreamInterpretationService.class);

    private static final String DEFAULT_SCHEMA_HINT = """
            Return only one JSON object with fields title, summary, overallTone, symbols, emotion,
            interpretations, fortune, suggestions, and optional tags. interpretations must include
            two entries whose school values are exactly 传统文化 and 心理学.
            """;

    private final DreamRecordMapper dreamRecordMapper;
    private final DreamResultMapper dreamResultMapper;
    private final FavoriteMapper favoriteMapper;
    private final AiConfigurationCache aiConfigurationCache;
    private final PromptRenderer promptRenderer;
    private final AiProviderRouter aiProviderRouter;
    private final DreamResultSchemaValidator schemaValidator;
    private final SafeDreamResultFactory safeDreamResultFactory;
    private final ContentSafetyService contentSafetyService;
    private final DreamQuotaService dreamQuotaService;
    private final DreamResultCacheService dreamResultCacheService;
    private final DreamResultSanitizer dreamResultSanitizer;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public DreamInterpretationService(
            DreamRecordMapper dreamRecordMapper,
            DreamResultMapper dreamResultMapper,
            FavoriteMapper favoriteMapper,
            AiConfigurationCache aiConfigurationCache,
            PromptRenderer promptRenderer,
            AiProviderRouter aiProviderRouter,
            DreamResultSchemaValidator schemaValidator,
            SafeDreamResultFactory safeDreamResultFactory,
            ContentSafetyService contentSafetyService,
            DreamQuotaService dreamQuotaService,
            DreamResultCacheService dreamResultCacheService,
            DreamResultSanitizer dreamResultSanitizer,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper) {
        this.dreamRecordMapper = dreamRecordMapper;
        this.dreamResultMapper = dreamResultMapper;
        this.favoriteMapper = favoriteMapper;
        this.aiConfigurationCache = aiConfigurationCache;
        this.promptRenderer = promptRenderer;
        this.aiProviderRouter = aiProviderRouter;
        this.schemaValidator = schemaValidator;
        this.safeDreamResultFactory = safeDreamResultFactory;
        this.contentSafetyService = contentSafetyService;
        this.dreamQuotaService = dreamQuotaService;
        this.dreamResultCacheService = dreamResultCacheService;
        this.dreamResultSanitizer = dreamResultSanitizer;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    public DreamInterpretResponse interpret(UserPrincipal principal, DreamInterpretRequest request) {
        Long userId = principal.userId();
        String dreamText = request.dreamText().trim();
        String school = normalizeSchool(request.school());
        List<String> tags = sanitizeTags(request.tags());

        contentSafetyService.checkInterpretRequest(principal.openid(), dreamText, tags, school);

        Optional<JsonNode> cachedResult = dreamResultCacheService.get(dreamText, tags, school)
                .map(dreamResultSanitizer::withDisclaimer);
        if (cachedResult.isPresent()) {
            return persistInTransaction(userId, dreamText, tags, school, new AiOutcome(
                    cachedResult.get(), "cache", "cache", "success", 0, 0, "cache"));
        }

        dreamQuotaService.consumeDailyFreeQuota(userId);

        PromptTemplate template = aiConfigurationCache.getPromptTemplate("interpret").orElseGet(this::defaultPromptTemplate);
        PromptRenderer.RenderedPrompt prompt = promptRenderer.render(template, dreamText, school);
        AiOutcome outcome = completeWithGuardrails(dreamText, tags, prompt);
        JsonNode result = dreamResultSanitizer.withDisclaimer(outcome.result());
        dreamResultCacheService.put(dreamText, tags, school, result);

        return persistInTransaction(userId, dreamText, tags, school, new AiOutcome(
                result,
                outcome.provider(),
                outcome.model(),
                outcome.status(),
                outcome.tokenIn(),
                outcome.tokenOut(),
                prompt.version()));
    }

    private DreamInterpretResponse persistInTransaction(
            Long userId,
            String dreamText,
            List<String> tags,
            String school,
            AiOutcome outcome) {
        return Objects.requireNonNull(transactionTemplate.execute(status ->
                persistInterpretation(userId, dreamText, tags, school, outcome)));
    }

    public PageResponse<DreamHistoryItemResponse> history(Long userId, int page, int size) {
        IPage<DreamRecord> recordPage = dreamRecordMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)),
                Wrappers.<DreamRecord>lambdaQuery()
                        .eq(DreamRecord::getUserId, userId)
                        .orderByDesc(DreamRecord::getCreatedAt)
                        .orderByDesc(DreamRecord::getId));

        List<Long> recordIds = recordPage.getRecords().stream()
                .map(DreamRecord::getId)
                .toList();
        Map<Long, DreamResult> resultsByRecordId = latestResultsByRecordIds(recordIds);
        Set<Long> favoriteIds = favoritedResultIds(userId, resultsByRecordId.values().stream()
                .map(DreamResult::getId)
                .toList());

        List<DreamHistoryItemResponse> items = recordPage.getRecords().stream()
                .map(record -> toHistoryItem(record, resultsByRecordId.get(record.getId()), favoriteIds))
                .toList();
        return new PageResponse<>(recordPage.getTotal(), items);
    }

    public DreamDetailResponse detail(Long userId, Long dreamRecordId) {
        DreamRecord record = dreamRecordMapper.selectOne(Wrappers.<DreamRecord>lambdaQuery()
                .eq(DreamRecord::getId, dreamRecordId)
                .eq(DreamRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "dream record not found");
        }

        DreamResult result = latestResultForRecord(record.getId());
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "dream result not found");
        }

        Set<Long> favoriteIds = favoritedResultIds(userId, List.of(result.getId()));
        DreamHistoryItemResponse item = toHistoryItem(record, result, favoriteIds);
        return new DreamDetailResponse(item, item.result());
    }

    private DreamInterpretResponse persistInterpretation(
            Long userId,
            String dreamText,
            List<String> tags,
            String school,
            AiOutcome outcome) {
        DreamRecord record = new DreamRecord();
        record.setUserId(userId);
        record.setDreamText(dreamText);
        record.setTags(tags.isEmpty() ? null : String.join(",", tags));
        dreamRecordMapper.insert(record);

        DreamResult result = new DreamResult();
        result.setDreamRecordId(record.getId());
        result.setSchool(StringUtils.hasText(school) ? school : null);
        result.setResultJson(writeJson(outcome.result()));
        result.setProvider(outcome.provider());
        result.setModel(outcome.model());
        result.setPromptVersion(outcome.promptVersion());
        result.setTokenIn(outcome.tokenIn());
        result.setTokenOut(outcome.tokenOut());
        result.setStatus(outcome.status());
        dreamResultMapper.insert(result);

        return new DreamInterpretResponse(record.getId(), result.getId(), school, outcome.result());
    }

    private AiOutcome completeWithGuardrails(String dreamText, List<String> tags, PromptRenderer.RenderedPrompt prompt) {
        int tokenIn = 0;
        int tokenOut = 0;
        boolean fallbackUsed = false;
        String provider = "safety";
        String model = "local";

        try {
            AiRouteResult first = aiProviderRouter.complete(new AiCompletionRequest(
                    prompt.systemPrompt(),
                    prompt.userPrompt(),
                    prompt.schemaJson(),
                    false));
            tokenIn += first.response().tokenIn();
            tokenOut += first.response().tokenOut();
            fallbackUsed = fallbackUsed || first.fallbackUsed();
            provider = first.config().getProvider();
            model = first.config().getModel();

            ParsedDreamResult parsed = parseAndValidate(first.response().content());
            if (parsed.valid()) {
                return new AiOutcome(parsed.result(), provider, model, fallbackUsed ? "fallback" : "success", tokenIn, tokenOut, prompt.version());
            }

            AiRouteResult repaired = aiProviderRouter.complete(new AiCompletionRequest(
                    repairSystemPrompt(),
                    repairUserPrompt(first.response().content(), parsed.errors(), prompt.schemaJson()),
                    prompt.schemaJson(),
                    true));
            tokenIn += repaired.response().tokenIn();
            tokenOut += repaired.response().tokenOut();
            fallbackUsed = fallbackUsed || repaired.fallbackUsed();
            provider = repaired.config().getProvider();
            model = repaired.config().getModel();

            ParsedDreamResult repairedParsed = parseAndValidate(repaired.response().content());
            if (repairedParsed.valid()) {
                return new AiOutcome(repairedParsed.result(), provider, model, fallbackUsed ? "fallback" : "success", tokenIn, tokenOut, prompt.version());
            }
            log.warn("AI result failed schema validation after repair errors={}", repairedParsed.errors());
        } catch (AiProviderException exception) {
            log.warn("AI provider routing failed: {}", exception.getMessage());
        }

        JsonNode safeResult = dreamResultSanitizer.withDisclaimer(safeDreamResultFactory.create(dreamText, tags));
        return new AiOutcome(safeResult, provider, model, "fallback", tokenIn, tokenOut, prompt.version());
    }

    private ParsedDreamResult parseAndValidate(String rawContent) {
        try {
            JsonNode node = dreamResultSanitizer.withDisclaimer(objectMapper.readTree(extractJsonObject(rawContent)));
            DreamValidationResult validationResult = schemaValidator.validate(node);
            if (validationResult.valid()) {
                return ParsedDreamResult.valid(node);
            }
            return ParsedDreamResult.invalid(validationResult.errors());
        } catch (JsonProcessingException exception) {
            return ParsedDreamResult.invalid(List.of("invalid json: " + exception.getOriginalMessage()));
        }
    }

    private String extractJsonObject(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String repairSystemPrompt() {
        return "你是严格的 JSON 修复器。只返回一个 JSON 对象，不要输出解释、Markdown 或代码块。";
    }

    private String repairUserPrompt(String rawContent, List<String> errors, String schemaJson) {
        return """
                上一次 AI 输出不符合结构化解梦结果要求。请根据校验错误修复为合法 JSON。

                校验错误：
                %s

                Schema/字段要求：
                %s

                原始输出：
                %s
                """.formatted(String.join("\n", errors), StringUtils.hasText(schemaJson) ? schemaJson : DEFAULT_SCHEMA_HINT, rawContent);
    }

    private PromptTemplate defaultPromptTemplate() {
        PromptTemplate template = new PromptTemplate();
        template.setSceneCode("interpret");
        template.setVersion("builtin-v1");
        template.setEnabled(true);
        template.setSystemPrompt("""
                你是一个谨慎的梦境解读助手，融合传统文化象征和现代心理学视角。
                你必须输出严格 JSON，不要输出 Markdown、解释文字或代码块。
                不做绝对吉凶、医疗、法律、财务等现实断言。
                """);
        template.setUserPromptTemplate("""
                请解读以下梦境：
                {{dreamText}}

                流派展示偏好：{{school}}

                输出要求：
                1. 只输出一个 JSON 对象。
                2. 字段必须符合 dream-result.schema.json。
                3. interpretations 必须同时包含 school 为“传统文化”和“心理学”的两条，即使用户有具体展示偏好也不要省略任一视角。
                4. 若展示偏好为具体流派，展示层会优先呈现该流派；模型仍需输出双视角完整结果。
                5. fortune.disclaimer 固定表达为“%s”。
                """.formatted(DreamResultSanitizer.DISCLAIMER));
        template.setSchemaJson(DEFAULT_SCHEMA_HINT);
        return template;
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

    private DreamResult latestResultForRecord(Long recordId) {
        return dreamResultMapper.selectOne(Wrappers.<DreamResult>lambdaQuery()
                .eq(DreamResult::getDreamRecordId, recordId)
                .orderByDesc(DreamResult::getCreatedAt)
                .orderByDesc(DreamResult::getId)
                .last("LIMIT 1"));
    }

    private Set<Long> favoritedResultIds(Long userId, List<Long> resultIds) {
        List<Long> safeResultIds = resultIds.stream()
                .filter(Objects::nonNull)
                .toList();
        if (safeResultIds.isEmpty()) {
            return Set.of();
        }
        return favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .in(Favorite::getDreamResultId, safeResultIds))
                .stream()
                .map(Favorite::getDreamResultId)
                .collect(Collectors.toSet());
    }

    private DreamHistoryItemResponse toHistoryItem(DreamRecord record, DreamResult result, Set<Long> favoriteIds) {
        JsonNode parsedResult = result == null ? null : readResultJson(result.getResultJson());
        return new DreamHistoryItemResponse(
                record.getId(),
                result == null ? null : result.getId(),
                record.getDreamText(),
                parsedResult == null ? "" : parsedResult.path("summary").asText(""),
                record.getCreatedAt(),
                parseTags(record.getTags()),
                responseSchool(result),
                result != null && favoriteIds.contains(result.getId()),
                parsedResult);
    }

    private String responseSchool(DreamResult result) {
        return result != null && StringUtils.hasText(result.getSchool()) ? result.getSchool() : "";
    }

    private JsonNode readResultJson(String resultJson) {
        try {
            return dreamResultSanitizer.withDisclaimer(objectMapper.readTree(resultJson));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "dream result json is invalid");
        }
    }

    private String writeJson(JsonNode result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize dream result", exception);
        }
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String trimmed = tag.trim();
            sanitized.add(trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed);
            if (sanitized.size() >= 8) {
                break;
            }
        }
        return sanitized;
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

    private String normalizeSchool(String school) {
        return StringUtils.hasText(school) ? school.trim() : "";
    }

    private record ParsedDreamResult(JsonNode result, boolean valid, List<String> errors) {

        private static ParsedDreamResult valid(JsonNode result) {
            return new ParsedDreamResult(result, true, List.of());
        }

        private static ParsedDreamResult invalid(List<String> errors) {
            return new ParsedDreamResult(null, false, List.copyOf(errors));
        }
    }

    private record AiOutcome(
            JsonNode result,
            String provider,
            String model,
            String status,
            int tokenIn,
            int tokenOut,
            String promptVersion
    ) {
    }
}
