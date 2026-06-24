package com.dream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dream.common.auth.UserPrincipal;
import com.dream.controller.dto.DreamInterpretRequest;
import com.dream.controller.dto.DreamInterpretResponse;
import com.dream.domain.DreamRecord;
import com.dream.domain.DreamResult;
import com.dream.mapper.DreamRecordMapper;
import com.dream.mapper.DreamResultMapper;
import com.dream.mapper.FavoriteMapper;
import com.dream.service.ai.AiConfigurationCache;
import com.dream.service.ai.AiProviderRouter;
import com.dream.service.ai.DreamResultSchemaValidator;
import com.dream.service.ai.PromptRenderer;
import com.dream.service.ai.SafeDreamResultFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class DreamInterpretationServiceTest {

    @Mock
    private DreamRecordMapper dreamRecordMapper;

    @Mock
    private DreamResultMapper dreamResultMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private AiConfigurationCache aiConfigurationCache;

    @Mock
    private PromptRenderer promptRenderer;

    @Mock
    private AiProviderRouter aiProviderRouter;

    @Mock
    private DreamResultSchemaValidator schemaValidator;

    @Mock
    private SafeDreamResultFactory safeDreamResultFactory;

    @Mock
    private ContentSafetyService contentSafetyService;

    @Mock
    private DreamQuotaService dreamQuotaService;

    @Mock
    private DreamResultCacheService dreamResultCacheService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DreamInterpretationService service;

    @BeforeEach
    void setUp() {
        DreamResultSanitizer dreamResultSanitizer = new DreamResultSanitizer(objectMapper);
        service = new DreamInterpretationService(
                dreamRecordMapper,
                dreamResultMapper,
                favoriteMapper,
                aiConfigurationCache,
                promptRenderer,
                aiProviderRouter,
                schemaValidator,
                safeDreamResultFactory,
                contentSafetyService,
                dreamQuotaService,
                dreamResultCacheService,
                dreamResultSanitizer,
                transactionTemplate,
                objectMapper);
    }

    @Test
    void cacheHitReusesStoredInterpretationWithoutPersistingDuplicateRows() throws Exception {
        JsonNode cached = cachedResult();
        DreamRecord record = new DreamRecord();
        record.setId(21L);
        record.setDreamText("梦见月亮");
        record.setTags("安静");
        DreamResult result = new DreamResult();
        result.setId(31L);
        result.setDreamRecordId(21L);
        result.setSchool("心理学");
        when(dreamResultCacheService.get(eq("梦见月亮"), eq(List.of("安静")), eq("心理学")))
                .thenReturn(Optional.of(cached));
        when(dreamRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(dreamResultMapper.selectOne(any())).thenReturn(result);

        DreamInterpretResponse response = service.interpret(
                new UserPrincipal(7L, "openid-1", "user"),
                new DreamInterpretRequest(" 梦见月亮 ", List.of("安静"), "心理学"));

        assertThat(response.dreamRecordId()).isEqualTo(21L);
        assertThat(response.dreamResultId()).isEqualTo(31L);
        assertThat(response.result().path("fortune").path("disclaimer").asText())
                .isEqualTo(DreamResultSanitizer.DISCLAIMER);
        verify(dreamRecordMapper, never()).insert(any(DreamRecord.class));
        verify(dreamResultMapper, never()).insert(any(DreamResult.class));
        verifyNoInteractions(dreamQuotaService, aiProviderRouter);
    }

    @Test
    void cacheHitWithoutStoredInterpretationReturnsNonPersistentResult() throws Exception {
        when(dreamResultCacheService.get(eq("梦见月亮"), eq(List.of()), eq("")))
                .thenReturn(Optional.of(cachedResult()));
        when(dreamRecordMapper.selectList(any())).thenReturn(List.of());

        DreamInterpretResponse response = service.interpret(
                new UserPrincipal(7L, "openid-1", "user"),
                new DreamInterpretRequest("梦见月亮", List.of(), ""));

        assertThat(response.dreamRecordId()).isNull();
        assertThat(response.dreamResultId()).isNull();
        assertThat(response.result().path("summary").asText()).isEqualTo("这可能提示你正在整理情绪。");
        verify(dreamRecordMapper, never()).insert(any(DreamRecord.class));
        verify(dreamResultMapper, never()).insert(any(DreamResult.class));
        verifyNoInteractions(dreamQuotaService, aiProviderRouter);
    }

    private JsonNode cachedResult() throws Exception {
        return objectMapper.readTree("""
                {
                  "title": "关于梦见月亮的解读",
                  "summary": "这可能提示你正在整理情绪。",
                  "overallTone": "neutral",
                  "symbols": [{"keyword": "月亮", "meaning": "情绪与直觉"}],
                  "emotion": {"primary": "平静", "description": "梦境氛围较稳定。"},
                  "interpretations": [
                    {"school": "传统文化", "content": "月亮可视为静观内心的象征。"},
                    {"school": "心理学", "content": "可能对应近期情绪的自我整理。"}
                  ],
                  "fortune": {"tendency": "宜静观"},
                  "suggestions": ["记录梦醒后的感受"]
                }
                """);
    }
}
