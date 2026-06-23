package com.dream.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class DreamResultSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DreamResultSchemaValidator validator = new DreamResultSchemaValidator();

    @Test
    void acceptsSchemaCompliantDualPerspectiveResult() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {
                  "title": "关于梦见蛇的解读",
                  "summary": "这可能提示你正在面对变化与压力。",
                  "overallTone": "mixed",
                  "symbols": [
                    {"keyword": "蛇", "meaning": "变化、压力或潜在能量", "category": "动物"}
                  ],
                  "emotion": {"primary": "紧张", "description": "梦中可能带有警觉和不安。"},
                  "interpretations": [
                    {"school": "传统文化", "content": "传统象征中可理解为变化前的提醒。"},
                    {"school": "心理学", "content": "心理学上可能对应近期压力的投射。"}
                  ],
                  "fortune": {
                    "tendency": "宜静观",
                    "disclaimer": "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"
                  },
                  "suggestions": ["记录情绪", "观察压力来源"],
                  "tags": ["反复出现"]
                }
                """);

        DreamValidationResult result = validator.validate(node);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsResultWithoutRequiredPsychologyPerspective() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {
                  "title": "关于梦境的解读",
                  "summary": "这可能提示近期心境变化。",
                  "overallTone": "neutral",
                  "symbols": [{"keyword": "水", "meaning": "情绪流动"}],
                  "emotion": {"primary": "平静", "description": "情绪较稳定。"},
                  "interpretations": [
                    {"school": "传统文化", "content": "可视为顺势而为的提示。"}
                  ],
                  "fortune": {
                    "tendency": "顺势而为",
                    "disclaimer": "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"
                  },
                  "suggestions": ["保持记录"]
                }
                """);

        DreamValidationResult result = validator.validate(node);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("root.interpretations must include 传统文化 and 心理学");
    }

    @Test
    void safeFallbackResultIsSchemaCompliant() {
        SafeDreamResultFactory factory = new SafeDreamResultFactory(objectMapper);

        DreamValidationResult result = validator.validate(factory.create("梦见从高处落下", List.of("压力")));

        assertThat(result.valid()).isTrue();
    }
}
