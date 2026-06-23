package com.dream.service.ai;

import com.dream.domain.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptRenderer {

    public RenderedPrompt render(PromptTemplate template, String dreamText, String school) {
        String requestedSchool = StringUtils.hasText(school) ? school : "全部，必须包含传统文化与心理学";
        return new RenderedPrompt(
                template.getSystemPrompt(),
                template.getUserPromptTemplate()
                        .replace("{{dreamText}}", dreamText)
                        .replace("{{school}}", requestedSchool),
                template.getVersion(),
                template.getSchemaJson());
    }

    public record RenderedPrompt(
            String systemPrompt,
            String userPrompt,
            String version,
            String schemaJson
    ) {
    }
}
