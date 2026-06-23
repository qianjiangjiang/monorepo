package com.dream.service;

import com.dream.controller.dto.DreamInterpretRequest;
import com.dream.controller.dto.DreamInterpretResponse;
import com.dream.domain.DreamRecord;
import com.dream.domain.DreamResult;
import com.dream.domain.PromptTemplate;
import com.dream.mapper.DreamRecordMapper;
import com.dream.mapper.DreamResultMapper;
import com.dream.service.ai.AiCompletionRequest;
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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final com.dream.service.ai.AiConfigurationCache aiConfigurationCache;
    private final PromptRenderer promptRenderer;
    private final AiProviderRouter aiProviderRouter;
    private final DreamResultSchemaValidator schemaValidator;
    private final SafeDreamResultFactory safeDreamResultFactory;
    private final ObjectMapper objectMapper;

    public DreamInterpretationService(
            DreamRecordMapper dreamRecordMapper,
            DreamResultMapper dreamResultMapper,
            com.dream.service.ai.AiConfigurationCache aiConfigurationCache,
            PromptRenderer promptRenderer,
            AiProviderRouter aiProviderRouter,
            DreamResultSchemaValidator schemaValidator,
            SafeDreamResultFactory safeDreamResultFactory,
            ObjectMapper objectMapper) {
        this.dreamRecordMapper = dreamRecordMapper;
        this.dreamResultMapper = dreamResultMapper;
        this.aiConfigurationCache = aiConfigurationCache;
        this.promptRenderer = promptRenderer;
        this.aiProviderRouter = aiProviderRouter;
        this.schemaValidator = schemaValidator;
        this.safeDreamResultFactory = safeDreamResultFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DreamInterpretResponse interpret(Long userId, DreamInterpretRequest request) {
        List<String> tags = sanitizeTags(request.tags());
        DreamRecord record = new DreamRecord();
        record.setUserId(userId);
        record.setDreamText(request.dreamText());
        record.setTags(tags.isEmpty() ? null : String.join(",", tags));
        dreamRecordMapper.insert(record);

        PromptTemplate template = aiConfigurationCache.getPromptTemplate("interpret").orElseGet(this::defaultPromptTemplate);
        PromptRenderer.RenderedPrompt prompt = promptRenderer.render(template, request.dreamText(), request.school());
        AiOutcome outcome = completeWithGuardrails(request.dreamText(), tags, prompt);

        DreamResult result = new DreamResult();
        result.setDreamRecordId(record.getId());
        result.setSchool(StringUtils.hasText(request.school()) ? request.school() : null);
        result.setResultJson(writeJson(outcome.result()));
        result.setProvider(outcome.provider());
        result.setModel(outcome.model());
        result.setPromptVersion(prompt.version());
        result.setTokenIn(outcome.tokenIn());
        result.setTokenOut(outcome.tokenOut());
        result.setStatus(outcome.status());
        dreamResultMapper.insert(result);

        return new DreamInterpretResponse(record.getId(), result.getId(), outcome.result());
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
                return new AiOutcome(parsed.result(), provider, model, fallbackUsed ? "fallback" : "success", tokenIn, tokenOut);
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
                return new AiOutcome(repairedParsed.result(), provider, model, fallbackUsed ? "fallback" : "success", tokenIn, tokenOut);
            }
            log.warn("AI result failed schema validation after repair errors={}", repairedParsed.errors());
        } catch (AiProviderException exception) {
            log.warn("AI provider routing failed: {}", exception.getMessage());
        }

        JsonNode safeResult = safeDreamResultFactory.create(dreamText, tags);
        return new AiOutcome(safeResult, provider, model, "fallback", tokenIn, tokenOut);
    }

    private ParsedDreamResult parseAndValidate(String rawContent) {
        try {
            JsonNode node = objectMapper.readTree(extractJsonObject(rawContent));
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

                请求流派：{{school}}

                输出要求：
                1. 只输出一个 JSON 对象。
                2. 字段必须符合 dream-result.schema.json。
                3. interpretations 至少包含 school 为“传统文化”和“心理学”的两条。
                4. fortune.disclaimer 固定表达为“解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。”。
                """);
        template.setSchemaJson(DEFAULT_SCHEMA_HINT);
        return template;
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

    private record ParsedDreamResult(JsonNode result, boolean valid, List<String> errors) {

        private static ParsedDreamResult valid(JsonNode result) {
            return new ParsedDreamResult(result, true, List.of());
        }

        private static ParsedDreamResult invalid(List<String> errors) {
            return new ParsedDreamResult(null, false, errors);
        }
    }

    private record AiOutcome(
            JsonNode result,
            String provider,
            String model,
            String status,
            int tokenIn,
            int tokenOut
    ) {
    }
}
